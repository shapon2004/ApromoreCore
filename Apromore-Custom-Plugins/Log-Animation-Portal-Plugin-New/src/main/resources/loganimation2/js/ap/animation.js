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
/**
 * Browser compatibility notes
 *
 * Chrome:
 * - Does not support reference variable to point DOM elements, must use selectors (getElementsBy)
 *   otherwise the innerHTML and element attributes are not updated
 * - svg.setCurrentTime is not processed properly, must call svg to reload via innerHTML
 *
 * Dependencies:
 * utils.js (for Clazz)
 *
 * The animation page has four animation components:
 *
 * 1. The process model with tokens moving along the nodes and edges: Canvas animation (TokenAnimation)
 * 2. The timeline bar with a cursor moving along: SVG animation
 * 3. The circular progress bar showing the completion percentage for the log: SVG animation
 * 4. The digital clock running and showing the passing time: animation with setTimeInterval
 * Four animations above have different internal state, but they share the same logical time which is
 * the timeline when speed level is 1.
 *
 * The timeline bar has a number of equal slots configured in the configuration file, e.g. TimelineSlots = 120.
 * Each slot represents a duration of time in the event log, called SlotDataUnit, i.e. how many seconds per slot
 * Each slot also represents a duration of time in the animation engine, called SlotEngineUnit
 * For example, if the log spans a long period of time, SlotDataUnit will have a large value.
 * SlotEngineUnit is used to calculate the speed of the cursor movement on the timeline bar
 * SlotDataUnit is used to calculate the location of a specific event date on the timeline bar
 * timeCoef: the ratio of SlotDataUnit to SlotEngineUnit, i.e. 1 second in the engine = how many seconds in the data.
 * The starting point of time in all logs is set in json data sent from the server: startMs.
 *
 */

class AnimationController {
  constructor(canvas, pluginExecutionId) {
    this.jsonModel = null; // Parsed objects of the process model
    this.jsonServer = null; // Parsed objects returned from the server
    this.timeline = null;
    this.logs = null;
    this.logNum = 0;

    // Animation environments: canvas, SVG documents and the clock
    this.canvas = canvas; // the editor canvas object
    this.svgViewport = null; // initialized in Controller.reset
    this.svgDocs = [];
    this.svgMain = null; // initialized in Controller.reset
    this.svgTimeline = undefined;
    this.svgProgresses = [];
    this.clockTimer = null;

    this.startMs = 0;
    this.endMs = 120;
    this.slotNum = 120;
    this.slotDataMs = 1000;

    this.textFont = {size: '11', anchor: 'middle'};
    this.PLAY_CLS = 'ap-mc-icon-play';
    this.PAUSE_CLS = 'ap-mc-icon-pause';
    this.SHOW_OTHER_LOGS_TIMESPAN = false;
    this.apPalette = ['#84c7e3', '#bb3a50', '#3ac16d', '#f96100', '#FBA525'];
    this.timelineOffset = {
      x: 20, y: 20,
    };

    this.pluginExecutionId = pluginExecutionId;
    this.pathElementCache = {};
    this.pathElementLengths = [];
  }

