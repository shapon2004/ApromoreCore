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

class PlayMode {
    static get SEQUENTIAL() {
        return 0;
    }
    static get RANDOM() {
        return 1;
    }
}

/**
 * Main class to control the animation via HTML5 Canvas
 * It reads frames from the Buffer into a Frame Queue and draws them on the canvas.
 * The animator has two endless loops:
 *  _readBuffer: read frames in chunks from the buffer into the frame queue.
 *  _drawLoop: draw frames from the frame queue, one by one.
 *  The other operations (e.g. pause/unpause, fast forward, fast backward) work by
 *  affecting these two main loops.
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

        this._readBufferLoopId = undefined;
        this._frameBuffer = new Buffer(animationContext); //the buffer start filling immediately based on the animation context.
        this._frameQueue = []; // queue of frames used for animating

        this._currentTime = 0; // milliseconds since the animation start (excluding pausing time)
        this._paused = false; // pausing flag
        this._then = window.performance.now(); // point in time since the last frame interval (millis since time origin)
        this._now = this._then; // current point in time (milliseconds since the time origin)

        this._playMode = PlayMode.SEQUENTIAL;

        // Initialize
        this._setPlayingFrameRate(animationContext.getRecordingFrameRate());
        this.pause();
    }

    /**
     * Change the playing frame rate
     * @param {Number} playingFrameRate
     * @private
     */
    _setPlayingFrameRate(playingFrameRate) {
        this._playingFrameRate = playingFrameRate;
        this._playingFrameInterval = 1000/playingFrameRate;
        this._bufferReadingInterval = Math.floor(this._frameBuffer.getChunkSize()/(2*playingFrameRate))*1000;
    }

    _setCanvasStyle() {
        this._canvasContext.lineWidth = 0.5;
        this._canvasContext.strokeStyle = '#000';
        this._canvasContext.fillStyle = "red";
    }

    start() {
        this._setCanvasStyle();
        this._currentTime = 0;
        this.unpause();
        this.startSequenceMode();
    }

    /**
     * Pause affects the two main loops by setting a paused flag.
     */
    pause() {
        this._paused = true;
        this._svgTimeline.pauseAnimations();
        this._svgProgressBar.pauseAnimations();
    }

    unpause() {
        this._now = this._then; //restart counting frame intervals
        this._paused = false;
        this._svgTimeline.unpauseAnimations();
        this._svgProgressBar.unpauseAnimations();
    }

    // The logical time since this Animator is created.
    getCurrentTime() {
        return this._currentTime;
    }

    /**
     * Read frames from the buffer into the playing store
     * This operation enters a loop of reading frames from the buffer.
     * This is to support sequential mode, so it doesn't apply to random play mode
     */
    _readBufferLoop() {
        this._readBufferLoopId = setTimeout(this._readBufferLoop.bind(this), 0);
        if (this._playMode !== PlayMode.SEQUENTIAL) return;
        if (this._paused) return;
        if (this._frameQueue.length >= 300) return;

        let frames = this._frameBuffer.readNext();
        if (frames && frames.length > 0) {
            this._frameQueue.push(...frames);
            this.unpause();
            console.log('SVGAnimator - readBufferLoop: readNext returns result for animation. Unpause and play.');
        } else if (this._frameQueue.length <= 0) {
            this.pause();
            console.log('SVGAnimator - readBufferLoop: readNext returns NO result for animation. Pause to wait.');
        }

        if (this._frameBuffer.isOutOfSupply()) {
            console.log('SVGAnimator - readBufferLoop: out of stock and no more frames in supply. The animateLoop stops.');
            this._clearPendingBufferReads();
        }
    }

    /**
     * Draw frames from the current store of play frames
     * Use window.requestAnimationFrame and elapsed time to control the speed of animation
     * The animation clock time is also controlled here
     * @param {Number} newTime: the passing time (milliseconds) since time origin
     * @private
     */
    _drawLoop(newTime) {
        window.requestAnimationFrame(this._drawLoop.bind(this));
        if (this._playMode !== PlayMode.SEQUENTIAL) return;
        this._now = newTime;
        let elapsed = this._now - this._then;
        if (elapsed >= this._playingFrameInterval) {
            this._then = this._now - (elapsed % this._playingFrameInterval);
            if (!this._paused) {
                let frame = this._frameQueue.shift();
                //let frame = this._frameBuffer.readOne();
                if (frame) {
                    this._drawFrame(frame);
                    this._currentTime += this._playingFrameInterval;
                }
            }
        }
    }

    /**
     * Draw a frame on the canvas
     * @param frame
     * @private
     */
    _drawFrame(frame) {
        this._clearCanvas();
        for (let element of frame.elements) {
            let elementIndex = Object.keys(element)[0];
            let elementId = this._getElementId(elementIndex);
            let pathElement = this._getPathElement(elementId);
            let totalLength = pathElement.getTotalLength();
            for (let token of element[elementIndex]) {
                let caseIndex = Object.keys(token)[0];
                let distance = token[caseIndex];
                let point = pathElement.getPointAtLength(totalLength * distance);
                this._canvasContext.beginPath();
                this._canvasContext.arc(point.x, point.y, 5, 0, 2 * Math.PI);
                this._canvasContext.fill();
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
        let newPlayingFrameRate = speedLevel*this._animationContext.getRecordingFrameRate();
        if (newPlayingFrameRate !== this._playingFrameRate) {
            this._setPlayingFrameRate(newPlayingFrameRate);
        }
    }

    startSequenceMode() {
        this._playMode = PlayMode.SEQUENTIAL;
        this._readBufferLoop();
        this._drawLoop(0);
    }

    startRandomMode() {
        this._playMode = PlayMode.RANDOM;
    }

    /**
     * Move to a random logical time mark, e.g. when the timeline tick is dragged to a new position.
     * Goto affects the two main loops by setting a playing mode from sequential to random.
     * @param {Number} logicalTimeMark: number of seconds from the start
     */
    goto(logicalTimeMark) {
        if (logicalTimeMark < 0 || logicalTimeMark > this._animationContext.getLogicalTimelineMax()) {
            console.log('SVGAnimator - goto: goto time is outside the timeline, do nothing');
            return;
        }
        this.startRandomMode();
        this._clearData();
        let currentFrameIndex = this._getFrameIndexFromLogicalTime(logicalTimeMark);
        this._frameBuffer.moveTo(currentFrameIndex);
        this._currentTime = logicalTimeMark*1000;
        this.startSequenceMode();
        console.log('SVGAnimator - goto: move to  logicalTime=' + logicalTimeMark + ' frame index = ' + currentFrameIndex);
    }

    fastForward() {
        let newLogicalTimeMark = Math.floor(this.getCurrentTime()/1000) + 5;
        if (newLogicalTimeMark > this._animationContext.getLogicalTimelineMax()) {
            newLogicalTimeMark = this._animationContext.getLogicalTimelineMax();
        }
        this.goto(newLogicalTimeMark);
        console.log('SVGAnimator - fastForward: new logical time=' + newLogicalTimeMark);
    }

    fastBackward() {
        let newLogicalTimeMark = Math.floor(this.getCurrentTime()/1000) - 5;
        if (newLogicalTimeMark < 0) newLogicalTimeMark = 0;
        this.goto(newLogicalTimeMark);
        console.log('SVGAnimator - fastBackward: new logical time=' + newLogicalTimeMark);
    }

    /**
     * Get the corresponding frame index at a logical time.
     * @param {Number} logicalTimeMark: number of seconds from the start.
     * @returns {number}: frame index
     * @private
     */
    _getFrameIndexFromLogicalTime(logicalTimeMark) {
        if (logicalTimeMark == 0) return 0;
        return (logicalTimeMark*this._animationContext.getRecordingFrameRate() - 1);
    }

    _getLogicalTimeFromFrameIndex(frameIndex) {
        return (frameIndex/this._animationContext.getRecordingFrameRate());
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

    _clearPendingBufferReads() {
        if (this._readBufferLoopId) window.clearTimeout(this._readBufferLoopId);
    }

    _clearData() {
        this._frameQueue = [];
        this._clearPendingBufferReads();
    }
}