import * as SVG from "@svgdotjs/svg.js";

/**
 * ProgressAnimation manages the progress indicator of the animation.
 *
 * @author Bruce Nguyen
 */
export default class ProgressAnimation {
    /**
     * @param {LogAnimation}animation
     * @param {HTMLDivElement} uiTopContainer
     * @param {HTMLDivElement} uiPopupContainer
     */
    constructor(animation, uiTopContainer, uiPopupContainer) {
        this._ANIMATION_CLASS = 'progress-animation';
        this._SVG_NS = "http://www.w3.org/2000/svg";
        this._animationController = animation;
        this._svgProgresses = this._createProgressIndicators(animation.getLogSummaries(), uiTopContainer, 1.0);
        this._createLogInfoPopups(animation.getLogSummaries(), uiTopContainer, uiPopupContainer);
        this._currentSpeedLevel = 1.0;
    }

    setCurrentTime(time) {
        for (let svg in this._svgProgresses) {
            svg.setCurrentTime(time);
        }
    }

    pause() {
        for (let svg in this._svgProgresses) {
            svg.pauseAnimations();
        }
    }

    unPause() {
        for (let svg in this._svgProgresses) {
            svg.unPauseAnimations();
        }
    }
    /**
     * @see TimelineAnimation.setSpeedLevel()
     * @param {Number} newSpeedLevel: the level number on the speed control component
     */
    setSpeedLevel(newSpeedLevel) {
        let speedRatio = newSpeedLevel/this._currentSpeedLevel;
        let animations = $j('.' + this._ANIMATION_CLASS);
        for (let i = 0; i < animations.length; i++) {
            let animateEl = animations[i];
            let curDur = animateEl.getAttribute('dur');
            curDur = curDur.substr(0, curDur.length - 1);
            animateEl.setAttributeNS(null,'dur', curDur/speedRatio + 's');
            let curBegin = animateEl.getAttribute('begin');
            curBegin = curBegin.substr(0, curBegin.length - 1);
            animateEl.setAttributeNS(null, 'begin', curBegin / speedRatio + 's');
            this._svgProgresses[i].setCurrentTime(this._svgProgresses[i].getCurrentTime()/speedRatio);
        }
        this._currentSpeedLevel = newSpeedLevel;
    }

    /**
     *
     * @param {Array} logSummaries
     * @param {HTMLDivElement} uiTopContainer
     * @param {Number} speedRatio
     * @return {SVGElement[]}
     * @private
     */
    _createProgressIndicators(logSummaries, uiTopContainer, speedRatio) {
        let log;
        let svgProgress, svgProgresses = [];
        let progressTopContainer = uiTopContainer;

        progressTopContainer.empty();
        for (let i = 0; i < logSummaries.length; i++) {
            log = logSummaries[i];
            svgProgress = $j(`<svg id="progressbar-${i}  xmlns="${this._SVG_NS}" viewBox="-10 0 20 40" ></svg>`);
            progressTopContainer.append(
                $j(`<div id="progress-c-${i}"></div>`).append(
                    svgProgress.append(this._createProgressIndicatorsForLog(i + 1, log, speedRatio)),
                ).append($j(`<div class="label">${log.filename}</div>`)),
            );
            svgProgress = svgProgress[0];
            svgProgresses.push(svgProgress);
        }

        return svgProgresses;
    }

    /*
     * Create progress indicator for one log
     * log: the log object (name, color, traceCount, progress, tokenAnimations)
     * x,y: the coordinates to draw the progress bar
     */
    /**
     * Create progress indicator for one log
     * @param logNo: ordinal number of one log
     * @param log: log summary data
     * @param speedRatio
     * @returns {*}
     * @private
     */
    _createProgressIndicatorsForLog(logNo, log, speedRatio) {
        speedRatio = speedRatio || 1;
        let {values, keyTimes, begin, dur} = log.progress;
        let color = this._animationController.getLogColor(logNo, log.color);
        let progress = new SVG.G().attr({
            id: 'ap-la-progress-' + logNo,
        }).node;

        let path = 'M ' + 0 + ',' + 0 + ' m 0, 0 a 20,20 0 1,0 0.00001,0';
        let pie = new SVG.Path().plot(path).attr({
            fill: color,
            'fill-opacity': 0.5,
            stroke: color,
            'stroke-width': '5',
            'stroke-dasharray': '0 126 126 0',
            'stroke-dashoffset': '1',
        }).node;

        let pieAnim = document.createElementNS(SVG_NS, 'animate');
        pieAnim.setAttributeNS(null, 'class', this._ANIMATION_CLASS );
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

    /**
     * Create a popup window when hovering the mouse over the progress indicator.
     * @param {Array} logSummaries
     * @param {HTMLDivElement} uiTopContainer
     * @param {HTMLDivElement} uiPopupContainer
     * @private
     */
    _createLogInfoPopups(logSummaries, uiTopContainer, uiPopupContainer) {
        let logInfo = uiPopupContainer;
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

        for (let i = 0; i < logSummaries.length; i++) {
            let pId = '#' + uiTopContainer.id + '-' + (i + 1);
            $j(pId).hover(
                (function(idx) {
                    let log = logSummaries[idx - 1];
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
}