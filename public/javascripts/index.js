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

    // レンタル情報一覧のビュー
    var RentalInfoListView = View.BaseView.extend({
        el: '.updateRentalInfos',
        initialize: function () {
            this.collection.on('reset', this.render, this);
        },
        render: function () {
            var models = this.collection.models;
            this.$el.render({
                "rentalInfos": models[0].attributes.result
            }, {
                '.rentalInfo': {
                    'rentalInfo<-rentalInfos': {
                        '.book_image@src': function (arg) {
                            return arg.rentalInfo.item.book_image_medium || '';
                        },
                        '.book_image@data-src': function (arg) {
                            if (!arg.rentalInfo.item.book_image_medium) {
                                return 'holder.js/300x200';
                            } else {
                                return '';
                            }
                        },
                        '.book_title': function (arg) {
                            return '<a href="/book/detail/' + arg.rentalInfo.item.book_id + '">' + arg.rentalInfo.item.book_name + '</a>';
                        },
                        '.rental_user': function (arg) {
                            return arg.rentalInfo.item.rental_user;
                        },
                        '.update_date': function (arg) {
                            return arg.rentalInfo.item.updated_date;
                        }
                    }
                }
            });

            return this;
        }
    });

    var rentalList = new Model.RentalList();
    var rentalInfoListView = new RentalInfoListView({
        collection: rentalList
    });

    rentalList.fetch({reset: true, data: $.param({start: 0, rows: 5})});

    // 本棚一覧のビュー
    var BookShelfInfoListView = View.BaseView.extend({
        el: '.updateBookShelfInfos',
        initialize: function () {
            this.collection.on('reset', this.render, this);
        },
        render: function () {
            var models = this.collection.models;
            this.$el.render({
                "shelfs": models[0].attributes.result
            }, {
                '.bookshelfInfo': {
                    'bookshelfInfo<-shelfs': {
                        '.name': function (arg) {
                            return '<a href="/book_shelf/detail/' + arg.bookshelfInfo.item.shelf_id + '">' + arg.bookshelfInfo.item.shelf_name + '</a>';
                        },
                        '.description': function (arg) {
                            return _(arg.bookshelfInfo.item.shelf_description).replaceAll('\n', '<br />');
                        },
                        '.update_date': function (arg) {
                            return arg.bookshelfInfo.item.updated_date;
                        }
                    }
                }
            });

            return this;
        }
    });

    var bookShelfList = new Model.BookShelfList();
    var bookShelfInfoListView = new BookShelfInfoListView({
        collection: bookShelfList
    });

    bookShelfList.fetch({reset: true, data: $.param({start: 0, rows: 5})});
});
