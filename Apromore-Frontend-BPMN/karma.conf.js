let webpackConfig = require('./webpack.config.js');

module.exports = function (config) {
    config.set({
        // base path that will be used to resolve all patterns (eg. files, exclude)
        basePath: '',

        plugins: ['@metahub/karma-jasmine-jquery', 'karma-*'],
        frameworks: ['jasmine-jquery'],

        // list of files / patterns to load in the browser
        files: [
            {pattern: 'test/fixtures/add/*.bpmn', watched: false, served: true, included: false},
            {pattern: 'test/fixtures/change/*.bpmn', watched: false, served: true, included: false},
            {pattern: 'test/fixtures/collaboration/*.bpmn', watched: false, served: true, included: false},
            {pattern: 'test/fixtures/layout-change/*.bpmn', watched: false, served: true, included: false},
            {pattern: 'test/fixtures/remove/*.bpmn', watched: false, served: true, included: false}
        ],

        preprocessors: {
            './test/spec/*.spec.js': ['webpack'],
        },
        webpack: webpackConfig,
        webpackMiddleware: {
            noInfo: true
        },

        exclude: [],
        reporters: ['progress'],
        port: 9876,
        colors: true,
        logLevel: config.LOG_DISABLE,
        autoWatch: true,
        browsers: ['ChromeHeadless'],
        singleRun: true,
    });
};
