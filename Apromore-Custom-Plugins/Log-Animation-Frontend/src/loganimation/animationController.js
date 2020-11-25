/*-
 * #%L
 * This file is part of "Apromore Core".
 *
 * Copyright (C) 2017 Queensland University of Technology.
 * %%
 * Copyright (C) 2018 - 2020 The University of Melbourne.
 * %%
 *
 * "Apromore" is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * "Apromore" is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program.
 * If not, see <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */
'use strict';

import {AnimationContext} from './animationContextState';
import {AnimationEvent, AnimationEventType} from './animationEvents';
import TokenAnimation from './tokenAnimation';
import TimelineAnimation from "./timelineAnimation";
import ProcessMapController from "./processMapController";
import SpeedControl from "./speedControl";
import ProgressAnimation from "./progressAnimation";
import ClockAnimation from "./clockAnimation";
import PlayActionControl from "./playActionControl";

/**
 * AnimationController is a Front Controller to manage the animation page.
 * It coordinates other components, including token animation, timeline, clock, log info table, progress indicator.
 * Each component is represented by a Controller.
 * The View and Controller of MVC are bundled as Controller. View is accessed via jQuery.
 * The Model of MVC is not explicitly managed as the data is read-only. Data include JSON and XML read from the server.
 * As the business gets more complex, a more complete MVC model would be considered.
 *
 * @author Bruce Nguyen
 */
export default class AnimationController {
    /**
     * @param {String} processMapXML: the BPMN XML text encoding the process model
     * @param {String} setupData: the JSON raw text containg initial setup data
     * @param {String} pluginExecutionId: ID of the running log animation instance, used for communicating with the server.
     */
    constructor(processMapXML, setupData, pluginExecutionId) {
        this.pluginExecutionId = pluginExecutionId;
        let jsonSetup = JSON.parse(setupData);
        this.logSummaries = jsonSetup.logs;
        this.isPlayingBeforeMovingModel = false;
        this.apPalette = [
            ['#84c7e3', '#76b3cc', '#699fb5', '#5c8b9e', '#4f7788', '#426371', '#344f5a', '#273b44'],
            ['#bb3a50', '#a83448', '#952e40', '#822838', '#702230', '#5d1d28', '#4a1720', '#381118']
        ];

        // Add token animation
        let {recordingFrameRate, elementMapping, startMs, endMs, totalEngineS,
            timelineSlots, slotEngineUnit, caseCountsByFrames} = jsonSetup;
        this.animationContext = new AnimationContext(this.pluginExecutionId, startMs, endMs, timelineSlots,
                                                    totalEngineS, slotEngineUnit, recordingFrameRate);
        this.processMapController = new ProcessMapController(this, 'editorcanvas', processMapXML, elementMapping);
        this.tokenAnimation = new TokenAnimation(this, $j("#canvas"), this.processMapController, this.apPalette);

        // Add other animation components
        this.timeline = new TimelineAnimation(this, $j('div.ap-la-timeline > div > svg')[0], caseCountsByFrames);
        this.speedControl = new SpeedControl(this, $j("#speed-control"));
        this.progress = new ProgressAnimation(this, $j('#ap-la-progress'), $j('#ap-la-info-tip'));
        this.clock = new ClockAnimation(this, $j('#date'), $j('#time'));
        this.buttonControls = new PlayActionControl(this,
                                $j('#start'), $j('#pause'), $j('#forward'), $j('#backward'), $j('#end'),
                                'ap-mc-icon-play', 'ap-mc-icon-pause');

        // Register events
        this.processMapController.registerListener(this);
        this.tokenAnimation.registerListener(this);
        this._setKeyboardEvents();

        // Start
        this.tokenAnimation.startEngine();
        this.pause();
    }

    _setKeyboardEvents() {
        let me = this;
        window.onkeydown = function (event) {
            if (event.keyCode === 32 || event.key === " ") {
                event.preventDefault();
                me.playPause();
            }
        };
    }

    /**
     * @returns {AnimationContext}
     */
    getAnimationContext() {
        return this.animationContext;
    }

    getNumberOfLogs() {
        return this.logSummaries.length;
    }

    getLogSummaries() {
        return this.logSummaries;
    }

    getLogColor(logNo, logColor) {
        return this.apPalette[logNo - 1][0] || logColor;
    }

    _getCurrentSVGTime() {
        return this.timeline.getCurrentTime();
    }

    _getSVGTimeFromLogicalTime(logicalTime) {
        return this.timeline.getSVGTimeFromLogicalTime(logicalTime);
    }

    _getLogicalTimeFromSVGTime(svgTime) {
        return this.timeline.getLogicalTimeFromSVGTime(svgTime);
    }

    _getLogTimeFromLogicalTime(logicalTime) {
        return this.timeline.getLogTimeFromLogicalTime(logicalTime);
    }

    /**
     * SVG animation controls speed on a fixed path via time duration and determines the current position via
     * the current engine time. However, TokenAnimation (canvas based) controls speed via frame rates.
     * The time system in TokenAnimation and SVG animations are different because of different frame rates
     * (we don't know what happens inside the SVG animation engine). However, we can use the current logical time
     * as the shared information to synchronize them.
     * @param {Number} frameRate: frames per second
     */
    setSpeedLevel(frameRate) {
        let newSpeedLevel = frameRate / this.animationContext.getRecordingFrameRate();
        console.log('AnimationController - changeSpeed: speedLevel = ' + newSpeedLevel);
        this._pauseSecondaryAnimations();
        this.progress.setSpeedLevel(newSpeedLevel);
        this.timeline.setSpeedLevel(newSpeedLevel);
        this.tokenAnimation.setPlayingFrameRate(frameRate);
    }