  initialize(jsonRaw) {
    console.log('AnimationController: reset/start');

    // Data for animation
    this.jsonServer = JSON.parse(jsonRaw);
    let {logs, timeline, elementIds} = this.jsonServer;
    this.logs = logs;
    this.logNum = logs.length;
    this.timeline = timeline;
    this.elementIds = elementIds;

    // Elements for other animations: timeline, progresses, clock
    this.svgMain = this.canvas.getSVGContainer();
    this.svgViewport = this.canvas.getSVGViewport();
    this.svgTimeline = $j('div.ap-la-timeline > svg')[0];
    this.timelineEl = null;

    this.svgProgresses = [];
    this.svgDocs.clear();
    this.svgDocs.push(this.svgMain);
    this.svgDocs.push(this.svgTimeline);

    // Time configuration for the animation
    this.startMs = new Date(timeline.startDateLabel).getTime(); // Start date in milliseconds
    this.endMs = new Date(timeline.endDateLabel).getTime(); // End date in milliseconds
    this.totalMs = this.endMs - this.startMs;
    this.endPos = timeline.endDateSlot; // End slot, currently set at 120
    this.slotNum = timeline.timelineSlots; // The number of timeline vertical bars or (timeline.endDateSlot - timeline.startDateSlot)
    this.slotDataMs = this.totalMs / this.slotNum; // Data milliseconds per slot

    this.totalEngineS = timeline.totalEngineSeconds; // Total engine seconds (may change according to the speed)
    this.slotEngineS = timeline.slotEngineUnit; // in seconds
    this.timeCoef = this.slotDataMs / this.slotEngineS / 1000; // Ratio for data ms / animation ms

    // Values for speed level = 1
    this.currentSpeedLevel = 1;
    this.oriTotalEngineS = this.totalEngineS;
    this.oriSlotEngineS = this.slotEngineS;
    this.oriTimeCoef = this.timeCoef;

    // Visual drawing for timeline
    this.slotWidth = 9;
    this.timelineWidth = this.slotNum * this.slotWidth;
    this.logIntervalSize = 5;
    this.logIntervalHeight = 7;
    this.logIntervalMargin = 8;

    // Cache path elements
    for (let log of this.logs) {
      for (let i=0; i<this.elementIds.length; i++) {
        let flowId = this.elementIds[i];
        this.pathElementCache[flowId] = $j('[data-element-id=' + flowId + ']').find('g').find('path').get(0);
        this.pathElementLengths[i] = this.pathElementCache[flowId].getTotalLength();
      }
    }

    // Create token animation
    let box = this.svgMain.getBoundingClientRect();
    let matrix = this.svgViewport.transform.baseVal.consolidate().matrix;
    let ctx = document.querySelector("#canvas").getContext('2d');
    ctx.canvas.width = box.width;
    ctx.canvas.height = box.height;
    ctx.canvas.x = box.x;
    ctx.canvas.y = box.y;
    ctx.setTransform(matrix.a, matrix.b, matrix.c, matrix.d, matrix.e, matrix.f);
    this.animationContext = new AnimationContext(this.pluginExecutionId, this.startMs, this.endMs, this.totalEngineS);
    this.tokenAnimation = new TokenAnimation(this.animationContext, ctx, this.pathElementCache, this.elementIds);
    this.tokenAnimation.registerListener(this);

    // Create visual controls
    this.createProgressIndicators();
    this.createLogInfoPopups();
    this.createTimeline();
    this.createLogIntervals();
    this.createTicks();
    this.createCursor();
    this.createMetricTables();

    let me = this;
    window.onkeydown = function(event) {
      if(event.keyCode === 32 || event.key === " ") {
        event.preventDefault();
        me.playPause();
      }
    };

    this.updateClockOnce(this.startMs);
    this.pause();
  }

  // Add log intervals to timeline
  createLogIntervals() {
    let {
      timeline, logNum, slotWidth, timelineOffset, timelineEl, apPalette,
      logIntervalHeight, logIntervalMargin, logIntervalSize,
    } = this;
    let ox = timelineOffset.x, y = timelineOffset.y + logIntervalMargin; // Start offset

    for (let i = 0; i < logNum; i++) {
      let log = timeline.logs[i];
      let x1 = ox + slotWidth * log.startDatePos; // Magic number 10 is slotWidth / slotEngineS
      let x2 = ox + slotWidth * log.endDatePos;
      let style = 'stroke: ' + (apPalette[i] || log.color) + '; stroke-width: ' + logIntervalSize;
      let opacity = 0.8;
      new SVG.Line().plot(x1, y, x2, y).attr({style, opacity}).addTo(timelineEl);

      // Display date label at the two ends
      if (this.SHOW_OTHER_LOGS_TIMESPAN && log.startDatePos % 10 != 0) {
        let txt = log.startDateLabel.substr(0, 19);
        let x = ox + slotWidth * log.startDatePos - 50;
        y += 5;
        new SVG.Text().plain(txt).font(this.textFont).attr({x, y}).addTo(timelineEl);
      }
      y += logIntervalHeight;
    }
  }

