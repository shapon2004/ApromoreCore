
onmessage = function(e) {
    let context = this;
    let startFrameIndex = e.data.startFrame;
    let pluginExecutionId = e.data.pluginExecutionId;

    let httpRequest = new XMLHttpRequest();

    httpRequest.onreadystatechange = function() {
        if (httpRequest.readyState === XMLHttpRequest.DONE) {
            if (httpRequest.status === 200) {
                //doPointlessComputationsWithBlocking();
                let jsonResponse = JSON.parse(httpRequest.responseText);
                context.postMessage({success: true, code: httpRequest.status, data: jsonResponse});
            } else {
                context.postMessage({success: false, code: httpRequest.status, data: httpRequest.responseText});
            }
        }
    };

    console.log("Before sending request: pluginExecutionId=" + pluginExecutionId + ", startFrame=" + startFrameIndex);
    httpRequest.open('GET',"/dataRequest/logAnimationData?pluginExecutionId=" + pluginExecutionId + "&startFrameIndex=" + startFrameIndex, true);
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



