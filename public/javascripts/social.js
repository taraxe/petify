(function($){
    var methods = {

        /**
         * this should be used only to pop authentication pages for facebook/twitter
         * actually it is not so useful because the auth page will be promped when
         * you invoke the 'tweet' and 'share' methods while not authenticated
         *
         * @param options
         */
        popup : function(options){
            if (!options || !options.path) {throw new Error("options.path must not be empty");}
            options = $.extend({
                windowName: 'ConnectWithOAuth' // should not include space for IE
                , windowOptions: 'personalbar=0,toolbar=0,scrollbars=1,resizable=1,location=0,status=0,width=800,height=400'
                , callback: function(){ window.location.reload(); }
            }, options);
            var oauthWindow   = window.open(options.path, options.windowName, options.windowOptions);
            var oauthInterval = window.setInterval(function(){
                if (oauthWindow.closed) {
                    window.clearInterval(oauthInterval);
                    options.callback();
                }
            }, 1000);
            return this;
        },

        tweet : function(options){
            if(!options || !options.text){throw new Error("options.text must not be empty");}
            window.twttr = window.twttr || {};
            var D = 550, A = 450, C = screen.height, B = screen.width, H = Math.round((B / 2) - (D / 2)), G = 0, F = document, E;
            if (C > A) {G = Math.round((C / 2) - (A / 2))}
            window.twttr.shareWin = window.open('http://twitter.com/share?url='+window.location.protocol + "//" + window.location.host+'&text='+options.text, '', 'left=' + H + ',top=' + G + ',width=' + D + ',height=' + A + ',personalbar=0,toolbar=0,scrollbars=1,resizable=1');
            E = F.createElement('script');
            E.src = 'http://platform.twitter.com/widget.js';
            F.getElementsByTagName('head')[0].appendChild(E);
            return this;
        },

        publish:function (opts) {

            (function (d) {
                var js, id = 'facebook-jssdk';
                if (d.getElementById(id)) {
                    FB.ui({
                        method:'stream.publish',
                        message:opts.text
                    });
                    return this;
                }
                js = d.createElement('script');
                js.id = id;
                js.async = true;
                js.src = "//connect.facebook.net/en_US/all.js";
                d.getElementsByTagName('head')[0].appendChild(js);

                window.fbAsyncInit = function () {
                    FB.ui({
                        method:'stream.publish',
                        message:opts.text
                    });
                };
            })(document);
            return this;
        }
    };
    $.social = function (method) {
        if (methods[method]) {
            return methods[method].apply(this, Array.prototype.slice.call(arguments, 1))
        }/*else if ( typeof method === 'object' || ! method ) {
         return methods.init.apply( this, arguments );
         }*/ else {
            $.error('Method ' + method + ' does not exist on jQuery.social')
        }
    };
})(jQuery);
