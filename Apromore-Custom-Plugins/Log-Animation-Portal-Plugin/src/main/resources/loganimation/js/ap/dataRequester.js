class DataRequester {
    /**
     * @param {Buffer} _buffer
     * @param {String} _receiveData
     * @param {Boolean} _hasDataRequestError
     */
    constructor() {
        this._buffer = undefined;
        this._receivedData = undefined;
        this._hasDataRequestError = false;

        this._worker = new Worker("dataRequestWorker.js");
        let self = this;
        this._worker.onmessage = function(e) {
            let result = e.data;
            if (result.success) {
                self._receivedData = result.data;
                if (self._buffer) {
                    self._buffer.writeNextChunk(result.data);
                }
            }
        }
    }

    /**
     *
     * @param {Number} frameIndex
     * @param {Buffer} buffer
     */
    requestData(frameIndex, buffer) {
        this._worker.postMessage({startFrame:frameIndex});
        this._buffer = buffer;
    }

    getReceivedData() {
        return this._receivedData;
    }

    hasDataRequestError() {
        return this._hasDataRequestError;
    }
}