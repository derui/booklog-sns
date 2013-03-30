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

requirejs(['lib/backbone', 'model', 'view', 'lib/pure', 'lib/zepto', 'lib/moment'], function (Backbone, Model, View) {
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
            $bookshelfAnchorLink.attr('href', $bookshelfAnchorLink.attr('href') + book.shelf_id);
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

    var bookId = _.getPrimaryKeyFromUrl();
    var book = new Model.Book({'id': bookId});
    var bookView = new BookView({
        model: book
    });
    book.fetch();

    var RentalInfoAreaView = View.BaseView.extend({
        el: '#rentalInfoArea',
        initialize: function () {
            var $rentalInfoArea = this.$el;

            $.getJSON('/api/rental/?book_id=' + bookId, function (data) {
                var totalCount = data.totalCount;

                if (totalCount) {
                    var result = data.result[0];
                    $rentalInfoArea.append('<div class="alert alert-info">' +
                        'この本は、すでに' + result.updated_user_name + 'さんにレンタルされています' +
                        '<br />' +
                        '<button class="btn" href="#" id="bringBackBookButton">' +
                        '<i class="icon-arrow-left"></i> この本を返却する' +
                        '</button>' +
                        '</div>');

                    var BringBackBookButtonView = View.BaseView.extend({
                        el: '#bringBackBookButton',
                        events: {
                            "click": "bringBackBook"
                        },
                        bringBackBook: function () {
                            this.$el.addClass('disabled');
                            this.model.destroy({success: function (model, response) {
                                location.reload();
                            }});
                        }
                    });

                    var bringBackBookButtonView = new BringBackBookButtonView({
                        model: new Model.Rental({'id': result.rental_id})
                    });
                } else {
                    $rentalInfoArea.append('<button class="btn" href="#" id="rentalBookButton">' +
                        '<i class="icon-arrow-right"></i> この本を借りる' +
                        '</button>');

                    var RentalBookButtonView = View.BaseView.extend({
                        el: '#rentalBookButton',
                        events: {
                            "click": "rentalBook"
                        },
                        rentalBook: function () {
                            this.$el.addClass('disabled');
                            this.model.save({'rental_book': bookId}, {success: function (model, response) {
                                location.reload();
                            }});
                        }
                    });

                    var rentalBookButtonView = new RentalBookButtonView({
                        model: new Model.Rental()
                    });
                }
            });
        }
    });

    var rentalInfoAreaView = new RentalInfoAreaView();
});
