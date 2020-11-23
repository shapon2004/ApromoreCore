import LogAnimation from "../../src/loganimation";

describe('After initializing the Log Animator', function () {
    var logAnim;
    beforeEach((done) => {
        jasmine.DEFAULT_TIMEOUT_INTERVAL = 999999;
        jasmine.getFixtures().fixturesPath = 'base/test/loganimation/fixtures';
        loadFixtures('logAnimationUI.html');
        const URL = "http://b3mn.org/stencilset/bpmn2.0#";
        let bpmn = require('./fixtures/simpleMap.bpmn');
        let setupData = require('./fixtures/setupData.txt');
        //jasmine.clock().install();
        logAnim = new LogAnimation(bpmn.default, URL, URL, setupData.default, '111');
        //jasmine.clock().uninstall();

        setTimeout(function() {
            logAnim.initController();
            done();
        }, 2000);
    });

    // afterEach(function() {
    //
    // });

    it('It loads all data successfully and button controls are in ready state', (done) => {
        expect($j("#start").get(0).disabled).toBeFalse();
        expect($j("#pause").get(0).disabled).toBeFalse();
        expect($j("#forward").get(0).disabled).toBeFalse();
        expect($j("#backward").get(0).disabled).toBeFalse();
        expect($j("#end").get(0).disabled).toBeFalse();
        expect($j("#speed-control").get(0).disabled).toBeFalse();
        done();
    });
});