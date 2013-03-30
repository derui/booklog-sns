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
                    $('#signinArea').prepend('<span id="loginUserInfoArea"><img src="' + loginUserInfo.googlePublicProfilePhotoUrl + '" id="profilePhoto" /><span>' + loginUserInfo.googleDisplayName + '</span></span>');
                    $('#logoutButton').removeClass('disabled');
                }
            });
        }
    };

    window.___gcfg = {
        lang: 'ja',
        parsetags: 'onload'
    };

    var SigninButtonView = View.BaseView.extend({
        el: '.g-signin',
        initialize: function () {
            var $signinSetting = this.$el;

            $.getJSON('/api/detect_env', function (data) {
                var clientid = '';
                var environment = data.environment;

                switch (environment) {
                    case 'develop':
                        clientid = '881447546058.apps.googleusercontent.com';
                        break;
                    case 'production':
                        clientid = '881447546058-f3ref6v66g5tnkubcob4g6t1i35s78u4.apps.googleusercontent.com';
                        break;
                }

                $signinSetting.attr('data-clientid', clientid);

                // _.environment(); で環境が取得できるよう定義
                _.mixin({
                    environment: function () {
                        return environment;
                    }
                });
            });

            var po = document.createElement('script');
            po.type = 'text/javascript';
            po.async = true;
            po.src = 'https://apis.google.com/js/client:plusone.js';
            var s = document.getElementsByTagName('script')[0];
            s.parentNode.insertBefore(po, s);
        }
    });

    new SigninButtonView();

});
