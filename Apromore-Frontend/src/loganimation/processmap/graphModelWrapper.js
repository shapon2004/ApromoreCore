import pd from '../../processdiscoverer';
import {AnimationEvent, AnimationEventType} from "../animationEvents";

/**
 * GraphModelWrapper encapsulates PD and and implementation needed by the token animation.
 * The token animation shows animation on the graph.
 *
 * @author Bruce Nguyen
 */
export default class GraphModelWrapper {
    /**
     * @param {LogAnimation} animation
     */
    constructor(animation) {
        this._logAnimation = animation;
        this._listeners = [];
        this._pd = pd;
        this._pd.init();
    }

    initialize(elementIndexIDMap) {

    }

    /**
     * @param elementMapping: mapping from element index to element id
     * @private
     */
    _createIndexToElementCache(elementMapping) {
        this._indexToElement = {};
        let elementIndexIDMap = elementMapping[0];
        let skipElementIndexIDMap = elementMapping[1];
        for (let elementIndex in elementIndexIDMap) {
            let elementId = elementIndexIDMap[elementIndex];
            let pathElement = this._pd.getElement(elementId);
            if (!pathElement) { // create cross and skip paths as they are not present
                this._createNodePathElements(elementId);

            }
            this._indexToElement[elementIndex] = pathElement;
        }

        for (let elementIndex in skipElementIndexIDMap) {
            this._indexToElement[elementIndex] = pathElement;
        }
    }

    /**
     * Create two paths: one crossing and one skipping a node
     * The paths are added as children to the node element
     * @param {String} nodeId
     */
    _createNodePathElements (nodeId) {

    }

    getBoundingClientRect() {
        return this._pd.getBoundingClientRect();
    }

    getTransformMatrix() {
        return {};
    }

    getPointAtDistance(elementIndex, distance) {
        let ele = this._indexToElement[elementIndex];
        if (ele.isEdge()) {
            return this._getPointAtDistanceOnEdge(ele, distance);
        }
        else {
            return this._getPointAtDistanceOnNode(ele, distance);
        }
    }

    _getPointAtDistanceOnEdge(e, distance) {
        let rs = e._private.rscratch;
        let pts = rs.allpts;
        let x, y;
        switch (rs.edgeType) {
            case 'bezier':
            case 'multibezier':
                let totalLength = _getTotalLength(e);
                let currentLength = pro * totalLength;
                let currentPoint = _getPointAtLength(e, currentLength);
                x = currentPoint.x;
                y = currentPoint.y;
                break;
            case 'haystack':
            case 'straight':
            case 'segments':
                let totalLengthSegments = this._getTotalLengthSegments(e._private.rscratch.allpts);
                let currentLengthSegments = pro * totalLengthSegments;
                let currentPointOnSegments = this._getPointAtLengthSegments(e._private.rscratch.allpts, currentLengthSegments);
                x = currentPointOnSegments.x;
                y = currentPointOnSegments.y;
                break;
            default:
        }
        return {x: x, y: y};
    }

    _getPointAtDistanceOnNode(element, distance) {
        let that = this;
        if (that.cy.$('#' + n.id()).incomers().length > 0 &&
            that.cy.$('#' + n.id()).outgoers().length > 0) {
            let startPoint = that.cy.$('#' + n.id()).incomers()[0].targetEndpoint();
            let endPoint = that.cy.$('#' + n.id()).outgoers()[0].sourceEndpoint();
            let boundingBox = n.boundingBox();
            let boxPoints = _getBoxPoints(boundingBox);
            console.log('startPoint', startPoint);
            console.log('endPoint', endPoint);
            console.log('boxPoints', boxPoints);

            let crossPath = this._getNodeCrossPath(startPoint, endPoint, boxPoints);
            let totalLengthSegments = this._getTotalLengthSegments(crossPath);
            let currentPointOnSegments = this._getPointAtLengthSegments(crossPath, distance);
            let x = currentPointOnSegments.x;
            let y = currentPointOnSegments.y;

            let skipPath = this._getNodeSkipPath(startPoint, endPoint, boxPoints);
            console.log('skipPath', skipPath);
            totalLengthSegments = this._getTotalLengthSegments(skipPath);
            console.log('totalLengthSegments', totalLengthSegments);
            currentPointOnSegments = this._getPointAtLengthSegments(skipPath, distance);
            x = currentPointOnSegments.x;
            y = currentPointOnSegments.y;

            return {x: x, y: y};
        }
    }

    registerListener(listener) {
        this._listeners.push(listener);
    }

