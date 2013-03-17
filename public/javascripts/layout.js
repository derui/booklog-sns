requirejs.config({
    baseUrl: '/javascripts'
});

requirejs(['lib/zepto'], function () {
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
                    $('#dissconectButton').removeClass('disabled');
                }
            });
        }
    };

    window.___gcfg = {
        lang: 'ja',
        parsetags: 'onload'
    };

    var po = document.createElement('script');
    po.type = 'text/javascript';
    po.async = true;
    po.src = 'https://apis.google.com/js/client:plusone.js';
    var s = document.getElementsByTagName('script')[0];
    s.parentNode.insertBefore(po, s);
});
