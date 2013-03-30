requirejs.config({
    baseUrl: '/javascripts',
    shim: {
        "lib/zepto": {
            exports: "Zepto"
        },
        "lib/underscore": {
            exports: "_"
        },
        "lib/pure": {
            deps: ["lib/zepto"],
            exports: "pure"
        },
        "lib/backbone": {
            deps: ["lib/zepto", "lib/underscore"],
            exports: "Backbone"
        }
    }
});

requirejs(['lib/backbone', 'view', 'lib/pure', 'lib/zepto', 'common'], function (Backbone, View) {
    'use strict';

    _.loginUserInfo(
        // ログインユーザの情報が取得できた場合、ログインユーザの情報を表示する
        function (loginUserInfo) {
            $('#signinArea').prepend('<span id="loginUserInfoArea">' +
                '<img src="' + loginUserInfo.google_public_profile_photo_url + '" id="profilePhoto" />' +
                '<span>' + loginUserInfo.google_display_name + '</span>' +
                '</span>');
            $('#logoutButton').removeClass('disabled');
        },
        // ログインユーザの情報が取得できなかった場合、未ログインとみなす
        function () {
            loginGooglePlus();
        });

    // ログアウトボタンのビュー
    var LogoutButtonView = View.BaseView.extend({
        el: '#logoutButton',
        events: {
            "click": "logout"
        },
        logout: function () {
            var $logoutButton = this.$el;
            if ($logoutButton.hasClass('disabled')) {
                return;
            }

            $.ajax({
                type: 'POST',
                url: '/api/logout',
                success: function () {
                    $('#loginUserInfoArea').remove();
                    $logoutButton.addClass('disabled');
                    var $signinButton = $('#___signin_0');

                    if ($signinButton.length) {
                        $signinButton.css('display', 'inline-block').show();
                    } else {
                        loginGooglePlus();
                    }

                }
            });
        }
    });

    var logoutButtonView = new LogoutButtonView();

    function loginGooglePlus() {
        // Google+でログイン後に呼ばれるコールバック関数を定義する
        window.signinCallback = function signinCallback(authResult) {
            if (authResult.access_token) {
                // ログインボタンから認証すると、g-oauth-windowにWindowオブジェクトがセットされ
                // JSON.stringify()でパースエラーとなるため、nullで初期化する
                authResult['g-oauth-window'] = null;

                $.ajax({
                    type: 'POST',
                    url: '/api/connect',
                    contentType: "application/json",
                    data: JSON.stringify(authResult),
                    success: function (response) {
                        var loginUserInfo = response.result[0];
                        $('#___signin_0').hide();
                        $('#signinArea').prepend('<span id="loginUserInfoArea">' +
                            '<img src="' + loginUserInfo.googlePublicProfilePhotoUrl + '" id="profilePhoto" />' +
                            '<span>' + loginUserInfo.googleDisplayName + '</span>' +
                            '</span>');
                        $('#logoutButton').removeClass('disabled');
                    }
                });
            } else {
                // TODO システムエラー画面に遷移する
            }
        };

        // 環境を取得する
        _.environment(function (environment) {
            var clientid = '';

            // 環境に応じてclientidを切り替える
            switch (_.environment()) {
                case 'develop':
                    clientid = '881447546058.apps.googleusercontent.com';
                    break;
                case 'production':
                    clientid = '881447546058-f3ref6v66g5tnkubcob4g6t1i35s78u4.apps.googleusercontent.com';
                    break;
            }

            $('.g-signin').attr('data-clientid', clientid);

            window.___gcfg = {
                lang: 'ja',
                parsetags: 'onload'
            };

            var SigninButtonView = View.BaseView.extend({
                el: '.g-signin',
                initialize: function () {
                    var po = document.createElement('script');
                    po.type = 'text/javascript';
                    po.async = true;
                    po.src = 'https://apis.google.com/js/client:plusone.js';
                    var s = document.getElementsByTagName('script')[0];
                    s.parentNode.insertBefore(po, s);
                }
            });

            var signinButtonView = new SigninButtonView();
        });
    }
});
