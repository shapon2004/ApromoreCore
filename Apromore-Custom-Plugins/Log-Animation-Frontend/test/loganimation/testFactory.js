import LogAnimation from '../../src/loganimation';

export function createSimpleLogAnimation() {
    jasmine.getFixtures().fixturesPath = 'base/test/loganimation/fixtures';
    loadFixtures('logAnimationUI.html');
    let bpmn = require('./fixtures/simpleMap.bpmn');
    let setupData = require('./fixtures/setupData.txt');

    let logAnimation = new LogAnimation('101');
    logAnimation.loadProcessModel(bpmn.default, function() {});
    window.setTimeout(function() {
        logAnimation.initialize(setupData.default);
    }, 1000);
}

/**
 * @returns {TokenAnimation}
 */
export function createSimpleTokenAnimation() {
    return this.createSimpleLogAnimation().tokenAnimation;
}
