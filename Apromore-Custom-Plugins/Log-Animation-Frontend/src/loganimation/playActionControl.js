/**
 * PlayActionControl groups buttons controlling the animation such as start, pause, fast foward, etc.
 *
 * @author Bruce Nguyen
 */
export default class PlayActionControl{
    /**
     * @param {LogAnimation} animation
     * @param {HTMLButtonElement} gotoStartButton
     * @param {HTMLButtonElement} pauseButton
     * @param {HTMLButtonElement} forwardButton
     * @param {HTMLButtonElement} backwardButton
     * @param {HTMLButtonElement} gotoEndButton
     * @param {String} playClassName: CSS class name of the play state
     * @param {String} pauseClassName: CSS class name of the pause state
     */
    constructor(animation,
                gotoStartButton, pauseButton, forwardButton,
                backwardButton, gotoEndButton,
                playClassName,
                pauseClassName) {
        this.animation = animation;
        this.gotoStartButton = gotoStartButton;
        this.gotoEndButton = gotoEndButton;
        this.pauseButton = pauseButton;
        this.forwardButton = forwardButton;
        this.backwardButton = backwardButton;

        gotoStartButton.addEventListener('click', animation.gotoStart);
        gotoEndButton.addEventListener('click', animation.gotoEnd);
        pauseButton.addEventListener('click', animation.playPause);
        forwardButton.addEventListener('click', animation.fastForward);
        backwardButton.addEventListener('click', animation.fastBackward);

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