Ext.define('Mock.view.BookShelfList', {
    extend: 'Ext.dataview.List',
    xtype: 'shelf_list',
    require : [
        'Ext.MessageBox'
    ],

    // Ext.createでdataだけを変更すればよい。
    config: {
        itemCls: 'x-extended-list-item',
        store: {xtype:'shelf_store'},
        itemTpl: 'hoge{name}'
    }
});
