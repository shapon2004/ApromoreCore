import LogAnimation from "../../src/loganimation";

describe('test LogAnimation library', function () {
    beforeEach(function() {
        jasmine.getFixtures().fixturesPath = 'base/test/loganimation/fixtures';
        loadFixtures('test.html');
    });

    it('should expose global library', function() {
        const URL = "http://b3mn.org/stencilset/bpmn2.0#";
        let bpmn = require('./fixtures/simple.bpmn');
        let setupData = require('./fixtures/setupData.json');
        let logAnim = new LogAnimation(bpmn, URL, URL, setupData, '111');
        expect(logAnim).not.toEqual(null);
        expect(logAnim).not.toEqual({});
    });

});