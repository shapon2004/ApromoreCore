class DataRequester {
    /**
     * @param {Buffer} _buffer
     * @param {String} _receiveData
     * @param {Boolean} _hasDataRequestError
     * @param {String} _pluginExecutionId
     */
    constructor() {
        this._buffer = undefined;
        this._receivedData = undefined;
        this._hasDataRequestError = false;
        this._pluginExecutionId = "";

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
     * @param {String} pluginExecutionId
     * @param {Buffer} buffer
     */
    requestData(frameIndex, pluginExecutionId, buffer) {
        this._worker.postMessage({startFrame: frameIndex, pluginExecutionId: pluginExecutionId});
        this._buffer = buffer;
    }

    getReceivedData() {
        return this._receivedData;
    }

    hasDataRequestError() {
        return this._hasDataRequestError;
    }
}