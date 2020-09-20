/**
 * SVGToken represents a token animated on a model element over a sequence of frames
 * It is created from reading frames, it keeps track of the first and last frame indexes and attributes.
 *
 * @author Bruce Nguyen
 */
class SVGToken {
    /**
     *
     * @param {String} elementId
     * @param {String} caseId
     * @param {Number} firstFrameIndex
     * @param {Array} firstAtts
     * @param {Number} lastFrameIndex
     * @param {Array} lastAtts
     */
    constructor(elementId, caseId, firstFrameIndex, firstAtts, lastFrameIndex, lastAtts) {
        this._elementId = elementId;
        this._caseId = caseId;
        this._firstFrameIndex = firstFrameIndex;
        this._lastFrameIndex = lastFrameIndex;
        this._firstAtts = firstAtts;
        this._lastAtts = lastAtts;
    }

    getId() {
        return this._elementId + "," + this._caseId;
    }

    getElementId() {
        return this._elementId;
    }

    getCaseId() {
        return this._caseId;
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

    getLogicalToLogFactor() {
        return this._logicalToLogFactor;
    }
}

/**
 * Main class to control the animation via the SVG engine.
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

        this._modelController = modelController;
        this._svgTokenAnimation = svgTokenAnimation;
        this._svgTimeline = svgTimeline;
        this._svgProgressBar = svgProgressBar;
        this._svgViewport = svgViewport;

        this._playingFrameRate = animationContext.getRecordingFrameRate();
        this._startingFrameIndexSinceLastRateChange = 0; //the starting frame index since applying the current playingFrameRate.
        this._startingTimeSinceLastRateChange = svgTokenAnimation.getCurrentTime(); //the starting time since applying the current playingFrameRate

        this._animationClockId = undefined;
        this._frameBuffer = new Buffer(animationContext); //the buffer start filling immediately based on the animation context.
        this._elementPool = []; //contains SVG elements to be reused, not to create new which is expensive
        this._elementPoolLimit = 20*this._frameBuffer.getChunkSize();

        // Initialize
        this._initElementPool();
        this.pause();
    }

    _initElementPool() {
        this._elementPool = [];
        for (let i=0; i<this._elementPoolLimit; i++) {
            let emptyElement = this._createNewElement("", "", 0, 0, 0, 0);
            this._elementPool.push(emptyElement);
        }
    }

    /**
     * @param {SVGElement} element
     */
    addToElementPool(element) {
        if (this._elementPool.length < this._elementPoolLimit) {
            this._elementPool.push(element);
        }
    }

    getFromElementPool() {
        return this._elementPool.pop();
    }

    /**
     * Read frames from the buffer and animate them
     * There are reasons the buffer could be slow in supplying frames, e.g. network issues.
     * The buffer is self-managed to fill itself with frames. The animator arranges asynchronous callback to check
     * the buffer status until non-empty result is returned or the buffer has no more frames to supply.
     */
    _animateLoop() {
        console.log('SVGAnimator - animateLoop');
        if (this._frameBuffer.isStockAvailable()) {
            let frames = this._frameBuffer.readNext();
            if (frames && frames.length > 0) {
                this._animate(frames);
                this.unpause();
                console.log('SVGAnimator - animateLoop: readNext returns result for animation. Unpause and play.');
            } else {
                this.pause();
                console.log('SVGAnimator - animateLoop: readNext returns NO result for animation. Pause to wait.');
            }
        }

        // Repeat reading the buffer until it has no more frames to supply (out of the server supply).
        if (!this._frameBuffer.isOutOfSupply()) {
            let timeOutInterval = Math.floor(this._frameBuffer.getChunkSize()/(5*this._playingFrameRate))*1000;
            this._animationClockId = setTimeout(this._animateLoop.bind(this), timeOutInterval);
            console.log('SVGAnimator - animateLoop: start new animateLoop after ' + timeOutInterval/1000 + 's.');
        }
        else {
            console.log('SVGAnimator - animateLoop: out of stock and no more frames in supply. The animateLoop stops.');
        }
    }

    getSVGViewport() {
        return this._svgViewport;
    }

    /**
     * Animate an array of frames by generating SVG elements.
     * These elements are added to the svg document which will start animating them.
     * For efficiency, these elements will be removed from the document once their animation finishes.
     * @param {[]} frames: the frames to be animated.
     * @private
     */
    _animate(frames) {
        let svgTokens = this._createSVGTokens(frames);
        //console.log(svgTokens);
        for (let token of svgTokens) {
            if (token) {
                let svgElement = this._createElement(token);
                //console.log(token);
                //console.log('SVGElement');
                //console.log(token);
                //console.log(svgElement);
                if (svgElement) {
                    this._svgViewport.appendChild(svgElement);
                }
            }


        }
    }

