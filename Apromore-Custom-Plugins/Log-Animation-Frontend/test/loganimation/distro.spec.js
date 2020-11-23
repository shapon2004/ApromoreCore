import LogAnimation from "../../src/loganimation";

describe('After loading library distribution', function () {
    beforeEach(function() {
        jasmine.getFixtures().fixturesPath = 'base/test/loganimation/fixtures';
        loadFixtures('logAnimationUI.html');
    });

    it('It should expose the global entry class (LogAnimation)', function() {
        const URL = "http://b3mn.org/stencilset/bpmn2.0#";
        let bpmn = require('./fixtures/simpleMap.bpmn');
        let setupData = require('./fixtures/setupData.txt');
        let logAnim = new LogAnimation(bpmn.default, URL, URL, setupData.default, '111');
        expect(logAnim).not.toEqual(null);
        expect(logAnim).not.toEqual({});
        expect(logAnim.getController()).not.toEqual(null);
        expect(logAnim.getEditor()).not.toEqual(null);
    });

});