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

requirejs(['lib/zepto', 'lib/canvas-to-blob'], function (Backbone, Model, View) {
    'use strict';

    function gotStream(stream) {
        var video = $('#monitor')[0];
        video.src = window.URL.createObjectURL(stream);
        video.onload = function () {
            window.URL.revokeObjectURL(this.src);
        };
        video.onerror = function () {
            stream.stop();
            streamError();
        };
        document.getElementById('splash').hidden = true;
        document.getElementById('app').hidden = false;
    }

    function noStream() {
        document.getElementById('errorMessage').textContent = 'No camera available.';
    }

    function streamError() {
        document.getElementById('errorMessage').textContent = 'Camera error.';
    }

    function snapshot() {
        var video = $('#monitor')[0];
        drawImage2Canvas(video, video.videoWidth, video.videoHeight);
    }

    function update(e) {
        var file = e.target.files[0];
        var image = new Image();
        var reader = new FileReader();
        reader.onload = function (evt) {
            image.onload = function () {
                drawImage2Canvas(image, image.width, image.height);
            };
            // 画像のURLをソースに設定
            image.src = evt.target.result;
        };

        // ファイルを読み込み、データをBase64でエンコードされたデータURLにして返す
        reader.readAsDataURL(file);
    }

    function drawImage2Canvas(image, width, height) {
        var canvas = $('#photo')[0];
        canvas.width = width;
        canvas.height = height;
        canvas.getContext('2d').drawImage(image, 0, 0);
    }

    function searchBookByBarcode() {
        var canvas = $('#photo')[0];
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
                        console.log(response);
                    }
                });
            },
            'image/png'
        );
    }

    $(function () {
        if (navigator.webkitGetUserMedia) {
            navigator.webkitGetUserMedia({video: true}, gotStream, noStream);
        } else {
            document.getElementById('errorMessage').textContent = 'No native camera support available.';
        }

        $('#selectPhoto').on('change', update);
        $("#snapshotbutton").on('click', snapshot);
        $('#searchBookFromBarcode').on('click', function () {
            searchBookByBarcode();
        });
    });
});
