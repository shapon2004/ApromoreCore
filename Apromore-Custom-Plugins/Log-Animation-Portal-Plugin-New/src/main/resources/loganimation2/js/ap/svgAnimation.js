
/**
 * Manage the frame index and frame time.
 * Frame indexes are sequential integers, e.g. 0, 1, 2...
 * Frame log time is a point in time (from an origin) in seconds, e.g. 0, 0.1, 0.2, 0.5, 1, 2,...
 */
class TimelineController {
    /**
     *
     * @param {AnimationContext} animationContext
     */
    constructor(animationContext) {
        this._animationContext = animationContext;
    }

    getLogTimeFromLogicalTime(logicalTime) {
        return logicalTime*this._animationContext.getLogicalToLogFactor();
    }

    getActualTimeFromLogicalTime(logicalTime) {
        if (this._animationContext.getActualToLogicalFactor() > 0) {
            return logicalTime * (1 / this._animationContext.getActualToLogicalFactor());
        }
    }

    getLogicalTimeFromActualTime(actualTime) {
        return actualTime * this._animationContext.getActualToLogicalFactor();
    }

    /**
     * Get the log time of this frame
     * @param {Number} frameIndex
     * @returns {number}: the log timestamp of this frame
     */
    getFrameLogTime(frameIndex) {
        return this._animationContext.getLogStartTime() + this._animationContext.getLogicalToLogFactor()*this.getFrameLogicalTime(frameIndex);
    }

    /**
     * Get the logical time of a frame
     * @param {Number} frameIndex
     * @returns {Number} the number of seconds from the start
     */
    getFrameLogicalTime(frameIndex) {
        return frameIndex*(1/this._animationContext.getFrameRate()*1000);
    }

    /**
     * Get the actual time of a frame
     * @param {Number} frameIndex
     * @returns {number} the number of seconds from the start
     */
    getFrameActualTime(frameIndex) {
        if (this._animationContext.getActualToLogicalFactor() > 0) {
            return this.getFrameLogicalTime(frameIndex)*(1/this._animationContext.getActualToLogicalFactor());
        }
    }

