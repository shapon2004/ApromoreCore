import 'jquery-ui-bundle';
import 'jquery-ui-slider-pips-npm';

/**
 * SpeedControl controls the speed with a slider.
 */
export default class SpeedControl{
    /**
     * @param {LogAnimation} animation
     * @param {String} uiContainerId: id of the containing div
     */
    constructor(animation, uiContainerId) {
        this._animationController = animation;
        let speedControl = $j('#' + uiContainerId);
        this._speedSlider = speedControl.slider({
            orientation: "horizontal",
            step: 1,
            min: 1,
            max: 11,
            value: 5
        });

        let STEP_VALUES = [10, 20, 30, 40, 60, 70, 80, 90, 120, 240, 480];
        speedControl.slider("float", {
            handle: true,
            pips: true,
            labels: true,
            prefix: "",
            suffix: ""
        });

        let lastSliderValue = speedControl.slider("value");
        speedControl.on("slidechange", function(event, ui) {
            this._animationController.setSpeedLevel(STEP_VALUES[ui.value - 1]);
            lastSliderValue = ui.value;
        });
    }
}