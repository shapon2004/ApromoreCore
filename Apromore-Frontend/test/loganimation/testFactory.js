import LogAnimation from '../../src/loganimation';
import {AnimationContext} from "../../src/loganimation/animationContextState";
import FrameBuffer from "../../src/loganimation/frameBuffer";
import ProcessModelController from "../../src/loganimation/processmap/processModelController";
import processModelController from "../../src/loganimation/processmap/processModelController";
import TokenAnimation from "../../src/loganimation/tokenAnimation";
import {Apromore} from "../../src/bpmneditor/apromoreEditor";

/**
 * @returns {LogAnimation}
 */
export function createSimpleLogAnimation() {
    jasmine.getFixtures().fixturesPath = 'base/test/loganimation/fixtures';
    loadFixtures('logAnimationUI.html');
    let bpmn = require('./fixtures/simpleMap.bpmn');
    let setupData = require('./fixtures/setupData.txt');

    //Turn off loading plugins as not in scope of testing
    Apromore.CONFIG.PLUGINS_ENABLED = false;

    let logAnimation = new LogAnimation('101');
    logAnimation.loadProcessModel(bpmn.default, function() {});
    window.setTimeout(function() {
        logAnimation.initialize(setupData.default);
        spyOn(logAnimation.getTokenAnimation(), '_loopDraw').and.stub();
        spyOn(logAnimation.getTokenAnimation(), '_loopBufferRead').and.stub();
    }, 1000);
    return logAnimation;
}

export function createFullDataLogAnimation() {
    jasmine.getFixtures().fixturesPath = 'base/test/loganimation/fixtures';
    loadFixtures('logAnimationUI.html');
    let bpmn = require('./fixtures/simpleMap.bpmn');
    let setupData = require('./fixtures/setupData.txt');

    //Turn off loading plugins as not in scope of testing
    Apromore.CONFIG.PLUGINS_ENABLED = false;

    let logAnimation = new LogAnimation('101');
    logAnimation.loadProcessModel(bpmn.default, function() {});
    window.setTimeout(function() {
        logAnimation.initialize(setupData.default);
    }, 1000);
    return logAnimation;
}

/**
 * @returns {FrameBuffer}
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
    let animationContext = new AnimationContext('101', startMs, endMs, timelineSlots,
        totalEngineS, slotEngineUnit, recordingFrameRate);

    let frameBuffer = new FrameBuffer(animationContext);
    return frameBuffer;
}

/**
 * @returns {FrameBuffer}
 */
export function createFullDataFrameBuffer() {
    let frameBuffer = createEmptyFrameBuffer();
    for (let i=1;i<=5;i++) {
        let chunkRaw = require('./fixtures/chunk' + i + '.txt');
        let frames = JSON.parse(chunkRaw.default);
        frameBuffer.write(frames, 0);
    }
    return frameBuffer;
}
