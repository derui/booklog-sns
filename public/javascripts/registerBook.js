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

requirejs(['lib/backbone', 'model', 'view', 'lib/zepto', 'lib/moment', 'lib/canvas-to-blob'], function (Backbone, Model, View) {
    'use strict';

    var shelfId = _.queryString2json()['shelf_id'];

    // 検索結果の書籍のビュー
    var SearchResultBookView = View.BaseView.extend({
        el: '#searchResultBookList',
        render: function () {
            var bookModelJson = this.model.toJSON();
            var bookInfo = _.template($("#bookInfoTemplate").html(), bookModelJson);
            var $bookInfo = $(bookInfo);
            $bookInfo.data('bookInfoJson', JSON.stringify(bookModelJson));
            this.$el.append($bookInfo);
        }
    });

    // 書籍キーワード検索用フォームのビュー
    var SearchBookFormView = View.BaseView.extend({
        events: {'submit': 'searchBookForAmazon'},
        searchBookForAmazon: function () {
            $('#searchBookButton').attr('disabled', 'disabled');
            $('#searchResultBookList').empty();
            $.getJSON('/api/amazon_search/' + encodeURIComponent($('#searchBookKeyword').val()),
                function (data) {
                    $('#searchBookButton').removeAttr('disabled');
                    var books = data.result.items;
                    renderSearchBookResult(books);
                });
            return false;
        }
    });

    var searchBookFormView = new SearchBookFormView({el: $('#searchBookForm')});

    // スマートデバイス用ファイル選択フィールドのビュー
    var SelectPhotoFieldView = View.BaseView.extend({
        el: '#selectPhoto',
        events: {'change': 'previewPhoto'},
        previewPhoto: function (e) {
            var file = e.target.files[0];

            if (!file.type.match(/image.*/)) {
                _.showErrorMessage('ファイル形式が不正です');
                return false;
            }

            var reader = new FileReader();
            reader.onload = function (evt) {
                var image = new Image();
                image.onload = function () {
                    $('#searchByBarcodeForSmartDevice').removeClass('disabled');
                    var previewPhotoForSmartDevice = $('#previewPhotoForSmartDevice')[0];
                    var sizeInfo = calcImageSize(image);

                    drawImage2Canvas(previewPhotoForSmartDevice, image, sizeInfo.width, sizeInfo.height);
                };
                // 画像のURLをソースに設定
                image.src = evt.target.result;
            };
            // ファイルを読み込み、データをBase64でエンコードされたデータURLにして返す
            reader.readAsDataURL(file);

            return false;
        }
    });

    new SelectPhotoFieldView();

    // バーコード検索ボタン（スマートデバイス用）のビュー
    var SearchByBarcodeForSmartDeviceButtonView = View.BaseView.extend({
        el: '#searchByBarcodeForSmartDevice',
        events: {'click': 'searchBookByBarcodeForSmartDevice'},
        searchBookByBarcodeForSmartDevice: function () {
            var $searchButton = this.$el;
            $searchButton.addClass('disabled');
            searchBookByBarcode($('#previewPhotoForSmartDevice')[0], $searchButton);

            return false;
        }
    });

    new SearchByBarcodeForSmartDeviceButtonView();

    // カメラ起動ボタンのビュー
    var LaunchCameraButtonView = View.BaseView.extend({
        el: '#launchCamera',
        events: {'click': 'launchCameara'},
        launchCameara: function () {
            var $launchCameraButton = this.$el;

            if ($launchCameraButton.hasClass('disabled')) {
                return false;
            } else {
                $launchCameraButton.addClass('disabled');
            }

            if (navigator.webkitGetUserMedia) {
                $('#snapshotbutton').removeClass('disabled');
                navigator.webkitGetUserMedia({video: true}, gotStream, failLaunchCamera);
            } else {
                failLaunchCamera();
            }

            return false;
        }
    });

    new LaunchCameraButtonView();

    // 写真を撮るボタンのビュー
    var SnapshotButtonView = View.BaseView.extend({
        el: '#snapshotbutton',
        events: {'click': 'snapshot'},
        snapshot: function () {
            $('#searchByBarcodeForPC').removeClass('disabled');
            var $previewPhoto = $('#previewPhotoForPC');
            drawImage2Canvas($previewPhoto[0], $('#monitor')[0], $previewPhoto.width(), $previewPhoto.height());
            return false;
        }
    });

    new SnapshotButtonView();

    // バーコード検索ボタン（PC用）のビュー
    var SearchByBarcodeForPCButtonView = View.BaseView.extend({
        el: '#searchByBarcodeForPC',
        events: {'click': 'searchBookByBarcodeForPC'},
        searchBookByBarcodeForPC: function () {
            var $searchButton = this.$el;
            $searchButton.addClass('disabled');
            searchBookByBarcode($('#previewPhotoForPC')[0], $searchButton);

            return false;
        }
    });

    new SearchByBarcodeForPCButtonView();

    // 指定されたcanvasに描画された画像を元に、バーコード検索を行う
    function searchBookByBarcode(canvas, $searchButton) {
        // エラーメッセージがあれば消去する
        _.hideMessages();
        // バーコード検索APIに渡すため、canvasに描画された画像をBlobに変換する
        canvas.toBlob(
            function (newBlob) {
                var formdata = new FormData();
                formdata.append("capture", newBlob);
                $.ajax({
                    url: '/api/amazon_barcode',
                    data: formdata,
                    cache: false,
                    contentType: false, // デフォルト：application/x-www-form-urlencoded; charset=UTF-8
                    processData: false, // デフォルト：application/x-www-form-urlencoded
                    dataType: "json",
                    type: 'POST',
                    success: function (response) {
                        renderSearchBookResult(response.result);
                    },
                    error: function () {
                        _.showErrorMessage('バーコードの読み取りに失敗しました');
                    },
                    complete: function () {
                        $searchButton.removeClass('disabled');
                    }
                });
            },
            'image/png'
        );
    }

    // canvasのサイズにあわせて画像を縮小した時のwidth、heightを計算して返す
    function calcImageSize(image) {
        var width = image.width;
        var height = image.height;
        var longSideSize = (width >= height) ? width : height;
        var targetWidth = $('#previewPhotoForSmartDevice').width();
        var adjustmentWidth, adjustmentHeight;

        if (longSideSize < targetWidth) {
            adjustmentWidth = width;
            adjustmentHeight = height;
        } else {
            adjustmentWidth = parseFloat(targetWidth) / longSideSize * width;
            adjustmentHeight = parseFloat(targetWidth) / longSideSize * height;
        }

        return {
            width: parseInt(adjustmentWidth),
            height: parseInt(adjustmentHeight)
        };
    }

    // カメラから取得したストリームをvideoタグに設定する
    function gotStream(stream) {
        $('#monitor')
            .attr('src', window.URL.createObjectURL(stream))
            .on('error', function () {
                stream.stop();
                _.showErrorMessage('カメラでエラーが発生しました');
            });
    }

    // カメラの起動に失敗した時の処理
    function failLaunchCamera() {
        _.showErrorMessage('カメラを起動できませんでした');
    }

    // 引数で指定された画像をcanvasに描画する
    function drawImage2Canvas(target, image, width, height) {
        target
            .getContext('2d')
            .drawImage(image, 0, 0, width, height);
    }

    // 検索結果の本一覧を描画する
    function renderSearchBookResult(books) {
        // 検索結果一覧をクリアする
        $('#searchResultBookList').empty();

        for (var i = 0, book; book = books[i]; ++i) {
            var bookView = new SearchResultBookView({model: new Model.Book(book)});
            bookView.render();
        }

        // 検索結果一覧にスクロールする
        _.scrollTo('#searchResultArea');

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
    }

    $(function () {
        var $bookshelfAnchorLink = $('#bookshelfAnchorLink');
        $bookshelfAnchorLink.attr('href', $bookshelfAnchorLink.attr('href') + shelfId);
    });
});
