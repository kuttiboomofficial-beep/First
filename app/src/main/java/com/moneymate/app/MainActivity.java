package com.moneymate.app;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.webkit.JavascriptInterface;
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
    public void onCreate(Bundle state) {
        super.onCreate(state);

        Window window = getWindow();

        // MoneyMate header / navigation bar colors
        window.setStatusBarColor(Color.rgb(15, 23, 42));
        window.setNavigationBarColor(Color.WHITE);

        // Android 15/16 edge-to-edge screen-fit fix
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false);

            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                controller.setSystemBarsAppearance(
                        WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                        WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                );
            }
        }

        webView = new WebView(this);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setMediaPlaybackRequiresUserGesture(true);

        // Keep content inside status/navigation safe areas
        webView.setOnApplyWindowInsetsListener((v, insets) -> {

            int top = insets.getInsets(
                    WindowInsets.Type.statusBars()
            ).top;

            int bottom = insets.getInsets(
                    WindowInsets.Type.navigationBars()
            ).bottom;

            v.setPadding(
                    0,
                    top,
                    0,
                    bottom
            );

            return insets;
        });

        webView.setWebViewClient(new WebViewClient());

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(
                    WebView view,
                    ValueCallback<Uri[]> callback,
                    FileChooserParams params
            ) {
                if (filePathCallback != null) {
                    filePathCallback.onReceiveValue(null);
                }

                filePathCallback = callback;

                Intent intent = params.createIntent();

                try {
                    startActivityForResult(intent, FILE_CHOOSER);
                } catch (Exception e) {
                    filePathCallback = null;
                    return true;
                }

                return true;
            }
        });

        webView.addJavascriptInterface(
                new NativeBridge(),
                "MoneyMateAndroid"
        );

        setContentView(webView);

        webView.loadUrl(
                "file:///android_asset/www/index.html"
        );
    }

    public class NativeBridge {

        @JavascriptInterface
        public void saveBackup(String json, String filename) {

            try {

                ContentResolver cr = getContentResolver();

                ContentValues values = new ContentValues();

                values.put(
                        MediaStore.Downloads.DISPLAY_NAME,
                        filename == null
                                ? "MoneyMate_Backup.json"
                                : filename
                );

                values.put(
                        MediaStore.Downloads.MIME_TYPE,
                        "application/json"
                );

                values.put(
                        MediaStore.Downloads.RELATIVE_PATH,
                        "Download/MoneyMate"
                );

                values.put(
                        MediaStore.Downloads.IS_PENDING,
                        1
                );

                Uri uri = cr.insert(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        values
                );

                if (uri == null) {
                    throw new Exception(
                            "Unable to create download"
                    );
                }

                try (
                        OutputStream out =
                                cr.openOutputStream(uri)
                ) {
                    out.write(
                            json.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );
                }

                values.clear();

                values.put(
                        MediaStore.Downloads.IS_PENDING,
                        0
                );

                cr.update(
                        uri,
                        values,
                        null,
                        null
                );

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
            Intent data
    ) {

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
