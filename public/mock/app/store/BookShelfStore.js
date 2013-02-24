Ext.define('Mock.store.BookShelfStore', {
    extend: 'Ext.data.Store',
    xtype: 'shelf_store',
    config: {
        model: 'Mock.model.BookShelfItem',
        autoLoad: true,
        proxy: {
            type: 'ajax',
            url: 'shelfs',
            reader: {type: 'json', rootProperty: 'shelfs'}
        }

    }
});
