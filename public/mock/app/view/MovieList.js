
Ext.define('Mock.view.MovieList', {
    extend: 'Ext.dataview.List',
    xtype: 'movie_list',
    requires: [
        'Ext.data.TreeStore'
    ],

    config: {
        // このリストで利用するstore。Ext.createでstoreプロパティを設定すれば、
        // 利用するstoreを上書きすることができる。
        store : 'MovieStore',
        
        emptyText : 'Sorry, we have no movies...',
        // リストのアイテム内容についてのテンプレート
        itemTpl : [
            '<div><span style="float: left"><img src="{image_url}" width="128" height="96"/></span>',
            '<div style="margin-left:100px;height:100%";float: none">{title}</div>',
            '<span>価格: {price}円 (税込)</span>',
            '<span>配信期間: {distribute_limit}まで</span></div>'
        ].join("")
    }
});
