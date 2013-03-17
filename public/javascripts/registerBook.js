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

requirejs(['lib/backbone', 'model', 'view', 'common', 'lib/zepto'], function (Backbone, Model, View) {
    'use strict';

    var shelfId = _.queryString2json()['shelf_id'];

    var RegisterBookFormView = View.BaseView.extend({
        events: {'submit': 'save'},
        initialize: function () {
            _.bindAll(this, 'save');
        }, save: function () {
            var arr = this.$el.serializeArray();
            var data = _(arr).reduce(function (acc, field) {
                acc[field.name] = field.value;
                return acc;
            }, {});

            data['shelf_id'] = shelfId;

            this.model.save(data, {success: function (model, response, options) {
                location.href = '/book/detail/' + response.result[0].id;
            }});
            return false;
        }
    });

    var registerBookFormView = new RegisterBookFormView({el: $('#registerBookForm'), model: new Model.Book()});

    $(function(){
        var $bookshelfAnchorLink =  $('#bookshelfAnchorLink');
        $bookshelfAnchorLink.attr('href', $bookshelfAnchorLink.attr('href') + shelfId);
    });
});
