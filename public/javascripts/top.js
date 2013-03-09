requirejs.config({
			shim : {
				"lib/zepto" : {
					exports : "Zepto"
				},
				"lib/underscore" : {
					exports : "_"
				},
				"lib/pure" : {
					deps : ["../lib/zepto"],
					exports : "pure"
				},
				"lib/backbone" : {
					deps : ["../lib/zepto", "../lib/underscore"],
					exports : "Backbone"
				}
			}
		});

requirejs(['lib/backbone', 'lib/pure', 'common'], function(Backbone) {
	// TODO モジュール化
	var BaseModel = Backbone.Model.extend({});
	var BaseCollection = Backbone.Collection.extend({});
	var BaseView = Backbone.View.extend({});
	var BookShelf = BaseModel.extend({
				urlRoot : '/shelf',
				validate : function(attrs) {
					if (!attrs.name) {
						return '本棚の名称は必須です';
					}
				}
			});
	var BookShelfList = BaseCollection.extend({
				url : '/shelf',
				model : BookShelf,
				parse : function(json) {
					return json.result;
				}
			});

	var BookShelfInfoView = BaseView.extend({});

	var BookShelfInfoListView = BaseView.extend({
		el : '.bookshelfInfos',
		initialize : function() {
			_.bindAll(this, 'render');
			this.collection.bind('reset', this.render);
			this.collection.bind('add', this.add);
		},
		render : function() {
			var models = this.collection.models;
			this.$el.render({
						"shelfs" : models
					}, {
						'.bookshelfInfo' : {
							'bookshelfInfo<-shelfs' : {
								'.name' : function(arg) {
									return '<a href="#">' + arg.bookshelfInfo.item.attributes.shelf_name + '</a>';
								},
								'.description' : function(arg) {
									return arg.bookshelfInfo.item.attributes.shelf_description;
								},
								'.update_date':function(arg){
									return arg.bookshelfInfo.item.attributes.updated_date;
								}
							}
						}
					});
		},
		add : function(model) {
			console.log("add event called", model);
		}
	});
	var bookShelfList = new BookShelfList();
	new BookShelfInfoListView({
				collection : bookShelfList
			});

	bookShelfList.fetch();
});
