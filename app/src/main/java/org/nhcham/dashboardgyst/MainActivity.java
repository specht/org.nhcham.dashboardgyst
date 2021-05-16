package org.nhcham.dashboardgyst;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.ColorUtils;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.Manifest;
import android.app.DownloadManager;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private ProgressBar mProgressBar;
    private SwipeRefreshLayout finalMySwipeRefreshLayout1;
    private Window window;
    private SharedPreferences sharedPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Runtime External storage permission for saving download files
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_DENIED) {
                Log.d("permission", "permission denied to WRITE_EXTERNAL_STORAGE - requesting it");
                String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
                requestPermissions(permissions, 1);
            }
        }

        webView = (WebView) findViewById(R.id.webview);
        webView.setWebViewClient(new mywebClient());
        webView.loadUrl("https://dashboard.gymnasiumsteglitz.de/");
        WebSettings webSettings=webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setLoadsImagesAutomatically(true);
        webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);

        window = this.getWindow();

        if (Build.VERSION.SDK_INT >= 21) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            webView.addJavascriptInterface(new MyJavaScriptInterface(), "android");
        }

        sharedPrefs = getApplicationContext().getSharedPreferences(
                "prefs", Context.MODE_PRIVATE);
        String primary_color = sharedPrefs.getString("primary_color", "");
        System.err.println("primary_color from prefs: " + primary_color);
        if (primary_color.length() > 0) {
            if (Build.VERSION.SDK_INT >= 21) {
                window.setStatusBarColor(ColorUtils.blendARGB(Color.parseColor(primary_color), Color.BLACK, 0.4f));
            }
        }

        //SwipeRefreshLayout
        finalMySwipeRefreshLayout1 = findViewById(R.id.swiperefresh);
        finalMySwipeRefreshLayout1.setOnRefreshListener( new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // This method performs the actual data-refresh operation.
                // The method calls setRefreshing(false) when it's finished.
                webView.loadUrl(webView.getUrl());
            }
        });

        //mProgressBar = findViewById(R.id.pb);

        webView.setWebChromeClient(new WebChromeClient() {
            public void onProgressChanged(WebView view, int newProgress){
                // Update the progress bar with page loading progress
                /*
                mProgressBar.setProgress(newProgress);
                if(newProgress == 100){
                    // Hide the progressbar
                    // mProgressBar.setVisibility(View.GONE);
                    mProgressBar.setProgress(0);
                }
                */
            }
        });

        webView.setDownloadListener(new DownloadListener()
        {
            @Override
            public void onDownloadStart(String url, String userAgent,
                                        String contentDisposition, String mimeType,
                                        long contentLength) {
                DownloadManager.Request request = new DownloadManager.Request(
                        Uri.parse(url));
                request.setMimeType(mimeType);
                String cookies = CookieManager.getInstance().getCookie(url);
                request.addRequestHeader("cookie", cookies);
                request.addRequestHeader("User-Agent", userAgent);
                request.setDescription("Downloading File...");
                request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimeType));
                request.allowScanningByMediaScanner();
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                request.setDestinationInExternalPublicDir(
                        Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(
                                url, contentDisposition, mimeType));
                DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                dm.enqueue(request);
                Toast.makeText(getApplicationContext(), "Datei wird heruntergeladen", Toast.LENGTH_LONG).show();
            }});
    }

    public class mywebClient extends WebViewClient{
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            //mProgressBar.setVisibility(View.VISIBLE);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            finalMySwipeRefreshLayout1.setRefreshing(false);
            System.err.println("Finished loading " + url);
            webView.loadUrl("javascript:android.report_color(window.color_palette.primary)");
            //mProgressBar.setVisibility(View.GONE);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            if ("dashboard.gymnasiumsteglitz.de".equals(request.getUrl().getHost())) {
                // This is my website, so do not override; let my WebView load the page
                return false;
            }
            if ("meet.gymnasiumsteglitz.de".equals(request.getUrl().getHost())) {
                // This is my website, so do not override; let my WebView load the page
                Intent intent = new Intent(Intent.ACTION_VIEW, request.getUrl());
                intent.setClassName("org.jitsi.meet", "org.jitsi.meet.MainActivity");
                startActivity(intent);
                return true;
            }
            Intent intent = new Intent(Intent.ACTION_VIEW, request.getUrl());
            startActivity(intent);
            return true;
        }
    }

    @Override
    public void onBackPressed(){
        if(webView.canGoBack()) {
            webView.goBack();
        }
        else{
            super.onBackPressed();
        }
    }

    final class MyJavaScriptInterface {
        MyJavaScriptInterface() {
        }

        @JavascriptInterface
        public void report_color(String value) {
            SharedPreferences.Editor editor = sharedPrefs.edit();
            editor.putString("primary_color", value);
            editor.apply();

            if (Build.VERSION.SDK_INT >= 21) {
                window.setStatusBarColor(ColorUtils.blendARGB(Color.parseColor(value), Color.BLACK, 0.4f));
                /*
                mProgressBar.setProgressBackgroundTintList(ColorStateList.valueOf(Color.BLACK));
                mProgressBar.setProgressTintList(ColorStateList.valueOf(Color.WHITE));
                //ColorUtils.blendARGB(Color.parseColor(value), Color.WHITE, 0.5f)
                mProgressBar.setSecondaryProgressTintList(ColorStateList.valueOf(Color.BLACK));
                Drawable bgDrawable = mProgressBar.getProgressDrawable();
                bgDrawable.setColorFilter(Color.parseColor(value), android.graphics.PorterDuff.Mode.MULTIPLY);
                mProgressBar.setProgressDrawable(bgDrawable);
                 */
            }
        }
    }

}
