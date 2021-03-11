/*-
 * #%L
 * This file is part of "Apromore Core".
 * %%
 * Copyright (C) 2018 - 2020 The University of Melbourne.
 * %%
 * Copyright (C) 2020, Apromore Pty Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */
const LAYOUT_MANUAL_BEZIER = 0;
const LAYOUT_DAGRE_LR = 1;
const LAYOUT_DAGRE_TB = 2;
const LAYOUT_BREADTH_FIRST = 3;
const NAME_PROP = 'oriname';
const MAX_AUTOFIT_ZOOM = 1;

let PDp = {};

let SIGNATURE = '/themes/ap/common/img/brand/logo-colour.svg';

const layouters = {
    [LAYOUT_MANUAL_BEZIER]: function () {
        cy.style().selector('edge').style({
            'curve-style': function (ele) {
                return ele.data('edge-style');
            },
            'edge-distances': 'intersection',
            'control-point-distances': function (ele) {
                if (ele.data('edge-style') == 'unbundled-bezier') {
                    return ele.data('point-distances');
                } else {
                    return '0';
                }
            },
            'control-point-weights': function (ele) {
                if (ele.data('edge-style') == 'unbundled-bezier') {
                    return ele.data('point-weights');
                } else {
                    return '0.5';
                }
            },
        }).update();

        cy.elements().layout({
            name: 'preset',
        }).run();
    },
    [LAYOUT_DAGRE_LR]: function () {
        cy.elements().layout({
            avoidOverlap: !0,
            edgeSep: 50,
            name: 'dagre',
            nodeSep: 110,
            randomize: false,
            rankDir: 'LR',
            ranker: 'network-simplex',
        }).run();
    },
    [LAYOUT_DAGRE_TB]: function (randomize) {
        cy.style().selector('edge').style({
            'text-background-opacity': 1,
            'text-margin-y': 0,
        }).update();

        cy.elements().layout({
            avoidOverlap: !0,
            edgeSep: 50,
            name: 'dagre',
            nodeSep: 110,
            randomize,
            rankDir: 'TB',
            ranker: 'tight-tree',
        }).run();
    },
    [LAYOUT_BREADTH_FIRST]: function () {
        cy.style().selector('edge').style({
            'text-background-opacity': 1,
            'text-margin-y': 0,
        }).update();

        cy.elements().layout({
            avoidOverlap: true,
            directed: !0,
            name: 'breadthfirst',
            spacingFactor: 1,
        }).run();
    },
};

let history = new Undoo();
history.save({
    event: 'onClearFilter',
    data: ''
});

let container;
let sourceJSON;
let cy = null;
let vizBridgeId = '$vizBridge';
let eventRegistered = false;
let options = {
    maxZoom: 1E50,
    minZoom: 1E-50,
    panningEnabled: true,
    userPanningEnabled: true,
    userZoomingEnabled: true,
    wheelSensitivity: .1,
    zoom: 1,
    zoomingEnabled: true,
};
// Cut text in the server side
// const cutText = (text) => {
//   if (text.length > 60) {
//     return text.substring(0, 60) + '...';
//   }
//   return text;
// }
const calcFontSize = (text) => {
    let len = text.length;
    if (len > 40) {
        return 14;
    } else if (len > 20) {
        return 15;
    } else {
        return 16;
    }
};
let style = [
    {
        selector: 'node',
        style: {
            'background-color': 'data(color)',
            'border-color': 'black',
            // 'border-width': 'data(borderwidth)',
            'border-width': '1px',
            'border-style': 'solid',
            'color': 'data(textcolor)',
            'content': 'data(name)',
            'font-size': function (ele) { // 'data(textsize)',
                if (ele.data('shape') === 'roundrectangle') {
                    let fontSize = calcFontSize(ele.data('oriname'));
                    return fontSize + 'px';
                }
                return ele.data('textsize');
            },
            'height': function (ele) { // 'data(height)',
                let oriHeight = ele.data('height');
                if (oriHeight === '70px') {
                    return '80px';
                }
                return oriHeight;
            },
            'padding': '5px',
            'shape': 'data(shape)',
            'text-border-width': 0,
            'text-max-width': 'data(textwidth)',
            'text-valign': 'center',
            'text-wrap': 'wrap',
            'width': 'data(width)',
        },
    },
    {
        selector: 'edge',
        style: {
            'color': 'data(color)',
            'control-point-step-size': 60,
            'curve-style': 'bezier',
            'edge-text-rotation': 0,
            'font-size': 16,
            'label': 'data(label)',
            'line-color': 'data(color)',
            'line-style': 'data(style)',
            'loop-direction': -41,
            'loop-sweep': 181,
            'opacity': 1,
            'source-arrow-color': 'data(color)',
            'target-arrow-color': 'data(color)',
            'target-arrow-shape': 'triangle',
            'text-background-color': '#ffffff',
            'text-background-opacity': 0,
            'text-background-padding': 5,
            'text-background-shape': 'roundrectangle',
            'text-margin-y': -16,
            'text-wrap': 'wrap',
            'width': 'mapData(strength, 0, 100, 1, 6)',
        },
    },
    {
        selector: 'node:selected',
        style: {
            'border-width': '2px',
            'overlay-color': '#f96100',
            'overlay-padding': '18px',
            'overlay-opacity': 0.2
        },
    },
    {
        selector: ':selected',
        style: {
            'border-color': '#f96100',
            'line-color': '#f96100',
            'line-style': 'solid',
            'target-arrow-color': '#f96100',
        },
    }
];
let elements = {
    nodes: [],
    edges: [],
};

