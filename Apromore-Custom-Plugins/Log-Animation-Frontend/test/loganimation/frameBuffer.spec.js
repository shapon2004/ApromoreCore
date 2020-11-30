import * as testFactory from "./testFactory";

describe('After creating an empty Buffer', function () {
    /** @type Buffer */
    let frameBuffer;
    beforeEach(function() {
        frameBuffer = testFactory.createEmptyFrameBuffer();
    });

    it('It sets up initial state correctly', function() {
        expect(frameBuffer.getFirstIndex()).toEqual(-1);
        expect(frameBuffer.getLastIndex()).toEqual(-1);
        expect(frameBuffer.getCurrentIndex()).toEqual(-1);
        expect(frameBuffer.getUsedStockLevel()).toEqual(0);
        expect(frameBuffer.getUnusedStockLevel()).toEqual(0);
        expect(frameBuffer.isSequentialMode()).toBeTrue();
        expect(frameBuffer.isStockAvailable()).toBeFalse();
        expect(frameBuffer.isSafetyStock()).toBeFalse();
        expect(frameBuffer.isEmpty()).toBeTrue();
        expect(frameBuffer.isObsoleteStock()).toBeFalse();
        expect(frameBuffer.isOutOfSupply()).toBeFalse();
    });
});


describe('After writing frames to a Buffer', function () {
    /** @type Buffer */
    let frameBuffer;
    beforeEach(function() {
        frameBuffer = testFactory.createEmptyFrameBuffer();
        let chunkRaw = require('./fixtures/chunk1.txt');
        let frames = JSON.parse(chunkRaw.default);
        frameBuffer.write(frames, 0);
    });

    it('The frames are written correctly', function() {
        expect(frameBuffer.getFirstIndex()).toEqual(0);
        expect(frameBuffer.getLastIndex()).toEqual(299);
        expect(frameBuffer.getCurrentIndex()).toEqual(0);
        expect(frameBuffer.getUsedStockLevel()).toEqual(0);
        expect(frameBuffer.getUnusedStockLevel()).toEqual(300);
        expect(frameBuffer.isSequentialMode()).toBeTrue();
        expect(frameBuffer.isStockAvailable()).toBeTrue();
        expect(frameBuffer.isSafetyStock()).toBeFalse();
        expect(frameBuffer.isEmpty()).toBeFalse();
        expect(frameBuffer.isObsoleteStock()).toBeFalse();
        expect(frameBuffer.isOutOfSupply()).toBeFalse();
    });

    it('Next frames are added correctly', function() {
        let chunkRaw = require('./fixtures/chunk2.txt');
        let frames = JSON.parse(chunkRaw.default);
        frameBuffer.write(frames, 0);

        expect(frameBuffer.getFirstIndex()).toEqual(0);
        expect(frameBuffer.getLastIndex()).toEqual(599);
        expect(frameBuffer.getCurrentIndex()).toEqual(0);
        expect(frameBuffer.getUsedStockLevel()).toEqual(0);
        expect(frameBuffer.getUnusedStockLevel()).toEqual(600);
        expect(frameBuffer.isSequentialMode()).toBeTrue();
        expect(frameBuffer.isStockAvailable()).toBeTrue();
        expect(frameBuffer.isSafetyStock()).toBeFalse();
        expect(frameBuffer.isEmpty()).toBeFalse();
        expect(frameBuffer.isObsoleteStock()).toBeFalse();
        expect(frameBuffer.isOutOfSupply()).toBeFalse();
    });

    it('Frames are read correctly', function() {
        let chunkRaw = require('./fixtures/chunk2.txt');
        let frames = JSON.parse(chunkRaw.default);
        frameBuffer.write(frames, 0);

        frameBuffer.readNextChunk();
        expect(frameBuffer.getFirstIndex()).toEqual(0);
        expect(frameBuffer.getLastIndex()).toEqual(599);
        expect(frameBuffer.getCurrentIndex()).toEqual(300);
        expect(frameBuffer.getUsedStockLevel()).toEqual(300);
        expect(frameBuffer.getUnusedStockLevel()).toEqual(300);
        expect(frameBuffer.isSequentialMode()).toBeTrue();
        expect(frameBuffer.isStockAvailable()).toBeTrue();
        expect(frameBuffer.isSafetyStock()).toBeFalse();
        expect(frameBuffer.isEmpty()).toBeFalse();
        expect(frameBuffer.isObsoleteStock()).toBeFalse();
        expect(frameBuffer.isOutOfSupply()).toBeFalse();
    });

    it('Frames are read correctly until the buffer is empty', function() {
        let chunkRaw = require('./fixtures/chunk2.txt');
        let frames = JSON.parse(chunkRaw.default);
        frameBuffer.write(frames, 0);

        frameBuffer.readNextChunk();
        frameBuffer.readNextChunk();
        expect(frameBuffer.getFirstIndex()).toEqual(0);
        expect(frameBuffer.getLastIndex()).toEqual(599);
        expect(frameBuffer.getCurrentIndex()).toEqual(600);
        expect(frameBuffer.getUsedStockLevel()).toEqual(600);
        expect(frameBuffer.getUnusedStockLevel()).toEqual(0);
        expect(frameBuffer.isSequentialMode()).toBeTrue();
        expect(frameBuffer.isStockAvailable()).toBeFalse();
        expect(frameBuffer.isSafetyStock()).toBeFalse();
        expect(frameBuffer.isEmpty()).toBeFalse();
        expect(frameBuffer.isObsoleteStock()).toBeFalse();
        expect(frameBuffer.isOutOfSupply()).toBeFalse();
    });
});