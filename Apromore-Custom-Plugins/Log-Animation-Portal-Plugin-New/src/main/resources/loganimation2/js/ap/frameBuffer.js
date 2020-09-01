
/**
 * Buffer contains array of frames like a stock (or store).
 * The buffer keeps three pointers to the frames:
 * - firstIndex: the buffer index of the first frame
 * - lastIndex: the buffer index of the last frame in the buffer
 * - currentIndex: the buffer index of the first frame in the next chunk available for reading from the buffer
 * The frames from firstIndex to currentIndex-1 is the used stock
 * The frames from currentIndex to lastIndex is the current stock
 * Note that the buffer index is not the frame index.
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
 * The used stock is kept in the buffer as long as it is under a history threshold, in case it will be read again (for read efficiency)
 * If the used stock is over a history threshold, old frames must be removed out (to avoid out of memory).
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
        this.clear();
    }

    static get DEFAULT_CHUNK_SIZE() {
        return 300;
    }

    static get DEFAULT_SAFETY_THRES() {
        return 50;
    }

    static get DEFAULT_MIN_THRES() {
        return 20;
    }

    static get DEFAULT_HISTORY_THRES() {
        return 50;
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

    setChunkSize(chunkSize) {
        this._chunkSize = chunkSize;
    }

    clear() {
        this._frames = [];
        this._firstIndex = -1;
        this._currentIndex = -1;
        if (!this._requestToken) {
            this._requestToken = 0;
        }
        else {
            this._requestToken++;
        }
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

    getLastIndex() {
        return (this._frames.length-1);
    }

    /**
     * The number of frames from currentIndex to lastIndex
     * @returns {number}
     */
    getCurrentStockLevel() {
        if (this.isEmpty()) return 0;
        return (this.getLastIndex() - this._currentIndex + 1);
    }

    /**
     * The number of frames from firstIndex to (currentIndex-1)
     * @returns {number}
     */
    getUsedStockLevel() {
        if (this.isEmpty()) return 0;
        return (this._currentIndex - this._firstIndex);
    }

    isSafetyStock() {
        return (this.getCurrentStockLevel() >= this._safetyThreshold);
    }

    isMinimumStock() {
        return (this.getCurrentStockLevel() >= this._minimumThreshold);
    }

    isStockAvailable() {
        return (!this.isEmpty() && (this._currentIndex + this._chunkSize - 1) <= this.getLastIndex());
    }

    hasObsoleteStock() {
        return (this.getUsedStockLevel() > this._historyThreshold);
    }

    /**
     * Sequential reading the next chunk from the buffer starting from currentIndex
     * @returns {Array}
     */
    readNext() {
        let frames = [];
        if (this.isStockAvailable()) {
            for (let i = 0; i <= (this._chunkSize-1); i++) {
                frames.push(this._frames[this._currentIndex + i]);
            }
        }
        this._currentIndex += frames.length;

        //House keeping
        if (!this.isMinimumStock()) {
            this._replenish();
        }
        if (this.hasObsoleteStock()) {
            this._removeObsolete();
        }

        return frames;
    }

    /**
     * Move the buffer currentIndex to a frame, e.g when the tick is dragged randomly on the timeline.
     * The frame index corresponds to a buffer index which can be less or greater than the currentIndex, or can
     * be outside the current frames in the buffer. In the latter case, it is too far reaching, the buffer will be
     * cleared and new frames must be read into the buffer starting from the input frame.
     *
     * @param {Number} frameIndex: the frame index
     */
    moveTo(frameIndex) {
        let bufferIndex = this._getBufferIndexFromFrameIndex(frameIndex);
        if (bufferIndex >= 0) {
            this._currentIndex = bufferIndex;
        }
        else { // the new requested frames are too far outside this buffer
            this.clear();
        }
    }

    /**
     * Write a chunk of frames to the buffer
     * The frames are concatenated to the end of the buffer
     * @param {Array} frames
     * @param {Number} batchNumber
     */
    write(frames, requestToken) {
        if (requestToken === this._requestToken) { // don't get old results
            this._frames.concat(frames);
            if (this._currentIndex < 0) {
                this._currentIndex = 0;
            }
            this._replenish();
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
     * @private
     */
    _replenish() {
        if (!this.isSafetyStock()) {
            let frameIndex = (this.isEmpty() ? 0 : (this._frames[this.getLastIndex()].index + 1));
            this._dataRequester.requestData(frameIndex, this, this._requestToken);
        }
    }

    _removeObsolete() {
        let obsoleteSize = this.getUsedStockLevel() - this._historyThreshold;
        if (obsoleteSize > 0) {
            this._frames.splice(0, obsoleteSize);
            this._firstIndex = this._frames[0].index;
        }
    }

}



