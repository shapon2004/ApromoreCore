/**
 * Manage data requests and responses to/from the server
 * Use a Web Worker to do it in order not to block the UI processing.
 *
 * @author Bruce Nguyen
 */
class DataRequester {
    /**
     * @param {Buffer} buffer
     * @param {String} pluginExecutionId
     */
    constructor(pluginExecutionId) {
        this._buffer = undefined;
        this._hasDataRequestError = false;
        this._pluginExecutionId = pluginExecutionId;

        this._workerProxy = new Worker("/loganimation2/js/ap/dataRequestWorker.js");
        let self = this;
        this._workerProxy.onmessage = function(e) {
            console.log('DataRequester - response received: requestToken=' + e.data.requestToken);
            let result = e.data;
            let requestToken = result.requestToken;
            if (result.success) {
                if (self._buffer) {
                    self._buffer.write(result.data, requestToken);
                }
            }

            //this.doPointlessComputationsWithBlocking();
        }
    }

    getPluginExecutionId() {
        return this._pluginExecutionId;
    }

    /**
     *
     * @param {Number} frameIndex
     * @param {Buffer} buffer
     * @param {Number} requestToken
     */
    requestData(buffer, requestToken, frameIndex, chunkSize) {
        console.log('DataRequester - requestData: frameIndex=' + frameIndex + ", requestToken=" + requestToken);
        this._buffer = buffer;
        this._workerProxy.postMessage({ 'pluginExecutionId': this._pluginExecutionId,
                                        'requestToken': requestToken,
                                        'startFrame': frameIndex,
                                        'chunkSize': chunkSize});
    }

    calculatePrimes(iterations, multiplier) {
        var primes = [];
        for (var i = 0; i < iterations; i++) {
            var candidate = i * (multiplier * Math.random());
            var isPrime = true;
            for (var c = 2; c <= Math.sqrt(candidate); ++c) {
                if (candidate % c === 0) {
                    // not prime
                    isPrime = false;
                    break;
                }
            }
            if (isPrime) {
                primes.push(candidate);
            }
        }
        return primes;
    }

    doPointlessComputationsWithBlocking() {
        var primes = calculatePrimes(iterations, multiplier);
        console.log(primes);
    }
}

