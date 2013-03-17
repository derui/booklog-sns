define(['lib/backbone'], function () {
    'use strict';

    var BaseModel = Backbone.Model.extend({});

    var BaseCollection = Backbone.Collection.extend({
        parse: function (json) {
            var result = json.result;
            // XSS対策
            var escapedResult = _.map(result, function (element, index) {
                for(var key in element){
                    if(element.hasOwnProperty(key)){
                        element[key] = _.escape(element[key]);
                    }
                }

                return element;
            });

            return escapedResult;
        }
    });

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
        model: BookShelf
    });


    var Rental = BaseModel.extend({
        urlRoot: '/rental',
        validate: function (attrs) {
            if (!attrs.rental_book) {
                return '書籍のIDは必須です';
            }
        }
    });

    var RentalList = BaseCollection.extend({
        url: '/rental',
        model: Rental
    });

    return {
        BookShelf: BookShelf,
        BookShelfList: BookShelfList,
        Rental: Rental,
        RentalList: RentalList
    };
});