  createProgressIndicators(speedRatio) {
    let {logs, logNum} = this;
    let log, progressContainer, svgProgressEl, label;
    let svgProgress, svgProgresses = [];
    let progressWrapper = $j('#ap-la-progress');

    progressWrapper.empty();
    for (let i = 0; i < logNum; i++) {
      log = logs[i];
      svgProgress = $j(`<svg id="progressbar-${i}  xmlns="${SVG_NS}" viewBox="-10 0 20 40" ></svg>`);
      progressWrapper.append(
          $j(`<div id="progress-c-${i}"></div>`).append(
              svgProgress.append(this.createProgressIndicatorsForLog(i + 1, log, this.timeline, 0, 0, speedRatio)),
          ).append($j(`<div class="label">${log.filename}</div>`)),
      );
      svgProgress = svgProgress[0];
      svgProgresses.push(svgProgress);
      this.svgDocs.push(svgProgress);
    }

    this.svgProgresses = svgProgresses;
  }

  createLogInfoPopups() {
    let {logs, logNum} = this;
    let logInfo = $j('#ap-la-info-tip');
    let props = [
      {
        id: 'info-no',
        key: 'index',
      },
      {
        id: 'info-log',
        key: 'filename',
      },
      {
        id: 'info-traces',
        key: 'total',
      },
      {
        id: 'info-replayed',
        key: 'play',
        title: 'unplayTraces',
      },
      {
        id: 'info-reliable',
        key: 'reliable',
        title: 'unreliableTraces',
      },
      {
        id: 'info-fitness',
        key: 'exactTraceFitness',
      },
    ];

    function getProps(log) {
      props.forEach(function(prop) {
        $j('#' + prop.id).text(log[prop.key]).attr('title', log[prop.title || prop.key]);
      });
    }

    for (let i = 0; i < logNum; i++) {
      let pId = '#ap-la-progress-' + (i + 1);
      $j(pId).hover(
          (function(idx) {
            let log = logs[idx - 1];
            return function() {
              getProps(log);
              let {top, left} = $j(pId).offset();
              let bottom = `calc(100vh - ${top - 10}px)`;
              left += 20;
              logInfo.attr('data-log-idx', idx);
              logInfo.css({bottom, left});
              logInfo.show();
            };
          })(i + 1),
          function() {
            logInfo.hide();
          },
      );
    }
  }

  /**
   * Logical time: the time as shown on the timeline when the cursor speed level is 1.
   * Actual time: the actual time of the timeline cursor when its speed is less than or greater than 1.
   * @param logicalTime
   * @returns {number}
   */
  getSVGTimeFromLogicalTime(logicalTime) {
    if (logicalTime <= 0) return 0;
    if (logicalTime >= this.oriTotalEngineS) return this.totalEngineS;
    return logicalTime/this.currentSpeedLevel;
  }

  getLogicalTimeFromSVGTime(svgTime) {
    if (svgTime <= 0) return 0;
    if (svgTime >= this.totalEngineS) return this.oriTotalEngineS;
    return svgTime*this.currentSpeedLevel;
  }

  getLogTimeFromLogicalTime(logicalTime) {
    return logicalTime * this.oriTimeCoef * 1000 + this.startMs;
  }

  getCurrentSVGTime() {
    return this.svgTimeline.getCurrentTime();
  }

  setCurrentSVGTime(time) {
    if (time < 0) { time = 0; }
    if (time > this.totalEngineS) { time = this.totalEngineS; }
    let self=this;
    this.svgDocs.forEach(function(svgDoc) {
      if (svgDoc != self.svgMain) svgDoc.setCurrentTime(time);
    });
  }

