'use strict';

export class AnimationContext {
    /**
     *
     * @param {String} pluginExecutionId
     * @param {Number} logStartTime: the start timestamp in the log
     * @param {Number} logEndTime: the end timestamp in the log
     * @param {Number} logicalTimelineMax: the maximum logical time in seconds
     * @param {Number} recordingFrameRate: the frame rate used to record frames
     */
    constructor(pluginExecutionId, logStartTime, logEndTime, logicalTimelineMax,
                recordingFrameRate) {
        this._pluginExecutionId = pluginExecutionId;
        this._logStartTime = logStartTime;
        this._logEndTime = logEndTime;
        this._logicalTimelineMax = logicalTimelineMax;
        this._recordingFrameRate = recordingFrameRate
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

    getTimelineRatio() {
        return (this._logEndTime - this._logStartTime)/(this._logicalTimelineMax*1000);
    }

    getLogicalTimelineMax() {
        return this._logicalTimelineMax;
    }

    getRecordingFrameRate() {
        return this._recordingFrameRate;
    }

    getTotalNumberOfFrames() {
        return this._recordingFrameRate*this._logicalTimelineMax;
    }

}

export class AnimationState {
    static get PLAYING() { // playing animation frame by frame
        return 0;
    }
    static get JUMPING() { // jumping backward or forward to a new frame
        return 1;
    }
    static get PAUSING() { // pausing
        return 2;
    }
}

