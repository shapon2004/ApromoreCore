import LogAnimation from '../../src/loganimation';
import {AnimationContext} from "../../src/loganimation/animationContextState";
import Buffer from "../../src/loganimation/frameBuffer";

/**
 * @returns {LogAnimation}
 */
export function createSimpleLogAnimation() {
    jasmine.getFixtures().fixturesPath = 'base/test/loganimation/fixtures';
    loadFixtures('logAnimationUI.html');
    let bpmn = require('./fixtures/simpleMap.bpmn');
    let setupData = require('./fixtures/setupData.txt');

    let logAnimation = new LogAnimation('101');
    logAnimation.loadProcessModel(bpmn.default, function() {});
    window.setTimeout(function() {
        logAnimation.initialize(setupData.default);
        spyOn(logAnimation.tokenAnimation, '_loopDraw').and.stub();
        spyOn(logAnimation.tokenAnimation, '_loopBufferRead').and.stub();
    }, 1000);
    return logAnimation;
}

export function createFullDataLogAnimation() {
    jasmine.getFixtures().fixturesPath = 'base/test/loganimation/fixtures';
    loadFixtures('logAnimationUI.html');
    let bpmn = require('./fixtures/simpleMap.bpmn');
    let setupData = require('./fixtures/setupData.txt');

    let logAnimation = new LogAnimation('101');
    logAnimation.loadProcessModel(bpmn.default, function() {});
    window.setTimeout(function() {
        logAnimation.initialize(setupData.default);
        spyOn(logAnimation.tokenAnimation._frameBuffer, '_loopRequestData').and.stub();
    }, 1000);
    return logAnimation;
}

/**
 * @returns {TokenAnimation}
 */
export function createFullDataTokenAnimation() {
    return this.createFullDataLogAnimation().tokenAnimation;
}

/**
 * @returns {Buffer}
 */
export function createEmptyFrameBuffer() {
    jasmine.getFixtures().fixturesPath = 'base/test/loganimation/fixtures';
    let setupDataRaw = require('./fixtures/setupData.txt');

    let setupData = JSON.parse(setupDataRaw.default);
    let {recordingFrameRate} = setupData;
    let startMs = new Date(setupData.timeline.startDateLabel).getTime(); // Start date in milliseconds
    let endMs = new Date(setupData.timeline.endDateLabel).getTime(); // End date in milliseconds
    let totalEngineS = setupData.timeline.totalEngineSeconds;
    let slotEngineUnit = setupData.timeline.slotEngineUnit;
    let timelineSlots = setupData.timeline.timelineSlots;

    let animationContext = new AnimationContext(this.pluginExecutionId, startMs, endMs, timelineSlots,
        totalEngineS, slotEngineUnit, recordingFrameRate);
    let frameBuffer = new Buffer(animationContext);
    return frameBuffer;
}

/**
 * @returns {Buffer}
 */
export function createNonEmptyFrameBuffer() {
    jasmine.getFixtures().fixturesPath = 'base/test/loganimation/fixtures';
    let setupDataRaw = require('./fixtures/setupData.txt');
    let chunkRaw = require('./fixtures/chunk1.txt');
    let frames = JSON.parse(chunkRaw.default);
    let frameBuffer = createEmptyFrameBuffer();
    frameBuffer.write(frames, 0);
    return frameBuffer;
}

/**
 * @returns {Buffer}
 */
export function createFullDataFrameBuffer() {
    jasmine.getFixtures().fixturesPath = 'base/test/loganimation/fixtures';
    let setupDataRaw = require('./fixtures/setupData.txt');
    let frames = JSON.parse(chunkRaw.default);
    let frameBuffer = createEmptyFrameBuffer();
    for (let i=1;i<=5;i++) {
        let chunkRaw = require('./fixtures/chunk' + i + '.txt');
        let frames = JSON.parse(chunkRaw.default);
        frameBuffer.write(frames, 0);
    }
    return frameBuffer;
}