  /*
   * This method is used to read SVG document current time at every interval based on timer mechanism
   * It stops reading when SVG document time reaches the end of the timeline
   * The end() method is used for ending tasks for the replay completion scenario
   * Thus, the end() method should NOT create a loopback to this method.
   */
  updateClock() {
    // Original implementation -- checks for termination, updates clock view
    if (this.getCurrentSVGTime() >= this.totalEngineS) {
      console.log('AnimationController - updateClock: gotoEnd because out of animation time.');
      this.gotoEnd();
    } else {
      this.updateClockOnce(this.startMs + this.getCurrentSVGTime()*this.timeCoef*1000);
    }
  }

  updateClockOnce(time) {
    let dateEl = document.getElementById('date');
    let timeEl = document.getElementById('time');
    let locales = 'en-GB';
    let date = new Date();
    date.setTime(time);

    if (window.Intl) {
      dateEl.innerHTML = new Intl.DateTimeFormat(locales, {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
      }).format(date);
      timeEl.innerHTML = new Intl.DateTimeFormat(locales, {
        hour12: false,
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
      }).format(date);
    } else {
      // Fallback for browsers that don't support Intl (e.g. Safari 8.0)
      dateEl.innerHTML = date.toDateString();
      timeEl.innerHTML = date.toTimeString();
    }
  }

  /**
   * SVG animation controls speed on a fixed path via time duration and determines the current position via
   * the current engine time. However, TokenAnimation (canvas based) controls speed via frame rates.
   * The time system in TokenAnimation and SVG animations are different because of different frame rates
   * (we don't know what happens inside the SVG animation engine). However, we can use the current logical time
   * as the shared information to synchronize them.
   * @param {Number} speedLevel: the level number on the speed control component
   */
  changeSpeed(speedLevel) {
    this.pause();
    console.log('AnimationController - changeSpeed: speedLevel = ' + speedLevel);
    this.updateSVGAnimations(speedLevel);
    this.tokenAnimation.setPlayingFrameRate(speedLevel*this.animationContext.getRecordingFrameRate());
    this.unPause();
    this.currentSpeedLevel = speedLevel;
  }

  /**
   * Below is the SVG rule to make sure the animation continues from the same position at the new speed.
   * Let L be the total length of an element where tokens are moved along (e.g. a sequence flow)
   * Let X be the current time duration set for the token to finish the length L (X is the value of dur attribute)
   * Let D be the distance that the token has done right before the speed is changed
   * Let Cx be the current engine time right before the speed is changed, e.g. Cx = svgDoc.getCurrentLogicalTime().
   * Let Y be the NEW time duration set for the token to travel through the length L.
   * Let Cy be the current engine time assuming that Y has been set and the token has finished the D distance.
   * Thus, the token can move faster or lower if Y < X or Y > X, respectively (Y is the new value of the dur attribute)
   * A requirement when changing the animation speed is all tokens must keep running from
   * the last position they were right before the speed change.
   * We have: D = Cy*L/Y = Cx*L/X => Cy = (Y/X)*Cx
   * Thus, for the token to start from the same position it was before the speed changes (i.e. dur changes from X to Y),
   * the engine time must be set to (Y/X)*Cx, where Cx = svgDoc.getCurrentTime().
   * Instead of making changes to the distances, the user sets the speed through a speed slider control.
   * Each level represents a speed rate of the tokens
   * Sy = L/Y, Sx = L/X, SpeedRatio = Sy/Sx = X/Y: the ratio between the new and old speed levels.
   * In the formula above:
   *  Cy = Cx/SpeedRatio
   *  Y = X/SpeedRatio.
   * In summary, by setting the animation duration as above and keeping the begin attribute UNCHANGED,
   * the SVG engine will automatically adjust its animation to go faster or slower. By setting the engine current time,
   * the engine will start from the current position.
   *
   * Note that when SVG Animation changes its speed (i.e. change its time duration and current time), its internal
   * current time has changed. This means svg.getCurrentTime() returns a different internal engine time depending on
   * the new speed.
   *
   * @param {Number} speedLevel: the level number on the speed control component
   */
  updateSVGAnimations(speedLevel) {
    let speedRatio = speedLevel/this.currentSpeedLevel;
    console.log('AnimationController - updateSVGAnimations: speedRatio = ' + speedRatio);

    // Update visual configurations to match the new speed
    this.totalEngineS = this.totalEngineS / speedRatio;
    this.slotEngineS = this.slotEngineS / speedRatio;
    this.timeCoef = this.slotDataMs / (this.slotEngineS*1000);

    // Update the speed of circle progress bar
    let animations = $j('.progress-animation');
    for (let i = 0; i < animations.length; i++) {
      let animateEl = animations[i];
      let curDur = animateEl.getAttribute('dur');
      curDur = curDur.substr(0, curDur.length - 1);
      animateEl.setAttributeNS(null,'dur', curDur/speedRatio + 's');
    }

    // Update the cursor. Must recreate the cursor because setAttributeNS doesn't work
    if (this.cursorEl) {
      this.timelineEl.removeChild(this.cursorEl);
    }
    this.createCursor();

    // Now set the current SVG engine time: the SVG animation will change speed at the same position
    let newActualTime = this.getCurrentSVGTime()/speedRatio;
    this.setCurrentSVGTime(newActualTime);
  }

