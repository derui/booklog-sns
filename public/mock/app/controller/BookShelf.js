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
        var store = Ext.create('Mock.store.MovieStore',
            { data : moviesPerGenre['genre' + (index + 1)]
            });
        var list = Ext.create('Mock.view.MovieList',
            { store : store,
            title : record.get('text')
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
            { url : 'makeshelf',
            method : "POST",
            params :
                { shelf_name : Ext.JSON.encode(name.getData()) },
            success : function() {
                alert("本棚ができました。");
            },
            failure : function() {
                alert("本棚ができませんでした。。。");
            }

            });
        parent.up('navigationview').pop();
    },

    // called when the Application is launched, remove if not needed
    launch : function(app) {

    }
    });
