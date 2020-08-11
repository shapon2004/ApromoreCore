'use strict'

/**
 * Each ElementFrame represents a token on an element at a point in time
 */
class ElementFrame {
    constructor(elementId, index) {
        this._elementId = elementId;
        this._index = index;
    }

    getElementId() {
        return this._elementId;
    }

    getIndex() {
        return this._index;
    }
}

/**
 * A sequence of ElementFrame objects belonging to an elementId over a period of time
 */
class ElementFrames {
    constructor() {
        this._elementId = undefined;
        this._elementFrames = new Array();
        this._frameIndexes = new Array();
    }

    getElementFrames() {
        return this._elementFrames;
    }

    add(elementFrame) {
        if (elementFrame instanceof ElementFrame) {
            if (this._elementId == undefined) {
                this._elementId = elementFrame.getElementId();

            }
            if (this._elementId === elementFrame.getElementId()) {
                this._elementFrames.push(elementFrame);
                this._frameIndexes.push(elementFrame.getIndex());
            }
        }
    }

    getElementId() {
        return this._elementId;
    }

    getFrameIndexes() {
        return this._frameIndexes;
    }
}


/**
 * A frame for a case which contains multiple ElementFrame object
 * For example, a CaseFrame has multiple tokens on a number of
 * sequence flows after an AND split gateway
 */
class CaseFrame {
    constructor(caseId, index) {
        this._caseId = caseId;
        this._index = index;
        this._elementFrames = new Array();
    }

    getCaseId() {
        return this._caseId;
    }

    getIndex() {
        return this._index;
    }

    getElementFrames() {
        return this._elementFrames;
    }

    addElementFrame(elementFrame) {
        if (elementFrame instanceof ElementFrame) {
            this._elementFrames.push(elementFrame);
        }
    }
}

/**
 * A sequence of CaseFrame objects belonging to a CaseId over a period of time
 * It also summarizes the ElementFrame for each elementId over this period
 */
class CaseFrames {
    constructor() {
        this._caseId = undefined;
        this._caseFrames = new Array();
        this._elementFrames = new Map(); // elementId => ElementFrames
    }

    /**
     *
     * @param {CaseFrame} caseFrame
     */
    add(caseFrame) {
        if (caseFrame instanceof CaseFrame) {
            if (this._caseId === undefined) {
                this._caseId = caseFrame.getCaseId();
            }

            if (caseFrame.getCaseId() === this._caseId) {
                this._caseFrames.push(caseFrame);
                for (const elementFrame in caseFrame.getElementFrames()) {
                    if (!this._elementFrames.has(elementFrame.getElementId())) {
                        this._elementFrames.set(elementFrame.getElementId(), new Array());
                    }
                    this._elementFrames.get(elementFrame.getElementId).push(elementFrame);
                }
            }
        }
    }

    getCaseFrames() {
        return this._caseFrames;
    }

    getCaseId() {
        return this._caseId;
    }

    getElementFrames() {
        return this._elementFrames.values;
    }

    getElementIds() {
        return this._elementFrames.keys();
    }

    getElementFramesByElementId(elementId) {
        return this._elementFrames.get(elementId);
    }
}

/**
 * An animation frame contains many CaseFrame objects at a point in time
 * These CaseFrame objects can be of different cases
 */
class Frame {
    /**
     * _caseFrames: map caseId => {CaseFrames}
     */
    constructor (index) {
        this._index = index;
        this._caseFrames = new Map();
    }

    getIndex() {
        return this._index;
    }

    /**
     *
     * @param {String} caseId
     * @param {CaseFrame} caseFrame
     */
    addCaseFrame(caseFrame) {
        if (caseFrame instanceof CaseFrame) {
            this._caseFrames.set(caseFrame.getCaseId(), caseFrame);
        }
    }

    getCaseFrames() {
        return this._caseFrames.values();
    }

    getCaseFramesByCaseId(caseId) {
        return this._caseFrames.get(caseId);
    }

    getCaseIds() {
        return this._caseFrames.keys();
    }
}

/**
 * A sequence of Frame objects
 * It also summarizes CaseFrame objects by caseId from the Frame objects
 */
class Frames {
    /**
     * _frames: array of {Frame}
     * _caseFrames: map caseId => {CaseFrames}
     */
    constructor(startIndex, endIndex) {
        this._startIndex = startIndex;
        this._endIndex = endIndex;
        this._frames = new Array();
        this._caseFrames = new Map();
    }

    getStartIndex() {
        return this._startIndex;
    }

    getEndIndex() {
        return this._endIndex;
    }

    /**
     *
     * @param {Frame} frame
     */
    addFrame(frame) {
        if (frame instanceof Frame) {
            this._frames.push(frame);
            for (const caseId in frame.getCaseIds()) {
                if (!this._caseFrames.has(caseId)) {
                    this._caseFrames.set(caseId, new CaseFrames());
                }
                this._caseFrames.get(caseId).add(caseId, frame.getCaseFramesByCaseId(caseId));
            }
        }
    }

    getFrames () {
        return this._frames;
    }

    getCaseFrames() {
        return this._caseFrames.values();
    }

    getCaseFramesByCaseId(caseId) {
        return this._caseFrames.get(caseId);
    }

    getCaseIds() {
        return this._caseFrames.keys();
    }
}

/**
 * Keep a store of Frames object
 */
class Buffer{
    /**
     *
     * @param {DataRequester} dataRequester
     */
    constructor(dataRequester) {
        this._dataRequester = dataRequester;
        this._chunks = new Queue(); // queue of Frames
        this._repleshThreshold = 1000;
        this._lastIndex = 0;
    }

    isEmpty() {
        return this._chunks.isEmpty();
    }

    size() {
        return this._chunks.getLength();
    }

    /**
     *
     * @returns {Frames}
     */
    readNextChunk() {
        if (!this.isEmpty()) {
            let nextChunk = this._chunks.dequeue();
            if (this.size() - 1 < this._repleshThreshold) {
                this._dataRequester.requestData(this._lastIndex+1, this);
            }
        }
    }

    /**
     *
     * @param {Frames} frames
     */
    writeNextChunk(chunks) {
        this._chunks.enqueue(chunks);
        this._lastIndex = chunks.getEndIndex();
    }

    getRepleshmentLimit() {
        return this._repleshThreshold;
    }
}

class BufferReader {
    /**
     * @param {Buffer} playBuffer
     * @param {Buffer} sourceBuffer
     */
    constructor(playBuffer, sourceBuffer) {
        this._playBuffer = playBuffer;
        this._sourceBuffer = sourceBuffer;
        setTimeout(this._readSourceBuffer, 1000);
    }

    _readSourceBuffer() {
        let chunk = this._sourceBuffer.readNextChunk();
        if (chunk) {
            this._playBuffer.writeNextChunk(chunk);
        }
    }
}



