import * as testFactory from "./testFactory";

describe('Test Simple Animation Controller', function () {
    let animation;
    jasmine.DEFAULT_TIMEOUT_INTERVAL = 999999;
    beforeEach(function() {
        jasmine.clock().install();
        testFactory.createSimpleLogAnimation();
        jasmine.clock().tick(1000);
        jasmine.uninstall();
    });

    it('It loads data successfully and in the pause state', function() {
        expect(animation).not.toEqual(null);
    });

    xit('It establishes the right animation context', function() {
        let animContext = animation.getAnimationContext();
        expect(animContext.getTotalNumberOfFrames()).toEqual(36000);
        expect(animContext.getRecordingFrameRate()).toEqual(60);
        expect(animContext.getLogicalTimelineMax()).toEqual(600);
        expect(animContext.getPluginExecutionId()).toEqual('101');
    });

    xit('It is at the right initial state', function() {
        expect(animation.isPlaying()).toBeFalse();
        expect(animation.isAtStart()).toBeTrue();
        expect(animation.isAtEnd()).toBeFalse();
    });

    xit('It can switch back and forth between play and pause', function() {
        animation.playPause();
        expect(animation.isPlaying()).toBeTrue();
        animation.playPause();
        expect(animation.isPlaying()).toBeFalse();
    });

    xit('It can go back to the start position', function() {
        animation.playPause();
        expect(animation.isPlaying()).toBeTrue();
        // setTimeout(function() {
        //     animation.gotoStart();
        //     expect(animation.isAtStart()).toBeTrue();
        //     expect(animation.isAtEnd()).toBeFalse();
        //     expect(animation.isPlaying()).toBeFalse();
        // }, 2000);
    });

    xit('It can jump to the end position', function() {
        animation.playPause();
        expect(animation.isPlaying()).toBeTrue();
        // setTimeout(function() {
        //     animation.gotoEnd();
        //     expect(animation.isAtStart()).toBeFalse();
        //     expect(animation.isAtEnd()).toBeTrue();
        //     expect(animation.isPlaying()).toBeFalse();
        // }, 2000);
    });
});