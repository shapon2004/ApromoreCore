// /**
//  * Each ElementFrame represents a token on an element at a point in time
//  */
// class ElementFrame {
//     constructor(elementId, index) {
//         this._elementId = elementId;
//         this._index = index;
//     }
//
//     getElementId() {
//         return this._elementId;
//     }
//
//     getIndex() {
//         return this._index;
//     }
// }
//
// /**
//  * A sequence of ElementFrame objects belonging to an elementId over a period of time
//  */
// class ElementFrames {
//     constructor() {
//         this._elementId = undefined;
//         this._elementFrames = new Array();
//         this._frameIndexes = new Array();
//     }
//
//     getElementFrames() {
//         return this._elementFrames;
//     }
//
//     add(elementFrame) {
//         if (elementFrame instanceof ElementFrame) {
//             if (this._elementId == undefined) {
//                 this._elementId = elementFrame.getElementId();
//
//             }
//             if (this._elementId === elementFrame.getElementId()) {
//                 this._elementFrames.push(elementFrame);
//                 this._frameIndexes.push(elementFrame.getIndex());
//             }
//         }
//     }
//
//     getElementId() {
//         return this._elementId;
//     }
//
//     getFrameIndexes() {
//         return this._frameIndexes;
//     }
// }
//
//
// /**
//  * A frame for a case which contains multiple ElementFrame object
//  * For example, a CaseFrame has multiple tokens on a number of
//  * sequence flows after an AND split gateway.
//  */
// class CaseFrame {
//     constructor(caseId, index) {
//         this._caseId = caseId;
//         this._index = index;
//         this._elementFrames = new Array();
//     }
//
//     getCaseId() {
//         return this._caseId;
//     }
//
//     getIndex() {
//         return this._index;
//     }
//
//     getElementFrames() {
//         return this._elementFrames;
//     }
//
//     addElementFrame(elementFrame) {
//         if (elementFrame instanceof ElementFrame) {
//             this._elementFrames.push(elementFrame);
//         }
//     }
// }
//
// /**
//  * A sequence of CaseFrame objects belonging to a CaseId over a period of time
//  * It also summarizes the ElementFrame for each elementId over this period
//  */
// class CaseFrames {
//     constructor() {
//         this._caseId = undefined;
//         this._caseFrames = new Array();
//         this._elementFrames = new Map(); // elementId => ElementFrames
//     }
//
//     /**
//      *
//      * @param {CaseFrame} caseFrame
//      */
//     add(caseFrame) {
//         if (caseFrame instanceof CaseFrame) {
//             if (this._caseId === undefined) {
//                 this._caseId = caseFrame.getCaseId();
//             }
//
//             if (caseFrame.getCaseId() === this._caseId) {
//                 this._caseFrames.push(caseFrame);
//                 for (const elementFrame in caseFrame.getElementFrames()) {
//                     if (!this._elementFrames.has(elementFrame.getElementId())) {
//                         this._elementFrames.set(elementFrame.getElementId(), new Array());
//                     }
//                     this._elementFrames.get(elementFrame.getElementId).push(elementFrame);
//                 }
//             }
//         }
//     }
//
//     getCaseFrames() {
//         return this._caseFrames;
//     }
//
//     getCaseId() {
//         return this._caseId;
//     }
//
//     getElementFrames() {
//         return this._elementFrames.values;
//     }
//
//     getElementIds() {
//         return this._elementFrames.keys();
//     }
//
//     getElementFramesByElementId(elementId) {
//         return this._elementFrames.get(elementId);
//     }
// }

class FrameToken {
    /**
     *
     * @param {String} elementId
     * @param {String} caseId
     * @param {Number} distance
     * @param {Number} size
     * @param {String} colorCode
     */
    constructor(elementId, caseId, distance, size, colorCode) {
        this._elementId = elementId;
        this._caseId = caseId;
        this._distance = distance;
        if (size) {
            this._size = size;
        }
        if (colorCode) {
            this._colorCode = colorCode
        }

        this._frameIndex = undefined;
    }

    getKey() {
        return this._key;
    }

    getElementId() {
        return this._elementId;
    }

    getCaseId() {
        return this._caseId;
    }

    getDistance() {
        return this._distance;
    }

    getSize() {
        if (this._size) {
            return this._size;
        }
    }

    getColorCode() {
        if (this._colorCode) {
            return this._colorCode;
        }
    }

    getFrameIndex() {
        return this._frameIndex;
    }

    setFrameIndex(frameIndex) {
        this._frameIndex = frameIndex;
    }

}

/**
 * An animation frame contains many CaseFrame objects at a point in time
 * These CaseFrame objects can be of different cases
 * Each frame is identified by an index starting from 0.
 */
