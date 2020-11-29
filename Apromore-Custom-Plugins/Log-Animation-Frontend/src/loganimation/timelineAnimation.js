import * as SVG from "@svgdotjs/svg.js";
import * as moment from "moment";

/**
 * TimelineAnimation shows a timeline and a running tick when the animation is going on.
 */
export default class TimelineAnimation {
    /**
     * @param {LogAnimation} animation
     * @param {String} uiContainerId: id of the div container
     * @param {Array} caseCountsByFrames
     */
    constructor(animation, uiContainerId, caseCountsByFrames) {
        this.animation = animation;
        this.animationContext = animation.getAnimationContext();

        // Parameters
        this.slotNum = this.animationContext.getTimelineSlots();
        this.endPos = this.slotNum;
        this.slotEngineS = this.animationContext.getLogicalSlotTime(); // in seconds
        this.logMillis = animation.getAnimationContext().getLogEndTime() -
            animation.getAnimationContext().getLogStartTime();
        this.slotDataMs = this.logMillis / this.slotNum;
        this.timeCoef = this.animationContext.getTimelineRatio();

        // Visual settings
        this.slotWidth = 9;
        this.timelineWidth = this.slotNum * this.slotWidth;
        this.logIntervalSize = 5;
        this.logIntervalHeight = 7;
        this.logIntervalMargin = 8;
        this.timelineOffset = {
            x: 20, y: 20,
        };
        this.SHOW_OTHER_LOGS_TIMESPAN = false;
        this.textFont = {size: '11', anchor: 'middle'};

        // Main elements
        this.svgTimeline = $j('#' + uiContainerId)[0];
        this.timelineEl = this._createTimelineElement()
        this.svgTimeline.append(this.timelineEl);
        this.currentSpeedLevel = 1.0;

        // Add components
        this._addTimelineDistribution(caseCountsByFrames);
        this._addLogIntervals();
        this._addTicks();
        this._addCursor();
    }

    getCurrentTime() {
        return this.svgTimeline.getCurrentTime();
    }

    setCurrentTime(time) {
        return this.svgTimeline.setCurrentTime(time);
    }

    isPlaying() {
        //return $j('#pause').hasClass(this.PAUSE_CLS);
        return !this.svgTimeline.animationsPaused();
    }

    isAtStart() {
        let currentLogicalTime = this.getLogicalTimeFromSVGTime(this.getCurrentTime());
        return (currentLogicalTime === 0);
    }

    isAtEnd() {
        let currentLogicalTime = this.getLogicalTimeFromSVGTime(this.getCurrentTime());
        return (currentLogicalTime === this.animationContext.getLogicalTimelineMax());
    }

    pause() {
        this.svgTimeline.pauseAnimations();
    }

    unPause() {
        this.svgTimeline.unpauseAnimations();
    }

    /**
     * Logical time: the time as shown on the timeline when the cursor speed level is 1.
     * Actual time: the actual time of the timeline cursor when its speed is less than or greater than 1.
     * @param logicalTime: in seconds
     * @returns {Number}: in seconds
     */
    getSVGTimeFromLogicalTime(logicalTime) {
        if (logicalTime <= 0) return 0;
        if (logicalTime >= this.animationContext.getLogicalTimelineMax()) return this.totalEngineS;
        return logicalTime/this.currentSpeedLevel;
    }

    /**
     * @param svgTime: in seconds
     * @returns {Number} in seconds
     */
    getLogicalTimeFromSVGTime(svgTime) {
        if (svgTime <= 0) return 0;
        if (svgTime >= this.animationContext.getLogicalTimelineMax()) {
            return this.animationContext.getLogicalTimelineMax();
        }
        return svgTime*this.currentSpeedLevel;
    }