    /**
     * Set a new speed for the animation
     * The effect of changing speed is that the position of tokens being shown is unchanged but they will move
     * slower or faster.
     *
     * The animation speed is driven by recordingFrameRate and playingFrameRate (both are frames per second).
     * - recordingFrameRate is the rate of generating frames at the server
     * - playingFrameRate is the rate of playing frames at the client
     * If playingFrameRate is higher than recordingFrameRate: the animation will be faster
     * If playingFrameRate is lower than recordingFrameRate: the animation will be slower.
     *
     * For example, if recordingFrameRate is 48fps and playingFrameRate is 24fps, then for 480 frames (10seconds recording),
     * the animation will take 20 seconds to finish, thus it will look slower than the recording. On the other hand,
     * if the playingFrameRate is 96fps, the animation will take 5 seconds and it looks faster than the recording.
     *
     * @param {Number} speedLevel
     */
    setSpeed(speedLevel) {
        let lastPlayingFrameRate = this._playingFrameRate;
        let currentTime = this._svgTokenAnimation.getCurrentTime();
        let newPlayingFrameRate = Math.floor(speedLevel*this._animationContext.getRecordingFrameRate());
        if (newPlayingFrameRate === 0) return;

        if (newPlayingFrameRate !== lastPlayingFrameRate) {
            //Wipe out all the current tokens
            this.pause();
            this._clearTokenAnimation();

            // Identify the current frame index that the animation has reached to at this point
            let passingTime = currentTime - this._startingTimeSinceLastRateChange;
            let currentFrameIndex = this._startingFrameIndexSinceLastRateChange + passingTime*lastPlayingFrameRate;

            // Set the new playing attributes for use by other calculations
            this._playingFrameRate = newPlayingFrameRate;
            this._startingFrameIndexSinceLastRateChange = currentFrameIndex;
            this._startingTimeSinceLastRateChange = currentTime;

            // Move the buffer to the current frame index and prepare it to store subsequent frames
            this._frameBuffer.moveTo(currentFrameIndex);
            //if (speedLevel > 1) this._frameBuffer.setChunkSize(Buffer.DEFAULT_CHUNK_SIZE + (speedLevel-1)*Buffer.DEFAULT_CHUNK_SIZE);

            // Animate frames in the buffer starting from the current frame index and using the new playing settings
            this.play();
        }
    }

    // This is the logical time
    _getCurrentTime() {
        return this._svgTokenAnimation.getCurrentTime();
    }

    /**
     * Use SVG engine to animate tokens which have been converted from frames.
     * The SVG engine uses 'begin' (time since the start) and 'dur' (duration) to animate tokens
     * Setting 'begin' and 'dur" is the same as setting a new frame rate for the token.
     *
     * The token moves from its first frame distance to its last frame distance.
     * keyPoints and keyTimes are used to control this movement. At the end of their movement, the SVG element will be
     * deleted and thus it will disappear.
     *
     * In order to achieve a continuation of token movement, chunks of frames must be accurately calculated such that
     * these distances are continued. For example, a token of id=1 must have distance 0.1-0.3 in one chunk and 0.3-0.7
     * in the next check of frames.
     *
     * @param {SVGToken} svgToken
     * @returns {SVGElement}
     */
    _createElement (svgToken) {
        let begin = this._getLogicalTimeFromFrameIndex(svgToken.getFirstFrameIndex());
        let dur = (svgToken.getLastFrameIndex() - svgToken.getFirstFrameIndex() + 1)/this._playingFrameRate;
        let pathElement = this._getPathElement(svgToken.getElementId());
        if (!pathElement) return;
        let path = pathElement.getAttribute('d');

        //console.log('Element pool POP one element: size = ' + this._elementPool.length);
        //console.log(this._elementPool);
        let animateMotion;
        let svgElement = this.getFromElementPool();
        if (!svgElement) {
            svgElement = this._createNewElement(svgToken.getElementId()+","+svgToken.getCaseId(),
                path, begin, dur, svgToken.getFirstDistance(), svgToken.getLastDistance());
            animateMotion = svgElement.childNodes[0];
        }
        else {
            //console.log(svgElement);
            svgElement.setAttributeNS(null, 'id', svgToken.getElementId() + "," + svgToken.getCaseId());
            animateMotion = svgElement.childNodes[0].cloneNode();
            svgElement.replaceChild(animateMotion, svgElement.childNodes[0]);
            //animateMotion = svgElement.childNodes[0];
            animateMotion.setAttributeNS(null, 'begin', begin);
            animateMotion.setAttributeNS(null, 'dur', dur);
            animateMotion.setAttributeNS(null, 'path', path);
            animateMotion.setAttributeNS(null, 'keyPoints', '0;' + svgToken.getFirstDistance() + ";" +
                svgToken.getLastDistance());
        }

        // if (this._listernerMap.has(svgElement)) {
        //     animateMotion.removeEventListener('endEvent', this._listernerMap.get(svgElement), true);
        // }
        if (animateMotion.onend) {
            animateMotion.onend = {};
        }

        let animator = this;
        function eventHandler() {
            animator.getSVGViewport().removeChild(svgElement);
            animator.addToElementPool(svgElement);
            //this.removeEventListener('onend');
            //console.log('Element pool PUSH one element: size = ' + animator._elementPool.length);
            //console.log(svgElement);
        };
        //animateMotion.addEventListener('endEvent', eventHandler, true);
        animateMotion.onend = eventHandler;
        //this._listernerMap.set(svgElement, eventHandler);

        return svgElement;
    }

