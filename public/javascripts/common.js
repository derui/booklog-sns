requirejs(['lib/underscore', 'lib/zepto', 'lib/pure', 'lib/moment'], function () {
    'use strict';

    // PUREをZepto.jsで使えるようにするおまじない
    (function ($) {
        $.extend($.fn, {
            directives: function (directive) {
                this._pure_d = directive;
                return this;
            },
            compile: function (directive, ctxt) {
                return $p(this).compile(this._pure_d || directive, ctxt);
            },
            render: function (ctxt, directive) {
                return Zepto(Array.prototype.slice.call($p(this).render(ctxt, this._pure_d || directive)));
            },
            autoRender: function (ctxt, directive) {
                return Zepto(Array.prototype.slice.call($p(this).autoRender(ctxt, this._pure_d || directive)));
            }
        });
    })(Zepto);

    // Ajax共通処理：Ajax通信前処理
    $(document).on('ajaxBeforeSend', function (e, xhr) {
        // CSRF対策
        xhr.setRequestHeader('X-From', location.href);
    });

    _.mixin({
        replaceAll: function (target, substr, newSubStr) {
            if (!target) {
                return target;
            }

            if (_.isRegExp(substr)) {
                return target.replace(new RegExp(substr.source, 'g'), newSubStr);
            }

            return target.replace(new RegExp(substr, 'g'), newSubStr);
        },
        queryString2json: function () {
            var queryStrings = location.search.replace('?', '').split('&');
            var json = {};
            for (var i = 0, queryString; queryString = queryStrings[i]; ++i) {
                var queryStringSplits = queryString.split('=');
                json[queryStringSplits[0]] = queryStringSplits[1];
            }

            return json;
        },
        getPrimaryKeyFromUrl: function (paramPathname) {
            var pathname = paramPathname || location.pathname;

            return pathname.split('/').pop();
        },
        environment: function (success, error) {
            $.ajax({
                url: '/api/detect_env',
                success: function (data) {
                    if (_.isFunction(success)) {
                        success(data.environment);
                    }
                },
                error: function () {
                    if (_.isFunction(error)) {
                        error();
                    }
                }
            });
        },
        loginUserInfo: function (success, error) {
            $.ajax({
                url: '/api/login_user_info',
                success: function (data) {
                    if (_.isFunction(success)) {
                        success(data.result[0]);
                    }
                },
                error: function () {
                    if (_.isFunction(error)) {
                        error();
                    }
                }
            });
        },
        convertLineBreak2BR: function (targetText) {
            return _(targetText).replaceAll('\n', '<br />');
        },
        parseDateForJST: function (targetDate) {
            return moment(targetDate).add('hours', 9);
        }
    });
});
