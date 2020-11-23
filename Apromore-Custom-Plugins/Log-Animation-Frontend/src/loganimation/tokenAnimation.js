/**
 * logStartTime, logEndTime: the starting and ending timestamps (milliseconds) in the log timeline
 * logicalTimelineMax: the maximum number of seconds on the logical timeline
 * actualToLogicalFactor: one second in the actual timeline equals how many seconds in the logical timeline
 * logicalToLogFactor: one second in the logical timeline equals how many seconds in the log
 */
'use strict';

import Buffer from "./frameBuffer";
import {AnimationContext, AnimationState, AnimationEventType} from "./animation";

/**
 * The animation reads frames from the Buffer into a Frame Queue and draws them on the canvas.
 * It has two endless loops:
 *  _loopBufferRead: read frames in chunks from the buffer into the frame queue.
 *  _loopDraw: draw frames from the frame queue, one by one.
 *
 * The animation moves between three states:
 *  - PLAYING: drawing frames sequentially by frames from Frame Queue
 *  - PAUSING: pausing
 *  - JUMPING: an state that jump non-sequentially (backward or forward) to a new frame
 *  JUMPING is an intermediate state while the animation could be playing or pausing.
 * The actions that change the state are named doXXX, e.g. doPause, doUnpause, doGoto
 * Other actions read the properties of the animation (getXXX, isXXX) or change visual styles (setXXX).
 *
 *  The animation configurations are contained in an AnimationContext
 *  The animation informs the outside via events and listeners.
 */

export class TokenAnimation {
    /**
     * @param {AnimationContext} animationContext
     * @param {RenderingContext} canvasContext
     * @param {Object} pathMap: map from element index to the corresponding SVG path element
     * @param {Array} colorPalette: color palette for tokens
     */
    constructor(animationContext, canvasContext, pathMap, colorPalette) {
        console.log('TokenAnimation - constructor');
        this._animationContext = animationContext;
        this._canvasContext = canvasContext;
        this._elementPathMap = pathMap;
        this._colorPalette = colorPalette;

        this._frameBuffer = new Buffer(animationContext); //the buffer start filling immediately based on the animation context.
        this._frameQueue = []; // queue of frames used for animating
        this._currentFrame = undefined;

        this._playingFrameRate = 0;
        this._MAX_BROWSER_REPAINT_RATE= 60; // maximum frame interval for 60fps rate
        this._frameSkip = 0; // number of frames to skip for speed increase
        this._drawingInterval = 0;

        this._currentTime = 0; // milliseconds since the animation start (excluding pausing time)
        this._then = window.performance.now(); // point in time since the last frame interval (millis since time origin)
        this._now = this._then; // current point in time (milliseconds since the time origin)
        this._state = AnimationState.PLAYING;

        this._listeners = [];
        this._tokenColors = ['#ff0000','#e50000', '#cc0000', '#b20000', '#990000', '#7f0000'];
    }

    // Set visual styles and start the main loops
    startEngine() {
        console.log('TokenAnimation: start');
        this.setTokenStyle();
        this._currentTime = 0;
        this._setState(AnimationState.PAUSING);
        this.setPlayingFrameRate(this._animationContext.getRecordingFrameRate());
        //this._loopBufferRead();
        //this._loopDraw(0);
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
        if (playingFrameRate === this._playingFrameRate) return;

        // frameSkip= 0 if rate = (0,_MAX_BROWSER_REPAINT_RATE]
        //          = 1 if rate = (_MAX_BROWSER_REPAINT_RATE, 2*_MAX_BROWSER_REPAINT_RATE]
        //          = 2 if rate = (2*_MAX_BROWSER_REPAINT_RATE, 3*_MAX_BROWSER_REPAINT_RATE]...
        let compoundRate = playingFrameRate >= this._MAX_BROWSER_REPAINT_RATE && (playingFrameRate%this._MAX_BROWSER_REPAINT_RATE) === 0;
        let newFrameSkip = Math.floor(playingFrameRate/this._MAX_BROWSER_REPAINT_RATE) - (compoundRate ? 1 : 0);

        // The actual drawing rate is  playingFrameRate/(this._frameSkip + 1): slower than playingFrameRate due to frame skipping
        this._drawingInterval = 1000 * (newFrameSkip + 1) / playingFrameRate;
        this._playingFrameRate = playingFrameRate;

        // Notify the server to do frame skipping and reset the frame buffer to the current frame index
        if (newFrameSkip !== this._frameSkip) {
            this._frameSkip = newFrameSkip;
            zAu.send(new zk.Event(zk.Widget.$('$win'), 'onFrameSkipChanged', newFrameSkip));
            this._frameBuffer.resetWithFrameSkip(this.getCurrentFrameIndex(), newFrameSkip);
            this._clearData();
        }

        console.log('TokenAnimation: setPlayingFrameRate: playingFrameRate=' +  playingFrameRate +
                    ', frameSkip=' + newFrameSkip+
                    ', drawingInterval=' + this._drawingInterval);
    }

    getPlayingFrameRate() {
        return this._playingFrameRate;
    }