  /**
   *
   * @param {Number} logicalTime: the time when speed level = 1.
   */
  goto(logicalTime) {
    let newLogicalTime = logicalTime;
    if (newLogicalTime < 0) { newLogicalTime = 0; }
    if (newLogicalTime > this.oriTotalEngineS) { newLogicalTime = this.oriTotalEngineS; }
    this.setCurrentSVGTime(this.getSVGTimeFromLogicalTime(newLogicalTime));
    this.tokenAnimation.goto(newLogicalTime);
    this.updateClockOnce(this.getLogTimeFromLogicalTime(newLogicalTime));
  }

  isAtStart() {
    let currentLogicalTime = this.getLogicalTimeFromSVGTime(this.getCurrentSVGTime());
    return (currentLogicalTime === 0);
  }

  isAtEnd() {
    let currentLogicalTime = this.getLogicalTimeFromSVGTime(this.getCurrentSVGTime());
    return (currentLogicalTime === this.oriTotalEngineS);
  }

  // Move forward 1 slot
  fastForward() {
    console.log('AnimationController - fastForward');
    if (this.isAtEnd()) return;
    let currentLogicalTime = this.getLogicalTimeFromSVGTime(this.getCurrentSVGTime());
    let newLogicalTime = currentLogicalTime + this.oriSlotEngineS;
    this.goto(newLogicalTime);
  }

  // Move backward 1 slot
  fastBackward() {
    console.log('AnimationController - fastBackward');
    if (this.isAtStart()) return;
    let currentLogicalTime = this.getLogicalTimeFromSVGTime(this.getCurrentSVGTime());
    let newLogicalTime = currentLogicalTime - this.oriSlotEngineS;
    this.goto(newLogicalTime);
  }

  gotoStart() {
    console.log('AnimationController - gotoStart');
    if (this.isAtStart()) return;
    this.tokenAnimation.clearCanvas();
    this.goto(0);
    this.pause();
  }

  gotoEnd() {
    console.log('AnimationController - gotoEnd');
    if (this.isAtEnd()) return;
    this.tokenAnimation.clearCanvas();
    this.goto(this.oriTotalEngineS);
    this.pause();
  }

  nextTrace() {
  }

  previousTrace() {
  }

  isPlayingState() {
    return $j('#pause').hasClass(this.PAUSE_CLS);
  }