    // Move forward 1 slot
    fastForward() {
        console.log('AnimationController - fastForward');
        if (this.timeline.isAtEnd()) return;
        let currentLogicalTime = this._getLogicalTimeFromSVGTime(this._getCurrentSVGTime());
        let newLogicalTime = currentLogicalTime + this.animationContext.getLogicalSlotTime();
        this.goto(newLogicalTime);
    }

    // Move backward 1 slot
    fastBackward() {
        console.log('AnimationController - fastBackward');
        if (this.timeline.isAtStart()) return;
        let currentLogicalTime = this._getLogicalTimeFromSVGTime(this._getCurrentSVGTime());
        let newLogicalTime = currentLogicalTime - this.animationContext.getLogicalSlotTime();
        this.goto(newLogicalTime);
    }

    gotoStart() {
        console.log('AnimationController - gotoStart');
        if (this.timeline.isAtStart()) return;
        this.goto(0);
        this.pause();
    }

    gotoEnd() {
        console.log('AnimationController - gotoEnd');
        if (this.timeline.isAtEnd()) return;
        this.goto(this.animationContext.getLogicalTimelineMax());
        this.pause();
    }

    /**
     *
     * @param {Number} logicalTime: the time when speed level = 1.
     */
    goto(logicalTime) {
        let newLogicalTime = logicalTime;
        if (newLogicalTime < 0) {
            newLogicalTime = 0;
        }
        if (newLogicalTime > this.animationContext.getLogicalTimelineMax()) {
            newLogicalTime = this.animationContext.getLogicalTimelineMax();
        }

        let svgTime = this._getSVGTimeFromLogicalTime(newLogicalTime);
        let logTime = this._getLogTimeFromLogicalTime(newLogicalTime);
        this.timeline.setCurrentTime(svgTime);
        this.progress.setCurrentTime(svgTime);
        this.clock.setCurrentTime(logTime);
        this.tokenAnimation.doGoto(newLogicalTime);
    }

    isPlaying() {
        return this.timeline.isPlaying();
    }

    pause() {
        console.log('AnimationController: pause');
        this.tokenAnimation.doPause();
        this._pauseSecondaryAnimations();
        this.buttonControls.setPlayPauseButton(true);
    }

    unPause() {
        console.log('AnimationController: unPause');
        this.tokenAnimation.doUnpause();
        this._unPauseSecondaryAnimations();
        this.buttonControls.setPlayPauseButton(false);
    }

    /**
     * Toggle between play and pause.
     */
    playPause() {
        if (this.timeline.isAtEnd()) return;
        console.log('AnimationController: toggle play/pause');
        if (this.isPlaying()) {
            this.pause();
        } else {
            this.unPause();
        }
    }

    _pauseSecondaryAnimations() {
        console.log('AnimationController - pauseSecondaryAnimations');
        this.timeline.pause();
        this.progress.pause();
        this.clock.pause();
    }

    _unPauseSecondaryAnimations() {
        console.log('AnimationController - unPauseSecondaryAnimations');
        this.timeline.unPause();
        this.progress.unPause();
        this.clock.unPause();
    }


    /**
     * @param {AnimationEventType} event
     * @param {Object} eventData
     */
    handleEvent(event, eventData) {
        //console.log('AnimationController: event processing');
        if (!(event instanceof AnimationEvent)) return;

        // Need to check playing state to avoid calling pause/unpause too many times
        // which will disable the digital clock
        if (event.getEventType() === AnimationEventType.FRAMES_NOT_AVAILABLE && this.isPlaying()) {
            this._pauseSecondaryAnimations();
        } else if (event.getEventType() === AnimationEventType.FRAMES_AVAILABLE && !this.isPlaying()) {
            this._unPauseSecondaryAnimations();
        } else if (event.getEventType() === AnimationEventType.END_OF_ANIMATION) {
            this.pause();
            this.clock.setTime(this.animationContext.getLogicalTimelineMax());
        } else if (event.getEventType() === AnimationEventType.MODEL_CANVAS_MOVING) {
            let modelBox = event.getEventData().viewbox;
            let modelMatrix = event.getEventData().transformMatrix;
            this.tokenAnimation.setPosition(modelBox.x, modelBox.y, modelBox.width, modelBox.height, modelMatrix);
            if (this.isPlaying()) {
                this.pause();
                this.isPlayingBeforeMovingModel = true;
            }
        } else if (event.getEventType() === AnimationEventType.MODEL_CANVAS_MOVED) {
            let modelBox = event.getEventData().viewbox;
            let modelMatrix = event.getEventData().transformMatrix;
            this.tokenAnimation.setPosition(modelBox.x, modelBox.y, modelBox.width, modelBox.height, modelMatrix);
            if (this.isPlayingBeforeMovingModel) {
                this.unPause();
                this.isPlayingBeforeMovingModel = false;
            }
        }
    }

}
