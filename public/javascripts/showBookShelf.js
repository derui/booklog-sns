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
        }
    }
});

requirejs(['lib/zepto'], function () {
    'use strict';

    $(function () {
        $('#registerBookButton').on('click', function () {
            location.href = '/book/register?shelf_id=' + location.pathname.split('/').pop();
        });
    });
});
