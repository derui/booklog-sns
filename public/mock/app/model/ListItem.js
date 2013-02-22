Ext.define('Mock.model.ListItem', {
    extend: 'Ext.data.Model',
    config: {
        fields: [
            {name: 'title', type:'string'},
            {name: 'price', type:'number'},
            {name: 'distribute_limit', type: 'string'},
            {name: 'image_url', type:'string'}
        ]
    }
});
