/**
 * Web Worker to communicate with the server
 * @author Bruce Nguyen
 */

onmessage = function(e) {
    console.log('DataRequestWorker - request received: requestToken=' + e.data.requestToken + ', startFrameIndex=' + e.data.startFrame);
    let context = this;
    let startFrameIndex = e.data.startFrame;
    let chunkSize = e.data.chunkSize;
    let pluginExecutionId = e.data.pluginExecutionId;
    this.requestToken = e.data.requestToken;
    let worker = this;

    let httpRequest = new XMLHttpRequest();

    httpRequest.onreadystatechange = function() {
        if (httpRequest.readyState === XMLHttpRequest.DONE) {
            console.log('DataRequestWorker - server response received: responseCode=' + httpRequest.status);
            if (httpRequest.status === 200) {
                //doPointlessComputationsWithBlocking();
                console.log(httpRequest.responseText)
                let jsonResponse = JSON.parse(httpRequest.responseText);
                context.postMessage({success: true, code: httpRequest.status, data: jsonResponse, requestToken: worker.requestToken});
            } else {
                context.postMessage({success: false, code: httpRequest.status, data: httpRequest.responseText});
            }
            console.log("DataRequestWorker - send reseponse, responseCode: " + httpRequest.status);
            //console.log("DataRequestWorker - send reseponse, responseText: " + httpRequest.responseText);
        }
    };

    console.log("DataRequestWorker - sending request to the server: pluginExecutionId=" + pluginExecutionId +
            ", startFrame=" + startFrameIndex + ', chunkSize=' + chunkSize);
    httpRequest.open('GET',"/dataRequest/logAnimationData?pluginExecutionId=" + pluginExecutionId +
                    "&startFrameIndex=" + startFrameIndex + "&chunkSize=" + chunkSize, true);
    httpRequest.send();

    function calculatePrimes(iterations, multiplier) {
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

    function doPointlessComputationsWithBlocking() {
        var primes = calculatePrimes(100, 1000000000);
        console.log(primes);
    }
}



