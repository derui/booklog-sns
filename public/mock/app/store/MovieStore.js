Ext.define('Mock.store.MovieStore', {
    extend: 'Ext.data.Store',
    xtype: 'movie_store',
    config: {
        model: 'Mock.model.ListItem',
        autoLoad: true
    }
});
