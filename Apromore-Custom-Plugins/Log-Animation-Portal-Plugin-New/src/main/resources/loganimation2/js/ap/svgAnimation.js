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

/**
 * logStartTime, logEndTime: the starting and ending timestamps (milliseconds) in the log timeline
 * logicalTimelineMax: the maximum number of seconds on the logical timeline
 * actualToLogicalFactor: one second in the actual timeline equals how many seconds in the logical timeline
 * logicalToLogFactor: one second in the logical timeline equals how many seconds in the log
 */
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
        this._recordingFrameRate = 24;
        this._actualToLogicalFactor = 1;
        if (this._logicalTimelineMax > 0) {
            this._logicalToLogFactor = (logEndTime - logStartTime)/(this._logicalTimelineMax*1000); //convert from logical timeline to log timeline
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

    getRecordingFrameRate() {
        return this._recordingFrameRate;
    }

    setRecordingFrameRate(frameRate) {
        this._recordingFrameRate = frameRate;
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
        console.log('SVGAnimator - being initialized');
        console.log('SVGAnimator - AnimationContext: ' + JSON.stringify(animationContext));

        this._animationContext = animationContext;
        this._frameBuffer = new Buffer(animationContext);

        this._modelController = modelController;
        this._svgTokenAnimation = svgTokenAnimation;
        this._svgTimeline = svgTimeline;
        this._svgProgressBar = svgProgressBar;
        this._svgViewport = svgViewport;

        this._playingFrameRate = animationContext.getRecordingFrameRate();
        this._startingFrameIndexSinceLastFrameRate = 0; //the starting frame index since applying the current playingFrameRate.
        this._startingTimeSinceLastFrameRate = svgTokenAnimation.getCurrentTime(); //the starting time since applying the current playingFrameRate

        this._animationClockId = undefined;
    }

    /**
     * Animation loop to inject SVG elements into the document
     */
    animateLoop() {
        console.log('SVGAnimator - animateLoop');
        let frames = this._frameBuffer.readNext();
        if (frames && frames.length > 0) {
            this._animate(frames);
            this.unpause();
        }
        else {
            this.pause();
        }
        this._animationClockId = setTimeout(this.animateLoop.bind(this), 1000/this._animationContext.getActualToLogicalFactor());
        console.log('SVGAnimator - animateLoop: call to new animateLoop with a timerId=' + this._animationClockId);
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
     * Set a new speed for the animation
     * The effect of changing speed is that the position of everything being shown on the UI is unchanged but they will move
     * slower or faster.
     * @param {Number} speedLevel
     */
    setSpeed(speedLevel) {
        let lastPlayingFrameRate = this._playingFrameRate;
        let currentTime = this._svgTokenAnimation.getCurrentTime();
        let newPlayingFrameRate = speedLevel*this._animationContext.getRecordingFrameRate();

        if (newPlayingFrameRate !== lastPlayingFrameRate) {
            //Wipe out all the current tokens
            this.pause();
            this._clearTokenAnimation();

            // Identify the current frame index the animation has reached to at this point
            let passingTime = currentTime - this._startingFrameIndexSinceLastFrameRate;
            let currentFrameIndex = this._startingFrameIndexSinceLastFrameRate + passingTime*lastPlayingFrameRate;
            this._startingFrameIndexSinceLastFrameRate = currentFrameIndex;
            this._startingTimeSinceLastFrameRate = currentTime;
            this._playingFrameRate = newPlayingFrameRate;

            // Move the buffer to the current frame index and prepare it with subsequent frames
            this._frameBuffer.moveTo(currentFrameIndex);
            //if (speedLevel > 1) this._frameBuffer.setChunkSize(Buffer.DEFAULT_CHUNK_SIZE + (speedLevel-1)*Buffer.DEFAULT_CHUNK_SIZE);

            // Set the new speed in the current context to be used by all measurements
            this._animationContext.setActualToLogicalFactor(speedLevel);

            // Animate frames in the buffer starting from the current frame index and
            // using the new speed setting
            this.play();
        }
    }

    _getCurrentTime() {
        this._svgTokenAnimation.getCurrentTime();
    }

    /**
     * Use SVG engine to animate tokens which have been converted from frames.
     * The SVG engine uses 'begin' (time since the start) and 'dur' (duration) to animate tokens
     * Setting 'begin' and 'dur" is similar to setting a new frame rate for the token
     *
     * @param {SVGToken} svgToken
     * @returns {SVGElement}
     */
    _createElement (svgToken) {
        let begin = this._getCurrentTime();
        let dur = (svgToken.setLastFrameIndex() - svgToken.getFirstFrameIndex() + 1)/this._playingFrameRate;
        let path = this._getPathElement(svgToken.getElementId());

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
        circle.setAttributeNS(null, 'cy', 0)
        circle.setAttributeNS(null, 'r', 5)
        circle.setAttributeNS(null, 'fill', 'red')
        svgElement.appendChild(circle)

        return svgElement;
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
        if (this._animationClockId) window.clearTimeout(this._animationClockId);
    }

    goto(logicalTime) {
        console.log('SVGAnimator - goto: a logical time, logicalTime=' + logicalTime + ', logTime=' + logTime);
        if (logicalTime < 0 || logicalTime > this._animationContext.getLogicalTimelineMax()) {
            console.log('SVGAnimator - goto: time is out of timeline, do nothing');
            return;
        }

        this.pause();
        this._clearTokenAnimation();

        // Identify the frame index at this logical time and
        // prepare the buffer starting from that frame index to play
        let currentFrameIndex = this._getFrameIndexFromLogicalTime(logicalTime);
        this._frameBuffer.moveTo(currentFrameIndex);

        this.play();
        console.log('SVGAnimator - goto: move to frame index = ' + currentFrameIndex);
    }

    fastForward() {
        console.log('SVGAnimator - fastForward: new logical time=' + logicalTime + 5);
        let logicalTime = this._svgTokenAnimation.getCurrentTime();
        this.goto(logicalTime + 5);
    }

    fastBackward() {
        console.log('SVGAnimator - fastBackward: new logical time=' + logicalTime - 5);
        let logicalTime = this._svgTokenAnimation.getCurrentTime();
        this.goto(logicalTime - 5);
    }

    _getFrameIndexFromLogicalTime(logicalTime) {
        return (logicalTime*this._animationContext.getRecordingFrameRate());
    }

    _clearTokenAnimation() {
        while (this._svgViewport.lastElementChild) {
            this._svgViewport.removeChild(this._svgViewport.lastElementChild);
        }
        if (this._animationClockId) window.clearTimeout(this._animationClockId);
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

        return tokenMap.values();
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


}