    setPosition(x, y, width, height, matrix) {
        this._canvasContext.canvas.width = width;
        this._canvasContext.canvas.height = height;
        this._canvasContext.canvas.x = x;
        this._canvasContext.canvas.y = y;
        this._canvasContext.setTransform(matrix.a, matrix.b, matrix.c, matrix.d, matrix.e, matrix.f);
        this.setTokenStyle();
    }

    setTokenStyle() {
        this._canvasContext.lineWidth = 6;
        this._canvasContext.strokeStyle = 'blue';
        this._canvasContext.fillStyle = "red";
        this._canvasContext.globalCompositeOperation = "lighten";
    }

    isInProgress () {
        let currentLogicalTime = this.getCurrentLogicalTime();
        return (currentLogicalTime > 0 && currentLogicalTime < this._animationContext.getLogicalTimelineMax());
    }

    isPausing() {
        return this._state === AnimationState.PAUSING;
    }

    getCurrentFrameIndex() {
        return this._currentFrame ? this._currentFrame.index : 0;
    }

    // This is the current actual clock time
    // How long the animation has run excluding pausing time.
    getCurrentClockTime() {
        return this._currentTime;
    }

    getCurrentLogicalTime() {
        return this.getCurrentFrameIndex()/this._animationContext.getRecordingFrameRate();
    }

    getCurrentLogTimeFromStart() {
        return this.getCurrentLogicalTime()*this._animationContext.getTimelineRatio()*1000;
    }

    getCurrentActualTime() {
        return this.getCurrentFrameIndex()/this.getPlayingFrameRate();
    }

    /**
     * Pause affects the two main loops by setting a paused flag.
     */
    doPause() {
        console.log('TokenAnimation: pause');
        this._setState(AnimationState.PAUSING);
    }

    doUnpause() {
        console.log('TokenAnimation: unpause');
        this._setState(AnimationState.PLAYING);
    }

    /**
     * Move to a random logical time mark, e.g. when the timeline tick is dragged to a new position.
     * Goto affects the two main loops by setting a playing mode from sequential to random.
     * @param {Number} logicalTimeMark: number of seconds from the start on the timeline
     */
    doGoto(logicalTimeMark) {
        if (logicalTimeMark < 0 || logicalTimeMark > this._animationContext.getLogicalTimelineMax()) {
            console.log('TokenAnimation - goto: goto time is outside the timeline, do nothing');
            return;
        }
        else if (logicalTimeMark === 0 || logicalTimeMark === this._animationContext.getLogicalTimelineMax()) {
            this._clearAnimation();
        }

        let previousState = this._getState();
        this._setState(AnimationState.JUMPING); //intermediate state
        let newFrameIndex = this._getFrameIndexFromLogicalTime(logicalTimeMark);
        console.log('TokenAnimation - goto: move to  logicalTime=' + logicalTimeMark + ' frame index = ' + newFrameIndex);
        this._frameBuffer.moveTo(newFrameIndex);
        this._setState(previousState);
    }

    registerListener(listener) {
        this._listeners.push(listener);
    }

    /**
     * Continuously read frames from the buffer into the Frame Queue
     */
    _loopBufferRead() {
        setTimeout(this._loopBufferRead.bind(this), 1000);
        if (this._state === AnimationState.PLAYING || this._state === AnimationState.PAUSING) {
            if (this._frameQueue.length >= 2*Buffer.DEFAULT_CHUNK_SIZE) return;
            let frames = this._frameBuffer.readNextChunk();
            if (frames && frames.length > 0) {
                //this._frameQueue.addSome(frames);
                this._frameQueue.push(...frames);
                console.log('TokenAnimation - loopBufferReading: readNext returns result, first frame index=' + frames[0].index);
            } else {
                console.log('TokenAnimation - loopBufferReading: readNext returns EMPTY. FrameQueue size=' + this._frameQueue.length);
            }
        }
    }

    /**
     * The main loop that draw frames from the frame queue
     * window.requestAnimationFrame and elapsed time are used to control the speed of animation
     * @param {Number} newTime: the passing time (milliseconds) since time origin
     * @private
     */
    _loopDraw(newTime) {
        window.requestAnimationFrame(this._loopDraw.bind(this));
        if (this._state === AnimationState.PLAYING) { // draw frames in the queue sequentially
            this._now = newTime;
            let elapsed = this._now - this._then;
            if (elapsed >= this._drawingInterval) {
                this._then = this._now - (elapsed % this._drawingInterval);
                let frame = this._frameQueue.shift();
                if (frame) {
                    this._currentTime += this._drawingInterval;
                    this._currentFrame = frame;
                    this._drawFrame(frame);
                    if (frame.index >= this._animationContext.getTotalNumberOfFrames()-1) {
                        console.log('Frame index = ' + frame.index + ' reached max frame index. Notify end of animation');
                        console.log('Frame queue size = ' + this._frameQueue.length);
                        this._notifyAll(new AnimationEvent(AnimationEventType.END_OF_ANIMATION));
                    }
                    else {
                        this._notifyAll(new AnimationEvent(AnimationEventType.FRAMES_AVAILABLE));
                    }
                } else {
                    this._notifyAll(new AnimationEvent(AnimationEventType.FRAMES_NOT_AVAILABLE));
                }
            }
        } else if (this._state === AnimationState.PAUSING) { // only draw the current frame
            let frame = this._currentFrame || this._frameQueue.shift();
            if (frame) {
                this._drawFrame(frame);
                this._currentFrame = frame;
            }
        }
    }

