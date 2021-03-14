import PD from '../../processdiscoverer';
import {AnimationEvent, AnimationEventType} from "../animationEvents";
import * as Math from '../../utils/math'

/**
 * GraphModelWrapper encapsulates PD and and implementation needed by the token animation.
 * The token animation shows animation on the graph.
 *
 * @author Bruce Nguyen
 */
export default class GraphModelWrapper {
    /**
     * @param {LogAnimation} animation: main animation controller
     * @param {PD} pd: process discoverer
     */
    constructor(animation, pd) {
        this._logAnimation = animation;
        this._listeners = [];
        this._pd = pd;
        this._elementIndexToPath = {}; // mapping from element index to array of points on the element
    }

    initialize(elementIndexIDMap) {

    }

    /**
     * @param elementMapping: mapping from element index to element id
     * @private
     */
    _createElementCache(elementMapping) {
        this._elementIndexToPath = {};
        let elementIndexIDMap = elementMapping[0];
        let skipElementIndexIDMap = elementMapping[1];
        for (let elementIndex in elementIndexIDMap) {
            let elementId = elementIndexIDMap[elementIndex];
            if (this._pd.isEdge(elementId)) {
                this._elementIndexToPath[elementIndex] = this._getNodeCrossPath(elementId);
            }
            else {

            }
            this._elementIndexToPath[elementIndex] = this._getNodeCrossPath(elementId);
            this._indexToSkipPath[elementIndex] = this._getNodeSkipPath(elementId);
        }

        for (let elementIndex in skipElementIndexIDMap) {
            this._indexToElement[elementIndex] = pathElement;
        }
    }

    getBoundingClientRect() {
        return this._pd.getBoundingClientRect();
    }

    getTransformMatrix() {
        return {};
    }

    getPointAtDistance(elementIndex, distance) {
        let elementId = this._indexToElementId[elementIndex];
        if (this._pd.isEdge(elementId)) {
            return this._getPointAtDistanceOnEdge(elementId, distance);
        }
        else {
            return this._getPointAtDistanceOnNode(elementId, distance);
        }
    }

    _getPointAtDistanceOnEdge(elementId, distance) {
        let pts = this._pd.getEdgeControlPoints(elementId);
        let edgeType = this._pd.getEdgeType(elementId);
        switch (edgeType) {
            case 'bezier':
            case 'multibezier':
                let totalLength = Math.getTotalLengthBezier(pts);
                let currentPoint = Math.getPointAtLengthBezier(pts, distance);
                x = currentPoint.x;
                y = currentPoint.y;
                break;
            case 'haystack':
            case 'straight':
            case 'segments':
                let totalLengthSegments = Math.getTotalLengthSegments(pts);
                let currentPointOnSegments = Math.getPointAtLengthSegments(pts, distance);
                x = currentPointOnSegments.x;
                y = currentPointOnSegments.y;
                break;
            default:
        }
        return {x: x, y: y};
    }

    _getPointAtDistanceOnNode(node, distance) {
        let cy = this._pd.cy();
        if (cy.$('#' + n.id()).incomers().length > 0 &&
            cy.$('#' + n.id()).outgoers().length > 0) {
            let startPoint = cy.$('#' + n.id()).incomers()[0].targetEndpoint();
            let endPoint = cy.$('#' + n.id()).outgoers()[0].sourceEndpoint();
            let boundingBox = node.boundingBox();
            let boxPoints = Math.getBoxPoints(boundingBox);
            console.log('startPoint', startPoint);
            console.log('endPoint', endPoint);
            console.log('boxPoints', boxPoints);

            let crossPath = Math.getBoxCrossPath(startPoint, endPoint, boxPoints);
            let totalLengthSegments = Math.getTotalLengthSegments(crossPath);
            let currentPointOnSegments = Math.getPointAtLengthSegments(crossPath, distance);
            let x = currentPointOnSegments.x;
            let y = currentPointOnSegments.y;

            let skipPath = Math.getBoxSkipPath(startPoint, endPoint, boxPoints);
            console.log('skipPath', skipPath);
            totalLengthSegments = Math.getTotalLengthSegments(skipPath);
            console.log('totalLengthSegments', totalLengthSegments);
            currentPointOnSegments = Math.getPointAtLengthSegments(skipPath, distance);
            x = currentPointOnSegments.x;
            y = currentPointOnSegments.y;

            return {x: x, y: y};
        }
    }

    registerListener(listener) {
        this._listeners.push(listener);
    }
}