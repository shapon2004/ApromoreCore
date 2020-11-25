import {ORYX} from "../bpmneditor/apromoreEditor";
import * as utils from "./utils";

/**
 * ProcessMapController encapsulates the process map editor and provides
 * interfaces into the editor needed by the token animation. The token animation
 * shows animation on the editor.
 */
export default class ProcessMapController {
    /**
     * @param {AnimationController} animationController
     * @param {String} uiElementId: id of the div element used for the editor
     * @param {String }processMapXML: BPMN XML of the process map
     * @param {Object} elementMapping: map from element index to element id
     */
    constructor(animationController, uiElementId, processMapXML, elementMapping) {
        this._animationController = animationController;
        const BPMN_NS = "http://b3mn.org/stencilset/bpmn2.0#";
        this._editor = this._init(uiElementId, processMapXML, BPMN_NS, BPMN_NS);
        this._svgMain = this._editor.getCanvas().getSVGContainer();
        this._svgViewport = this._editor.getCanvas().getSVGViewport();
        this._initIndexToElementMapping(elementMapping);
        this._registerEvents();
    }

    /**
     * @returns {DOMRect}
     */
    getBoundingClientRect() {
        return this._svgMain.getBoundingClientRect();
    }

    /**
     * @returns {SVGMatrix}
     */
    getTransformMatrix() {
        return this._svgViewport.transform.baseVal.consolidate().matrix;
    }

    /**
     * @param {Number} elementIndex: element index
     * @returns {SVGElement} SVG element
     */
    getPathElement(elementIndex) {
        return this._indexToElement[elementIndex];
    }

    setPosition(x, y) {

    }

    _registerEvents() {
        let isPlayingBeforeChanging = false;
        this._editor.getCanvas().addEventBusListener("canvas.viewbox.changing", function(event) {
            let modelBox = this._svgMain.getBoundingClientRect();
            let modelMatrix = this._svgViewport.transform.baseVal.consolidate().matrix;
            this._animationController.getTokenAnimation().setPosition(modelBox.x, modelBox.y, modelBox.width, modelBox.height, modelMatrix);
            if (this._animationController.isPlaying()) {
                this._animationController.pause();
                isPlayingBeforeChanging = true;
            }
        });

        this._editor.getCanvas().addEventBusListener("canvas.viewbox.changed", function(event) {
            let modelBox = this._svgMain.getBoundingClientRect();
            let modelMatrix = this._svgViewport.transform.baseVal.consolidate().matrix;
            this._animationController.getTokenAnimation().setPosition(modelBox.x, modelBox.y, modelBox.width, modelBox.height, modelMatrix);
            if (isPlayingBeforeChanging) {
                this._animationController.unPause();
                isPlayingBeforeChanging = false;
            }
        });
    }


    /**
     * @param {Object} elementMapping: map from element index to element id
     * @return {Object} mapping from element index to SVG Path Element having the element id
     * @private
     */
    _initIndexToElementMapping(elementMapping) {
        this._indexToElement = {};
        let elementIndexIDMap = elementMapping[0];
        let skipElementIndexIDMap = elementMapping[1];
        for (let elementIndex in elementIndexIDMap) {
            let elementId = elementIndexIDMap[elementIndex];
            let pathElement = $j('[data-element-id=' + elementId + ']').find('g').find('path').get(0);
            if (!pathElement) { // create cross and skip paths as they are not present
                this._createNodePathElements(elementId);
                pathElement = $j('[data-element-id=' + elementId + ']').find('g').find('path').get(0);
            }
            this._indexToElement[elementIndex] = pathElement;
        }

        for (let elementIndex in skipElementIndexIDMap) {
            let elementId = skipElementIndexIDMap[elementIndex];
            let pathElement = $j('[data-element-id=' + elementId + ']').find('g').find('path').get(1);
            this._indexToElement[elementIndex] = pathElement;
        }
    }

