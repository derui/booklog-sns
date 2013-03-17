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
            _.bindAll(this, 'render');
            this.collection.bind('reset', this.render);
        },
        render: function () {
            var models = this.collection.models;
            this.$el.render({
                "rentalInfos": models
            }, {
                '.rentalInfo': {
                    'rentalInfo<-rentalInfos': {
                        '.book_image@src': function (arg) {
                            return arg.rentalInfo.item.attributes.book_image_medium || '';
                        },
                        '.book_image@data-src': function (arg) {
                            if(!arg.rentalInfo.item.attributes.book_image_medium){
                                return 'holder.js/300x200';
                            } else {
                                return '';
                            }
                        },
                        '.book_title': function (arg) {
                            return '<a href="/book/detail/' + arg.rentalInfo.item.attributes.book_id + '">' + arg.rentalInfo.item.attributes.book_name + '</a>';
                        },
                        '.rental_user': function (arg) {
                            return arg.rentalInfo.item.attributes.rental_user;
                        },
                        '.update_date': function (arg) {
                            return arg.rentalInfo.item.attributes.updated_date;
                        }
                    }
                }
            });
        }
    });

    var rentalList = new Model.RentalList();
    var rentalInfoListView = new RentalInfoListView({
        collection: rentalList
    });

    rentalList.fetch({data: $.param({start: 0, rows: 5})});

    // 本棚一覧のビュー
    var BookShelfInfoListView = View.BaseView.extend({
        el: '.updateBookShelfInfos',
        initialize: function () {
            _.bindAll(this, 'render');
            this.collection.bind('reset', this.render);
        },
        render: function () {
            var models = this.collection.models;
            this.$el.render({
                "shelfs": models
            }, {
                '.bookshelfInfo': {
                    'bookshelfInfo<-shelfs': {
                        '.name': function (arg) {
                            return '<a href="/book_shelf/detail/' + arg.bookshelfInfo.item.attributes.shelf_id + '">' + arg.bookshelfInfo.item.attributes.shelf_name + '</a>';
                        },
                        '.description': function (arg) {
                            return _(arg.bookshelfInfo.item.attributes.shelf_description).replaceAll('\n', '<br />');
                        },
                        '.update_date': function (arg) {
                            return arg.bookshelfInfo.item.attributes.updated_date;
                        }
                    }
                }
            });
        }
    });

    var bookShelfList = new Model.BookShelfList();
    var bookShelfInfoListView = new BookShelfInfoListView({
        collection: bookShelfList
    });

    bookShelfList.fetch({data: $.param({start: 0, rows: 5})});

    $(function () {
        $('#dissconectButton').on('click', function () {
            var $this = $(this);

            if ($this.hasClass('disabled')) {
                return;
            }

            $.ajax({
                type: 'GET',
                url: '/disconnect',
                success: function () {
                    $('#loginUserInfoArea').remove();
                    $('#___signin_0').css('display', 'inline-block').show();
                    $this.addClass('disabled');
                }
            });
        });
    });
});
