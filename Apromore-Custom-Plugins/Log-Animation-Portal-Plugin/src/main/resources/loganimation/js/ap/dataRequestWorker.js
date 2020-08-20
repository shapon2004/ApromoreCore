

onmessage = function(e) {
    let context = this;
    let startFrameIndex = e.data.startFrame;
    let pluginExecutionId = e.data.pluginExecutionId;
    new Ajax.Request("/dataRequest/logAnimationData?pluginExecutionId=" + pluginExecutionId + "&startFrameIndex=" + startFrameIndex, {
        method: 'POST',

        parameters: {
            framePos: framePos
        },

        onSuccess: (function(request) {
            //Testing
            doPointlessComputationsWithBlocking();
            this.postMessage({success: true, data: request.responseText});
        }).bind(context),

        onFailure: (function(){
            this.postMessage({success: false});
        }).bind(context)
    });

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
        var primes = calculatePrimes(iterations, multiplier);
        console.log(primes);
    }
}



