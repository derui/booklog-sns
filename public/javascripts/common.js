requirejs(['lib/zepto','lib/pure'], function () {
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
        })
    })(Zepto);
});
