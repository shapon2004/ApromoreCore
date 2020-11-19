const webpack = require("webpack");
const path = require('path');

//const UglifyJSPlugin = require('uglifyjs-webpack-plugin');
//const TerserPlugin = require("terser-webpack-plugin");
const HtmlWebpackPlugin = require("html-webpack-plugin");

module.exports = {
    entry: './src/loganimation/index.js',
    output: {
        filename: 'logAnimation.bundle.js',
        path: path.resolve(__dirname, 'dist'),
        library: "logAnimation",   // Important
        libraryTarget: 'umd',   // Important
        umdNamedDefine: true   // Important
    },
    mode: 'development',
    devtool: false,
    module: {
        rules: [
            { test: /\.css$/, use: ["style-loader", "css-loader"] }
        ]
    },
    plugins: [
        new HtmlWebpackPlugin({
            template: path.resolve(__dirname, "src", "index.html")
        })
    ]
};