function getStraighLineFunctionValue(p1, p2, pi) {
    let a = (p1.y - p2.y) / (p1.x - p2.x);
    let b = p1.y - (p1.x * (p1.y - p2.y)) / (p1.x - p2.x);
    return a * pi.x + b;
}

/**
 *
 * @param {x1,x2,y1,y2,w,h} boundingBox
 * @returns {{cc: {}, se: {}, sw: {}, ne: {}, nw: {}}}
 */
function getBoxPoints(boundingBox) {
    let corners = {nw:{}, ne:{}, sw:{}, se:{}, cc:{}};
    corners.nw.x = boundingBox.x1;
    corners.nw.y = boundingBox.y1;
    corners.ne.x = boundingBox.x1 + boundingBox.w;
    corners.ne.y = boundingBox.y1;
    corners.se.x += boundingBox.x1;
    corners.se.y += boundingBox.y1 + boundingBox.h;
    corners.sw.x = boundingBox.x2;
    corners.sw.y = boundingBox.y2;
    corners.cc.x += boundingBox.x1 + boundingBox.w / 2;
    corners.cc.y += boundingBox.y1 + boundingBox.height / 2;
    return corners;
}

/**
 *
 * @param {x,y} startPoint: source point of the node
 * @param {x,y} endPoint: end point of the node
 * @param {Object} taskRectPoints: points on the bounding box of the node
 * @return {Array} array of points.
 */
function getNodeCrossPath(startPoint, endPoint) {
    let crossPath = [];
    crossPath.push({startPoint, endPoint});
    return crossPath;
}

/**
 *
 * @param {x,y} startPoint: source point of the node
 * @param {x,y} endPoint: end point of the node
 * @param {Object} taskRectPoints: points on the bounding box of the node
 */
function getNodeSkipPath(startPoint, endPoint, taskRectPoints) {
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
        skipPath.push(startPoint, endPoint);
    } else {
        arrayAbove = []
        arrayBelow = []

        if (
            taskRectPoints.se.y <
            getStraighLineFunctionValue(startPoint, endPoint, taskRectPoints.se)
        ) {
            arrayAbove.push(taskRectPoints.se)
        } else {
            arrayBelow.push(taskRectPoints.se)
        }

        if (
            taskRectPoints.sw.y <
            getStraighLineFunctionValue(startPoint, endPoint, taskRectPoints.sw)
        ) {
            arrayAbove.push(taskRectPoints.sw)
        } else {
            arrayBelow.push(taskRectPoints.sw)
        }

        if (
            taskRectPoints.ne.y <
            getStraighLineFunctionValue(startPoint, endPoint, taskRectPoints.ne)
        ) {
            arrayAbove.push(taskRectPoints.ne)
        } else {
            arrayBelow.push(taskRectPoints.ne)
        }

        if (
            taskRectPoints.nw.y <
            getStraighLineFunctionValue(startPoint, endPoint, taskRectPoints.nw)
        ) {
            arrayAbove.push(taskRectPoints.nw)
        } else {
            arrayBelow.push(taskRectPoints.nw)
        }

        if (arrayAbove.length === 1) {
            skipPath =
                'm' + startPoint.x + ',' + startPoint.y + ' ' +
                'L' + arrayAbove[0].x + ',' + arrayAbove[0].y + ' ' +
                'L' + endPoint.x + ',' + endPoint.y;
            skipPath.push({});
        } else if (arrayBelow.length === 1) {
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


}