const webpack = require("webpack");
const path = require('path');

//const UglifyJSPlugin = require('uglifyjs-webpack-plugin');
//const TerserPlugin = require("terser-webpack-plugin");
//const HtmlWebpackPlugin = require("html-webpack-plugin");

module.exports = {
    entry: './src/loganimation/index.js',
    output: {
        filename: 'logAnimation.bundle.js',
        path: path.resolve(__dirname, 'dist'),
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