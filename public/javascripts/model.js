define(['lib/backbone'], function () {
    'use strict';

    var BaseModel = Backbone.Model.extend({});
    var BaseCollection = Backbone.Collection.extend({});

    var BookShelf = BaseModel.extend({
        urlRoot: '/shelf',
        validate: function (attrs) {
            if (!attrs.shelf_name) {
                return '本棚の名前は必須です';
            }

            if (!attrs.shelf_description) {
                return '本棚の説明は必須です';
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