  /**
   * @param {Boolean} changeToPlay: true means setting the button to a Play shape, false: set it to a Pause shape.
   */
  setPlayPauseButton(changeToPlay) {
    const {PAUSE_CLS, PLAY_CLS} = this;
    const button = $j('#pause');

    if (typeof changeToPlay === 'undefined') {
      changeToPlay = !this.isPlayingState();
    }
    if (changeToPlay) {
      button.removeClass(PAUSE_CLS).addClass(PLAY_CLS);
    } else {
      button.removeClass(PLAY_CLS).addClass(PAUSE_CLS);
    }
  }

  pause() {
    console.log('AnimationController: pause');
    this.tokenAnimation.pause();
    this.pauseSecondaryAnimations();
    this.setPlayPauseButton(true);
  }

  pauseSecondaryAnimations() {
    console.log('AnimationController - pauseSecondaryAnimations');
    this.svgDocs.forEach(function(svgDoc) {
      svgDoc.pauseAnimations();
    });

    if (this.clockTimer) {
      clearInterval(this.clockTimer);
    }
  }

  unPause() {
    console.log('AnimationController: unPause');
    this.tokenAnimation.unpause();
    this.unPauseSecondaryAnimations();
    this.setPlayPauseButton(false);
  }

  unPauseSecondaryAnimations() {
    console.log('AnimationController - unPauseSecondaryAnimations');
    let me = this;
    this.svgDocs.forEach(function(svgDoc) {
      svgDoc.unpauseAnimations();
    });

    if (this.clockTimer) clearInterval(this.clockTimer);
    this.clockTimer = setInterval(me.updateClock.bind(this),100);
  }

  playPause() {
    console.log('AnimationController: toggle play/pause');
    if (this.isPlayingState()) {
      this.pause();
    } else {
      this.unPause();
    }
  }

  /*
   * Create progress indicator for one log
   * log: the log object (name, color, traceCount, progress, tokenAnimations)
   * x,y: the coordinates to draw the progress bar
   */
  createProgressIndicatorsForLog(logNo, log, timeline, x, y, speedRatio) {
    speedRatio = speedRatio || 1;
    let {values, keyTimes, begin, dur} = log.progress;
    let color = this.apPalette[logNo - 1] || log.color;
    let progress = new SVG.G().attr({
      id: 'ap-la-progress-' + logNo,
    }).node;

    let path = 'M ' + x + ',' + y + ' m 0, 0 a 20,20 0 1,0 0.00001,0';
    let pie = new SVG.Path().plot(path).attr({
      fill: color,
      'fill-opacity': 0.5,
      stroke: color,
      'stroke-width': '5',
      'stroke-dasharray': '0 126 126 0',
      'stroke-dashoffset': '1',
    }).node;

    let pieAnim = document.createElementNS(SVG_NS, 'animate');
    pieAnim.setAttributeNS(null, 'class', 'progress-animation');
    pieAnim.setAttributeNS(null, 'attributeName', 'stroke-dashoffset');
    pieAnim.setAttributeNS(null, 'values', values);
    pieAnim.setAttributeNS(null, 'keyTimes', keyTimes);
    pieAnim.setAttributeNS(null, 'begin', begin / speedRatio + 's');
    pieAnim.setAttributeNS(null, 'dur', dur / speedRatio + 's');
    pieAnim.setAttributeNS(null, 'fill', 'freeze');
    pieAnim.setAttributeNS(null, 'repeatCount', '1');
    pie.appendChild(pieAnim);
    progress.appendChild(pie);
    return progress;
  }

  createTick(x, y, tickSize, color, textToTickGap, dateTxt, timeTxt, timelineEl) {
    new SVG.Line().plot(x, y, x, y + tickSize).stroke({color, width: 0.5}).addTo(timelineEl);
    y -= textToTickGap;
    new SVG.Text().plain(timeTxt).font(this.textFont).attr({x, y}).addTo(timelineEl);
    y -= this.textFont.size * 1.5; // lineHeight
    new SVG.Text().plain(dateTxt).font(this.textFont).attr({x, y}).addTo(timelineEl);
  }

