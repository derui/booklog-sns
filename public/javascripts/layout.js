requirejs.config({
    baseUrl: '/javascripts',
    shim: {
        "lib/zepto": {
            exports: "Zepto"
        },
        "lib/underscore": {
            exports: "_"
        }
    }
});

requirejs(['view', 'common', 'lib/zepto'], function (View) {
    'use strict';

    _.loginUserInfo(
        // ログインユーザの情報が取得できた場合、ログインユーザの情報を表示する
        function (loginUserInfo) {
            showLoginUserInfo(loginUserInfo);
        },
        // ログインユーザの情報が取得できなかった場合、未ログインとみなす
        function () {
            loginGooglePlus();
        });

    // ログアウトボタンのビュー
    // TODO ログアウト処理再検討のため、コメントアウト
//    var LogoutButtonView = View.BaseView.extend({
//        el: '#logoutButton',
//        events: {
//            "click": "logout"
//        },
//        logout: function () {
//            var $logoutButton = this.$el;
//            if ($logoutButton.hasClass('disabled')) {
//                return;
//            }
//
//            $.ajax({
//                type: 'POST',
//                url: '/api/logout',
//                success: function () {
//                    $('#loginUserInfoArea').remove();
//                    $logoutButton.addClass('disabled');
//                    var $signinButton = $('#___signin_0');
//
//                    if ($signinButton.length) {
//                        $signinButton.css('display', 'inline-block').show();
//                    } else {
//                        loginGooglePlus();
//                    }
//
//                }
//            });
//        }
//    });
//
//    var logoutButtonView = new LogoutButtonView();

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
                        showLoginUserInfo(loginUserInfo);
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
            switch (environment) {
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

    var LoginUserDisplayNameView = View.BaseView.extend({
        events: {'blur': 'updateUserName'},
        updateUserName: function () {
            $('#editButton').removeClass('disabled');
            var $loginUserDisplayName = this.$el;
            var loginUserDisplayName = $.trim($loginUserDisplayName.val());
            var prevLoginUserName = $loginUserDisplayName.attr('data-loginUserName');

            if (loginUserDisplayName && loginUserDisplayName !== prevLoginUserName) {
                var escapedLoginUserDisplayName = _.escape(loginUserDisplayName);
                $loginUserDisplayName.replaceWith('<span id="loginUserDisplayName" data-loginUserName="' +
                    escapedLoginUserDisplayName +
                    '">' +
                    escapedLoginUserDisplayName +
                    '</span>');
                $.ajax({
                    type: 'PUT',
                    url: '/api/login_user_info',
                    data: {"user_display_name": loginUserDisplayName}
                });
            } else {
                var escapedPrevLoginUserName = _.escape(prevLoginUserName);
                $loginUserDisplayName.replaceWith('<span id="loginUserDisplayName" data-loginUserName="' +
                    escapedPrevLoginUserName +
                    '">' +
                    escapedPrevLoginUserName +
                    '</span>');
            }
        }
    });

    function showLoginUserInfo(loginUserInfo) {
        var displayName = _.escape(loginUserInfo.user_display_name);
        $('#signinArea').prepend('<span id="loginUserInfoArea">' +
            '<img src="' + loginUserInfo.google_public_profile_photo_url + '" id="profilePhoto" />' +
            '<span id="loginUserDisplayName" data-loginUserName="' + displayName + '">' + displayName + '</span>' +
            '&nbsp;' +
            '<a href="#" id="editButton" class="btn btn-mini"><i class="icon-search icon-pencil"></i></a>' +
            '</span>');

        var EditButtonView = View.BaseView.extend({
            el: '#editButton',
            events: {'click': 'switchEditable'},
            switchEditable: function () {
                this.$el.addClass('disabled');
                var $loginUserDisplayName = $('#loginUserDisplayName');
                var loginUserDisplayName = _.escape($loginUserDisplayName.text());
                $loginUserDisplayName.replaceWith('<input type="text" id="loginUserDisplayName" class="input-small" value="' +
                    loginUserDisplayName +
                    '" data-loginUserName="' +
                    loginUserDisplayName + '" />');
                $('#loginUserDisplayName').focus();
                var loginUserDisplayNameView = new LoginUserDisplayNameView({el: '#loginUserDisplayName'});

                return false;
            }
        });

        var editButtonView = new EditButtonView();

        $('#logoutButton').removeClass('disabled');
    }
});
