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
    }, 1000);
    return logAnimation;
}

/**
 * @returns {TokenAnimation}
 */
export function createSimpleTokenAnimation() {
    return this.createSimpleLogAnimation().tokenAnimation;
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
    spyOn(frameBuffer, '_loopRequestData').and.stub();
    spyOn(frameBuffer, '_loopCleanup').and.stub();
    return frameBuffer;
}

/**
 * @returns {Buffer}
 */
export function createNonEmptyFrameBuffer() {
    jasmine.getFixtures().fixturesPath = 'base/test/loganimation/fixtures';
    let setupDataRaw = require('./fixtures/setupData.txt');
    let chunkRaw = require('./fixtures/chunk1.txt');

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
    spyOn(frameBuffer, '_loopRequestData').and.stub();
    spyOn(frameBuffer, '_loopCleanup').and.stub();
    let frames = JSON.parse(chunkRaw.default);
    frameBuffer.write(frames, 0);
    return frameBuffer;
}
