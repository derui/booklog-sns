Ext.define('Mock.view.Main', {
    extend: 'Ext.Panel',
    xtype: 'main',
    requires: [
    ],
    config: {
        fullscreen:true,
        items: [
            {
                xtype: 'slide_menu',
            }
        ]
    }
});
