define(['lib/backbone'], function () {
    'use strict';

    var BaseModel = Backbone.Model.extend({
        urlRoot: function () {
            return '/api' + this.path;
        }
    });

    var BaseCollection = Backbone.Collection.extend({
        url: function () {
            return '/api' + this.path;
        },
        parse: function (json) {
            var result = json.result;
            // XSS対策
            var escapedResult = _.map(result, function (element, index) {
                for (var key in element) {
                    if (element.hasOwnProperty(key)) {
                        element[key] = _.escape(element[key]);
                    }
                }

                return element;
            });

            return escapedResult;
        }
    });

    var BookShelf = BaseModel.extend({
        path: '/shelf',
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
        path: '/shelf',
        model: BookShelf
    });


    var Rental = BaseModel.extend({
        path: '/rental',
        validate: function (attrs) {
            if (!attrs.rental_book) {
                return '書籍のIDは必須です';
            }
        }
    });

    var RentalList = BaseCollection.extend({
        path: '/rental',
        model: Rental
    });

    return {
        BookShelf: BookShelf,
        BookShelfList: BookShelfList,
        Rental: Rental,
        RentalList: RentalList
    };
});