    getFrameIndexFromLogicalTime(logicalTime) {
        return (logicalTime*this._animationContext.getFrameRate());
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
 * SVGToken represents a token animated on a model element over a sequence of frames
 * It is created from reading frames, it keeps track of the first and last frame indexes and attributes
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

class AnimationContext {
    /**
     *
     * @param {String} pluginExecutionId
     * @param {Number} logStartTime: the start timestamp in the log
     * @param {Number} logEndTime: the end timestamp in the log
     * @param {Number} logicalTimelineMax: the maximum logical time in seconds
     */
    constructor(pluginExecutionId, logStartTime, logEndTime, logicalTimelineMax) {
        this._pluginExecutionId = pluginExecutionId;
        this._logStartTime = logStartTime;
        this._logEndTime = logEndTime;
        this._logicalTimelineMax = logicalTimelineMax;
        this._frameRate = 24;
        this._actualToLogicalFactor = 1;
        if (logEndTime > logStartTime) {
            this._logicalToLogFactor = this._logicalTimelineMax*1000/(logEndTime - logStartTime); //convert from logical timeline to log timeline
        }
    }

    getPluginExecutionId() {
        return this._pluginExecutionId;
    }

    getLogStartTime() {
        return this._logStartTime;
    }

    getLogEndTime() {
        return this._logEndTime;
    }

    getLogicalTimelineMax() {
        return this._logicalTimelineMax;
    }

    getFrameRate() {
        return this._frameRate;
    }

    setFrameRate(frameRate) {
        this._frameRate = frameRate;
    }

    getActualToLogicalFactor() {
        return this._actualToLogicalFactor;
    }

    setActualToLogicalFactor(actualToLogicalFactor) {
        this._actualToLogicalFactor = actualToLogicalFactor;
    }

    getLogicalToLogFactor() {
        return this._logicalToLogFactor;
    }
}

/**
 * This class controls the animation
 */
class SVGAnimator {
    /**
     * @param {AnimationContext} animationContext
     * @param {AnimationController} modelController
     * @param {SVGDocument} svgTokenAnimation
     * @param {SVGDocument} svgTimeline
     * @param {SVGDocument} svgProgressBar
     * @param {SVGDocument} svgViewport
     */
    constructor(animationContext,
                modelController,
                svgTokenAnimation,
                svgTimeline,
                svgProgressBar,
                svgViewport) {
        this._animationContext = animationContext;
        this._frameBuffer = new Buffer(animationContext);
        this._timeController = new TimelineController(animationContext);
        this._formatController = new FormatController();
        this._modelController = modelController;
        this._svgTokenAnimation = svgTokenAnimation;
        this._svgTimeline = svgTimeline;
        this._svgProgressBar = svgProgressBar;
        this._svgViewport = svgViewport;
        this._animationClockId = undefined;
    }

    /**
     * Animation loop to inject SVG elements into the document
     */
    animateLoop() {
        let frames = this._frameBuffer.readNext();
        if (frames && frames.length > 0) {
            this._animate(frames);
            this.unpause();
        }
        else {
            this.pause();
        }
        this._animationClockId = setTimeout(this.animateLoop.bind(this), 1000/this._animationContext.getActualToLogicalFactor());
    }

    /**
     * Set a new speed for the animation
     * The effect of changing speed is that the position of everything on the UI is unchanged but then they will move
     * slower or faster. To create that effect, SVG elements for tokens are all cleared and recreated based on the new
     * speed setting
     * @param {Number} speed
     */
    setSpeed(speed) {
        if (speed && speed !== this._animationContext.getActualToLogicalFactor()) {
            //Wipe out all the current tokens
            this.pause();
            this._clearTokenAnimation();

            //Move the buffer to the current frame index being played at this moment
            let currentActualTime = this._svgTokenAnimation.getCurrentTime();
            let currentLogicalTime = this._timeController.getLogicalTimeFromActualTime(currentActualTime);
            //The request rate for frames is in sync with the animation speed, thus the current frame index can be found from
            //the current actual time used as the current logical time.
            let currentFrameIndex = this._timeController.getFrameIndexFromLogicalTime(currentLogicalTime);
            this._frameBuffer.moveTo(currentFrameIndex);

            //Set the context to the new speed and adjust the buffer to adapt to the new speed
            this._animationContext.setActualToLogicalFactor(speed);
            if (speed > 1) {
                this._frameBuffer.setChunkSize(Buffer.DEFAULT_CHUNK_SIZE + (speed-1)*Buffer.DEFAULT_CHUNK_SIZE);
            }
            //Animate again from the current frame index with the new setting
            this.play();
        }
    }

    play() {
        this.animateLoop();
        this.unpause();
    }

    pause() {
        this._svgTokenAnimation.pauseAnimations();
        this._svgTimeline.pauseAnimations();
        this._svgProgressBar.pauseAnimations();
    }

    unpause() {
        this._svgTokenAnimation.unpauseAnimations();
        this._svgTimeline.unpauseAnimations();
        this._svgProgressBar.unpauseAnimations();
    }

    goto(logicalTime) {
        let logTime = this._timeController.getLogTimeFromLogicalTime(logicalTime);
        if (logTime > this._animationContext.getLogEndTime() || logTime < this._animationContext.getLogStartTime()) {
            return;
        }

        this.pause();
        this._clearTokenAnimation();

        let currentFrameIndex = this._timeController.getFrameIndexFromLogicalTime(logicalTime);
        this._frameBuffer.moveTo(currentFrameIndex);

        this.play();
    }

    fastForward() {
        let currentActualTime = this._svgTokenAnimation.getCurrentTime();
        let currentLogicalTime = this._timeController.getLogicalTimeFromActualTime(currentActualTime);
        this.goto(currentLogicalTime + 5);
    }

    fastBackward() {
        let currentActualTime = this._svgTokenAnimation.getCurrentTime();
        let currentLogicalTime = this._timeController.getLogicalTimeFromActualTime(currentActualTime);
        this.goto(currentLogicalTime - 5);
    }

    _clearTokenAnimation() {
        while (this._svgViewport.lastElementChild) {
            this._svgViewport.removeChild(this._svgViewport.lastElementChild);
        }
        if (this._animationClockId) window.clearTimeout(this._animationClockId);
    }

    /**
     * Animate an array of frames
     * @param {Frame[]} frames
     * @private
     */
    _animate(frames) {
        let svgTokens = this._readSVGTokens(frames);
        for (let token of svgTokens) {
            let svgElement = this._createElement(token);
            this._svgViewport.appendChild(svgElement);
        }
    }

    /**
     * Read a collection of SVGToken from an array of frames
     * Frames are ordered in the increasing value of frame indexes and this is also the timing order of frames
     * Thus, tokens with the same token key (elementId+caseId) are also ordered in consecutive frames
     * @param {[]} frames
     * @returns {IterableIterator<any>}
     * @private
     */
    _readSVGTokens(frames) {
        let tokenMap = new Map();
        for (let frame of frames) {
            let frameIndex = frame.index;
            for (let element of frame.elements) {
                let elementId = Object.keys(element)[0];
                for (let token of element[elementId]) {
                    let caseId = Object.keys(token)[0];
                    let tokenKey = elementId + "," + caseId;
                    if (!tokenMap.has(tokenKey)) {
                        tokenMap.set(tokenKey, new SVGToken(elementId, frameIndex, token[caseId], frameIndex, token[caseId]));
                    }
                    else {
                        tokenMap.get(tokenKey).setLastFrameIndex(frameIndex);
                        tokenMap.get(tokenKey).setLastAtts(token[caseId]);
                    }
                }
            }
        }

        return tokenMap.values;
    }

    _getPathElement(elementId) {
        return this._modelController.getPathElement(elementId);
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
     * @returns {SVGElement}
     */
    _createElement (svgToken) {
        let beginActualTime = this._timeController.getFrameActualTime(svgToken.getFirstFrameIndex());
        let endActualTime = this._timeController.getFrameActualTime(svgToken.getLastFrameIndex());
        let begin = beginActualTime;
        let dur = (endActualTime - beginActualTime);
        let path = this._getPathElement(svgToken.getElementId());
        let raisedLevel = this._formatController.getTokenRaisedLevel();
        let color = this._formatController.getTokenColor();

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