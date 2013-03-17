requirejs(['lib/zepto', 'lib/pure'], function () {
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

    window.jQuery = Zepto;

    // Ajax共通処理：Ajax通信前処理
    $(document).on('ajaxBeforeSend', function (e, xhr, options) {
        // CSRF対策
        xhr.setRequestHeader('X-From', location.href);
    });

    _.mixin({
        replaceAll : function(target, substr, newSubStr){
            if(!target){
                return target;
            }

            if(_.isRegExp(substr)){
                return target.replace(new RegExp(substr.source,'g'), newSubStr);
            }

            return target.replace(new RegExp(substr,'g'), newSubStr);
        }
    });
});