let currentLayout = 0;
let isCtrlPressed = false;
let isAltPressed = false;
let isShiftPressed = false;

let currentNodeTooltip;
let currentZoomLevel = 1;
let currentPanPosition;
let isTraceMode = false; // source trace or source full log

PDp.init = function() {

    SIGNATURE = `/themes/${Ap.theme}/common/img/brand/logo-colour.svg`;
    container = document.getElementById('ap-pd-process-model');
    cy = cytoscape(Object.assign(options, {
        container,
        style,
        elements,
    }));

    cy.on('cxttap', 'edge', function (source) {
        if (!isTraceMode) {
            this.removeEdge(source);
        }
    });
    cy.on('cxttap', 'node', function (source) {
        if (!isTraceMode) {
            this.removeNode(source);
        }
    });
    cy.on('pan', function (event) {
        if (!isTraceMode) {
            currentPanPosition = cy.pan();
        }
    });
    cy.on('zoom', function (event) {
        if (!isTraceMode) {
            currentZoomLevel = cy.zoom();
        }
    });

    // cy.on('beforeUndo', function() {
    // });

    cy.on('mouseover', 'node', function (event) {
        let node = event.target;
        if (node.data(NAME_PROP)) {
            currentNodeTooltip = this.makeTippy(node, node.data(NAME_PROP));
            currentNodeTooltip.show();
        } else {
            currentNodeTooltip = undefined;
        }
    });

    cy.on('mouseout', 'node', function (event) {
        if (currentNodeTooltip) currentNodeTooltip.hide();
    });

    if (!eventRegistered) {
        eventRegistered = true;
        $(document).keydown(function (evt) {
            if (evt.shiftKey || 16 === evt.keyCode || 16 === evt.which) {
                isShiftPressed = true;
            }
            if (evt.ctrlKey || 17 === evt.keyCode || 17 === evt.which) {
                isCtrlPressed = true;
            }
            if (evt.altKey || 18 === evt.keyCode || 18 === evt.which) {
                isAltPressed = true;
            }
            if (isCtrlPressed && evt.which === 90) { // "Z" key
                this.undo();
            } else if (isCtrlPressed && evt.which === 89) { // "Y" key
                this.redo();
            }
        });
        $(document).keyup(function () {
            isAltPressed = isCtrlPressed = isShiftPressed = false;
        });
    }
}

PDp.undo = function() {
    if (cy.undoRedo().isUndoStackEmpty()) {
        history.undo((hist) => {
            if (hist) {
                this.zkSendEvent(vizBridgeId, hist.event, hist.payload);
            }
        });
    } else {
        cy.undoRedo().undo();
    }
}

PDp.redo = function() {
    if (cy.undoRedo().isRedoStackEmpty()) {
        let hist = history.redo((hist) => {
            if (hist) {
                this.zkSendEvent(vizBridgeId, hist.event, hist.payload);
            }
        });
    } else {
        cy.undoRedo().redo();
    }
}

PDp.makeTippy = function(node, text) {
    return tippy(node.popperRef(), {
        content: function () {
            let div = document.createElement('div');
            div.innerHTML = text;
            return div;
        },
        trigger: 'manual',
        arrow: true,
        placement: 'bottom',
        hideOnClick: true,
        multiple: false,
        sticky: true,
    });
}

PDp.reset = function() {
    if (cy) {
        cy.destroy();
    }
}

