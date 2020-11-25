'use strict';

export class AnimationContext {
    /**
     *
     * @param {String} pluginExecutionId
     * @param {Number} logStartTime: the start timestamp in the log
     * @param {Number} logEndTime: the end timestamp in the log
     * @param {Number} timelineSlots: the number of slots on the timeline
     * @param {Number} togalTimelineTime: the maximum logical time on the timeline in seconds
     * @param {Number} logicalSlotTime: the logical time in seconds of one timeline slot
     * @param {Number} recordingFrameRate: the frame rate used to record frames
     */
    constructor(pluginExecutionId, logStartTime, logEndTime,
                timelineSlots, togalTimelineTime, logicalSlotTime,
                recordingFrameRate) {
        this._pluginExecutionId = pluginExecutionId;
        this._logStartTime = logStartTime;
        this._logEndTime = logEndTime;
        this._timelineSlots = timelineSlots;
        this._logicalTimelineMax = togalTimelineTime;
        this._logicalSlotTime = logicalSlotTime;
        this._recordingFrameRate = recordingFrameRate
    }

    getPluginExecutionId() {
        return this._pluginExecutionId;
    }

    // in milliseconds
    getLogStartTime() {
        return this._logStartTime;
    }

    // in milliseconds
    getLogEndTime() {
        return this._logEndTime;
    }

    // in milliseconds
    getTotalLogTime() {
        return this._logEndTime - this._logStartTime;
    }

    // the ratio between log time and logical time: 1 logical time unit = x log time unit
    getTimelineRatio() {
        return this.getTotalLogTime()/(this._logicalTimelineMax*1000);
    }

    // Number of slots
    getTimelineSlots() {
        return this._timelineSlots;
    }

    // in seconds
    getLogicalTimelineMax() {
        return this._logicalTimelineMax;
    }

    // in seconds
    getLogicalSlotTime() {
        return this._logicalSlotTime;
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