    _getTotalLength (edge) {
        let pts = edge._private.rscratch.allpts;
        let totalLength = 0;
        for (let i = 0; i + 5 < pts.length; i += 4) {
            let x1 = pts[i];
            let y1 = pts[i + 1];
            let x2 = pts[i + 2];
            let y2 = pts[i + 3];
            let x3 = pts[i + 4];
            let y3 = pts[i + 5];
            totalLength += _quadraticBezierLength(x1, y1, x2, y2, x3, y3);
        }
        return totalLength;
    }

    _getPointAtLength (edge, length) {
        let pts = edge._private.rscratch.allpts;
        let totalLength = 0;
        for (let i = 0; i + 5 < pts.length; i += 4) {
            let x1 = pts[i];
            let y1 = pts[i + 1];
            let x2 = pts[i + 2];
            let y2 = pts[i + 3];
            let x3 = pts[i + 4];
            let y3 = pts[i + 5];
            let localLength = _quadraticBezierLength(x1, y1, x2, y2, x3, y3);
            if ((totalLength + localLength) >= length) {
                let localDistance = length - totalLength;
                let px = _qbezierAt(x1, x2, x3, localDistance / localLength);
                let py = _qbezierAt(y1, y2, y3, localDistance / localLength);
                return {x: px, y: py};
            } else {
                totalLength += localLength;
            }
        }
        return {x: pts[pts.length - 2], y: pts[pts.length - 1]};
    }

    _quadraticBezierLength (x1, y1, x2, y2, x3, y3) {
        let a, b, e, c, d, u, a1, e1, c1, d1, u1, v1x, v1y;

        v1x = x2 * 2;
        v1y = y2 * 2;
        d = x1 - v1x + x3;
        d1 = y1 - v1y + y3;
        e = v1x - 2 * x1;
        e1 = v1y - 2 * y1;
        c1 = (a = 4 * (d * d + d1 * d1));
        c1 += (b = 4 * (d * e + d1 * e1));
        c1 += (c = e * e + e1 * e1);
        c1 = 2 * Math.sqrt(c1);
        a1 = 2 * a * (u = Math.sqrt(a));
        u1 = b / u;
        a = 4 * c * a - b * b;
        c = 2 * Math.sqrt(c);
        return (a1 * c1 + u * b * (c1 - c) + a * Math.log((2 * u + u1 + c1) / (u1 + c))) / (4 * a1);
    }

    // from http://en.wikipedia.org/wiki/Bézier_curve#Quadratic_curves
    _qbezierAt (p0, p1, p2, t) {
        return (1 - t) * (1 - t) * p0 + 2 * (1 - t) * t * p1 + t * t * p2;
    }

    /**
     *
     * @param pts: series of points on segments
     * @returns {number}
     */
    _getTotalLengthSegments (pts) {
        //let pts = edge._private.rscratch.allpts;
        let totalLength = 0;
        for (let i = 0; i + 3 < pts.length; i += 2) {
            let x1 = pts[i];
            let y1 = pts[i + 1];
            let x2 = pts[i + 2];
            let y2 = pts[i + 3];
            totalLength += _segmentLength(x1, y1, x2, y2);
        }
        return totalLength;
    }

    /**
     *
     * @param pts: series of points on segments
     * @returns {number}
     */
    _getPointAtLengthSegments (pts, length) {
        //let pts = edge._private.rscratch.allpts;
        let totalLength = 0;
        for (let i = 0; i + 3 < pts.length; i += 2) {
            let x1 = pts[i];
            let y1 = pts[i + 1];
            let x2 = pts[i + 2];
            let y2 = pts[i + 3];
            let localLength = _segmentLength(x1, y1, x2, y2);
            if ((totalLength + localLength) >= length) {
                let localDistance = length - totalLength;
                let px = _segmentsAt(x1, x2, localDistance / localLength);
                let py = _segmentsAt(y1, y2, localDistance / localLength);
                return {x: px, y: py};
            } else {
                totalLength += localLength;
            }
        }
        return {x: pts[pts.length - 2], y: pts[pts.length - 1]};
    };

    _segmentLength (x1, y1, x2, y2) {
        return Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
    }

    _segmentsAt (p0, p1, t) {
        return (1 - t) * p0 + t * p1;
    };

    _getStraighLineFunctionValue(p1, p2, pi) {
        let a = (p1.y - p2.y) / (p1.x - p2.x);
        let b = p1.y - (p1.x * (p1.y - p2.y)) / (p1.x - p2.x);
        return a * pi.x + b;
    }

