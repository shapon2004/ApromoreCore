import AnimationMain from "../../src/loganimation";
import * as testHelper from './helper';

describe('After initializing the Log Animator', function () {
    var logAnimator;
    beforeEach(function() {
        logAnimator = testHelper.createSimpleLogAnimator();
    });

    it('It loads all data successfully and the button controls are in ready state', function() {
        expect(logAnimator).not.toEqual(null);
        expect(logAnimator.editor).not.toEqual(null);
        expect(logAnimator.controller).not.toEqual(null);

        // Main components of Animation Controller
        expect(logAnimator.controller.svgMain).not.toEqual(null);
        expect(logAnimator.controller.svgTimeline).not.toEqual(null);
        expect(logAnimator.controller.svgProgresses).not.toEqual(null);
        expect(logAnimator.controller.tokenAnimation).not.toEqual(null);
        expect($j("#speed-control").slider).not.toEqual(null);

        // Pause state after initialization
        expect(logAnimator.getAnimationController().isPlaying()).toBeFalse();
        expect($j("#start").get(0).disabled).toBeFalse();
        expect($j("#pause").get(0).disabled).toBeFalse();
        expect($j("#forward").get(0).disabled).toBeFalse();
        expect($j("#backward").get(0).disabled).toBeFalse();
        expect($j("#end").get(0).disabled).toBeFalse();
        expect($j("#speed-control").get(0).disabled).toBeFalse();
    });
});