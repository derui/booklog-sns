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

requirejs(['lib/backbone', 'model', 'view', 'lib/pure', 'common', 'lib/zepto'], function (Backbone, Model, View) {
    'use strict';

    var RegisterBookShelfFormView = View.BaseView.extend({
        events: {'submit': 'save'},
        initialize: function () {
            _.bindAll(this, 'save');
        }, save: function () {
            var arr = this.$el.serializeArray();
            var data = _(arr).reduce(function (acc, field) {
                acc[field.name] = field.value;
                return acc;
            }, {});

            this.model.save(data, {success:function(){
                location.href = '/book_shelf/detail';
            }});
            return false;
        }
    });

    var registerBookShelfFormView = new RegisterBookShelfFormView({el: $('#registerBookShelfForm'), model: new Model.BookShelf()});
});
