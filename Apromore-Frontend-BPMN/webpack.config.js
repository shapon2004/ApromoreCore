const webpack = require("webpack");
const path = require('path');
const LowerCaseNamePlugin = require('webpack-lowercase-name');

module.exports = {
    // entry: {
    //     BpmnDiff: './app/app.js',
    //     BpmnJS: './app/bpmn.js',
    // },
    entry: ['./app/app.js'],
    output: {
        filename: 'bpmndiff.js',
        path: path.resolve(__dirname, 'app'),
        library: ['Apromore', '[name]'],   // Important
        libraryTarget: 'umd',   // Important
        libraryExport: 'default',
        umdNamedDefine: true
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
        new LowerCaseNamePlugin()
    ],

};