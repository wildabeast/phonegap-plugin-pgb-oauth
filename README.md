# Phonegap Build Oauth Plugin

Use this plugin in a PhoneGap / Cordova application to authenticate users via Oauth on Phonegap Build, and obtain an access token which can be used with the [PhoneGap Build Developer API](http://docs.phonegap.com/phonegap-build/developer-api/). 

### Supported Platforms

iOS only so far.

### Installation

	plugman install --platform ios --project /path/to/myproject --plugin https://github.com/wildabeast/phonegap-plugin-pgb-oauth.git --var CLIENT_ID=5tg33it CLIENT_SECRET=93htf93hr

A ```CLIENT_ID``` and ```CLIENT_SECRET``` can be obtained by [registering your application on Phonegap Build](http://docs.phonegap.com/phonegap-build/developer-api/oauth/).

### Usage

Simple username / password login:

	PhonegapBuildOauth.login(username, password, successCallback, failureCallback);

Example:

	PhonegapBuildOauth.login("wildabeast@github.com", "password", function(r) {
		console.log('Authenticated successfully, access token is ' + r.access_token);
	}, function(r) {
		console.log('Failed to authenticate, response code was ', r.code);
	});

Using oauth web flow and the inappbrowser plugin:

	var API_HOST = "build.phonegap.com";
	var authWindow = cordova.InAppBrowser.open(API_HOST + "/authorize?client_id=5tg33it", "_blank", "clearcache=yes");

    authWindow.addEventListener('loadstart', function(e) {
        var url = e.url;
        if (url.match(/^(https?:\/\/)phonegap\.com\/?\?(code|error)=[a-zA-Z0-9]*$/)) {
            console.log('Callback url found.')
            var qs = getQueryString(url);
            if (qs['code']) {
                authWindow.close();
                PhonegapBuildOauth.authorizeByCode(qs['code'], function(a) {
                    var access_token = a.access_token;
                    // can now use this access_token to query the PGB API
                }, function(a) {
                    console.log("Auth failure: " + a.message);
                });
            } else if (qs['error']) {
                console.log("Auth failure: " + a.message);
            }
        }
    });

    // query string helper function

	function getQueryString(url) {
	    var a = url.slice((url.indexOf('?') + 1)).split('&')
	    if (a == "") return {};
	    var b = {};
	    for (var i = 0; i < a.length; ++i)
	    {
	        var p=a[i].split('=', 2);
	        if (p.length == 1)
	            b[p[0]] = "";
	        else
	            b[p[0]] = decodeURIComponent(p[1].replace(/\+/g, " "));
	    }
	    return b;
	}