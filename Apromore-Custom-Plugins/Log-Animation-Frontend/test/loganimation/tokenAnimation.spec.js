import * as testHelper from "./helper";

describe('Test Simple Token Animation', function () {
    let tokenAnimation;
    beforeEach(function() {
        tokenAnimation = testHelper.createSimpleTokenAnimation();
    });

    it('It is paused after creation', function() {
        expect(tokenAnimation.isInProgress()).toBeTrue();
        expect(tokenAnimation.isPausing()).toBeTrue();
    });

    it('It can switch back and forth between play and pause', function() {
        tokenAnimation.doUnpause();
        expect(tokenAnimation.isInProgress()).toBeTrue();
        expect(tokenAnimation.isPausing()).toBeFalse();
        tokenAnimation.doPause();
        expect(tokenAnimation.isInProgress()).toBeTrue();
        expect(tokenAnimation.isPausing()).toBeTrue();
    });

    it('It can go back to start', function() {
        //tokenAnimation.do
    });

    it('It can jump to end', function() {

    });
});