    _createNewElement(tokenId, path, begin, dur, startDist, endDist) {
        let svgElement = document.createElementNS(SVG_NS, 'g');
        svgElement.setAttributeNS(null, 'stroke', 'none');
        svgElement.setAttributeNS(null, 'class', 'token');
        svgElement.setAttributeNS(null, 'id', tokenId);

        let animateMotion = document.createElementNS(SVG_NS, 'animateMotion');
        animateMotion.setAttributeNS(null, 'begin', begin);
        animateMotion.setAttributeNS(null, 'dur', dur);
        animateMotion.setAttributeNS(null, 'fill', 'freeze');
        animateMotion.setAttributeNS(null, 'path', path);
        animateMotion.setAttributeNS(null, 'rotate', 'auto');
        animateMotion.setAttributeNS(null, 'calcMode', 'linear');
        animateMotion.setAttributeNS(null, 'keyPoints', '0;' + startDist + ";" + endDist);
        animateMotion.setAttributeNS(null, 'keyTimes', '0;0;1');
        svgElement.appendChild(animateMotion);

        let circle = document.createElementNS(SVG_NS, 'circle');
        circle.setAttributeNS(null, 'cx', 0);
        circle.setAttributeNS(null, 'cy', 0);
        circle.setAttributeNS(null, 'r', 5);
        circle.setAttributeNS(null, 'fill', 'red');
        svgElement.appendChild(circle)

        return svgElement;
    }

    play() {
        this.unpause();
        this._animateLoop();
    }

    pause() {
        this._svgTokenAnimation.pauseAnimations();
        this._svgTimeline.pauseAnimations();
        this._svgProgressBar.pauseAnimations();
        if (this._animationClockId) window.clearTimeout(this._animationClockId);
    }

    unpause() {
        this._svgTokenAnimation.unpauseAnimations();
        this._svgTimeline.unpauseAnimations();
        this._svgProgressBar.unpauseAnimations();
    }

    /**
     * Move to a random logical timepoint, e.g. when the timeline tick is dragged to a new position.
     *
     * @param {Number} logicalTime: number of seconds from the start
     */
    goto(logicalTime) {
        console.log('SVGAnimator - goto: a logical time, logicalTime=' + logicalTime);
        if (logicalTime < 0 || logicalTime > this._animationContext.getLogicalTimelineMax()) {
            console.log('SVGAnimator - goto: goto time is outside the timeline, do nothing');
            return;
        }

        // Stop and clear all the current tokens on the screen
        this.pause();
        this._clearTokenAnimation();

        // Identify the frame index at this logical time and
        // prepare the buffer to contain frames starting from that frame index
        let currentFrameIndex = this._getFrameIndexFromLogicalTime(logicalTime);
        this._frameBuffer.moveTo(currentFrameIndex);

        // Animate the frames in the buffer starting from the current frame index
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

    /**
     * Get the corresponding frame index at a logical time.
     * @param {Number} logicalTime: the position of the tick on the timeline, how many seconds it is from the start.
     * @returns {number}
     * @private
     */
    _getFrameIndexFromLogicalTime(logicalTime) {
        return (logicalTime*this._animationContext.getRecordingFrameRate());
    }

    _getLogicalTimeFromFrameIndex(frameIndex) {
        return (frameIndex/this._animationContext.getRecordingFrameRate());
    }

    _clearTokenAnimation() {
        if (this._animationClockId) window.clearTimeout(this._animationClockId); // ensure no tokens show up after clearing.
        for (let tokenId of this._tokenElements.keys()) {
            this._svgViewport.removeChild(tokenId);
        }
        this._tokenElements.clear();
    }

    /**
     * Create a collection of SVGToken from an array of frames
     * Frames are ordered by frame indexes, this is also the timing order of frames
     * Thus, tokens with the same token key (elementId+caseId) are also ordered in consecutive frames which is convenient
     * for identifying the starting and ending frames of the same token key.
     * @param {[]} frames
     * @returns {IterableIterator<any>}
     * @private
     */
    _createSVGTokens(frames) {
        let tokenMap = new Map();
        for (let frame of frames) {
            let frameIndex = frame.index;
            for (let element of frame.elements) {
                let elementId = Object.keys(element)[0];
                for (let token of element[elementId]) {
                    let caseId = Object.keys(token)[0];
                    let tokenKey = elementId + "," + caseId;
                    if (!tokenMap.has(tokenKey)) {
                        tokenMap.set(tokenKey, new SVGToken(elementId, caseId, frameIndex, token[caseId], frameIndex, token[caseId]));
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