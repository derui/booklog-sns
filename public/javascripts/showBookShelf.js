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

    // 本棚詳細のビュー
    var BookShelfView = View.BaseView.extend({
        el: '.bookShelfInfo',
        initialize: function () {
            _.bindAll(this, 'render');
            this.model.bind('change', this.render);
        },
        render: function () {
            var bookShelf = this.model.attributes.result[0];
            var book = this.model.attributes.result[0].books;
            console.log(book);
            this.$el.render({
                "bookShelf": bookShelf,
                "book" : book
            }, {
                '.shelf_name': function (arg) {
                    return arg.context.bookShelf.shelf_name;
                },
                '.shelf_description': function (arg) {
                    return arg.context.bookShelf.shelf_description;
                },
                '.bookInfo': {
                    'bookInfo<-books': {
                        '.book_title' : function (arg) {
                            return arg.context.book.book_title;
                        },
                        '.book_image@src' : function (arg) {
                            return arg.context.book.medium_image_url;
                        }
                    }
                }
            });
        }
    });

//    // 書籍一覧のビュー
//    var BookView = View.BaseView.extend({
//        el: '.bookInfo',
//        initialize: function () {
//            _.bindAll(this, 'render');
//            this.model.bind('change', this.render);
//        },
//        render: function () {
//            var book = this.model.attributes.result[0].books;
//            console.log(book);
//            var $bookshelfAnchorLink = $('#bookshelfAnchorLink');
//            $bookshelfAnchorLink.attr('href', $bookshelfAnchorLink.attr('href') + book['shelf_id']);
//            this.$el.render({
//                "book": book
//            }, {
//                '.book_image@src': function (arg) {
//                    return arg.context.book.medium_image_url;
//                },
//                '.book_title': function (arg) {
//                    return arg.context.book.book_name;
//                },
//                '.book_author': function (arg) {
//                    return arg.context.book.book_author;
//                },
//                '.published_date': function (arg) {
//                    return moment(arg.context.book.published_date).format('YYYY/MM/DD');
//                }
//            });
//        }
//    });

    var bookShelf = new Model.BookShelf({'id': location.pathname.split('/').pop()});
    var bookShelfView = new BookShelfView({
        model: bookShelf
    });
    bookShelf.fetch();

//    var book = new Model.Book();
//    var bookView = new BookView({
//        model: book
//    });


    $(function () {
        $('#registerBookButton').on('click', function () {
            location.href = '/bookShelf/register?shelf_id=' + location.pathname.split('/').pop();
        });
    });
});
