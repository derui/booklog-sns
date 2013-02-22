Ext.define('Mock.view.ShelfMaker',
  { extend : 'Ext.form.Panel',
  xtype : 'shelf_maker_form',
  requires : [],
  config :
    { items :
      [
        { xtype : 'textfield',
        name : 'name',
        label : '本棚のなまえ'
        },
        { xtype : 'button',
        text : '作る',
        ui: 'action',
        action : 'make_shelf'
        }
      ]
    }
  });
