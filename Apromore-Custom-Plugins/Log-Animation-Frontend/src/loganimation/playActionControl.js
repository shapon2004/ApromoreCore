/**
 * PlayActionControl groups buttons controlling the animation such as start, pause, fast foward, etc.
 *
 * @author Bruce Nguyen
 */
export default class PlayActionControl{
    /**
     * @param {LogAnimation} animation
     * @param {String} gotoStartButtonId: button id
     * @param {String} pauseButtonId
     * @param {String} forwardButtonId
     * @param {String} backwardButtonId
     * @param {String} gotoEndButtonId
     * @param {String} playClassName: CSS class name of the play state
     * @param {String} pauseClassName: CSS class name of the pause state
     */
    constructor(animation,
                gotoStartButtonId,
                pauseButtonId,
                forwardButtonId,
                backwardButtonId,
                gotoEndButtonId,
                playClassName,
                pauseClassName) {
        this.animation = animation;
        this.gotoStartButton = $j('#' + gotoStartButtonId);
        this.pauseButton = $j('#' + pauseButtonId);
        this.forwardButton = $j('#' + forwardButtonId);
        this.backwardButton = $j('#' + backwardButtonId);
        this.gotoEndButton = $j('#' + gotoEndButtonId);

        this.gotoStartButton.on('click', animation.gotoStart.bind(animation));
        this.gotoEndButton.on('click', animation.gotoEnd.bind(animation));
        this.pauseButton.on('click', animation.playPause.bind(animation));
        this.forwardButton.on('click', animation.fastForward.bind(animation));
        this.backwardButton.on('click', animation.fastBackward.bind(animation));

        this.PLAY_CLS = playClassName;
        this.PAUSE_CLS = pauseClassName;
    }

    /**
     * @param {Boolean} changeToPlay: true means setting the button to a Play shape, false: set it to a Pause shape.
     */
    setPlayPauseButton(changeToPlay) {
        if (typeof changeToPlay === 'undefined') {
            changeToPlay = !this.animation.isPlaying();
        }
        if (changeToPlay) {
            this.pauseButton.removeClass(this.PAUSE_CLS).addClass(this.PLAY_CLS);
        } else {
            this.pauseButton.removeClass(this.PLAY_CLS).addClass(this.PAUSE_CLS);
        }
    }
}