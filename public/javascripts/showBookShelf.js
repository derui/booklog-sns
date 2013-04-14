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

requirejs(['lib/backbone', 'model', 'view', 'lib/pure', 'lib/zepto'], function (Backbone, Model, View) {
    'use strict';

    // 本棚詳細のビュー
    var BookShelfView = View.BaseView.extend({
        el: '.bookShelfInfo',
        initialize: function () {
            this.model.on('change', this.render, this);
        },
        render: function () {
            var bookShelf = this.model.attributes.result[0];
            this.$el.render({
                "bookShelf": bookShelf
            }, {
                '.shelf_name': function (arg) {
                    return arg.context.bookShelf.shelf_name;
                },
                '.shelf_description': function (arg) {
                    return arg.context.bookShelf.shelf_description;
                }
            });

            if (bookShelf.book_count > 5) {
                // TODO 「さらに読み込む」ボタン表示
            }
            return this;
        }
    });

    // 書籍一覧のビュー
    var BookListView = View.BaseView.extend({
        el: '.bookList',
        initialize: function () {
            this.collection.on('reset', this.render, this);
        },
        render: function () {
            var bookInfo = this.collection.models[0].attributes;
            this.$el.render({
                "books": bookInfo.result
            }, {
                '.bookInfoArea': {
                    'bookInfoArea<-books': {
                        '.book_title': function (arg) {
                            return '<a href="/book/detail/' + arg.bookInfoArea.item.book_id + '">' +
                            arg.bookInfoArea.item.book_name + '</a>';
                        },
                        '.book_image@src': function (arg) {
                            return arg.bookInfoArea.item.medium_image_url;
                        },
                        '.book_author': function (arg) {
                            return arg.bookInfoArea.item.book_author;
                        }
                    }
                }
            });

            return this;
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
      // TODO ページング出来るようになったらこっちを使う
//    bookList.fetch({reset: true, data: $.param({'shelf': location.pathname.split('/').pop(), start: 0, rows: 5})});
    bookList.fetch({reset: true, data: $.param({'shelf': location.pathname.split('/').pop()})});

    $(function () {
        $('#registerBookButton').on('click', function () {
            location.href = '/book/register?shelf_id=' + location.pathname.split('/').pop();
        });
    });
});
