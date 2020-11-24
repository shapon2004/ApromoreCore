import LogAnimation from "../../src/loganimation";
import AnimationController from "../../src/loganimation/animationController";

export function createSimpleLogAnimator() {
    jasmine.DEFAULT_TIMEOUT_INTERVAL = 999999;
    jasmine.getFixtures().fixturesPath = 'base/test/loganimation/fixtures';
    loadFixtures('logAnimationUI.html');

    const URL = "http://b3mn.org/stencilset/bpmn2.0#";
    let bpmn = require('./fixtures/simpleMap.bpmn');
    let setupData = require('./fixtures/setupData.txt');
    jasmine.clock().install();
    let logAnimator = new LogAnimation('editorcanvas', bpmn.default, URL, URL, setupData.default, '111');
    jasmine.clock().tick(1000);
    jasmine.clock().uninstall();

    return logAnimator;
}

export function createSimpleAnimationController() {
    return this.createSimpleLogAnimator().getAnimationController();
}
