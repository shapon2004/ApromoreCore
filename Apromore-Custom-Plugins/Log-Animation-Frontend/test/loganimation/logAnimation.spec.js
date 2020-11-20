
describe('test LogAnimation', function () {
    beforeEach(function() {
        jasmine.getFixtures().fixturesPath = 'base/test/loganimation/fixtures';
        loadFixtures('test.html');
    });

    it('should expose globals', function() {
        const URL = "http://b3mn.org/stencilset/bpmn2.0#";
        let LogAnimation = logAnimation.LogAnimation;
        let bpmn = require('./fixtures/simple.bpmn');
        //console.log(bpmn);
        let setupData = require('./fixtures/setupData.json');
        //console.log(setupData);
        let logAnim = new LogAnimation(bpmn, URL, URL, setupData, '111');
        //expect(LogAnimation).to.exist;
        //expect(logAnimation).to.exist;
    });

});