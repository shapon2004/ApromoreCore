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
class CanvasAnimator {
    /**
     * @param {AnimationContext} animationContext
     * @param {AnimationController} modelController
     * @param {RenderingContext} canvasContext
     * @param {SVGDocument} svgMain
     * @param {SVGDocument} svgTimeline
     * @param {SVGDocument} svgProgressBar
     * @param {SVGDocument} svgViewport
     */
    constructor(animationContext,
                modelController,
                canvasContext,
                svgMain,
                svgTimeline,
                svgProgressBar,
                svgViewport) {
        console.log('SVGAnimator - being initialized');
        //console.log('SVGAnimator - AnimationContext: ' + JSON.stringify(animationContext));
        this._animationContext = animationContext;

        this._modelController = modelController;

        this._canvasContext = canvasContext;
        let box = svgMain.getBoundingClientRect();
        let matrix = svgViewport.transform.baseVal.consolidate().matrix;
        this._canvasContext.canvas.width = box.width;
        this._canvasContext.canvas.height = box.height;
        this._canvasContext.canvas.x = box.x;
        this._canvasContext.canvas.y = box.y;
        this._canvasContext.setTransform(matrix.a, matrix.b, matrix.c, matrix.d, matrix.e, matrix.f);
        this._canvasTransform = matrix;


        this._svgTimeline = svgTimeline;
        this._svgProgressBar = svgProgressBar;

        this._playingFrameRate = animationContext.getRecordingFrameRate();
        this._startingFrameIndexSinceLastRateChange = 0; //the starting frame index since applying the current playingFrameRate.
        this._startingTimeSinceLastRateChange = this._getCurrentTime(); //the starting time since applying the current playingFrameRate

        this._animationClockId = undefined;
        this._frameBuffer = new Buffer(animationContext); //the buffer start filling immediately based on the animation context.
        this._frames = [];

        this._paused = false;
        this._stop = false;

        this._then = window.performance.now();
        this._now = this._then;
        this._elapsed = 0;
        this._currentTime = 0; //seconds since start

        this._frameInterval = 1000/this._playingFrameRate;
        this._readFrameInterval = Math.floor(this._frameBuffer.getChunkSize()/(this._playingFrameRate))*1000;

        // Initialize
        this.pause();
    }

    /**
     * Read frames from the buffer and animate them
     * There are reasons the buffer could be slow in supplying frames, e.g. network issues.
     * The buffer is self-managed to fill itself with frames. The animator arranges asynchronous callback to check
     * the buffer status until non-empty result is returned or the buffer has no more frames to supply.
     */
    _readFrames() {
        console.log('SVGAnimator - animateLoop');
        if (this._paused) {
            this._animationClockId = setTimeout(this._readFrames.bind(this), 0);
            return;
        }

        if (this._frameBuffer.isStockAvailable()) {
            let frames = this._frameBuffer.readNext();
            if (frames && frames.length > 0) {
                this._frames.push(...frames);
                this.unpause();
                console.log('SVGAnimator - animateLoop: readNext returns result for animation. Unpause and play.');
            } else {
                this.pause();
                console.log('SVGAnimator - animateLoop: readNext returns NO result for animation. Pause to wait.');
            }
        }

        // Repeat reading the buffer until it has no more frames to supply (out of the server supply).
        if (!this._frameBuffer.isOutOfSupply()) {
            this._animationClockId = setTimeout(this._readFrames.bind(this), this._readFrameInterval);
            console.log('SVGAnimator - animateLoop: start new animateLoop after ' + this._readFrameInterval/1000 + 's.');
        }
        else {
            console.log('SVGAnimator - animateLoop: out of stock and no more frames in supply. The animateLoop stops.');
        }
    }

    play() {
        this._clearCanvas();
        this._canvasContext.lineWidth = 0.5;
        this._canvasContext.strokeStyle = '#000';
        this._canvasContext.fillStyle = "red";

        this._frameInterval = 1000/this._playingFrameRate;
        this._then = window.performance.now();
        this._now = this._then;

        this.unpause();
        this._readFrames();
        this._animate();
    }

    pause() {
        this._paused = true;
        this._then = window.performance.now();
        this._now = this._then;

        this._svgTimeline.pauseAnimations();
        this._svgProgressBar.pauseAnimations();
        if (this._animationClockId) window.clearTimeout(this._animationClockId);
    }

    unpause() {
        this._paused = false;
        this._svgTimeline.unpauseAnimations();
        this._svgProgressBar.unpauseAnimations();
    }

    stop() {
        this._frames = [];
        this._stop = true;
    }

    /**
     * Animate an array of frames by generating SVG elements.
     * These elements are added to the svg document which will start animating them.
     * For efficiency, these elements will be removed from the document once their animation finishes.
     * @param {Number} newTime: the frames to be animated.
     * @private
     */
    _animate(newTime) {
        if (this._stop) {
            return;
        }
        else if (this._paused) {
            window.requestAnimationFrame(this._animate.bind(this));
            return;
        }

        window.requestAnimationFrame(this._animate.bind(this));
        this._now = newTime;
        this._elapsed = this._now - this._then;

        if (this._elapsed >= 1000) {
            this._currentTime++;
        }

        if (this._elapsed >= this._frameInterval) {
            this._then = this._now - (this._elapsed % this._frameInterval);
            let frame = this._frames.shift();
            if (frame) {
                this._drawFrame(frame);
            }
            else {
                this.pause();
            }
        }
    }

    /**
     * Draw a frame on the canvas
     * @param frame
     * @private
     */
    _drawFrame(frame) {
        console.log(frame);
        this._clearCanvas();
        for (let element of frame.elements) {
            let elementIndex = Object.keys(element)[0];
            let elementId = this._getElementId(elementIndex);
            let pathElement = this._getPathElement(elementId);
            for (let token of element[elementIndex]) {
                let caseIndex = Object.keys(token)[0];
                let distance = token[caseIndex];
                let point = pathElement.getPointAtLength(this._getPathElementLength(elementIndex) * distance);
                this._canvasContext.beginPath();
                this._canvasContext.arc(point.x, point.y, 5, 0, 2 * Math.PI);
                this._canvasContext.fill();
                this._canvasContext.stroke();
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
        let currentTime = this._getCurrentTime();
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
        return this._currentTime;
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
        let logicalTime = this._getCurrentTime();
        this.goto(logicalTime + 5);
    }

    fastBackward() {
        console.log('SVGAnimator - fastBackward: new logical time=' + logicalTime - 5);
        let logicalTime = this._getCurrentTime();
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
    }

    _clearCanvas() {
        this._canvasContext.setTransform(1,0,0,1,0,0);
        this._canvasContext.clearRect(0, 0, this._canvasContext.canvas.width, this._canvasContext.canvas.height);
        let matrix = this._canvasTransform;
        this._canvasContext.setTransform(matrix.a, matrix.b, matrix.c, matrix.d, matrix.e, matrix.f);
    }

    _getPathElement(elementId) {
        return this._modelController.getPathElement(elementId);
    }

    _getPathElementLength(elementIndex) {
        return this._modelController.getPathElementLength(elementIndex);
    }

    _getElementId(elementIndex) {
        return this._modelController.getElementId(elementIndex);
    }


}