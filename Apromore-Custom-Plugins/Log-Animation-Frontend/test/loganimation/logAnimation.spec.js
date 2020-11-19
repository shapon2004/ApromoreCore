describe('test LogAnimation', function () {
    beforeEach(function() {
        jasmine.getFixtures().fixturesPath = 'base/test/loganimation/fixtures';
        loadFixtures('test.html');
    });

    it('should expose globals', function() {
        let LogAnimation = logAnimation.LogAnimation;
        // then
        //expect(LogAnimation).to.exist;
        //expect(new LogAnimation()).to.exist;
    });

});