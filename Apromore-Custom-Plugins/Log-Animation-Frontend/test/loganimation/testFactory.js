import LogAnimation from '../../src/loganimation/animationMain';

/**
 * @returns {LogAnimation}
 */
export function createSimpleLogAnimation() {
    jasmine.DEFAULT_TIMEOUT_INTERVAL = 999999;
    jasmine.getFixtures().fixturesPath = 'base/test/loganimation/fixtures';
    loadFixtures('logAnimationUI.html');

    const URL = "http://b3mn.org/stencilset/bpmn2.0#";
    let bpmn = require('./fixtures/simpleMap.bpmn');
    let setupData = require('./fixtures/setupData.txt');
    jasmine.clock().install();
    let logAnimation = new LogAnimation('101', setupData.default, bpmn.default);
    jasmine.clock().tick(1000);
    jasmine.clock().uninstall();
    return logAnimation;
}

/**
 * @returns {TokenAnimation}
 */
export function createSimpleTokenAnimation() {
    return this.createSimpleLogAnimation().tokenAnimation;
}