    /**
     * Create two paths: one crossing and one skipping a node
     * @param {String} nodeId
     */
    _createNodePathElements (nodeId) {
        let incomingEndPoint = $j(
            '[data-element-id=' + this.canvas.getIncomingFlowId(nodeId) +
            ']',
        )
        let incomingPathE = incomingEndPoint.find('g').find('path').get(0)
        incomingEndPoint = incomingPathE.getPointAtLength(
            incomingPathE.getTotalLength(),
        )
        let crossPath, skipPath;
        let arrayAbove, arrayBelow;

        let outgoingStartPoint = $j(
            '[data-element-id=' + this.canvas.getOutgoingFlowId(nodeId) +
            ']',
        )
        let outgoingPathE = outgoingStartPoint.find('g').find('path').get(0)
        outgoingStartPoint = outgoingPathE.getPointAtLength(0)

        let startPoint = incomingEndPoint
        let endPoint = outgoingStartPoint

        let nodeTransformE = $j('[data-element-id=' + nodeId + ']').get(0) //this <g> element contains the translate function
        let nodeRectE = $j('[data-element-id=' + nodeId + ']').
        find('g').
        find('rect').
        get(0)
        let taskRectPoints = utils.getViewportPoints(
            this.svgMain,
            nodeRectE,
            nodeTransformE,
        )

        crossPath =
            'm' + startPoint.x + ',' + startPoint.y +
            ' L' + taskRectPoints.cc.x + ',' + taskRectPoints.cc.y +
            ' L' + endPoint.x + ',' + endPoint.y

        // Both points are on a same edge
        if (
            (Math.abs(startPoint.x - endPoint.x) < 10 &&
                Math.abs(endPoint.x - taskRectPoints.se.x) < 10) ||
            (Math.abs(startPoint.x - endPoint.x) < 10 &&
                Math.abs(endPoint.x - taskRectPoints.sw.x) < 10) ||
            (Math.abs(startPoint.y - endPoint.y) < 10 &&
                Math.abs(endPoint.y - taskRectPoints.nw.y) < 10) ||
            (Math.abs(startPoint.y - endPoint.y) < 10 &&
                Math.abs(endPoint.y - taskRectPoints.sw.y) < 10)
        ) {
            skipPath =
                'm' + startPoint.x + ',' + startPoint.y +
                ' L' + endPoint.x + ',' + endPoint.y
        } else {
            arrayAbove = new Array()
            arrayBelow = new Array()

            if (
                taskRectPoints.se.y <
                utils.getStraighLineFunctionValue(startPoint, endPoint, taskRectPoints.se)
            ) {
                arrayAbove.push(taskRectPoints.se)
            } else {
                arrayBelow.push(taskRectPoints.se)
            }

            if (
                taskRectPoints.sw.y <
                utils.getStraighLineFunctionValue(startPoint, endPoint, taskRectPoints.sw)
            ) {
                arrayAbove.push(taskRectPoints.sw)
            } else {
                arrayBelow.push(taskRectPoints.sw)
            }

            if (
                taskRectPoints.ne.y <
                utils.getStraighLineFunctionValue(startPoint, endPoint, taskRectPoints.ne)
            ) {
                arrayAbove.push(taskRectPoints.ne)
            } else {
                arrayBelow.push(taskRectPoints.ne)
            }

            if (
                taskRectPoints.nw.y <
                utils.getStraighLineFunctionValue(startPoint, endPoint, taskRectPoints.nw)
            ) {
                arrayAbove.push(taskRectPoints.nw)
            } else {
                arrayBelow.push(taskRectPoints.nw)
            }

            if (arrayAbove.length == 1) {
                skipPath =
                    'm' + startPoint.x + ',' + startPoint.y + ' ' +
                    'L' + arrayAbove[0].x + ',' + arrayAbove[0].y + ' ' +
                    'L' + endPoint.x + ',' + endPoint.y
            } else if (arrayBelow.length == 1) {
                skipPath =
                    'm' + startPoint.x + ',' + startPoint.y + ' ' +
                    'L' + arrayBelow[0].x + ',' + arrayBelow[0].y + ' ' +
                    'L' + endPoint.x + ',' + endPoint.y
            } else {
                if (Math.abs(startPoint.x - taskRectPoints.sw.x) < 10) {
                    skipPath =
                        'm' + startPoint.x + ',' + startPoint.y + ' ' +
                        'L' + taskRectPoints.sw.x + ',' + taskRectPoints.sw.y + ' ' +
                        'L' + taskRectPoints.se.x + ',' + taskRectPoints.se.y + ' ' +
                        'L' + endPoint.x + ',' + endPoint.y
                } else if (Math.abs(startPoint.x - taskRectPoints.se.x) < 10) {
                    skipPath =
                        'm' + startPoint.x + ',' + startPoint.y + ' ' +
                        'L' + taskRectPoints.se.x + ',' + taskRectPoints.se.y + ' ' +
                        'L' + taskRectPoints.sw.x + ',' + taskRectPoints.sw.y + ' ' +
                        'L' + endPoint.x + ',' + endPoint.y
                } else if (Math.abs(startPoint.y - taskRectPoints.sw.y) < 10) {
                    skipPath =
                        'm' + startPoint.x + ',' + startPoint.y + ' ' +
                        'L' + taskRectPoints.sw.x + ',' + taskRectPoints.sw.y + ' ' +
                        'L' + taskRectPoints.nw.x + ',' + taskRectPoints.nw.y + ' ' +
                        'L' + endPoint.x + ',' + endPoint.y
                } else if (Math.abs(startPoint.y - taskRectPoints.nw.y) < 10) {
                    skipPath =
                        'm' + startPoint.x + ',' + startPoint.y + ' ' +
                        'L' + taskRectPoints.nw.x + ',' + taskRectPoints.nw.y + ' ' +
                        'L' + taskRectPoints.sw.x + ',' + taskRectPoints.sw.y + ' ' +
                        'L' + endPoint.x + ',' + endPoint.y
                }
            }
        }

        let crossPathE = document.createElementNS(SVG_NS, 'path');
        crossPathE.setAttributeNS(null, 'd', crossPath);
        crossPathE.setAttributeNS(null, 'fill', 'transparent');
        crossPathE.setAttributeNS(null, 'stroke', 'none');

        let skipPathE = document.createElementNS(SVG_NS, 'path');
        skipPathE.setAttributeNS(null, 'd', skipPath);
        skipPathE.setAttributeNS(null, 'fill', 'transparent');
        skipPathE.setAttributeNS(null, 'stroke', 'none');

        let nodeGroupE = $j('[data-element-id=' + nodeId + ']').find('g').get(0);
        nodeGroupE.appendChild(crossPathE);
        nodeGroupE.appendChild(skipPathE);
    }

    /**
     *
     * @param {String} editorParentId: id of the div element hosting the editor
     * @param {String} xml: XML content of the BPMN map/model
     * @param {String} url:
     * @param namespace
     * @returns {*}
     */
    _init(editorParentId, xml, url, namespace) {
        return new ORYX.Editor({
            xml,
            model: {
                id: editorParentId,
                showPlugins: false,
                stencilset: {
                    url,
                    namespace
                }
            },
            fullscreen: true // false
        });
    }
}