  createTicks() {
    // Add text and line for the bar

    let {
      slotNum, logNum, slotEngineS, slotWidth, slotDataMs, timelineEl, timelineOffset,
      logIntervalHeight, logIntervalMargin,
    } = this;
    let tickSize = logIntervalHeight * (logNum - 1) + 2 * logIntervalMargin;
    let textToTickGap = 5;
    let x = timelineOffset.x;
    let y = timelineOffset.y;
    let time = this.startMs;
    let color;
    let date, dateTxt, timeTxt;
    let skip;

    for (let i = 0; i <= slotNum; i++) {
      if (i % 10 == 0) {
        date = moment(time);
        dateTxt = date.format('D MMM YY');
        timeTxt = date.format('H:mm:ss');
        color = 'grey';
        skip = false;
      } else {
        dateTxt = '';
        timeTxt = '';
        color = '#e0e0e0';
        skip = true;
      }
      if (!skip) {
        this.createTick(x, y, tickSize, color, textToTickGap, dateTxt, timeTxt, timelineEl);
      }
      x += slotWidth;
      time += slotDataMs;
    }
  }

  createCursor() {
    let {
      logNum,
      totalEngineS,
      svgTimeline,
      slotNum,
      slotWidth,
      slotEngineS,
      timelineWidth,
      timelineEl,
      timelineOffset,
    } = this;
    let x = timelineOffset.x;
    let y = timelineOffset.y + 5;
    let cursorEl;
    let me = this;

    let path = 'M0 0 L8 8 L8 25 L-8 25 L-8 8 Z';
    cursorEl = new SVG.Path().plot(path).attr({
      fill: '#FAF0E6',
      stroke: 'grey',
      style: 'cursor: move',
      transform: `translate(${x},${y})`,
    }).node;

    let cursorAnim = document.createElementNS(SVG_NS, 'animateTransform');
    cursorAnim.setAttributeNS(null, 'attributeName', 'transform');
    cursorAnim.setAttributeNS(null, 'type', 'translate');
    cursorAnim.setAttributeNS(null, 'id', 'cursor-animation');
    cursorAnim.setAttributeNS(null, 'begin', '0s');
    //cursorAnim.setAttributeNS(null, 'dur', this.animationContext.getLogicalTimelineMax() + 's');
    cursorAnim.setAttributeNS(null, 'dur', slotNum*slotEngineS + 's');
    cursorAnim.setAttributeNS(null, 'by', 1);
    cursorAnim.setAttributeNS(null, 'from', x + ',' + y);
    cursorAnim.setAttributeNS(null, 'to', x + slotNum * slotWidth + ',' + y);
    cursorAnim.setAttributeNS(null, 'fill', 'freeze');

    cursorEl.appendChild(cursorAnim);
    timelineEl.appendChild(cursorEl);

    this.cursorEl = cursorEl;
    this.cursorAnim = cursorAnim;

    // Control dragging of the timeline cursor
    let dragging = false;
    let isPlayingBeforeDrag = false;

    cursorEl.addEventListener('mousedown', startDragging.bind(this));
    svgTimeline.addEventListener('mouseup', stopDragging.bind(this));
    svgTimeline.addEventListener('mouseleave', stopDragging.bind(this));

    function startDragging(evt) {
      isPlayingBeforeDrag = me.isPlayingState();
      evt.preventDefault();
      dragging = true;
      me.pause();
    }

    function stopDragging(evt) {
      if (!dragging) return; // Avoid doing the below two times
      if (evt.type == 'mouseleave' && dragging) {
        return;
      }
      dragging = false;
      let logicalTime = getLogicalTimeFromMouseX(evt);
      me.goto(logicalTime);
      if (isPlayingBeforeDrag) {
        me.unPause();
      }
    }

    function getLogicalTimeFromMouseX(evt) {
      let x = getSVGMousePosition(evt).x;
      let dx = x - me.timelineOffset.x;
      return (dx / me.timelineWidth) * me.oriTotalEngineS;
    }

    // Convert from screen coordinates to SVG document coordinates
    function getSVGMousePosition(evt) {
      let svg = me.svgTimeline;
      let matrix = svg.getScreenCTM().inverse();
      let point = svg.createSVGPoint();
      point.x = evt.clientX;
      point.y = evt.clientY;
      return point.matrixTransform(matrix);
    }

  }
  /*
   * <g id="timeline">
   *   <-- timeline bar -->
   *   <line>
   *     <text>
   *     ...
   *   <line>

   *   <text>
   *     <!-- timeline cursor -->
   *     <rect>
   *       <animationMotion>
   *
   * Use: this.slotNum, this.slotEngineMs
   */
  createTimeline() {
    // Create the main timeline container group
    let timelineEl = new SVG.G().attr({
      id: 'timeline',
      style: '-webkit-touch-callout: none; -webkit-user-select: none; -khtml-user-select: none; -moz-user-select: none; -ms-user-select: none; user-select: none',
    }).node;
    this.timelineEl = timelineEl;
    this.svgTimeline.append(timelineEl);
    return timelineEl;
  }