PDp.loadLog = function(json, layoutType, retain) {
    currentLayout = layoutType;

    // Need to set the current zoom/pan level again as the reset/zoom/pan actions
    // will generate zoom and pan events and change them.
    let zoom = currentZoomLevel;
    let pan = currentPanPosition;

    isTraceMode = false;
    this.reset();
    this.init();
    let source = $.parseJSON(json);
    sourceJSON = source;

    cy.add(source);
    this.layout(layoutType);
    this.setupSearch(source, true);

    if (retain) {
        cy.zoom(zoom);
        cy.pan(pan);
        currentZoomLevel = zoom;
        currentPanPosition = pan;
    } else {
        fit(layoutType);
    }

    cy.edgeBendEditing({
        bendShapeSizeFactor: 6,
        enabled: true,
        initBendPointsAutomatically: false,
        undoable: true,
    });
}

PDp.loadTrace = function(json) {
    isTraceMode = true;
    this.reset();
    this.init();
    const source = $.parseJSON(json);
    cy.add(source);
    this.layout(LAYOUT_MANUAL_BEZIER);
    this.setupSearch(source);
    fit(1);
}

PDp.zoomIn = function() {
    cy.zoom(cy.zoom() + 0.1);
    cy.center();
}

PDp.zoomOut = function() {
    cy.zoom(cy.zoom() - 0.1);
    cy.center();
}

PDp.fit = function(layoutType) {
    cy.fit();
    if (cy.zoom() > MAX_AUTOFIT_ZOOM) {
        cy.zoom(MAX_AUTOFIT_ZOOM);
        cy.center();
    }
    //moveTop(layoutType);
}

PDp.center = function(layoutType) {
    cy.center();
    //moveTop(layoutType);
}

PDp.resize = function() {
    cy.resize();
    fit();
}

PDp.moveTop = function(layoutType) {
    let currentPos = cy.pan();
    let box = cy.elements().boundingBox({includeNodes: true, includeEdges: true});

    switch (layoutType) {
        case 0:
        case 1:
            if (cy.zoom() > 1.0) {
                cy.pan({x: currentPos.x, y: -box.y1 + 10});
            } else {
                cy.pan({x: currentPos.x, y: -box.y1 * cy.zoom() + 10});
            }
            break;
        case 2:
            cy.center(cy.nodes().filter(function (ele) {
                return ele.data(NAME_PROP) == '|>';
            }));
            cy.pan({x: currentPos.x, y: 0});
            break;
        case 3:
            cy.fit();
            break;
        default:
        // code block
    }
}

PDp.layout = function(layoutType) {
    let layouter = layouters[layoutType];
    if (layouter) {
        layouter(true);
    }
}

PDp.pos = function(source, b) {
    let c = 0,
        e = 0,
        d;
    for (d in source.incomers().sources().outgoers().targets()) c += d.position()[0], e += 1;
    return 0 == e ? 0 : c / e;
}

PDp.removeEdge = function(evt) {
    let evTarget = evt.target;
    let source = evTarget.source().data(NAME_PROP);
    let target = evTarget.target().data(NAME_PROP);
    if (source === '') {
        source = '|>';
    }
    if (target === '') {
        target = '[]';
    }
    let graphEvent;
    let payload = source.concat(' => ', target);
    let compId = vizBridgeId;
    if (isShiftPressed) {
        payload = {type: 'ATTRIBUTE_ARC_DURATION', source, target};
        compId = '$filter';
        graphEvent = 'onInvokeExt';
    } else if (isCtrlPressed) {
        graphEvent = 'onEdgeRetained';
    } else {
        graphEvent = 'onEdgeRemoved';
    }
    this.zkSendEvent(compId, graphEvent, payload);
    history.save({
        compId,
        event: graphEvent,
        payload
    });
}

PDp.removeNode = function(evt) {
    let evTarget = evt.target;
    let graphEvent;
    let data = evTarget.data(NAME_PROP);
    let compId = vizBridgeId;
    let payload = data;
    if (data !== '') {
        if (isShiftPressed) {
            compId = '$filter';
            graphEvent = 'onInvokeExt';
            if (isCtrlPressed) {
                payload = {type: 'CASE_SECTION_ATTRIBUTE_COMBINATION', data};
            } else {
                payload = {type: 'EVENT_ATTRIBUTE_DURATION', data};
            }
        } else if (isCtrlPressed || isAltPressed) {
            if (isCtrlPressed && !isAltPressed) {
                graphEvent = 'onNodeRetainedTrace';
            } else if (!isCtrlPressed && isAltPressed) {
                graphEvent = 'onNodeRemovedEvent';
            } else {
                graphEvent = 'onNodeRetainedEvent';
            }
        } else {
            graphEvent = 'onNodeRemovedTrace';
        }
        zkSendEvent(vizBridgeId, graphEvent, payload);
        history.save({
            compId,
            event: graphEvent,
            payload
        });
    }
}

