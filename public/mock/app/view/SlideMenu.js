var menuConfig =
    [
        { title : 'ホーム',
          xtype : 'navigationview',
          navigationBar :
          { items :
            { xtype : 'titlebar',
              title : '本棚'
            }
          },
          items :
          [
              { xtype : 'shelf_list',
                title : '本棚',
              }
          ]
        },
        { title : 'ランキング',
          iconCls : 'ranking',
          xtype : 'navigationview',
          navigationBar :
          { items :
            { xtype : 'titlebar',
              title : 'ranking'
            }
          },
          items :
          [
              { xtype : 'shelf_list',
                title : 'ranking',
              }
          ]
        }
    ]

// Viewport.getSize()は、{width:number,height:number}という構造。
// width プロパティが%表記でも、実際のサイズが設定される。
var getDesiredWidth = function() {
    return Ext.Viewport.getSize().width * 0.8;
};

Ext.define("Mock.view.SlideMenu",
           { extend : 'Ext.ux.slidenavigation.View',
             xtype : 'slide_menu',

             requires :
             [ 'Ext.TitleBar', 'Ext.event.publisher.Dom', 'Ext.tab.Panel'
             ],

             push : function(config) {
                 return this.push(config);
             },

             config :
             {

                 // ドラッグでメニューをスライドさせる要素のclassを指定する。
                 // x-というのは、sencha touchで定義しているデフォルトのプレフィックス。
                 slideSelector : 'x-container',

                 containerSlideDelay : 0,

                 selectSlideDuration : 100,

                 itemMask : true,
                 width : '100%',

                 list :
                 { maxDrag : '90%',
                   width : getDesiredWidth(),
                   grouped : false,
                   items :
                   [
                       { docked : 'top',
                         ui : 'light',
                         title :
                         { xtype : 'toolbar',
                           ui : 'light',
                           title : 'slide menu',
                           centered : false,
                           left : 0
                         }
                       }
                   ]

                 },

                 listPosition : 'right',

                 defaults :
                 { style : 'background: #fff',
                   xtype : 'container'
                 },

                 // itemsの1レコードが、slidenavigationの一つの項目として扱われる。
                 items :
                 [
                     { slideButton :
                       { selector : 'titlebar',
                         iconMask : true,
                         iconCls : 'more',
                         align : 'right',
                       },

                       title : '本棚一覧',
                       xtype : 'panel',
                       items :
                       [
                           // { xtype : 'toolbar',
                           //   id : 'tab-like-toolbar',
                           //   docked : 'bottom',
                           //   padding : '0 0 0 0',
                           //   items :
                           //   [
                           //       { xtype : 'button',
                           //         text : '本棚を作る',
                           //         action : 'goto_shelf_maker'
                           //       },
                           //       { xtype : 'button',
                           //         text : '本を登録する',
                           //         action : 'goto_book_register'
                           //       }
                           //   ]
                           // },

                           { title : '本棚一覧',
                             xtype : 'navigationview',
                             navigationBar :
                             { items :
                               { xtype : 'titlebar',
                               }
                             },
                             items :
                             [
                                 { xtype : 'shelf_list',
                                   title : '本棚'
                                 }
                             ]
                           }
                       ]
                     },
                     { slideButton :
                       { selector : 'titlebar',
                         iconMask : true,
                         iconCls : 'more',
                         align : 'right',
                       },

                       title : '本棚一覧2',
                       xtype : 'panel',
                       items :
                       [
                           { title : '本棚一覧',
                             xtype : 'navigationview',
                             navigationBar :
                             { items :
                               { xtype : 'titlebar',
                               }
                             },
                             items :
                             [
                                 { xtype : 'shelf_list',
                                   title : '本棚',
                                 }
                             ]
                           }
                       ]
                     }
                 ]
             }
           });
