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

requirejs(['lib/backbone', 'model', 'view', 'lib/pure', 'lib/zepto', 'lib/moment'], function (Backbone, Model, View) {
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
                        '.thumbnailBookImage': function (arg) {
                            var imageUrl = arg.rentalInfo.item.medium_image_url;

                            if (imageUrl) {
                                return '<img src="' + imageUrl + '" class="book_image img-polaroid" />';
                            } else {
                                return '';
                            }
                        },

                        '.book_title': function (arg) {
                            var rentalInfo = arg.rentalInfo.item;
                            return '<a href="/book/detail/' + rentalInfo.rental_book_id + '">' +
                                rentalInfo.rental_book_name +
                                '</a>';
                        },
                        '.rentalUserInfo': function (arg) {
                            var rentalInfo = arg.rentalInfo.item;
                            return rentalInfo.updated_user_name +
                                'さんが' +
                                moment(rentalInfo.updated_date).format('YYYY/MM/DD') +
                                'にレンタルしました';
                        },
                        '.rentalUserInfo@class+': function (arg) {
                            return ' alert alert-info';
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
                            return _.convertLineBreak2BR(arg.bookshelfInfo.item.shelf_description);
                        },
                        '.update_date': function (arg) {
                            return _.parseDateForJST(arg.bookshelfInfo.item.updated_date).format('YYYY/MM/DD HH:mm');
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
