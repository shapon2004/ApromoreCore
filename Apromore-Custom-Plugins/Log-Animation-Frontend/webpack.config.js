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
        //library: "LogAnimation",   // Important
        libraryTarget: 'umd',   // Important
        libraryExport: 'default'
        //umdNamedDefine: true   // Important
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
    }
    /*
    plugins: [
        new HtmlWebpackPlugin({
            template: path.resolve(__dirname, "src", "index.html")
        })
    ],
     */
};