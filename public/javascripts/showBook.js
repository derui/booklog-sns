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

    var BookView = View.BaseView.extend({
        el: '.bookInfo',
        initialize: function () {
            _.bindAll(this, 'render');
            this.model.bind('change', this.render);
        },
        render: function () {
            var book = this.model.attributes[0];
            var $bookshelfAnchorLink = $('#bookshelfAnchorLink');
            $bookshelfAnchorLink.attr('href', $bookshelfAnchorLink.attr('href') + book['shelf_id']);
            this.$el.render({
                "book": book
            }, {
                '.book_image@src': function (arg) {
                    return arg.context.book.medium_image_url;
                },
                '.book_title': function (arg) {
                    return arg.context.book.book_name;
                },
                '.book_author': function (arg) {
                    return arg.context.book.book_author;
                }
            });
        }
    });

    var book = new Model.Book({'id': location.pathname.split('/').pop()});
    var bookView = new BookView({
        model: book
    });
    book.fetch();
});
