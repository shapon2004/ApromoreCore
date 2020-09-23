
/**
 * Buffer contains array of frames like a stock (or store) containing frames as items.
 * The buffer keeps a DataRequester to communicate with the server for frame supply. It self-manages the stock level
 * by monitoring different stock thresholds such as minimum stock, safety stock, unused stock, etc. Replenishment is
 * the process of stock refilling.
 *
 * The buffer keeps three pointers to the frames:
 * - firstIndex: the buffer index of the first frame
 * - lastIndex: the buffer index of the last frame in the buffer
 * - currentIndex: the buffer index of the first frame in the next chunk available for reading from the buffer
 * The frames from firstIndex to currentIndex-1 is the used stock
 * The frames from currentIndex to lastIndex is the current stock
 * Note that the buffer index is not the frame index.
 * The currentIndex pointer is used to slide back and forth on the buffer to read frames. The buffer can self-manage to get
 * frames from the server when the currentIndex is within the buffer or outside.
 *
 * Reading operation:
 * A read request is only fulfilled if there is available stock (chunkSize).
 * The stock is replenished as soon as it is below the minimum stock to avoid interruptions to the reading operation (i.e.
 * not waiting until the stock is empty to replenish).
 * The replenishment will fill up the buffer until reaching a safety stock level which is higher than the minimum stock level.
 * This is to avoid too many replenishment requests (do it a bit more than enough).
 *
 * Write operation:
 * The write operation adds frames to the end of the buffer.
 *
 * Cleaning operation:
 * The used stock is kept as long as it is under a history threshold, in case it will be read again (for read efficiency)
 * If the used stock is over a history threshold, old frames must be removed out (to avoid out of memory issue).
 *
 * Each frame in the buffer has this format:
 * {
 *      index:10,
 *      elements: [
 *          {elementId1: [{caseId1:[0.1, 1, “#abcd”]}, {caseId2:[0.1, 1, “#abcd”]},…]},
 *          {elementId2: [{caseId1:[0.1, 1, “#abcd”]}, {caseId2:[0.1, 1, “#abcd”]},…]},
 *          ...
 *          {elementIdN: [{caseId1:[0.1, 1, “#abcd”]}, {caseId2:[0.1, 1, “#abcd”]},…]},
 *      ]
 * }
 *
 * @author Bruce Nguyen
 */
class Buffer {
    /**
     *
     * @param {AnimationContext} animationContext
     * @param {Number} chunkSize: the number of frames read from the stock in every request
     * @param {Number} safetyThreshold: the number of remaining frames in the buffer that replenishment must achieve
     * @param {Number} minimumThreshold: minimum stock level before replenishment
     * @param {Number} historyThreshold: the number of old frames kept in the buffer
     */
    constructor(animationContext) {
        this._dataRequester = new DataRequester(animationContext.getPluginExecutionId());
        this._chunkSize = Buffer.DEFAULT_CHUNK_SIZE; //number of frames in every read
        this._safetyThreshold = Buffer.DEFAULT_SAFETY_THRES;
        this._minimumThreshold = Buffer.DEFAULT_MIN_THRES;
        this._historyThreshold = Buffer.DEFAULT_HISTORY_THRES;
        this._serverOutOfFrames = false;

        this._clear();
        this._replenish();
    }

    static get DEFAULT_CHUNK_SIZE() {
        return 120;
    }

    static get DEFAULT_SAFETY_THRES() {
        return 600;
    }

    static get DEFAULT_MIN_THRES() {
        return 300;
    }

    static get DEFAULT_HISTORY_THRES() {
        return 300;
    }

    getSafefyThreshold() {
        return this._safetyThreshold;
    }

    setSafetyThreshold(safetyThreshold) {
        this._safetyThreshold = safetyThreshold;
    }

    getMinimumThreshold() {
        return this._minimumThreshold;
    }

    setMinimumThreshold(minimumThreshold) {
        this._minimumThreshold = minimumThreshold;
    }

