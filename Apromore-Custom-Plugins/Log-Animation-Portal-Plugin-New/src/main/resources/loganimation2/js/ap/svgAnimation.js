
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
     * @param {Number} actualToLogicalFactor: the animation speed (integers) 0.1x, 0.2x, 1x, 2x, 3x
     */
    constructor(startLogTime, endLogTime, logicalTimelineMax, frameRate, actualToLogicalFactor) {
        this._startLogTime = startLogTime;
        this._endLogTime = endLogTime;
        this._logicalTimelineMax = logicalTimelineMax;
        this._frameRate = frameRate;
        this._actualToLogical = actualToLogicalFactor; // convert from actual timeline to logical timeline
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

    getFrameIndexFromLogicalTime(logicalTime) {
        return (0 + logicalTime*this._frameRate);
    }

    getActualToLogicalFactor() {
        return this._actualToLogical;
    }

    /**
     * Set the conversion factor between actual and logical timeline
     * @param {Number} actualToLogicalFactor
     */
    setActualToLogicalFactor(actualToLogicalFactor) {
        this._actualToLogical = actualToLogicalFactor;
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

/**
 * SVGToken represents a token animated on a model element
 * It is created from reading frames, so it keeps track of the first and last frame indexes
 * and attributes
 */
class SVGToken {
    /**
     *
     * @param {String} elementId
     * @param {Number} firstFrameIndex
     * @param {Array} firstAtts
     * @param {Number} lastFrameIndex
     * @param {Array} lastAtts
     */
    constructor(elementId, firstFrameIndex, firstAtts, lastFrameIndex, lastAtts) {
        this._elementId = elementId;
        this._firstFrameIndex = firstFrameIndex;
        this._lastFrameIndex = undefined;
        this._firstAtts = firstAtts;
        this._lastAtts = lastAtts;
    }

    getElementId() {
        return this._elementId;
    }

    getFirstFrameIndex() {
        return this._firstFrameIndex;
    }

    getLastFrameIndex() {
        return this._lastFrameIndex;
    }

    setLastFrameIndex(lastFrameIndex) {
        this._lastFrameIndex = lastFrameIndex;
    }

    setLastAtts(lastAtts) {
        this._lastAtts = lastAtts;
    }

    getFirstDistance() {
        return this._firstAtts[0];
    }

    getLastDistance() {
        return this._lastAtts[0];
    }
}

/**
 * This class is to convert from a sequence of frames to SVG animation elements
 */
class SVGAnimator {
    /**
     * @param {String} pluginExecutionId
     * @param {Buffer} frameBuffer
     * @param {TimelineController} timeController
     * @param {FormatController} formatController
     * @param {AnimationController} modelController
     * @param {SVGDocument} svgTokenAnimation
     * @param {SVGDocument} svgTimeline
     * @param {SVGDocument} svgProgressBar
     */
    constructor(pluginExecutionId, timeController,
                formatController, modelController,
                svgTokenAnimation,
                svgTimeline,
                svgProgressBar) {
        this._chunkSize = 100;
        this._replenishmentThres = 100;
        this._obsoleteThres = 10;

        this._frameBuffer = new Buffer(new DataRequester(pluginExecutionId), this._chunkSize, this._replenishmentThres, this._obsoleteThres);

        this._timeController = timeController;
        this._formatController = formatController;
        this._modelController = modelController;
        this._svgTokenAnimation = svgTokenAnimation;
        this._svgTimeline = svgTimeline;
        this._svgProgressBar = svgProgressBar;
        this._animationTimeOutId = undefined;
    }

    /**
     * Animation loop to inject SVG elements into the document
     */
    animateLoop() {
        let frames = this._frameBuffer.readNextChunk();
        if (frames) {
            this._animate(frames);
        }
        this._animationTimeOutId = setTimeout(this.animateLoop.bind(this), 0);
    }

    /**
     *
     * @param {Number} animationSpeed
     */
    setAnimationSpeed(animationSpeed) {
        this._clearAnimation();
        let currentLogicalTime = this._svgTokenAnimation.getCurrentTime();
        let currentFrameIndex = this._timeController.getFrameIndexFromLogicalTime(currentLogicalTime);
        this._frameBuffer.setCurrentIndex(currentFrameIndex);

        this._timeController.setActualToLogicalFactor(animationSpeed);
        this.animateLoop();
    }

    pause() {
        this._svgTokenAnimation.pauseAnimations();
        this._svgTimeline.pauseAnimations();
        this._svgProgressBar.pauseAnimations();
    }

    continue() {
        this._svgTokenAnimation.unpauseAnimations();
        this._svgTimeline.unpauseAnimations();
        this._svgProgressBar.unpauseAnimations();
    }

    goto(currentLogicalTime) {
        this._clearAnimation();
        let currentFrameIndex = this._timeController.getFrameIndexFromLogicalTime(currentLogicalTime);
        this._frameBuffer.setCurrentIndex(currentFrameIndex);
        this.animateLoop();
    }

    _clearAnimation() {
        while (this._svgTokenAnimation.lastElementChild) {
            this._svgTokenAnimation.removeChild(this._svgTokenAnimation.lastElementChild);
        }
        if (this._animationTimeOutId) window.clearTimeout(this._animationTimeOutId);
    }

    /**
     * Animate an array of frames
     * @param {Frame[]} frames
     * @private
     */
    _animate(frames) {
        let svgTokens = this._readSVGTokens(frames);
        for (let token of svgTokens) {
            let svgElement = this._createElement(token, this._timeController, this._formatController, this._modelController);
            this._svgTokenAnimation.appendChild(svgElement);
        }
    }

    /**
     * Read a collection of SVGToken from an array of frames
     * @param {Frame[]} frames
     * @returns {() => IterableIterator<any>}
     * @private
     */
    _readSVGTokens(frames) {
        let tokenMap = new Map();
        for (let frame of frames) {
            let frameIndex = frame.index;
            for (let element of frame.elements) {
                let elementId = element[0];
                for (let token of element[elementId]) {
                    let tokenKey = elementId + "," + token[0];
                    if (!tokenMap.has(tokenKey)) {
                        tokenMap.set(tokenKey, new SVGToken(elementId, frameIndex, token[token[0]], frameIndex, token[token[0]]));
                    }
                    else {
                        tokenMap.get(tokenKey).setLastFrameIndex(frameIndex);
                        tokenMap.get(tokenKey).setLastAtts(token[token[0]]);
                    }
                }
            }
        }

        return tokenMap.values;
    }

    //Not used
    // _cleanUp() {
    //     let currentTime = this._svgDoc.getCurentTime();
    //     let removedIndexes = [];
    //     for (let i = this._tokens.length -1; i >= 0 ; i--){
    //         let token = this._tokens[i];
    //         if (token.getEndTimePoint() < currentTime) {
    //             this._svgTokenAnimation.removeChild(token.getVisualElement());
    //             this._tokens.splice(i,1);
    //         }
    //     }
    // }

    /**
     * @param {SVGToken} svgToken
     * @param {TimelineController} timeController
     * @param {FormatController} formatController
     * @returns {SVGGElement}
     */
    _createElement (svgToken, timeController, formatController, modelController) {
        let beginActualTime = timeController.getFrameActualTime(svgToken.getFirstFrameIndex());
        let endActualTime = timeController.getFrameActualTime(svgToken.getLastFrameIndex());
        let begin = beginActualTime;
        let dur = (endActualTime - beginActualTime);
        let path = modelController.getPathElement(svgToken.getElementId());
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
        animateMotion.setAttributeNS(null, 'keyPoints', '0;' + svgToken.getFirstDistance() + ";" +
            svgToken.getLastDistance())
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