    /**
     * @param logicalTime: in seconds
     * @returns {Number}: in milliseconds
     */
    getLogTimeFromLogicalTime(logicalTime) {
        return this.animationContext.getLogStartTime() + logicalTime * this.animationContext.getTimelineRatio() * 1000;
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
     * @param {Number} newSpeedLevel: the level number on the speed control component
     */
    setSpeedLevel(newSpeedLevel) {
        let speedRatio = newSpeedLevel/this.currentSpeedLevel;
        this.totalEngineS = this.totalEngineS / speedRatio;
        this.slotEngineS = this.slotEngineS / speedRatio;
        this.timeCoef = this.slotDataMs / (this.slotEngineS*1000);
        this._addCursor();
        this.svgTimeline.setCurrentTime(this.svgTimeline.getCurrentTime()/speedRatio);
        this.currentSpeedLevel = newSpeedLevel;
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
    _createTimelineElement() {
        // Create the main timeline container group
        let timelineEl = new SVG.G().attr({
            id: 'timeline',
            style: '-webkit-touch-callout: none; -webkit-user-select: none; -khtml-user-select: none; -moz-user-select: none; -ms-user-select: none; user-select: none',
        }).node;
        return timelineEl;
    }

    _addTick(x, y, tickSize, color, textToTickGap, dateTxt, timeTxt) {
        new SVG.Line().plot(x, y, x, y + tickSize).stroke({color, width: 0.5}).addTo(this.timelineEl);
        y -= textToTickGap;
        new SVG.Text().plain(timeTxt).font(this.textFont).attr({x, y}).addTo(this.timelineEl);
        y -= this.textFont.size * 1.5; // lineHeight
        new SVG.Text().plain(dateTxt).font(this.textFont).attr({x, y}).addTo(this.timelineEl);
    }

    _addTicks() {
        // Add text and line for the bar
        let tickSize = this.logIntervalHeight * (this.animation.getNumberOfLogs() - 1) + 2 * this.logIntervalMargin;
        let textToTickGap = 5;
        let x = this.timelineOffset.x;
        let y = this.timelineOffset.y;
        let time = this.animationContext.getLogStartTime();
        let color;
        let date, dateTxt, timeTxt;
        let skip;

        for (let i = 0; i <= this.slotNum; i++) {
            if (i % 10 === 0) {
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
                this._addTick(x, y, tickSize, color, textToTickGap, dateTxt, timeTxt, this.timelineEl);
            }
            x += this.slotWidth;
            time += this.slotDataMs;
        }
    }

    /**
     * @param {Array} caseCountsByFrames: data with case count for every frame.
     * @private
     */
    _addTimelineDistribution(caseCountsByFrames) {
        // Create a virtual horizontal line
        let startX = this.timelineOffset.x;
        let endX = startX + this.slotNum*this.slotWidth;
        let timelinePathY = this.timelineOffset.y + this.logIntervalMargin;
        let timelinePath = 'm' + startX + ',' + timelinePathY + ' L' + endX + ',' + timelinePathY;
        let timelinePathE = new SVG.Path().plot(timelinePath).attr({fill: 'transparent', stroke: 'none'}).node;
        this.timelineEl.appendChild(timelinePathE);
        let totalLength = timelinePathE.getTotalLength();

        // Set up canvas
        let timelineBox = this.svgTimeline.getBoundingClientRect();
        let ctx = document.querySelector("#timelineCanvas").getContext('2d');
        ctx.canvas.width = timelineBox.width;
        ctx.canvas.height = timelineBox.height;
        ctx.canvas.x = timelineBox.x;
        ctx.canvas.y = timelineBox.y;
        ctx.strokeStyle = '#D3D3D3';
        ctx.lineWidth = 2;
        let matrix = timelinePathE.getCTM();
        ctx.setTransform(matrix.a, matrix.b, matrix.c, matrix.d, matrix.e, matrix.f);

        // Draw distribution
        if (caseCountsByFrames) {
            const MAX_HEIGHT = ctx.canvas.height/4;
            let maxCount = 0;
            for (let count of Object.values(caseCountsByFrames)) {
                if (typeof(count) != 'function' && maxCount < count) {
                    maxCount = count;
                }
            }
            let totalFrames = caseCountsByFrames.length;
            for (let i=0;i<totalFrames;i++) {
                let distance = i/totalFrames;
                let point = timelinePathE.getPointAtLength(totalLength * distance);
                let height = (caseCountsByFrames[i]/maxCount)*MAX_HEIGHT;
                ctx.beginPath();
                ctx.moveTo(point.x, timelinePathY);
                ctx.lineTo(point.x, timelinePathY - height);
                ctx.stroke();
            }
        }
    }

    _addCursor() {
        if (this.cursorEl) this.timelineEl.removeChild(this.cursorEl);

        let x = this.timelineOffset.x;
        let y = this.timelineOffset.y + 5;
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
        cursorAnim.setAttributeNS(null, 'dur', this.slotNum * this.slotEngineS + 's');
        cursorAnim.setAttributeNS(null, 'by', '1');
        cursorAnim.setAttributeNS(null, 'from', x + ',' + y);
        cursorAnim.setAttributeNS(null, 'to', x + this.slotNum * this.slotWidth + ',' + y);
        cursorAnim.setAttributeNS(null, 'fill', 'freeze');

        cursorEl.appendChild(cursorAnim);
        this.timelineEl.appendChild(cursorEl);

        this.cursorEl = cursorEl;
        this.cursorAnim = cursorAnim;

        // Control dragging of the timeline cursor
        let dragging = false;
        let isPlayingBeforeDrag = false;

        cursorEl.addEventListener('mousedown', startDragging.bind(this));
        this.svgTimeline.addEventListener('mouseup', stopDragging.bind(this));
        this.svgTimeline.addEventListener('mouseleave', stopDragging.bind(this));

        function startDragging(evt) {
            isPlayingBeforeDrag = me.animation.isPlaying();
            evt.preventDefault();
            dragging = true;
            me.pause();
        }

        function stopDragging(evt) {
            if (!dragging) return; // Avoid doing the below two times
            if (evt.type === 'mouseleave' && dragging) {
                return;
            }
            dragging = false;
            let logicalTime = getLogicalTimeFromMouseX(evt);
            me.animation.goto(logicalTime);
            if (isPlayingBeforeDrag) {
                me.animation.unPause();
            }
        }

        function getLogicalTimeFromMouseX(evt) {
            let x = getSVGMousePosition(evt).x;
            let dx = x - me.timelineOffset.x;
            return (dx / me.timelineWidth) * me.animationContext.getLogicalTimelineMax();
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

    // Add log intervals to timeline
    _addLogIntervals() {
        let ox = this.timelineOffset.x, y = this.timelineOffset.y + this.logIntervalMargin; // Start offset
        let logSummaries = this.animation.getLogSummaries();
        for (let i = 0; i < logSummaries.length; i++) {
            let log = logSummaries[i];
            let x1 = ox + this.slotWidth * log.startDatePos;
            let x2 = ox + this.slotWidth * log.endDatePos;
            let style = 'stroke: ' + this.animation.getLogColor(i+1, log.color) + '; stroke-width: ' + this.logIntervalSize;
            let opacity = 0.8;
            new SVG.Line().plot(x1, y, x2, y).attr({style, opacity}).addTo(this.timelineEl);

            // Display date label at the two ends
            if (this.SHOW_OTHER_LOGS_TIMESPAN && log.startDatePos % 10 !== 0) {
                let txt = log.startDateLabel.substr(0, 19);
                let x = ox + this.slotWidth * log.startDatePos - 50;
                y += 5;
                new SVG.Text().plain(txt).font(this.textFont).attr({x, y}).addTo(this.timelineEl);
            }
            y += this.logIntervalHeight;
        }
    }
}