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

class EventType {
    static get OUT_OF_FRAME() {
        return 1;
    }
    static get TIME_CHANGED() {
        return 2;
    }
    static get FRAMES_AVAILABLE() {
        return 3;
    }
}

class AnimationEvent {
    /**
     *
     * @param {Number} eventType
     * @param {Object} eventData
     */
    constructor(eventType, eventData) {
        this._eventType = eventType
        this._eventData = eventData;
    }

    getEventType() {
        return this._eventType;
    }

    getEventData() {
        return this._eventData;
    }
}

/**
 * The engine reads frames from the Buffer into a Frame Queue and draws them on the canvas.
 * It has two endless loops:
 *  _readBuffer: read frames in chunks from the buffer into the frame queue.
 *  _drawLoop: draw frames from the frame queue, one by one.
 *  The other operations (e.g. pause/unpause, fast forward, fast backward) work by
 *  affecting these two main loops.
 *  The main animation configurations are contained in an AnimationContext
 *  The engine informs the outside via events and listeners.
 */
class AnimationEngine {
    /**
     * @param {AnimationContext} animationContext
     * @param {RenderingContext} canvasContext
     * @param {Object} pathMap
     * @param {Array} elementIds
     */
    constructor(animationContext, canvasContext, pathMap, elementIds) {
        console.log('AnimationEngine - initializing');
        this._animationContext = animationContext;
        this._canvasContext = canvasContext;
        this._canvasTransform = this._canvasContext.getTransform();
        this._pathMap = pathMap;
        this._elementIds = elementIds;
        this._pathElementLengths = [];
        for (let i=0; i<elementIds.length; i++) {
            let eleId = elementIds[i];
            this._pathElementLengths[i] = pathMap[eleId].getTotalLength();
        }

        this._readBufferLoopId = undefined;
        this._frameBuffer = new Buffer(animationContext); //the buffer start filling immediately based on the animation context.
        this._frameQueue = []; // queue of frames used for animating
        this._currentFrameIndex = 0;

        this._currentTime = 0; // milliseconds since the animation start (excluding pausing time)
        this._paused = false; // pausing flag
        this._then = window.performance.now(); // point in time since the last frame interval (millis since time origin)
        this._now = this._then; // current point in time (milliseconds since the time origin)
        this._playMode = PlayMode.SEQUENTIAL;

        this._listeners = [];
        this.setPlayingFrameRate(animationContext.getRecordingFrameRate());
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
     * @param {Number} playingFrameRate
     */
    setPlayingFrameRate(playingFrameRate) {
        this._playingFrameRate = playingFrameRate;
        this._playingFrameInterval = 1000/playingFrameRate;
    }

    getPlayingFrameRate() {
        return this._playingFrameRate;
    }

    getPlayingFrameRateLevel() {
        return this._playingFrameRate/this._animationContext.getRecordingFrameRate();
    }

    _setCanvasStyle() {
        this._canvasContext.lineWidth = 0.5;
        this._canvasContext.strokeStyle = '#000';
        this._canvasContext.fillStyle = "red";
    }

    start() {
        console.log('AnimationEngine: start');
        this._setCanvasStyle();
        this._currentTime = 0;
        this.unpause();
        this.startSequenceMode();
        this._loopBufferReading();
        this._loopDrawing(0);
    }

    isInProgress () {
        let currentLogicalTime = this.getCurrentLogicalTime();
        return (currentLogicalTime > 0 && currentLogicalTime < this._animationContext.getLogicalTimelineMax());
    }

    /**
     * Pause affects the two main loops by setting a paused flag.
     */
    pause() {
        console.log('AnimationEngine: pause');
        this._paused = true;
    }

    unpause() {
        console.log('AnimationEngine: unpause');
        this._paused = false;
        this._now = this._then; //restart counting frame intervals
        this._loopBufferReading();
        this._loopDrawing(this._now);
    }

    // This is the current actual clock time
    // How long the animation has run excluding pausing time.
    getCurrentTime() {
        return this._currentTime;
    }

    // The actual logical time as shown on the timeline at the cursor
    // It is based on the number of frames has passed so far from the start of the animation
    getCurrentLogicalTime() {
        return this._currentFrameIndex/this._animationContext.getRecordingFrameRate();
    }

    getCurrentFrameIndex() {
        return this._currentFrameIndex;
    }

    /**
     * Read frames from the buffer into the playing store
     * This operation enters a loop of reading frames from the buffer.
     * This is to support sequential mode, so it doesn't apply to random play mode
     */
    _loopBufferReading() {
        //console.log('AnimationEngine - loopBufferReading');
        if (this._paused) return;
        this._readBufferLoopId = setTimeout(this._loopBufferReading.bind(this), 1000);
        if (this._playMode !== PlayMode.SEQUENTIAL) return;
        if (this._frameQueue.length >= 2*Buffer.DEFAULT_CHUNK_SIZE) return;

        let frames = this._frameBuffer.readNext();
        if (frames && frames.length > 0) {
            this._frameQueue.push(...frames);
            console.log('AnimationEngine - loopBufferReading: readNext returns with first frame index=' + frames[0].index);
        } else {
            console.log('AnimationEngine - loopBufferReading: readNext returns EMPTY result.');
        }

        // if (this._frameBuffer.isOutOfSupply()) {
        //     console.log('AnimationEngine - loopBufferReading: out of stock and no more frames in supply. The readBufferLoop stops.');
        //     this._clearPendingBufferReads();
        // }
    }

    /**
     * The main loop that draw frames from the frame queue
     * window.requestAnimationFrame and elapsed time are used to control the speed of animation
     * @param {Number} newTime: the passing time (milliseconds) since time origin
     * @private
     */
    _loopDrawing(newTime) {
        if (this._paused) return;
        window.requestAnimationFrame(this._loopDrawing.bind(this));
        if (this._playMode !== PlayMode.SEQUENTIAL) return;
        this._now = newTime;
        let elapsed = this._now - this._then;
        if (elapsed >= this._playingFrameInterval) {
            this._then = this._now - (elapsed % this._playingFrameInterval);
            let frame = this._frameQueue.shift();
            if (frame) {
                this._currentTime += this._playingFrameInterval;
                this._currentFrameIndex = frame.index;
                this._drawFrame(frame);
                this._notifyAll(new AnimationEvent(EventType.FRAMES_AVAILABLE, undefined));
            }
            else {
                this._notifyAll(new AnimationEvent(EventType.OUT_OF_FRAME, undefined));
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

    startSequenceMode() {
        console.log('AnimationEngine: start SEQUENTIAL model');
        this._playMode = PlayMode.SEQUENTIAL;
        //this._readBufferLoop();
        //this._drawLoop(0);
    }

    startRandomMode() {
        console.log('AnimationEngine: start RANDOM mode');
        this._playMode = PlayMode.RANDOM;
    }

    /**
     * Move to a random logical time mark, e.g. when the timeline tick is dragged to a new position.
     * Goto affects the two main loops by setting a playing mode from sequential to random.
     * @param {Number} logicalTimeMark: number of seconds from the start on the timeline
     */
    goto(logicalTimeMark) {
        if (logicalTimeMark < 0 || logicalTimeMark > this._animationContext.getLogicalTimelineMax()) {
            console.log('AnimationEngine - goto: goto time is outside the timeline, do nothing');
            return;
        }
        this.startRandomMode();
        this._clearData();
        this._currentFrameIndex = this._getFrameIndexFromLogicalTime(logicalTimeMark);
        console.log('AnimationEngine - goto: move to  logicalTime=' + logicalTimeMark + ' frame index = ' + this._currentFrameIndex);
        this._frameBuffer.moveTo(this._currentFrameIndex);
        this.startSequenceMode();
        this._notifyAll(new AnimationEvent(EventType.TIME_CHANGED, logicalTimeMark*1000));
    }

    gotoStart() {
        console.log('AnimationEngine: gotoStart');
        this.goto(0);
        this._clearData();
        this._clearCanvas();
    }

    gotoEnd() {
        console.log('AnimationEngine: gotoEnd');
        this.goto(this._animationContext.getLogicalTimelineMax());
        this._clearData();
        this._clearCanvas();
    }

    fastForward() {
        console.log('AnimationEngine: fastForward');
        let newLogicalTimeMark = Math.floor(this.getCurrentTime()/1000) + 5;
        console.log('AnimationEngine - fastForward: new logical time=' + newLogicalTimeMark);
        if (newLogicalTimeMark > this._animationContext.getLogicalTimelineMax()) {
            this._clearData();
            this._clearCanvas();
            this._notifyAll(new AnimationEvent(EventType.TIME_CHANGED, this._animationContext.getLogicalTimelineMax()));
            return;
        }
        this.goto(newLogicalTimeMark);
    }

    fastBackward() {
        console.log('AnimationEngine: fastBackward');
        let newLogicalTimeMark = Math.floor(this.getCurrentTime()/1000) - 5;
        console.log('AnimationEngine - fastBackward: new logical time=' + newLogicalTimeMark);
        if (newLogicalTimeMark < 0) {
            this._clearData();
            this._clearCanvas();
            this._notifyAll(new AnimationEvent(EventType.TIME_CHANGED, 0));
            return;
        }
        this.goto(newLogicalTimeMark);
    }

    /**
     * Get the corresponding frame index at a logical time.
     * @param {Number} logicalTimeMark: number of seconds from the start.
     * @returns {number}: frame index
     * @private
     */
    _getFrameIndexFromLogicalTime(logicalTimeMark) {
        if (logicalTimeMark === 0) return 0;
        return (Math.floor(logicalTimeMark*this._animationContext.getRecordingFrameRate()) - 1);
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
        return this._pathMap[elementId];
    }

    _getPathElementLength(elementIndex) {
        return this._pathElementLengths[elementIndex];
    }

    _getElementId(elementIndex) {
        return this._elementIds[elementIndex];
    }

    _clearPendingBufferReads() {
        if (this._readBufferLoopId) window.clearTimeout(this._readBufferLoopId);
    }

    _clearData() {
        this._frameQueue = [];
    }

    registerListener(listener) {
        this._listeners.push(listener);
    }

    /**
     *
     * @param {AnimationEvent} event
     */
    _notifyAll(event) {
        let engine = this;
        this._listeners.forEach(function(listener){
            listener.update(event);
        })
    }
}