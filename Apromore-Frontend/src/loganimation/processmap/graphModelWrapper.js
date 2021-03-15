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
    }

    initialize(elementIndexIDMap) {
        this._createElementCache(elementIndexIDMap);
    }

    /**
     * @param elementMapping: mapping from element index to element id
     * @private
     */
    _createElementCache(elementMapping) {
        let pd = this._pd;
        let elementIndexIDMap = elementMapping[0];
        let skipElementIndexIDMap = elementMapping[1];
        this._elementIndexToElement = {};
        this._elementIndexToPath = {}; // mapping from element index to array of points on the element

        for (let elementIndex in elementIndexIDMap) {
            let elementId = elementIndexIDMap[elementIndex];
            let element = pd.cy().getElementById(elementId);
            this._elementIndexToElement[elementIndex] = element;
            this._elementIndexToPath[elementIndex] = element.isEdge() ? element._private.rscratch.allpts
                                                                        : pd.getNodeCrossPath(elementId);
        }

        for (let elementIndex in skipElementIndexIDMap) {
            let elementId = skipElementIndexIDMap[elementIndex];
            let element = pd.cy().getElementById(elementId);
            this._elementIndexToElement[elementIndex] = element;
            this._elementIndexToPath[elementIndex] = pd.getNodeSkipPath(elementId);
        }
    }

    getBoundingClientRect() {
        return this._pd.getBoundingClientRect();
    }

    getTransformMatrix() {
        return {};
    }

    getPointAtDistance(elementIndex, distance) {
        return this._element(elementIndex).isEdge() ? this._getPointAtDistanceOnEdge(elementIndex, distance)
                                            : this._getPointAtDistanceOnSegments(elementIndex, distance);
    }

    _element(elementIndex) {
        return this._elementIndexToElement[elementIndex];
    }

    _getPointAtDistanceOnEdge(elementIndex, distance) {
        let pts = this._elementIndexToPath[elementIndex];
        let edgeType = this._element(elementIndex)._private.rscratch.edgeType;
        switch (edgeType) {
            case 'bezier':
            case 'multibezier':
                return this._getPointAtDistanceOnBezier(pts, distance);
                break;
            case 'haystack':
            case 'straight':
            case 'segments':
                return this._getPointAtDistanceOnSegments(pts, distance);
                break;
            default:
        }
    }

    _getPointAtDistanceOnNode(elementIndex, distance) {
        let pts = this._elementIndexToPath[elementIndex];
        return this._getPointAtDistanceOnSegments(pts, distance);
    }

    _getPointAtDistanceOnBezier(pts, distance) {
        let totalLength = Math.getTotalLengthBezier(pts);
        let currentPoint = Math.getPointAtLengthBezier(pts, distance);
        return {x: currentPoint.x, y: currentPoint.y};
    }

    _getPointAtDistanceOnSegments(pts, distance) {
        let cy = this._pd.cy();
        if (pts.length >= 2) {
            let totalLengthSegments = Math.getTotalLengthSegments(pts);
            let currentPointOnSegments = Math.getPointAtLengthSegments(pts, distance);
            return {x: currentPointOnSegments.x, y: currentPointOnSegments.y};
        }
    }

    registerListener(listener) {
        this._listeners.push(listener);
    }
}