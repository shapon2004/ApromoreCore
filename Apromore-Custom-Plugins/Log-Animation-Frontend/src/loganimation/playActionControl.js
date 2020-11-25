/**
 * PlayActionControl groups buttons controlling the animation such as start, pause, fast foward, etc.
 */
export default class PlayActionControl{
    /**
     * @param {AnimationController} animationController
     * @param {HTMLButtonElement} gotoStartButton
     * @param {HTMLButtonElement} pauseButton
     * @param {HTMLButtonElement} forwardButton
     * @param {HTMLButtonElement} backwardButton
     * @param {HTMLButtonElement} gotoEndButton
     * @param {String} playClassName: CSS class name of the play state
     * @param {String} pauseClassName: CSS class name of the pause state
     */
    constructor(animationController,
                gotoStartButton, pauseButton, forwardButton,
                backwardButton, gotoEndButton,
                playClassName,
                pauseClassName) {
        this.animationController = animationController;
        this.gotoStartButton = gotoStartButton;
        this.gotoEndButton = gotoEndButton;
        this.pauseButton = pauseButton;
        this.forwardButton = forwardButton;
        this.backwardButton = backwardButton;

        gotoStartButton.addEventListener('click', animationController.gotoStart);
        gotoEndButton.addEventListener('click', animationController.gotoEnd);
        pauseButton.addEventListener('click', animationController.playPause);
        forwardButton.addEventListener('click', animationController.fastForward);
        backwardButton.addEventListener('click', animationController.fastBackward);

        this.PLAY_CLS = playClassName;
        this.PAUSE_CLS = pauseClassName;
    }

    /**
     * @param {Boolean} changeToPlay: true means setting the button to a Play shape, false: set it to a Pause shape.
     */
    setPlayPauseButton(changeToPlay) {
        if (typeof changeToPlay === 'undefined') {
            changeToPlay = !this.animationController.isPlaying();
        }
        if (changeToPlay) {
            this.pauseButton.removeClass(this.PAUSE_CLS).addClass(this.PLAY_CLS);
        } else {
            this.pauseButton.removeClass(this.PLAY_CLS).addClass(this.PAUSE_CLS);
        }
    }
}