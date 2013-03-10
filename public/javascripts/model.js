define(['lib/backbone'], function () {
    'use strict';

    var BaseModel = Backbone.Model.extend({});
    var BaseCollection = Backbone.Collection.extend({});

    var BookShelf = BaseModel.extend({
        urlRoot: '/shelf',
        validate: function (attrs) {
            if (!attrs.name) {
                return '本棚の名称は必須です';
            }
        }
    });

    var BookShelfList = BaseCollection.extend({
        url: '/shelf',
        model: BookShelf,
        parse: function (json) {
            return json.result;
        }
    });

    return {
        BookShelf : BookShelf,
        BookShelfList : BookShelfList
    };
});
