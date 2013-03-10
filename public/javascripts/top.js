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

    var BookShelfInfoListView = View.BaseView.extend({
        el: '.bookshelfInfos',
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
                            return '<a href="#">' + arg.bookshelfInfo.item.attributes.shelf_name + '</a>';
                        },
                        '.description': function (arg) {
                            return arg.bookshelfInfo.item.attributes.shelf_description;
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
