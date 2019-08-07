package com.berov.mywebview;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {
    private WebView webView;
    private TextView textView;
    private Button button;
    private ProgressBar progressBar;
    private static String url;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = (WebView) findViewById(R.id.webview);
        textView = (TextView) findViewById(R.id.textView);
        button = (Button) findViewById(R.id.button);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        if (savedInstanceState != null) {
            url = savedInstanceState.getString("savedUrl");
        } else {
            url = getResources().getString(R.string.BASE_URL);
        }

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);     //dangerous!!!
        webView.addJavascriptInterface(new WebAppInterface(this), "Android");
        //webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        //webView.getSettings().setBuiltInZoomControls(true);
        webView.setWebViewClient(new MyWebViewClient());
        //webView.clearCache(true);
        //webView.getSettings().setBuiltInZoomControls(true);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadUrl();
            }
        });

        loadUrl();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //return super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.right_menu, menu);
        return true;
    }


    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.about:
                Toast.makeText(this, this.getString(R.string.about), Toast.LENGTH_SHORT).show();
                return true;
            case R.id.exit:
                finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("savedUrl", url);
    }


    protected void loadUrl() {
        ConnectivityManager connectivityManager = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        boolean hasConnection = connectivityManager.getActiveNetworkInfo() != null
                && connectivityManager.getActiveNetworkInfo().isAvailable()
                && connectivityManager.getActiveNetworkInfo().isConnected();

        if (!hasConnection) {
            webView.setVisibility(View.GONE);
            button.setVisibility(View.VISIBLE);
            textView.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);

            //String unencodedHtml = "<html><body>No Internet Connection!</body></html>";
            //String encodedHtml = Base64.encodeToString(unencodedHtml.getBytes(), Base64.NO_PADDING);
            //webView.loadData(encodedHtml, "text/html", "base64");
        } else {
            AsyncWebChecker asyncTask = new AsyncWebChecker(this);
            asyncTask.execute(webView, button, textView, progressBar);
        }
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Check if the key event was the Back button and if there's history
        if ((keyCode == KeyEvent.KEYCODE_BACK) && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        // If it wasn't the Back key or there's no web page history, bubble up to the default
        // system behavior (probably exit the activity)
        return super.onKeyDown(keyCode, event);
    }


    public class WebAppInterface {
        Context context;

        //Instantiate the interface and set the context
        WebAppInterface(Context context) {
            this.context = context;
        }

        //Show a toast from the web page
        @JavascriptInterface
        public void showToast(String toast) {
            Toast.makeText(context, toast, Toast.LENGTH_SHORT).show();
        }
    }


    private class MyWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String newUrl) {
            url = newUrl;

            //if (Uri.parse(newUrl).getHost().equals(Uri.parse(getResources().getString(R.string.BASE_URL)).getHost())) {
            if (Uri.parse(newUrl).getHost().equals(Uri.parse(getResources().getString(R.string.BASE_URL)).getHost())) {

                loadUrl();

                // This is my website, so do not override; let my WebView load the page
                return true;
            }

            // Otherwise, the link is not for a page on my site, so launch another Activity that handles URLs
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(newUrl));
            startActivity(intent);
            return false;
        }
    }


    private static class AsyncWebChecker extends AsyncTask<View, Void, Boolean> {
        private WeakReference<MainActivity> parentActivity;

        AsyncWebChecker(MainActivity context) {
            parentActivity = new WeakReference<>(context);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            MainActivity activity = parentActivity.get();
            if (activity == null || activity.isFinishing()) return;

            activity.webView.setVisibility(View.GONE);  //webView
            activity.button.setVisibility(View.GONE);  //button
            activity.textView.setVisibility(View.GONE);  //textView
            activity.progressBar.setVisibility(View.VISIBLE);  //progressBar
        }

        @Override
        protected Boolean doInBackground(View... views) {
            boolean isUrlAccessible = false;
            HttpURLConnection urlConnection = null;

            try {
                urlConnection = (HttpURLConnection) new URL(url).openConnection();
                urlConnection.setRequestMethod(parentActivity.get().getString(R.string.REQUEST_METHOD));
                urlConnection.setConnectTimeout(2000);
                urlConnection.setReadTimeout(1000);
                urlConnection.connect();

                isUrlAccessible = urlConnection.getResponseCode() == 200;
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }

            return isUrlAccessible;
        }

        @Override
        protected void onPostExecute(Boolean isWebAddressReachable) {
            super.onPostExecute(isWebAddressReachable);

            MainActivity activity = parentActivity.get();
            if (activity == null || activity.isFinishing()) return;

            if (isWebAddressReachable) {
                //findViewById(R.id.webview).setVisibility(View.VISIBLE);
                activity.webView.setVisibility(View.VISIBLE);  //webView
                activity.button.setVisibility(View.GONE);  //button
                activity.textView.setVisibility(View.GONE);  //textView
                activity.progressBar.setVisibility(View.GONE);  //progressBar
                activity.webView.loadUrl(url);
            } else {
                activity.webView.setVisibility(View.GONE);  //webView
                activity.button.setVisibility(View.VISIBLE);  //button
                activity.textView.setVisibility(View.VISIBLE);  //textView
                activity.progressBar.setVisibility(View.GONE);  //progressBar
                activity.webView.loadUrl(url);
            }
        }
    }
}

