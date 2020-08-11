
onmessage = function(e) {
    let dataRequester = new DataRequester();
    let data = dataRequester.requestData();
    postMessage(data);
}

class DataRequester {
    requestData() {
        //send request to the server and receive result
    }
}