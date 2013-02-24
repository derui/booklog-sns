Ext.define('Mock.model.BookItem', {
    extend: 'Ext.data.Model',
    config: {
        fields: [
            {name: 'id', type:'number'},
            {name: 'shelf_id', type:'number'},
            {name: 'name', type:'string'},
            {name: 'author', type: 'string'},
            {name: 'isbn', type: 'string'}
        ]
    }
});
