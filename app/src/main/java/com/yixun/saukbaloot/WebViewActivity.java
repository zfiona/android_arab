package com.yixun.saukbaloot;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import com.yixun.message.Message;

public class WebViewActivity extends Activity
{
	@SuppressLint("SetJavaScriptEnabled") @Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		int mainId = getResources().getIdentifier( "main" , "layout" , getPackageName());
		int webViewId = getResources().getIdentifier( "webView" , "id" , getPackageName());
		String url = getIntent().getStringExtra("url");

		setContentView(mainId);
		WebView webView = (WebView) findViewById(webViewId);
		webView.loadUrl(url);
		webView.setWebViewClient(new WebViewClient());
		
		webView.getSettings().setJavaScriptEnabled(true);
		webView.addJavascriptInterface(new JsCallAndroid(), "android");
	}
 
	private class WebViewClient extends android.webkit.WebViewClient {
		@SuppressWarnings("deprecation")
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {	
			Uri uri = Uri.parse(url);
			if ( uri.getScheme().equals("js")) {
				String msg = uri.getAuthority();
				Message.UnityLog("recharge:" + msg);
                //document.location = "js://cancel"; document.location = "js://success";
                WebViewActivity.this.finish();
                return true;
            }			
			view.loadUrl(url);
			return super.shouldOverrideUrlLoading(view, url);
		}
	}
	
	private class JsCallAndroid {
	    @JavascriptInterface
	    public void call(String msg) {	
	    	//window.android.call("success"); window.android.call("cancel");
			Message.UnityLog("recharge:" + msg);
	    	WebViewActivity.this.finish();
	    }	    
	}
}
