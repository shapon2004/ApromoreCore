
let dataRequester = new DataRequester();
window.setInterval(dataRequester.requestData.bind(dataRequester), 10000, getRandomInt(1000), window.pluginExecutionId);
function getRandomInt(max) {
    return Math.floor(Math.random() * Math.floor(max));
}
console.log('test');
