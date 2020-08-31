
/**
 * Buffer is an array of frames
 */
class Buffer {
    /**
     *
     * @param {DataRequester} dataRequester
     * @param {Number} chunkSize
     * @param {Number} replenishThreshold
     * @param {Number} obsoleteThreshold
     */
    constructor(dataRequester, chunkSize, replenishThreshold, obsoleteThreshold) {
        this._dataRequester = dataRequester;
        this._chunkSize = chunkSize; //number of frames
        this._replenishThreshold = replenishThreshold;
        this._obsoleteThreshold = obsoleteThreshold;
        this._frames = [];
        this._firstIndex = 0; //the index of the first frame in this buffer
        this._currentIndex = -1 //the index of he current frame
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

    setCurrentIndex(currentIndex) {
        this._currentIndex = currentIndex;
        this._requestData();
    }

    getLastIndex() {
        return this._lastIndex;
    }

    /**
     *
     * @returns {Array}
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
        else {
            this._requestData();
        }
    }

    /**
     *
     * @param {Array} frames
     */
    writeNextChunk(frames) {
        this._frames.concat(frames);
        this._lastIndex = this._frames.length-1;
        if (this._currentIndex < 0) {
            this._currentIndex = 0;
        }
    }

    _requestData() {
        let remainingSize = this._lastIndex - this._currentIndex + 1;
        if (remainingSize < this._replenishThreshold) {
            this._dataRequester.requestData(this._lastIndex + 1, this);
        }
    }

    _removeObsolete() {
        let pastSize = this._currentIndex - this._firstIndex;
        if (pastSize >= this._obsoleteThreshold) {
            this._frames.splice(0, pastSize);
        }
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