    /**
     * Draw a frame on the canvas
     * @param {JSON} frame
     * Frame format:
     *	{
     * 	    index: 100,
     * 	    elements: [
     * 		    {elementIndex1: [{caseIndex1:[0.1,1]}, {caseIndex2:[0.2,1]}, {caseIndex3:[0.1,2]}]},
     * 		    {elementIndex2: [{caseIndex1:[0.2,5]}, {caseIndex2:[0.5,3]}]},
     * 		    {elementIndex3: [{caseIndex4:[0.1,1]}]}
     * 	    ]
     * }
     * @private
     */
    _drawFrame(frame) {
        this._clearAnimation();
        for (let element of frame.elements) {
            let elementIndex = Object.keys(element)[0];
            let pathElement = this._getPathElement(elementIndex);
            let totalLength = pathElement.getTotalLength();
            for (let token of element[elementIndex]) {
                let caseIndex = Object.keys(token)[0];
                let distance = token[caseIndex][0];
                let count = token[caseIndex][1];
                let point = pathElement.getPointAtLength(totalLength * distance);
                let radius = count;
                if (radius > 3) radius = 3;
                this._canvasContext.beginPath();
                this._canvasContext.strokeStyle = this._getTokenBorderColor(0);
                this._canvasContext.fillStyle = this._getTokenFillColor(0, count);
                this._canvasContext.arc(point.x, point.y, 5*radius, 0, 2 * Math.PI);
                this._canvasContext.stroke();
                this._canvasContext.fill();
            }
        }
    }

    /**
     * @param {Number} logNo: the ordinal number of the log
     * @return {String} color code
     * @private
     */
    _getTokenBorderColor(logNo) {
        return this._colorPalette[logNo][this._colorPalette[logNo].length-1];
    }

    /**
     * @param {Number} logNo: the ordinal number of the log
     * @param {Number} tokenSize: the size of the token
     * @return {String} color code
     * @private
     */
    _getTokenFillColor(logNo, tokenSize) {
        let colorIndex = 0;
        if (tokenSize <= 2) {
            colorIndex = 0;
        }
        else if (tokenSize <= 4) {
            colorIndex = 2;
        }
        else if (tokenSize <= 6) {
            colorIndex = 4;
        }
        else if (tokenSize <= 8) {
            colorIndex = 5;
        }
        else if (tokenSize <= 10) {
            colorIndex = 6;
        }
        else {
            colorIndex = 7;
        }
        return this._colorPalette[logNo][colorIndex];
    }

    /**
     *
     * @param {Number} numberOfTokens
     * @returns {string}
     * @private
     */
    _selectTokenColor(numberOfTokens) {
        let colorIndex = 0;
        if (numberOfTokens <= 2) colorIndex = 0;
        else if (numberOfTokens <= 4) colorIndex = 1;
        else if (numberOfTokens <= 6) colorIndex = 2;
        else if (numberOfTokens <= 8) colorIndex = 3;
        else if (numberOfTokens <= 10) colorIndex = 4;
        else {
            colorIndex = 5;
        }
        return this._tokenColors[colorIndex];
    }

    /**
     * @param {Number} newState
     */
    _setState(newState) {
        this._state = newState;
        if (newState === AnimationState.PLAYING) {
            console.log('TokenAnimation: set state PLAYING');
            this._now = this._then; //restart counting frame intervals
        }
        else if (newState === AnimationState.JUMPING) {
            console.log('TokenAnimation: set state JUMPING');
            this._clearData();
        }
        else if (newState === AnimationState.PAUSING) {
            console.log('TokenAnimation: set state PAUSING');
        }
    }

    /**
     * @returns {Number}
     */
    _getState() {
        return this._state;
    }

    // Require switching transformation matrix back and forth to clear the canvas properly.
    _clearAnimation() {
        let matrix = this._canvasContext.getTransform();
        this._canvasContext.setTransform(1,0,0,1,0,0);
        this._canvasContext.clearRect(0, 0, this._canvasContext.canvas.width, this._canvasContext.canvas.height);
        this._canvasContext.setTransform(matrix.a, matrix.b, matrix.c, matrix.d, matrix.e, matrix.f);
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

    _getPathElement(elementIndex) {
        return this._elementPathMap[elementIndex];
    }

    _clearData() {
        this._frameQueue = [];
        this._currentFrame = undefined;
    }

    /**
     * @param {AnimationEvent} event
     */
    _notifyAll(event) {
        let engine = this;
        this._listeners.forEach(function(listener){
            listener.update(event);
        })
    }
}
