const webpack = require("webpack");
const path = require('path');
const LowerCaseNamePlugin = require('webpack-lowercase-name');
const UglifyJsPlugin = require('uglifyjs-webpack-plugin');
const debug = process.env.NODE_ENV !== "production";

let plugins = [
    new webpack.ProvidePlugin({
        "$":"jquery",
        "$j":"jquery",
        "jQuery":"jquery",
        "window.jQuery":"jquery"
    }),
    new LowerCaseNamePlugin(),
];

if (!debug) {
    plugins.push(
        new UglifyJsPlugin({
            uglifyOptions: {
                output: {
                    // comments: false, // removing comments
                },
                compress: {
                    drop_console: true, // remove console.logs
                },
            },
        })
    );
}

module.exports = {
    entry: {
        LogAnimationBpmn: './src/loganimation/logAnimationBpmn.js',
        ProcessDiscoverer: './src/processdiscoverer/index.js'
    },
    output: {
        filename: '[lc-name].js',
        path: path.resolve(__dirname, 'dist'),
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
    plugins: plugins
};