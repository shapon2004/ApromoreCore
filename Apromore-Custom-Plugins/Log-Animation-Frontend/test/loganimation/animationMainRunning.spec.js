import * as testFactory from "./testFactory";

describe('Test while the animation is running', function () {
    let animation;
    jasmine.DEFAULT_TIMEOUT_INTERVAL = 999999;
    beforeEach((done) => {
        jasmine.clock().install();
        animation = testFactory.createSimpleLogAnimation();
        jasmine.clock().tick(1000);
        jasmine.clock().uninstall();

        animation.playPause();
        setTimeout(function() {
            done();
        }, 2000);
    });

    it('It can play the animation at a correct speed', (done) => {
        animation.playPause(); //pause
        expect(animation.isPlaying()).toBeFalse();
        expect(animation.getCurrentLogicalTime()).toBeCloseTo(2, 0);
        done();
    });

    it('It can jump backward to the START position', function() {
        expect(animation.isPlaying()).toBeTrue();
        animation.gotoStart();
        expect(animation.isAtStart()).toBeTrue();
        expect(animation.isAtEnd()).toBeFalse();
        expect(animation.timeline.isPlaying()).toBeFalse();
    });

    it('It can jump forward to the END position', function() {
        expect(animation.isPlaying()).toBeTrue();
        animation.gotoEnd();
        expect(animation.isAtStart()).toBeFalse();
        expect(animation.isAtEnd()).toBeTrue();
        expect(animation.timeline.isPlaying()).toBeFalse();
    });
});