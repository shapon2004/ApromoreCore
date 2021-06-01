'use strict';

import * as Diffing from './differ';
import * as _ from 'lodash';
import * as $ from 'jquery';
import CustomViewer from './custom-viewer';
import * as sampleDiff from './diff.json';

/**
 * The main app used for BPMN model comparison
 * @param leftContainerId: the div container id for the left-handed BPMN model
 * @param rightContainerId: the div container id for the right-handed BPMN model
 * @param changesContainerId: the div container id for the list of changes box
 * @constructor
 */
export default function BpmnDiffApp(leftContainerId, rightContainerId, changesContainerId) {

    //////////// INITIALIZE /////////////////////////////////////

    let viewers = createViewers(leftContainerId, rightContainerId);

    $('#' + changesContainerId + ' .show-hide-toggle').click(function () {
        $('#' + changesContainerId).toggleClass('collapsed');
    });

    // These calls are only used for index.html for quick testing
    // Must be commented out in server use
    $('.drop-zone').each(function () {
        let node = this,
            element = $(node);

        element.append('<div class="drop-marker" />');

        function removeMarker() {
            $('.drop-zone').removeClass('dropping');
        }

        function handleFileSelect(e) {
            e.stopPropagation();
            e.preventDefault();

            let files = e.dataTransfer.files;
            openFile(files[0], element.attr('target'), openDiagram);

            removeMarker();
        }

        function handleDragOver(e) {
            removeMarker();

            e.stopPropagation();
            e.preventDefault();

            element.addClass('dropping');

            e.dataTransfer.dropEffect = 'copy';
        }

        function handleDragLeave(e) {
            removeMarker();
        }

        node.addEventListener('dragover', handleDragOver, false);
        node.ownerDocument.body.addEventListener('dragover', handleDragLeave, false);
        node.addEventListener('drop', handleFileSelect, false);
    });

    $('.file').on('change', function (e) {
        openFile(e.target.files[0], $(this).attr('target'), openDiagram);
    });



    //////////// FUNCTIONS /////////////////////////////////////

    function loadDiagrams(leftDiagramXML, rightDiagramXML) {
        loadDiagram(leftContainerId, {xml: leftDiagramXML});
        loadDiagram(rightContainerId, {url: rightDiagramXML});
    }

    function createViewer(containerId) {
        let viewer = new CustomViewer({
            container: '#' + containerId,
            height: '100%',
            width: '100%'
        });
        let canvasObj = viewer.get('canvas');
        canvasObj._viewboxChanged = function () {
            canvasObj._eventBus.fire('canvas.viewbox.changed',
                {viewbox: canvasObj.viewbox(false)});
        };
        return viewer;
    }

    function syncViewers(a, b) {
        let changing;

        function update(viewer) {

            return function (e) {
                if (changing) {
                    return;
                }

                changing = true;
                viewer.get('canvas').viewbox(e.viewbox);
                changing = false;
            };
        }

        function syncViewbox(a, b) {
            a.on('canvas.viewbox.changed', update(b));
        }

        syncViewbox(a, b);
        syncViewbox(b, a);
    }

    function createViewers(left, right) {
        let sides = {};
        sides[left] = createViewer(left);
        sides[right] = createViewer(right);
        // sync navigation
        syncViewers(sides[left], sides[right]);
        return sides;
    }

    function getViewer(side) {
        return viewers[side];
    }

    function isLoaded(v) {
        return v.loading !== undefined && !v.loading;
    }

    function allDiagramsLoaded() {
        return _.every(viewers, isLoaded);
    }

    function setLoading(viewer, loading) {
        viewer.loading = loading;
    }

    function clearDiffs(viewer) {
        viewer.get('overlays').remove({type: 'diff'});

        // TODO(nre): expose as external API
        _.forEach(viewer.get('elementRegistry')._elementMap, function (container) {
            let gfx = container.gfx;

            gfx
                .removeClass('diff-added')
                .removeClass('diff-changed')
                .removeClass('diff-removed')
                .removeClass('diff-layout-changed');
        });

    }

    function diagramLoading(side, viewer) {
        setLoading(viewer, true);
        let loaded = _.filter(viewers, isLoaded);
        // clear diffs on loaded
        _.forEach(loaded, function (v) {
            clearDiffs(v);
        });
    }

    function diagramLoaded(err, side, viewer) {
        if (err) {
            console.error('load error', err);
        }

        setLoading(viewer, err);

        if (allDiagramsLoaded()) {
            // sync viewboxes
            let other = getViewer(side == leftContainerId ? rightContainerId : leftContainerId);
            viewer.get('canvas').viewbox(other.get('canvas').viewbox());

            showDiff(getViewer(leftContainerId), getViewer(rightContainerId));
        }
    }

    function loadDiagram(side, diagram) {
        let viewer = getViewer(side);

        function done(err) {
            diagramLoaded(err, side, viewer);
        }

        diagramLoading(side, viewer);

        if (diagram.xml) {
            return viewer.importXML(diagram.xml, done);
        }

        // $.get(diagram.url, function(xml) {
        //   viewer.importXML(xml, done);
        // });
    }


    function showDiff(viewerOld, viewerNew) {
        console.log("viewerOld", viewerOld);
        console.log("viewerNew", viewerNew);
        console.log("definitions", viewerOld._definitions, viewerNew._definitions);
        let result = Diffing.diff(viewerOld._definitions, viewerNew._definitions);

        // var result = sampleDiff;
        console.log("diff result", result);
        $.each(result._removed, function (i, obj) {
            highlight(viewerOld, i, 'diff-removed');
            addMarker(viewerOld, i, 'marker-removed', '&minus;');
        });


        $.each(result._added, function (i, obj) {
            highlight(viewerNew, i, 'diff-added');
            addMarker(viewerNew, i, 'marker-added', '&#43;');
        });


        $.each(result._layoutChanged, function (i, obj) {
            highlight(viewerOld, i, 'diff-layout-changed');
            addMarker(viewerOld, i, 'marker-layout-changed', '&#8680;');

            highlight(viewerNew, i, 'diff-layout-changed');
            addMarker(viewerNew, i, 'marker-layout-changed', '&#8680;');
        });


        $.each(result._changed, function (i, obj) {

            highlight(viewerOld, i, 'diff-changed');
            addMarker(viewerOld, i, 'marker-changed', '&#9998;');

            highlight(viewerNew, i, 'diff-changed');
            addMarker(viewerNew, i, 'marker-changed', '&#9998;');

            let details = '<table ><tr><th>Attribute</th><th>old</th><th>new</th></tr>';
            $.each(obj.attrs, function (attr, changes) {
                details = details + '<tr>' +
                    '<td>' + attr + '</td><td>' + changes.oldValue + '</td>' +
                    '<td>' + changes.newValue + '</td>' +
                    '</tr>';
            });

            details = details + '</table></div>';
            console.log("viewerOld.get('elementRegistry')", viewerOld.get('elementRegistry'));
            viewerOld.get('elementRegistry').getGraphics(i).addEventListener("click", function (event) {
                $('#changeDetailsOld_' + i).toggle();
            });


            let detailsOld = '<div id="changeDetailsOld_' + i + '" class="changeDetails">' + details;

            // attach an overlay to a node
            viewerOld.get('overlays').add(i, 'diff', {
                position: {
                    bottom: -5,
                    left: 0
                },
                html: detailsOld
            });

            $('#changeDetailsOld_' + i).toggle();
            viewerNew.get('elementRegistry').getGraphics(i).addEventListener("click", function (event) {
                $('#changeDetailsNew_' + i).toggle();
            });

            let detailsNew = '<div id="changeDetailsNew_' + i + '" class="changeDetails">' + details;

            // attach an overlay to a node
            viewerNew.get('overlays').add(i, 'diff', {
                position: {
                    bottom: -5,
                    left: 0
                },
                html: detailsNew
            });

            $('#changeDetailsNew_' + i).toggle();
        });

        // create Table Overview of Changes
        showChangesOverview(result, viewerOld, viewerNew);

    }

    function openDiagram(xml, side) {
        loadDiagram(side, {xml: xml});
    }

    function openFile(file, target, done) {
        let reader = new FileReader();

        reader.onload = function (e) {
            let xml = e.target.result;
            done(xml, target);
        };

        reader.readAsText(file);
    }

    function addMarker(viewer, elementId, className, symbol) {
        let overlays = viewer.get('overlays');

        try {
            // attach an overlay to a node
            overlays.add(elementId, 'diff', {
                position: {
                    top: -12,
                    right: 12
                },
                html: '<span class="marker ' + className + '">' + symbol + '</span>'
            });
        } catch (e) {
            window.alert('Error in attaching an overlay to a node. Error message: ' + e.message);
        }
    }

    function highlight(viewer, elementId, marker) {
        viewer.get('canvas').addMarker(elementId, marker);
    }

    function unhighlight(viewer, elementId, marker) {
        viewer.get('canvas').removeMarker(elementId, marker);
    }

    function showChangesOverview(result, viewerOld, viewerNew) {

        $('#' + changesContainerId + ' table').remove();

        let changesTable = $(
            '<table>' +
            '<thead><tr><th>#</th><th>Name</th><th>Type</th><th>Change</th></tr></thead>' +
            '</table>');

        let count = 0;

        function addRow(element, type, label) {
            let html =
                '<tr class="entry">' +
                '<td>' + (count++) + '</td><td>' + (element.name || '') + '</td>' +
                '<td>' + element.$type.replace('bpmn:', '') + '</td>' +
                '<td><span class="status">' + label + '</span></td>' +
                '</tr>';

            let row = $(html).data({
                changed: type,
                element: element.id
            }).addClass(type).appendTo(changesTable);
        }

        $.each(result._removed, function (i, obj) {
            addRow(obj, 'removed', 'Removed');
        });

        $.each(result._added, function (i, obj) {
            addRow(obj, 'added', 'Added');
        });

        $.each(result._changed, function (i, obj) {
            addRow(obj.model, 'changed', 'Changed');
        });

        $.each(result._layoutChanged, function (i, obj) {
            addRow(obj, 'layout-changed', 'Layout Changed');
        });

        changesTable.appendTo('#' + changesContainerId + ' .changes');

        let HIGHLIGHT_CLS = 'highlight';

        $('#' + changesContainerId + ' tr.entry').each(function () {

            let row = $(this);
            let id = row.data('element');
            let changed = row.data('changed');

            row.hover(function () {

                if (changed == 'removed') {
                    highlight(viewerOld, id, HIGHLIGHT_CLS);
                } else if (changed == 'added') {
                    highlight(viewerNew, id, HIGHLIGHT_CLS);
                } else {
                    highlight(viewerOld, id, HIGHLIGHT_CLS);
                    highlight(viewerNew, id, HIGHLIGHT_CLS);
                }
            }, function () {

                if (changed == 'removed') {
                    unhighlight(viewerOld, id, HIGHLIGHT_CLS);
                } else if (changed == 'added') {
                    unhighlight(viewerNew, id, HIGHLIGHT_CLS);
                } else {
                    unhighlight(viewerOld, id, HIGHLIGHT_CLS);
                    unhighlight(viewerNew, id, HIGHLIGHT_CLS);
                }
            });

            row.click(function () {

                let containerWidth = $('.di-container').width();
                let containerHeight = $('.di-container').height();
                let viewer = (changed == 'removed' ? viewerOld : viewerNew);
                let element = viewer.get('elementRegistry').get(id);
                let x, y;

                if (element.waypoints) {
                    x = element.waypoints[0].x;
                    y = element.waypoints[0].y;
                } else {
                    x = element.x + element.width / 2;
                    y = element.y + element.height / 2;
                }

                viewer.get('canvas').viewbox({
                    x: x - (containerWidth / 2),
                    y: y - ((containerHeight / 2) - 100),
                    width: containerWidth,
                    height: containerHeight
                });
            });

        });
    }
}