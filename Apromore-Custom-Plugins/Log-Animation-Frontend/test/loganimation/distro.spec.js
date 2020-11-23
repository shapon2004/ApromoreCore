import LogAnimation from "../../src/loganimation";

xdescribe('test library distribution', function () {
    beforeEach(function() {
        jasmine.getFixtures().fixturesPath = 'base/test/loganimation/fixtures';
        loadFixtures('test.html');
    });

    it('should expose global entry class', function() {
        const URL = "http://b3mn.org/stencilset/bpmn2.0#";
        let bpmn = require('./fixtures/simple.bpmn');
        let setupData = require('./fixtures/setupData.txt');
        let logAnim = new LogAnimation(bpmn.default, URL, URL, setupData.default, '111');
        expect(logAnim).not.toEqual(null);
        expect(logAnim).not.toEqual({});
    });

});