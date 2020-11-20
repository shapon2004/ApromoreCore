import {require} from './helper';

describe('test LogAnimation', function () {
    beforeEach(function() {
        jasmine.getFixtures().fixturesPath = 'base/test/loganimation/fixtures';
        loadFixtures('test.html');
    });

    it('should expose globals', function() {
        const URL = "http://b3mn.org/stencilset/bpmn2.0#";
        let LogAnimation = logAnimation.LogAnimation;
        let bpmn = require('./fixtures/simple.bpmn');
        let setupData = require('./fixtures/setup.txt');
        let logAnim = new LogAnimation(bpmn, URL, URL, setupData, '111');
        //expect(LogAnimation).to.exist;
        expect(logAnim).to.exist;
    });

});