class Frame {
    /**
     * _caseFrames: map caseId => {CaseFrames}
     */
    constructor (index) {
        this._index = index;
        this._tokens = new Map(); //tokenKey => FrameToken
    }

    getIndex() {
        return this._index;
    }

    /**
     *
     * @param {FrameToken}
     */
    addToken(token) {
        if (token instanceof FrameToken) {
            this._tokens.set(token.getKey(), token);
            token.setFrameIndex(this._index);
        }
    }

    /**
     *
     * @returns {IterableIterator<FrameToken>}
     */
    getTokens() {
        return this._tokens.values();
    }

    /**
     *
     * @param tokenKey
     * @returns {FrameToken}
     */
    getToken(tokenKey) {
        return this._tokens.get(tokenKey);
    }
}

// /**
//  * A sequence of Frame objects
//  * It also summarizes CaseFrame objects by caseId from the Frame objects
//  */
// class Frames {
//     /**
//      * _frames: array of {Frame}
//      * _caseFrames: map caseId => {CaseFrames}
//      */
//     constructor(startIndex, endIndex) {
//         this._startIndex = startIndex;
//         this._endIndex = endIndex;
//         this._frames = new Array();
//         this._caseFrames = new Map();
//     }
//
//     getStartIndex() {
//         return this._startIndex;
//     }
//
//     getEndIndex() {
//         return this._endIndex;
//     }
//
//     /**
//      *
//      * @param {Frame} frame
//      */
//     addFrame(frame) {
//         if (frame instanceof Frame) {
//             this._frames.push(frame);
//             for (const caseId in frame.getCaseIds()) {
//                 if (!this._caseFrames.has(caseId)) {
//                     this._caseFrames.set(caseId, new CaseFrames());
//                 }
//                 this._caseFrames.get(caseId).add(caseId, frame.getCaseFramesByCaseId(caseId));
//             }
//         }
//     }
//
//     getFrames () {
//         return this._frames;
//     }
//
//     getCaseFrames() {
//         return this._caseFrames.values();
//     }
//
//     getCaseFramesByCaseId(caseId) {
//         return this._caseFrames.get(caseId);
//     }
//
//     getCaseIds() {
//         return this._caseFrames.keys();
//     }
// }

/**
 * Buffer is an array of frames
 */
class Buffer {
    /**
     *
     * @param {DataRequester} dataRequester
     * @param {Number} chunkSize
     * @param {Number} repleshThreshold
     * @param {Number} obsoleteThreshold
     */
    constructor(dataRequester, chunkSize, repleshThreshold, obsoleteThreshold) {
        this._dataRequester = dataRequester;
        this._chunkSize = chunkSize; //number of frames
        this._repleshThreshold = repleshThreshold;
        this._obsoleteThreshold = obsoleteThreshold;
        this._frames = [];
        this._firstIndex = 0; //the index of the first frame in this buffer
        this._currentIndex = 0; //the index of he current frame
        this._lastIndex = -1; //the index of the last frame

    }

    isEmpty() {
        return this._frames.length === 0;
    }

    size() {
        return this._frames.length;
    }

    getFirstIndex() {
        return this._firstIndex;
    }

    getCurrentIndex() {
        return this._currentIndex;
    }

    getLastIndex() {
        return this._lastIndex;
    }

    /**
     *
     * @returns {[Frame]}
     */
    readNextChunk() {
        return this.readChunkAtIndex(this._currentIndex);
    }

    readChunkAtIndex(frameIndex) {
        if (frameIndex >= this._firstIndex) {
            let frames = [];
            if (!this.isEmpty()) {
                for (let i = frameIndex; i <= this._lastIndex; i++) {
                    frames.push(this._frames[i]);
                    if (frames.length == this._chunkSize) {
                        this._currentIndex = i + 1;
                        break;
                    }
                }
                this._requestData();
                this._removeObsolete();
                return frames;
            }
        }
    }

    /**
     *
     * @param {[Frame]} frames
     */
    writeNextChunk(frames) {
        this._frames.push(frames);
        this._lastIndex = this._frames.length-1;
    }


    _requestData() {
        let remainingSize = this._lastIndex - this._currentIndex + 1;
        if (remainingSize < this._repleshThreshold) {
            this._dataRequester.fetchData(this._lastIndex + 1);
        }
    }

    _removeObsolete() {
        let pastSize = this._currentIndex - this._firstIndex;
        if (pastSize >= this._obsoleteThreshold) {
            this._frames.splice(0, pastSize);
        }
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
        setInterval(this._readSourceBuffer, 1000);
    }

    _readSourceBuffer() {
        let chunk = this._sourceBuffer.readNextChunk();
        if (chunk) {
            this._playBuffer.writeNextChunk(chunk);
        }
    }
}



