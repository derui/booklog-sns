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
        "lib/moment": {
            exports: "moment"
        }
    }
});

requirejs(['lib/backbone', 'model', 'view', 'lib/pure', 'common', 'lib/zepto', 'lib/moment'], function (Backbone, Model, View) {
    'use strict';

    var BookView = View.BaseView.extend({
        el: '.bookInfo',
        initialize: function () {
            _.bindAll(this, 'render');
            this.model.bind('change', this.render);
        },
        render: function () {
            var book = this.model.attributes.result[0];
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
                },
                '.published_date': function (arg) {
                    var publishedDate = arg.context.book.published_date;

                    if (!publishedDate) {
                        return '';
                    } else {
                        return moment(publishedDate).format('YYYY/MM/DD');
                    }
                }
            });
        }
    });

    var book = new Model.Book({'id': location.pathname.split('/').pop()});
    var bookView = new BookView({
        model: book
    });
    book.fetch();

    var RentalBookButtonView = View.BaseView.extend({
        el: '#rentalBookButton',
        events: {
            "click": "rentalBook"
        },
        rentalBook: function () {
            this.model.save();
        }
    });

    var rental = new Model.Rental({'rental_book': location.pathname.split('/').pop()});
    new RentalBookButtonView({
        model: rental
    });
});