  createMetricTables() {
    let logs = this.logs;
    // Show metrics for every log
    let metricsTable = $j('#metrics_table')[0];
    for (let i = 0; i < logs.length; i++) {
      let row = metricsTable.insertRow(i + 1);
      let cellLogNo = row.insertCell(0);
      let cellLogName = row.insertCell(1);
      let cellTotalCount = row.insertCell(2);
      let cellPlayCount = row.insertCell(3);
      let cellReliableCount = row.insertCell(4);
      let cellExactFitness = row.insertCell(5);

      cellLogNo.innerHTML = i + 1;
      cellLogNo.style.backgroundColor = logs[i].color;
      cellLogNo.style.textAlign = 'center';

      if (logs[i].filename.length > 50) {
        cellLogName.innerHTML = logs[i].filename.substr(0, 50) + '...';
      } else {
        cellLogName.innerHTML = logs[i].filename;
      }
      cellLogName.title = logs[i].filename;
      cellLogName.style.font = '1em monospace';
      //cellLogName.style.backgroundColor = logs[i].color;

      cellTotalCount.innerHTML = logs[i].total;
      cellTotalCount.style.textAlign = 'center';
      cellTotalCount.style.font = '1em monospace';

      cellPlayCount.innerHTML = logs[i].play;
      cellPlayCount.title = logs[i].unplayTraces;
      cellPlayCount.style.textAlign = 'center';
      cellPlayCount.style.font = '1em monospace';

      cellReliableCount.innerHTML = logs[i].reliable;
      cellReliableCount.title = logs[i].unreliableTraces;
      cellReliableCount.style.textAlign = 'center';
      cellReliableCount.style.font = '1em monospace';

      cellExactFitness.innerHTML = logs[i].exactTraceFitness;
      cellExactFitness.style.textAlign = 'center';
      cellExactFitness.style.font = '1em monospace';
    }
  }

  /**
   * @param {EventType} event
   */
  update(event) {
    //console.log('AnimationController: event processing');
    if (!(event instanceof AnimationEvent)) return;

    // Need to check playing state to avoid calling pause/unpause too many times
    // which will disable the digital clock
    if (event.getEventType() === EventType.OUT_OF_FRAME && this.isPlayingState()) {
      this.pauseSecondaryAnimations();
    }
    else if (event.getEventType() === EventType.FRAMES_AVAILABLE && !this.isPlayingState()) {
      this.unPauseSecondaryAnimations();
    }
  }

}
