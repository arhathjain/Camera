package com.arhath.camera.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.webkit.HttpAuthHandler;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.arhath.camera.R;
import com.google.gson.GsonBuilder;

import java.util.HashMap;
import java.util.Map;

public class DeveloperInfoActivity extends AppCompatActivity
{

    WebView mywebview;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_developer_info);
        mywebview = (WebView) findViewById(R.id.webView);
        this.setTitle("Developer Info");

        loadUrl("https://arhathjain.github.io/");

    }
    public void loadUrl(String url)
    {
        final ProgressDialog dialog = ProgressDialog.show(this, "", "Loading", true);
        dialog.setCancelable(true);
        mywebview.setVerticalScrollBarEnabled(false);
        mywebview.setHorizontalScrollBarEnabled(false);
        WebSettings webSettings = mywebview.getSettings();

        webSettings.setJavaScriptEnabled(true);
        webSettings.setAppCacheMaxSize( 5 * 1024 * 1024 ); // 5MB
        webSettings.setAppCachePath( getApplicationContext().getCacheDir().getAbsolutePath() );
        webSettings.setAllowFileAccess( true );
        webSettings.setAppCacheEnabled( true );
        webSettings.setCacheMode( WebSettings.LOAD_DEFAULT ); // load online by default
        if ( !isNetworkAvailable() ) { // loading offline
            webSettings.setCacheMode( WebSettings.LOAD_CACHE_ELSE_NETWORK );
        }
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        mywebview.getSettings().setRenderPriority(WebSettings.RenderPriority.HIGH);

        mywebview.setWebViewClient(new WebViewClient()
        {

            public void onPageFinished(WebView view, String url)
            {
                //Toast.makeText(myActivity.this, "Oh no!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }

            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl)
            {
                Toast.makeText(DeveloperInfoActivity.this, description, Toast.LENGTH_SHORT).show();
                String summary = "<html><body><strong> lost_connection" + "</body></html>";
                mywebview.loadData(summary, "text/html", "utf-8");
                finish();
            }

        }); //End WebViewClient

        mywebview.loadUrl(url);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService( CONNECTIVITY_SERVICE );
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

}