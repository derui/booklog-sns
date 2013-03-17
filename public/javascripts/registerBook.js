requirejs.config({
    shim: {
        "lib/zepto": {
            exports: "Zepto"
        },
        "lib/underscore": {
            exports: "_"
        },
        "lib/pure": {
            deps: ["../lib/zepto"],
            exports: "pure"
        },
        "lib/backbone": {
            deps: ["../lib/zepto", "../lib/underscore"],
            exports: "Backbone"
        },
        "lib/bootstrap": {
            deps: ["../lib/zepto"],
            exports: "bootstrap"
        }
    }
});

requirejs(['lib/underscore', 'common', 'lib/zepto', 'lib/bootstrap'], function (Backbone, Model, View) {
    'use strict';

    $(function () {
        $('#myModal').modal({});
    });
});
