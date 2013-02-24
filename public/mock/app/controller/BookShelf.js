var moviesPerGenre =
    { genre1 :
        [
            { title : 'movie title 1',
            price : '1000',
            distribute_limit : '2013/12/31',
            image_url : './img/nomovie.png'
            },
            { title : 'movie title 2',
            price : '3000',
            distribute_limit : '2014/12/31',
            image_url : './img/nomovie.png'
            }
        ],
    genre2 :
        [
            { title : 'movie title 3',
            price : '2000',
            distribute_limit : '2013/12/31',
            image_url : './img/nomovie.png'
            },
            { title : 'movie title 4',
            price : '4000',
            distribute_limit : '2014/12/31',
            image_url : './img/nomovie.png'
            }
        ]
    }

Ext.define('Mock.controller.BookShelf',
    { extend : 'Ext.app.Controller',

    config :
        {
        // 指定したxtype/idを持つコンポーネントの参照を保持する
        refs :
            { genreList : 'shelf_list',
            tabbar : '#tab-like-toolbar'
            },
        // 指定した参照/xtype/idのコンポーネントについて、イベントとハンドラを
        // 定義する。
        control :
            { 'genreList' :
                { itemtap : 'onItemTap'
                },
            'button[action=goto_shelf_maker]' :
                { tap : 'onGotoShelfMaker'
                },
            'button[action=make_shelf]' :
                { tap : 'onMakeShelf'
                }
            }
        },

    onItemTap : function(list, index, target, record) {
        // storeを新しく作成して、切り替える。
        // ここで渡されるlistは、navigationviewの直下に常にいるため、
        // 親を取得できる
        var parent = list.up('navigationview');
        var store = Ext.create(
            'Ext.data.Store',
            { model: 'Mock.model.BookItem',
              proxy : {
                  type:'ajax',
                  url : 'books?shelf=' + record.data['id'],
                  reader: {
                      type : 'json',
                      rootProperty : 'books'
                  }
              },
              autoLoad : true
            });
        var list = Ext.create('Mock.view.BookList',
            { store : store,
              title : record.get('name') + 'の中身',
            });

        parent.push(list);
    },

    onGotoShelfMaker : function(button, event, opts) {
        var parent = button.up('panel');
        var navigation = parent.down('navigationview');

        // 新規に画面を作成して遷移する。
        var maker = Ext.create('Mock.view.ShelfMaker');
        navigation.push(maker);
    },

    onMakeShelf : function(button) {
        // 受け取ったbuttonの周辺からデータを取得する
        var parent = button.up('shelf_maker_form');
        var name = parent.down('textfield');

        Ext.Ajax.request(
            { url : 'http://localhost:9000/makeshelf',
            method : "POST",
            params :
                { shelf_name : name.getValue() },
            success : function() {
                alert("本棚ができました。");
            },
            failure : function() {
                alert("本棚ができませんでした。。。");
            }

            });
        parent.up('navigationview').down('shelf_list').getStore().load();
        parent.up('navigationview').pop();
    },

    // called when the Application is launched, remove if not needed
    launch : function(app) {

    }
    });
