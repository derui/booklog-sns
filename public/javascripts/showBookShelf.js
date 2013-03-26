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
            var book = bookShelf;
            this.$el.render({
                "bookShelf": bookShelf,
                "book" : book
            }, {
                '.shelf_name': function (arg) {
                    return arg.context.bookShelf.shelf_name;
                },
                '.shelf_description': function (arg) {
                    return arg.context.bookShelf.shelf_description;
                }
            });
        }
    });

    // 書籍一覧のビュー
    var BookListView = View.BaseView.extend({
        el: '.test',
        initialize: function () {
            _.bindAll(this, 'render');
            this.collection.bind('reset', this.render);
        },
        render: function () {
            var models = this.collection.models;
            this.$el.render({
                "books": models[0].attributes.result
            }, {
                '.bookInfoArea': {
                    'bookInfoArea<-books': {
                        '.book_title' : function (arg) {
                            return arg.bookInfoArea.item.book_name;
                        },
                        '.book_image@src' : function (arg) {
                            return arg.bookInfoArea.item.medium_image_url;
                        }
                    }
                }
            });
        }
    });

    var bookShelf = new Model.BookShelf({'id': location.pathname.split('/').pop()});
    var bookShelfView = new BookShelfView({
        model: bookShelf
    });
    bookShelf.fetch();

    var bookList = new Model.BookList();
    var bookListView = new BookListView({
        collection: bookList
    });
    bookList.fetch({data: $.param({'shelf': location.pathname.split('/').pop()})});

    $(function () {
        $('#registerBookButton').on('click', function () {
            location.href = '/book/register?shelf_id=' + location.pathname.split('/').pop();
        });
    });
});
