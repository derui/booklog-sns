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

function signinCallback(authResult) {
    if (authResult['access_token']) {
        // ログインボタンから認証すると、g-oauth-windowにWindowオブジェクトがセットされ
        // JSON.stringify()でパースエラーとなるため、nullで初期化する
        authResult['g-oauth-window'] = null;

        $.ajax({
            type: 'POST',
            url: '/connect',
            contentType: "application/json",
            data: JSON.stringify(authResult),
            success: function (response) {
                var loginUserInfo = response.result[0];
                $('#___signin_0').hide();
                $('#signinArea').prepend('<span id="loginUserInfoArea"><img src="' + loginUserInfo.googlePublicProfilePhotoUrl + '" id="profilePhoto" /><span>' + loginUserInfo.googleDisplayName + '</span></span>');
                $('#dissconectButton').removeClass('disabled');
            }
        });
    }
}

requirejs(['lib/backbone', 'lib/pure', 'common', 'lib/zepto'], function (Backbone) {
    "use strict";

    // TODO モジュール化
    var BaseModel = Backbone.Model.extend({});
    var BaseCollection = Backbone.Collection.extend({});
    var BaseView = Backbone.View.extend({});
    var BookShelf = BaseModel.extend({
        urlRoot: '/shelf',
        validate: function (attrs) {
            if (!attrs.name) {
                return '本棚の名称は必須です';
            }
        }
    });
    var BookShelfList = BaseCollection.extend({
        url: '/shelf',
        model: BookShelf,
        parse: function (json) {
            return json.result;
        }
    });

    var BookShelfInfoListView = BaseView.extend({
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
    var bookShelfList = new BookShelfList();
    var bookShelfInfoListView = new BookShelfInfoListView({
        collection: bookShelfList
    });

    bookShelfList.fetch();

    window.___gcfg = {
        lang: 'ja',
        parsetags: 'onload'
    };

    (function () {
        var po = document.createElement('script');
        po.type = 'text/javascript';
        po.async = true;
        po.src = 'https://apis.google.com/js/client:plusone.js';
        var s = document.getElementsByTagName('script')[0];
        s.parentNode.insertBefore(po, s);
    })();

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