PDp.zkSendEvent = function(widgetId, event, payload) {
    zAu.send(new zk.Event(zk.Widget.$(widgetId), event, payload));
}

PDp.rediscover = function() {
    this.zkSendEvent(vizBridgeId, 'onNodeFiltered', cy.json());
}

PDp.animate = function() {
    this.zkSendEvent(vizBridgeId, 'onAnimate', cy.json());
}

PDp.exportFitted = function() {
    this.zkSendEvent('$exportFitted', 'onExport', cy.json());
}

PDp.exportUnfitted = function () {
    this.zkSendEvent('$exportUnfitted', 'onExport', cy.json());
}

PDp.loadImage = function(src) {
    return new Promise((resolve, reject) => {
        const img = new Image();
        img.addEventListener("load", () => resolve(img));
        img.addEventListener("error", err => reject(err));
        img.src = src;
    });
};

const SIGN_HEIGHT = 100;
const MARGIN = 100;

PDp.rasterizeForPrint = function() {
    return Promise.all([
        this.loadImage(SIGNATURE),
        this.loadImage('data:image/png;base64,' + cy.png({
            full: true,
            output: 'base64',
            scale: 1.0,
            quality: 1.0,
        }))
    ]).then(function ([sign, graph]) {
        let canvas = document.createElement('canvas');
        let context = canvas.getContext('2d');
        let signHeight = SIGN_HEIGHT;
        let signWidth = signHeight * sign.width / sign.height;
        sign.width = signWidth;
        sign.height = signHeight;
        canvas.width = graph.width + 2 * MARGIN;
        canvas.height = graph.height + signHeight + 2 * MARGIN;
        context.fillStyle = 'white';
        context.fillRect(0, 0, canvas.width, canvas.height);
        context.drawImage(sign, MARGIN, MARGIN, signWidth, signHeight);
        context.drawImage(graph, MARGIN, signHeight + MARGIN);
        return canvas;
    });
}

PDp.exportPDF = function(filename) {
    this.rasterizeForPrint()
        .then(function (canvas) {
            let pdf = new jsPDF2('l', 'px', [canvas.width, canvas.height], false, true);
            this.loadImage(canvas.toDataURL())
                .then(function (raster) {
                    pdf.addImage(raster, 'PNG', 0, 0, canvas.width, canvas.height, NaN, 'FAST');
                    pdf.save(filename + '.pdf', {returnPromise: true});
                });
        });
}

PDp.exportPNG = function(filename) {
    this.rasterizeForPrint()
        .then(function (canvas) {
            let a = document.createElement('a');
            canvas.toBlob(function (blob) {
                a.href = URL.createObjectURL(blob);
                a.download = filename + '.png';
                a.click();
            });
        });
}

PDp.saveAsFile = function(t, f, m) {
    try {
        var b = new Blob([t], {type: m});
        saveAs(b, f);
    } catch (e) {
        window.open("data:" + m + "," + encodeURIComponent(t), '_blank', '');
    }
}

PDp.exportJSON = function(filename) {
    if (!sourceJSON) {
        return;
    }
    filename = filename || $('.ap-pd-log-title').text();
    this.saveAsFile(
        JSON.stringify(sourceJSON, null, 2),
        filename + ".json",
        "application/json;charset=utf-8"
    );
}

PDp.showCaseDetails = function() {
    let {left, top} = $('.ap-pd-logstats').offset();
    left -= 700; // width of caseDetail window
    this.zkSendEvent('$caseDetails', 'onApShow', {top: top + 'px', left: left + 'px'});
}

PDp.showPerspectiveDetails = function() {
    let {left, top} = $('.ap-pd-logstats').offset();
    left -= 700; // width of perspectiveDetail window
    this.zkSendEvent('$perspectiveDetails', 'onApShow', {top: top + 'px', left: left + 'px'});
}

PDp.getBoundingClientRect = function() {
    return cy.elements().renderedBoundingBox({includeNodes: true, includeEdges: true});
}

PDp.getElement = function(elementId) {
    return cy.getElementById(elementId);
}

export default PDp;
