
Ext.define('Mock.view.BookList', {
    extend: 'Ext.dataview.List',
    xtype: 'book_list',
    requires: [
    ],

    config: {
        // このリストで利用するstore。Ext.createでstoreプロパティを設定すれば、
        // 利用するstoreを上書きすることができる。
        emptyText : 'we have no any books in the book shelf...',
        // リストのアイテム内容についてのテンプレート
        itemTpl : [
            '<div><span style="float: left"><img src="{image_url}" width="128" height="96"/></span>',
            '<div style="margin-left:100px;height:100%";float: none">{name}</div>',
            '<span>著者:{author}</span>',
            '<span>ISBN:{isbn}</span></div>'
        ].join(""),
        items : { xtype: 'button',
                  text: '本を追加する',
                  action: 'goto_shelf_maker',
                  ui: 'action',
                  docked: 'bottom',
                }

    }
});
