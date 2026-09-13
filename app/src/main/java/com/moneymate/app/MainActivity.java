package com.moneymate.app;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.provider.MediaStore;
import android.view.View;
import android.view.Window;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {

    private WebView webView;
    private ValueCallback<Uri[]> filePathCallback;
    private static final int FILE_CHOOSER = 1001;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        // Safe system bar setup
        Window window = getWindow();
        window.setStatusBarColor(Color.rgb(15, 23, 42));
        window.setNavigationBarColor(Color.WHITE);

        webView = new WebView(this);

        WebSettings settings = webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setMediaPlaybackRequiresUserGesture(true);

        // Android 15/16 safe screen-fit
webView.setFitsSystemWindows(false);

if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

    webView.setOnApplyWindowInsetsListener((v, insets) -> {

        android.graphics.Insets systemBars =
                insets.getInsets(
                        android.view.WindowInsets.Type.systemBars()
                );

        v.setPadding(
                0,
                systemBars.top,
                0,
                systemBars.bottom
        );

        return insets;
    });

} else {

    webView.setPadding(
            0,
            24,
            0,
            24
    );
}
        

        webView.setWebViewClient(new WebViewClient());

        // Restore / file picker
        webView.setWebChromeClient(new WebChromeClient() {

            @Override
            public boolean onShowFileChooser(
                    WebView view,
                    ValueCallback<Uri[]> callback,
                    FileChooserParams params) {

                if (filePathCallback != null) {
                    filePathCallback.onReceiveValue(null);
                }

                filePathCallback = callback;

                try {
                    Intent intent = params.createIntent();
                    startActivityForResult(intent, FILE_CHOOSER);
                } catch (Exception e) {
                    filePathCallback = null;
                    Toast.makeText(
                            MainActivity.this,
                            "File picker failed",
                            Toast.LENGTH_SHORT
                    ).show();
                }

                return true;
            }
        });

        // JavaScript bridge for Android backup
        webView.addJavascriptInterface(
                new NativeBridge(),
                "MoneyMateAndroid"
        );

        setContentView(webView);

        // Load MoneyMate
        webView.loadUrl(
                "file:///android_asset/www/index.html"
        );
    }

    public class NativeBridge {

        @android.webkit.JavascriptInterface
        public void saveBackup(
                String json,
                String filename
        ) {

            try {

                ContentResolver resolver = getContentResolver();

                ContentValues values = new ContentValues();

                String safeName =
                        (filename == null || filename.trim().isEmpty())
                        ? "MoneyMate_Backup.json"
                        : filename;

                values.put(
                        MediaStore.Downloads.DISPLAY_NAME,
                        safeName
                );

                values.put(
                        MediaStore.Downloads.MIME_TYPE,
                        "application/json"
                );

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

                    values.put(
                            MediaStore.Downloads.RELATIVE_PATH,
                            "Download/MoneyMate"
                    );

                    values.put(
                            MediaStore.Downloads.IS_PENDING,
                            1
                    );
                }

                Uri uri = resolver.insert(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        values
                );

                if (uri == null) {
                    throw new Exception(
                            "Unable to create backup file"
                    );
                }

                OutputStream out =
                        resolver.openOutputStream(uri);

                if (out == null) {
                    throw new Exception(
                            "Unable to open backup file"
                    );
                }

                out.write(
                        json.getBytes(
                                StandardCharsets.UTF_8
                        )
                );

                out.close();

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

                    values.clear();

                    values.put(
                            MediaStore.Downloads.IS_PENDING,
                            0
                    );

                    resolver.update(
                            uri,
                            values,
                            null,
                            null
                    );
                }

                runOnUiThread(() ->
                        Toast.makeText(
                                MainActivity.this,
                                "Backup saved to Downloads/MoneyMate",
                                Toast.LENGTH_LONG
                        ).show()
                );

            } catch (Exception e) {

                runOnUiThread(() ->
                        Toast.makeText(
                                MainActivity.this,
                                "Backup save failed",
                                Toast.LENGTH_LONG
                        ).show()
                );
            }
        }
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data) {

        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );

        if (requestCode == FILE_CHOOSER) {

            if (filePathCallback == null) {
                return;
            }

            Uri[] results =
                    WebChromeClient.FileChooserParams
                            .parseResult(
                                    resultCode,
                                    data
                            );

            filePathCallback.onReceiveValue(results);
            filePathCallback = null;
        }
    }

    @Override
    public void onBackPressed() {

        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
