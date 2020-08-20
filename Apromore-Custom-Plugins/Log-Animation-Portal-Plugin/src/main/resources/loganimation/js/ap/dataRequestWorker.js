

onmessage = function(e) {
    let context = this;
    let framePos = e.data.startFrame;
    let pluginExecutionId = e.data.pluginExecutionId;
    new Ajax.Request("/dataRequest/logAnimationData?pluginExecutionId=" + pluginExecutionId + "&startFrame=" + framePos, {
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