    getHistoryThreshold() {
        return this._historyThreshold;
    }

    setHistoryThreshold(historyThreshold) {
        this._historyThreshold = historyThreshold;
    }

    getChunkSize() {
        return this._chunkSize;
    }

    setChunkSize(chunkSize) {
        this._chunkSize = chunkSize;
    }

    _clear() {
        this._frames = [];
        this._currentIndex = -1;

        // A request token is used to control the result in the asynchronous communication with the server.
        // Result returned from previous setting can be recognized from its request token and so not to use it.
        // Only result relevant for the current setting should be used.
        this._requestToken = (!this._requestToken ? 0 : this._requestToken+1);

        this._cancelPendingTasks();
    }

    _cancelPendingTasks() {
        if (this._timerIds && this._timerIds instanceof Array) {
            this._timerIds.forEach(timeOutId => window.clearTimeout(timeOutId));
        }
        this._timerIds = [];
    }

    isEmpty() {
        return (this._frames.length === 0);
    }

    size() {
        return this._frames.length;
    }

    getFirstIndex() {
        return (this.isEmpty() ? -1 : 0);
    }

    getLastIndex() {
        return (this._frames.length-1);
    }

    getCurrentIndex() {
        return this._currentIndex;
    }

    getNextFrameIndex() {
        return (this.isEmpty() ? 0 : this._frames[this.getLastIndex()].index + 1);
    }

    /**
     * The number of frames from currentIndex to lastIndex
     * @returns {number}
     */
    getUnusedStockLevel() {
        if (this.isEmpty()) return 0;
        return (this.getLastIndex() - this._currentIndex + 1);
    }

    /**
     * The number of frames from firstIndex to (currentIndex-1)
     * @returns {number}
     */
    getUsedStockLevel() {
        if (this.isEmpty()) return 0;
        return (this._currentIndex - this.getFirstIndex());
    }

    isSafetyStock() {
        return (this.getUnusedStockLevel() >= this._safetyThreshold);
    }

    isMinimumStock() {
        return (this.getUnusedStockLevel() >= this._minimumThreshold);
    }

    isObsoleteStock() {
        return (this.getUsedStockLevel() > this._historyThreshold);
    }

    // Has unused frames in the buffer
    isStockAvailable() {
        return (this.getUnusedStockLevel() > 0);
    }

    // Has no unused frames in the buffer and cannot provide any more
    isOutOfSupply() {
        return this._serverOutOfFrames && !this.isStockAvailable();
    }

    /**
     * Sequential reading the next chunk from the buffer starting from currentIndex
     * @returns {Array}
     */
    readNext() {
        console.log('Buffer - readNext');
        let frames = [];
        if (this.isStockAvailable()) {
            let lastIndex = this._currentIndex + this._chunkSize - 1;
            lastIndex = (lastIndex <= this.getLastIndex() ? lastIndex : this.getLastIndex());
            for (let i = this._currentIndex; i <= lastIndex; i++) {
                frames.push(this._frames[i]);
            }
            this._currentIndex += frames.length;

            //House keeping
            if (!this.isMinimumStock()) {
                this._replenish();
            }
            if (this.isObsoleteStock()) {
                this._removeObsolete();
            }
        }
        else {
            this._replenish();
        }

        this._logStockLevel();
        return frames;
    }

