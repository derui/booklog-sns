requirejs.config({
    shim: {
        "lib/zepto": {
            exports: "Zepto"
        },
        "lib/underscore": {
            exports: "_"
        },
        "lib/pure": {
            deps: ["../lib/zepto"],
            exports: "pure"
        },
        "lib/backbone": {
            deps: ["../lib/zepto", "../lib/underscore"],
            exports: "Backbone"
        },
        "lib/moment": {
            exports: "moment"
        }
    }
});

requirejs(['lib/backbone', 'model', 'view', 'common', 'lib/zepto', 'lib/moment'], function (Backbone, Model, View) {
    'use strict';

    var SearchBookFormView = View.BaseView.extend({
        events: {'submit': 'searchBookForAmazon'},
        searchBookForAmazon: function () {
            $('#searchBookButton').attr('disabled', 'disabled');
            $('#searchResultBookList').empty();
            $.getJSON('/api/amazon_search/' + encodeURIComponent($('#searchBookKeyword').val()),
                function (data) {
                    var books = data.result.items;
                    for (var i = 0, book; book = books[i]; ++i) {
                        var bookView = new searchResultBookView({model: new Model.Book(book)});
                        bookView.render();
                    }

                    $('#searchBookButton').removeAttr('disabled');

                    var RegisterButtonView = View.BaseView.extend({
                        el: '.registerButton',
                        events: {
                            'click': 'registerBook'
                        },
                        registerBook: function (e) {
                            var $targetBookInfo = $(e.target).closest('.bookInfo');
                            var bookInfo = JSON.parse($targetBookInfo.data('bookInfoJson'));
                            var data = {
                                'shelf_id': shelfId,
                                'book_name': bookInfo.title,
                                'book_author': bookInfo.author,
                                'large_image_url': bookInfo.large_image,
                                'medium_image_url': bookInfo.medium_image,
                                'small_image_url': bookInfo.small_image,
                                'book_isbn': bookInfo.isbn,
                                'published_date': bookInfo.publication_date
                                    ? moment(bookInfo.publication_dat).format('YYYY/MM/DD')
                                    : ''
                            };

                            this.model.save(data, {success: function (model, response, options) {
                                location.href = '/book/detail/' + response.result[0].id;
                            }});

                            return false;
                        }
                    });

                    var registerButtonView = new RegisterButtonView({model: new Model.Book()});
                });
            return false;
        }
    });

    var searchBookFormView = new SearchBookFormView({el: $('#searchBookForm')});
    var shelfId = _.queryString2json()['shelf_id'];

    var searchResultBookView = View.BaseView.extend({
        el: '#searchResultBookList',
        render: function () {
            var bookModelJson = this.model.toJSON();
            var bookInfo = _.template($("#bookInfoTemplate").html(), bookModelJson);
            var $bookInfo = $(bookInfo);
            $bookInfo.data('bookInfoJson', JSON.stringify(bookModelJson));
            this.$el.append($bookInfo);
        }
    });

    $(function () {
        var $bookshelfAnchorLink = $('#bookshelfAnchorLink');
        $bookshelfAnchorLink.attr('href', $bookshelfAnchorLink.attr('href') + shelfId);
    });
});
