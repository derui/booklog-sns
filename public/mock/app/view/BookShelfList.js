Ext.define('Mock.view.BookShelfList', {
    extend: 'Ext.dataview.List',
    xtype: 'shelf_list',
    require : [
        'Ext.MessageBox'
    ],

    // Ext.createでdataだけを変更すればよい。
    config: {
        itemCls: 'x-extended-list-item',
        data: [{text: 'genre 1'},
               {text: 'genre 2'},
               {text: 'genre 3'}],
        itemTpl: '{text}'
    }
});
