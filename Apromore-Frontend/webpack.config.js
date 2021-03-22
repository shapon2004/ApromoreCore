const webpack = require("webpack");
const path = require('path');

module.exports = {
    entry: {
        LogAnimation: './src/loganimation/index.js',
        ProcessDiscoverer: './src/processdiscoverer/index.js',
        BPMNModelWrapper: './src/processmap/bpmnModelWrapper.js'
    },
    output: {
        filename: '[name].js',
        path: path.resolve(__dirname, 'dist'),
        library: ['Apromore', '[name]'],   // Important
        libraryTarget: 'umd',   // Important
        libraryExport: 'default'
    },
    mode: 'development',
    devtool: 'source-map',
    module: {
        rules: [
            {test: /\.css|\.bpmn|\.txt$/, use: 'raw-loader'}
        ]
    },
    resolve: {
        mainFields: [
            'browser',
            'module',
            'main'
        ]
    },

    plugins: [
        new webpack.ProvidePlugin({
            "$":"jquery",
            "$j":"jquery",
            "jQuery":"jquery",
            "window.jQuery":"jquery"
        }),
    ],

};