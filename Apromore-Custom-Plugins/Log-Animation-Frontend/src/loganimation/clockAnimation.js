/**
 * ClockAnimation manages the clock on the animation page.
 * It uses setInterval and javascript to create a clock and sync
 * with the main token animation.
 *
 */
export default class ClockAnimation {
    /**
<<<<<<< HEAD
     * @param {LogAnimation} animationController
=======
     * @param {AnimationController} animationController
>>>>>>> 2791775f869062606402bfdea765a3e517946d0e
     * @param {HTMLElement} dateElement
     * @param {HTMLElement} timeElement
     */
    constructor(animationController, dateElement, timeElement) {
        this._animationController = animationController;
        this._animationContext = animationController.getAnimationContext();
        this._dateElement = dateElement;
        this._timeElement = timeElement;
        this.clockTimer = null; // id of the window timer returned from setInterval.
    }

    unPause() {
        if (!this.clockTimer) clearInterval(this.clockTimer);
        this.clockTimer = setInterval(this._updateClock.bind(this),100);
    }

    pause() {
        if (!this.clockTimer) clearInterval(this.clockTimer);
    }

    setCurrentTime(dateTime) {
        this._updateClockOnce(dateTime);
    }

    /*
     * This method is used to read SVG document current time at every interval based on timer mechanism
     * It stops reading when SVG document time reaches the end of the timeline
     * The end() method is used for ending tasks for the replay completion scenario
     * Thus, the end() method should NOT create a loopback to this method.
     */
    _updateClock() {
        if (this._animationController._getCurrentSVGTime() >= this._animationContext.getLogicalTimelineMax()) {
        } else {
            this._updateClockOnce(this._animationContext.getLogStartTime() +
                                this._animationController._getCurrentSVGTime()*this._animationContext.getTimelineRatio()*1000);
        }
    }

    _updateClockOnce(time) {
        let dateEl = this._dateElement;
        let timeEl = this._timeElement;
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
}