    /**
     * Move the buffer currentIndex to a frame, e.g when the tick is dragged randomly on the timeline.
     * The frame index corresponds to a buffer index which can be less or greater than the currentIndex, or can
     * be outside the current frames in the buffer. In the latter case, it is too far reaching, the buffer will be
     * cleared and new frames must be read into the buffer starting from the input frame.
     *
     * After the buffer has been cleared, results returned from previous communication with the server may not be used.
     * This is controlled via a request token sent and received in the communication.
     *
     * @param {Number} frameIndex: the frame index
     */
    moveTo(frameIndex) {
        console.log('Buffer - moveTo: frameIndex=' + frameIndex);
        let bufferIndex = this._getBufferIndexFromFrameIndex(frameIndex);
        if (bufferIndex >= 0) {
            console.log('Buffer - moveTo: moveTo point is within buffer with index=' + bufferIndex);
            this._currentIndex = bufferIndex;
            this._replenish();
        }
        else { // the new requested frames are too far outside this buffer
            console.log('Buffer - moveTo: moveTo point is out of buffer, buffer cleared to read new frames');
            this._clear();
            this._replenish(frameIndex);
        }
        this._logStockLevel();
    }

    /**
     * Write a chunk of frames to the buffer
     * The frames are concatenated to the end of the buffer
     * @param {Array} frames
     * @param {Number} batchNumber
     */
    write(frames, requestToken) {
        if (requestToken === this._requestToken) { // don't get old results
            console.log('Buffer - write: valid requestToken, frames accepted, token=' + requestToken);
            if (frames && frames instanceof  Array && frames.length > 0) {
                this._frames = this._frames.concat(frames);
                this._serverOutOfFrames = false;
                if (this._currentIndex < 0) {
                    this._currentIndex = 0;
                }
                //console.log('Added Frames: ' + frames);
                //console.log('Buffer: ' + this._frames);
                this._logStockLevel();
                this._replenish();
            }
            else {
                this._serverOutOfFrames = true;
                console.log('Buffer - write: receive empty result. Server is out of frames.');
            }
        }
        else {
            console.log('Buffer - write: obsolete requestToken, frames rejected, token=' + requestToken);
        }

    }

    /**
     * Convert from frame index to the buffer index
     * This depends on the frame index attribute of the last frame in the buffer
     * @param {Number} frameIndex
     * @private
     */
    _getBufferIndexFromFrameIndex(frameIndex) {
        if (!this.isEmpty()) {
            let firstFrameIndex = this._frames[0].index;
            if (frameIndex >= firstFrameIndex) {
                return (frameIndex - firstFrameIndex);
            }
        }
        return -1;
    }

    /**
     * Buffer replenishment reads data until reaching a safety stock level
     * Replenish - data request - write forms a loop.
     * @param {Number} frameIndex: frame index of the first frame in the chunk to be added to the buffer
     * @private
     */
    _replenish(frameIndex) {
        let _frameIndex = (!frameIndex ? this.getNextFrameIndex() : frameIndex);
        console.log('Buffer - replenish: frameIndex=' + _frameIndex + ', safetyStockThreshold=' + this.getSafefyThreshold());
        if (!this.isSafetyStock() && !this._serverOutOfFrames) {
            console.log('Buffer - replenish: safety stock not yet reached, send request to DataRequester');
            this._dataRequester.requestData(this, this._requestToken, _frameIndex, this._chunkSize);
        }
        else {
            console.log('Buffer - replenish: safety stock REACHED or Server out of frames, stop sending request to DataRequester');
        }
        this._logStockLevel();
    }

    _removeObsolete() {
        console.log('Buffer - removeObsolote: historyThreshold=' + this._historyThreshold);
        this._logStockLevel();
        let obsoleteSize = this.getUsedStockLevel() - this._historyThreshold;
        if (obsoleteSize > 0) {
            console.log('Buffer - remove obsolete frames, amount of removed frames: ' + obsoleteSize);
            this._frames.splice(0, obsoleteSize);
            this._currentIndex -= obsoleteSize;
        }
        else {
            console.log('Buffer - no obsolete frames: ' + obsoleteSize);
        }
    }

    _logStockLevel() {
        console.log('Buffer - currentIndex=' + this._currentIndex);
        console.log('Buffer - lastIndex=' + this.getLastIndex());
        console.log('Buffer - current stock level: ' + this.getUnusedStockLevel());
        console.log('Buffer - current used level: ' + this.getUsedStockLevel());
    }

}