    /**
     *
     * @param {x1,x2,y1,y2,w,h} boundingBox
     * @returns {{cc: {}, se: {}, sw: {}, ne: {}, nw: {}}}
     */
    _getBoxPoints(boundingBox) {
        let corners = {nw: {}, ne: {}, sw: {}, se: {}, cc: {}};

        corners.nw.x = boundingBox.x1;
        corners.nw.y = boundingBox.y1;

        corners.se.x = boundingBox.x2;
        corners.se.y = boundingBox.y2;

        corners.ne.x = boundingBox.x2;
        corners.ne.y = boundingBox.y1;

        corners.sw.x = boundingBox.x1;
        corners.sw.y = boundingBox.y2;

        corners.cc.x = boundingBox.x1 + boundingBox.w / 2;
        corners.cc.y = boundingBox.y1 + boundingBox.h / 2;
        return corners;
    }

    /**
     *
     * @param {x,y} startPoint: source point of the node
     * @param {x,y} endPoint: end point of the node
     * @param {Object} taskRectPoints: end point of the node
     * @return {Array} array of points.
     */
    _getNodeCrossPath(startPoint, endPoint, taskRectPoints) {
        let crossPath = [];
        crossPath.push(startPoint.x, startPoint.y, taskRectPoints.cc.x, taskRectPoints.cc.y, endPoint.x, endPoint.y);
        return crossPath;
    }

    /**
     *
     * @param {x,y} startPoint: source point of the node
     * @param {x,y} endPoint: end point of the node
     * @param {Object} taskRectPoints: points on the bounding box of the node
     * @return {Array} array of points on the path
     */
    _getNodeSkipPath(startPoint, endPoint, taskRectPoints) {
        let skipPath = [];
        let arrayAbove, arrayBelow;

        // Both points are approximately on the same line
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
            skipPath.push(startPoint.x, startPoint.y, endPoint.x, endPoint.y);
        } else {
            arrayAbove = [];
            arrayBelow = [];

            if (
                taskRectPoints.se.y <
                _getStraighLineFunctionValue(startPoint, endPoint, taskRectPoints.se)
            ) {
                arrayAbove.push(taskRectPoints.se);
            } else {
                arrayBelow.push(taskRectPoints.se);
            }

            if (
                taskRectPoints.sw.y <
                _getStraighLineFunctionValue(startPoint, endPoint, taskRectPoints.sw)
            ) {
                arrayAbove.push(taskRectPoints.sw);
            } else {
                arrayBelow.push(taskRectPoints.sw);
            }

            if (
                taskRectPoints.ne.y <
                _getStraighLineFunctionValue(startPoint, endPoint, taskRectPoints.ne)
            ) {
                arrayAbove.push(taskRectPoints.ne);
            } else {
                arrayBelow.push(taskRectPoints.ne);
            }

            if (
                taskRectPoints.nw.y <
                _getStraighLineFunctionValue(startPoint, endPoint, taskRectPoints.nw)
            ) {
                arrayAbove.push(taskRectPoints.nw);
            } else {
                arrayBelow.push(taskRectPoints.nw);
            }

            if (arrayAbove.length === 1) {
                skipPath.push(startPoint.x, startPoint.y,
                    arrayAbove[0].x, arrayAbove[0].y,
                    endPoint.x, endPoint.y);
            } else if (arrayBelow.length === 1) {
                skipPath.push(startPoint.x, startPoint.y,
                    arrayBelow[0].x, arrayBelow[0].y,
                    endPoint.x, endPoint.y);
            } else {
                if (Math.abs(startPoint.x - taskRectPoints.sw.x) < 10) {
                    skipPath.push(startPoint.x, startPoint.y,
                        taskRectPoints.sw.x, taskRectPoints.sw.y,
                        taskRectPoints.se.x, taskRectPoints.se.y,
                        endPoint.x, endPoint.y);
                } else if (Math.abs(startPoint.x - taskRectPoints.se.x) < 10) {
                    skipPath.push(startPoint.x, startPoint.y,
                        taskRectPoints.se.x, taskRectPoints.se.y,
                        taskRectPoints.sw.x, taskRectPoints.sw.y,
                        endPoint.x, endPoint.y);
                } else if (Math.abs(startPoint.y - taskRectPoints.sw.y) < 10) {
                    skipPath.push(startPoint.x, startPoint.y,
                        taskRectPoints.sw.x, taskRectPoints.sw.y,
                        taskRectPoints.nw.x, taskRectPoints.nw.y,
                        endPoint.x, endPoint.y);
                } else if (Math.abs(startPoint.y - taskRectPoints.nw.y) < 10) {
                    skipPath.push(startPoint.x, startPoint.y,
                        taskRectPoints.nw.x, taskRectPoints.nw.y,
                        taskRectPoints.sw.x, taskRectPoints.sw.y,
                        endPoint.x, endPoint.y);
                }
            }
        }
        return skipPath;
    }
}