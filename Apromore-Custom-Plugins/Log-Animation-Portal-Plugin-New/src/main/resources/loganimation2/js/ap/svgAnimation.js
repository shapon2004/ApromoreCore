
/**
 * Manage the frame index and frame time.
 * Frame indexes are sequential integers, e.g. 0, 1, 2...
 * Frame log time is a point in time (from an origin) in seconds, e.g. 0, 0.1, 0.2, 0.5, 1, 2,...
 */
class TimelineController {
    /**
     *
     * @param {Number} startLogTime: the start timestamp in the log
     * @param {Number} endLogTime: the end timestamp in the log
     * @param {Number} logicalTimelineMax: the maximum logical time in seconds
     * @param {Number} frameRate: the frame rate, fps
     * @param {Number} animationSpeed: the animation speed (integers) 0.1x, 0.2x, 1x, 2x, 3x
     */
    constructor(startLogTime, endLogTime, logicalTimelineMax, frameRate, animationSpeed) {
        this._startLogTime = startLogTime;
        this._endLogTime = endLogTime;
        this._logicalTimelineMax = logicalTimelineMax;
        this._frameRate = frameRate;
        this._actualToLogical = animationSpeed; // convert from actual timeline to logical timeline
        if (endLogTime > startLogTime) {
            this._logicalToLog = this._logicalTimelineMax*1000/(endLogTime - startLogTime); //convert from logical timeline to log timeline
        }

    }

    getStartLogTime() {
        return this._startLogTime;
    }

    /**
     * Get the log time of this frame
     * @param {Number} frameIndex
     * @returns {number}: the log timestamp of this frame
     */
    getFrameLogTime(frameIndex) {
        return this._startLogTime + this._logicalToLog*this.getFrameLogicalTime(frameIndex);
    }

    /**
     * Get the logical time of a frame
     * @param {Number} frameIndex
     * @returns {Number} the number of seconds from the start
     */
    getFrameLogicalTime(frameIndex) {
        return frameIndex*(1/this._frameRate*1000);
    }

    /**
     * Get the actual time of a frame
     * @param {Number} frameIndex
     * @returns {number} the number of seconds from the start
     */
    getFrameActualTime(frameIndex) {
        if (this._actualToLogical > 0) {
            return this.getFrameLogicalTime(frameIndex)*(1/this._actualToLogical);
        }
    }
}

/**
 * This object controls the format/styling for animation
 */
class FormatController {
    constructor() {
        this._tokenRaisedLevel = 0; // the gap between the token and the underlying line
        this._tokenColor = undefined;
    }

    getTokenRaisedLevel() {
        return this._tokenRaisedLevel;
    }

    getTokenColor() {
        return this._tokenColor;
    }
}

class SVGElementGenerator {

    /**
     * @param {Number} elementId: the id of the model element
     * @param {FrameToken} startFrameToken
     * @param {FrameToken} endFrameToken
     * @param {TimelineController} timeController
     * @param {FormatController} formatController
     * @returns {SVGGElement}
     */
    createElement (elementId, startFrameToken, endFrameToken, timeController, formatController, modelController) {
        let beginActualTime = timeController.getFrameActualTime(startFrameToken.getFrameIndex());
        let endActualTime = timeController.getFrameActualTime(endFrameToken.getFrameIndex());
        let begin = beginActualTime;
        let dur = (endActualTime - beginActualTime);
        let path = modelController.getPathElement(elementId);
        let raisedLevel = formatController.getTokenRaisedLevel();
        let color = formatController.getTokenColor();

        let svgElement = document.createElementNS(SVG_NS, 'g')
        svgElement.setAttributeNS(null, 'stroke', 'none')

        let animateMotion = document.createElementNS(SVG_NS, 'animateMotion')
        animateMotion.setAttributeNS(null, 'begin', begin)
        animateMotion.setAttributeNS(null, 'dur', dur)
        animateMotion.setAttributeNS(null, 'fill', 'freeze')
        animateMotion.setAttributeNS(null, 'path', path)
        animateMotion.setAttributeNS(null, 'rotate', 'auto')
        animateMotion.setAttributeNS(null, 'calcMode', 'linear')
        animateMotion.setAttributeNS(null, 'keyPoints', '0;' + startFrameToken.getDistance() + ";" +
                                        endFrameToken.getDistance())
        animateMotion.setAttributeNS(null, 'keyTimes', '0;0;1');
        animateMotion.setAttributeNS(null, 'onend', 'parentElement.removeChild(this);');
        svgElement.appendChild(animateMotion)

        let circle = document.createElementNS(SVG_NS, 'circle')
        // Bruce 15/6/2015: add offset as a parameter, add 'rotate' attribute, put markers of different logs on separate lines.
        // let offset = 2;
        // circle.setAttributeNS(null, "cx", offset * Math.sin(this.offsetAngle));
        // circle.setAttributeNS(null, "cy", offset * Math.cos(this.offsetAngle));
        circle.setAttributeNS(null, 'cx', 0)
        circle.setAttributeNS(null, 'cy', raisedLevel)
        circle.setAttributeNS(null, 'r', 5)
        circle.setAttributeNS(null, 'fill', color)
        svgElement.appendChild(circle)

        return svgElement;
    }
}

/**
 * This class is to convert from a sequence of frames to SVG animation elements
 */
class SVGAnimator {
    /**
     *
     * @param {Buffer} playBuffer
     * @param {AnimationModel} animationModel
     * @param {TimelineController} timeController
     * @param {FormatController} formatController
     * @param {AnimationController} modelController
     * @param {SVGDocument} svgDoc
     */
    constructor(playBuffer, animationModel, timeController,
                formatController, modelController,
                svgDoc) {
        this._playBuffer = playBuffer;
        this._tokens = []; // array of SVGToken
        this._animationModel = animationModel;
        this._timeController = timeController;
        this._formatController = formatController;
        this._modelController = modelController;
        this._svgDoc = svgDoc;
    }

    /**
     * Animation loop to inject SVG elements into the document
     */
    animateLoop() {
        //this._cleanUp();
        if (!this._playBuffer.isEmpty()) return;
        let frames = this._playBuffer.readNextChunk();
        let tokenMap = new Map(); //tokenKey => array of FrameToken
        for (let frame of frames) {
            for (let frameToken of frame.getTokens()) {
                if (!tokenMap.has(frameToken.getKey())) {
                    tokenMap.set(frameToken.getKey(), {first:frameToken, last:frameToken});
                }
                else {
                    tokenMap.get(frameToken.getKey()).last = frameToken;
                }
            }
        }

        let svgElementGenerator = new SVGElementGenerator();
        for (let token of tokenMap.values) {
            let svgElement = svgElementGenerator.createElement(token.first.getElementId(),
                                                                token.first.getFrameIndex(),
                                                                token.last.getFrameIndex(),
                                                                this._timeController,
                                                                this._formatController,
                                                                this._modelController);
            this._svgDoc.appendChild(svgElement);
        }

        setTimeout(this.animateLoop.bind(this),0);
    }

    //Not used
    _cleanUp() {
        let currentTime = this._svgDoc.getCurentTime();
        let removedIndexes = [];
        for (let i = this._tokens.length -1; i >= 0 ; i--){
            let token = this._tokens[i];
            if (token.getEndTimePoint() < currentTime) {
                this._svgDoc.removeChild(token.getVisualElement());
                this._tokens.splice(i,1);
            }
        }
    }


}