import * as testHelper from "./helper";

describe('Test Simple Animation Controller', function () {
    var animController;
    beforeEach(function() {
        animController = testHelper.createSimpleAnimationController();
    });

    it('It is paused after creation', function() {
        expect(animController.isPlaying()).toBeFalse();
        expect(animController.isAtStart()).toBeTrue();
        expect(animController.isAtEnd()).toBeFalse();
    });

    it('It can switch back and forth between play and pause', function() {
        animController.playPause();
        expect(animController.isPlaying()).toBeTrue();
        animController.playPause();
        expect(animController.isPlaying()).toBeFalse();
    });

    it('It can go back to start', function() {
        animController.playPause();
        expect(animController.isPlaying()).toBeTrue();
        setTimeout(function() {
            animController.gotoStart();
            expect(animController.isAtStart()).toBeTrue();
            expect(animController.isAtEnd()).toBeFalse();
            expect(animController.isPlaying()).toBeFalse();
        }, 2000);
    });

    it('It can jump to end', function() {
        animController.playPause();
        expect(animController.isPlaying()).toBeTrue();
        setTimeout(function() {
            animController.gotoEnd();
            expect(animController.isAtStart()).toBeFalse();
            expect(animController.isAtEnd()).toBeTrue();
            expect(animController.isPlaying()).toBeFalse();
        }, 2000);
    });
});