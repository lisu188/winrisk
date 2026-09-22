package com.winrisk.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.Toast;

import java.util.Locale;

public final class MainActivity extends Activity {
    private static final String PREFS = "winrisk";
    private static final String PREF_SERVER_URL = "server_url";
    private static final int MENU_RELOAD = 1;
    private static final int MENU_SERVER = 2;

    private WebView webView;
    private SharedPreferences preferences;
    private String serverUrl = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        preferences = getSharedPreferences(PREFS, MODE_PRIVATE);
        serverUrl = normalizeServerUrl(preferences.getString(PREF_SERVER_URL, ""));

        webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(38, 49, 60));
        webView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        configureWebView();
        setContentView(webView);

        if (savedInstanceState != null && webView.restoreState(savedInstanceState) != null) {
            return;
        }

        if (serverUrl.isEmpty()) {
            showServerDialog(false);
        } else {
            loadServer();
        }
    }

    private void configureWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setSupportZoom(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setUserAgentString(settings.getUserAgentString() + " WinRiskAndroid/0.1");

        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri target = request.getUrl();
                if (isServerUri(target)) {
                    return false;
                }
                if ("http".equalsIgnoreCase(target.getScheme())
                        || "https".equalsIgnoreCase(target.getScheme())) {
                    startActivity(new Intent(Intent.ACTION_VIEW, target));
                    return true;
                }
                return true;
            }

            @Override
            public void onReceivedError(
                    WebView view,
                    WebResourceRequest request,
                    WebResourceError error) {
                if (request.isForMainFrame()) {
                    Toast.makeText(
                            MainActivity.this,
                            "Cannot reach WinRisk server: " + error.getDescription(),
                            Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private boolean isServerUri(Uri target) {
        Uri server = Uri.parse(serverUrl);
        if (server.getHost() == null || target.getHost() == null) {
            return false;
        }
        return server.getScheme() != null
                && server.getScheme().equalsIgnoreCase(target.getScheme())
                && server.getHost().equalsIgnoreCase(target.getHost())
                && effectivePort(server) == effectivePort(target);
    }

    private int effectivePort(Uri uri) {
        if (uri.getPort() >= 0) {
            return uri.getPort();
        }
        return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
    }

    private void loadServer() {
        webView.loadUrl(serverUrl + "/");
    }

    private void showServerDialog(boolean cancelable) {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        input.setHint("http://192.168.1.10:8080");
        input.setText(serverUrl.isEmpty() ? "http://10.0.2.2:8080" : serverUrl);
        input.setSelectAllOnFocus(true);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("WinRisk server")
                .setMessage("Enter the URL of a running WinRisk server. 10.0.2.2 reaches the host machine from the Android emulator.")
                .setView(input)
                .setPositiveButton("Connect", null)
                .setNegativeButton(cancelable ? "Cancel" : "Exit", null)
                .create();
        dialog.setCanceledOnTouchOutside(cancelable);
        dialog.setCancelable(cancelable);
        dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String normalized = normalizeServerUrl(input.getText().toString());
                if (!isValidServerUrl(normalized)) {
                    input.setError("Enter an http:// or https:// URL with a host");
                    return;
                }
                serverUrl = normalized;
                preferences.edit().putString(PREF_SERVER_URL, serverUrl).apply();
                dialog.dismiss();
                loadServer();
            });
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(v -> {
                dialog.dismiss();
                if (!cancelable) {
                    finish();
                }
            });
        });
        dialog.show();
    }

    private boolean isValidServerUrl(String value) {
        if (value.isEmpty()) {
            return false;
        }
        Uri uri = Uri.parse(value);
        String scheme = uri.getScheme();
        return uri.getHost() != null
                && ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme));
    }

    private String normalizeServerUrl(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return "";
        }
        String lower = normalized.toLowerCase(Locale.ROOT);
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            normalized = "http://" + normalized;
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, MENU_RELOAD, Menu.NONE, "Reload")
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        menu.add(Menu.NONE, MENU_SERVER, Menu.NONE, "Server");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == MENU_RELOAD) {
            webView.reload();
            return true;
        }
        if (item.getItemId() == MENU_SERVER) {
            showServerDialog(true);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        webView.saveState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.stopLoading();
            webView.setWebChromeClient(null);
            webView.setWebViewClient(null);
            webView.destroy();
        }
        super.onDestroy();
    }
}
