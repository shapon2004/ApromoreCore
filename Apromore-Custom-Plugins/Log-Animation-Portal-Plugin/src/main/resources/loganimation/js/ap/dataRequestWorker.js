

onmessage = function(e) {
    let context = this;
    let framePos = e.data;
    new Ajax.Request("/loganimation/requestData", {
        method: 'POST',

        parameters: {
            framePos: framePos
        },

        onSuccess: (function(request) {
            this.postMessage({success: true, data: request.responseText});
        }).bind(context),

        onFailure: (function(){
            this.postMessage({success: false});
        }).bind(context)
    });
}