package com.lucas.googletvlauncher;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Base64;
import android.view.KeyEvent;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.List;

public class MainActivity extends Activity {
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);

        webView.addJavascriptInterface(new AndroidBridge(), "AndroidTV");
        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl("file:///android_asset/index.html");
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public class AndroidBridge {
        @JavascriptInterface
        public String getInstalledApps() {
            JSONArray appsArray = new JSONArray();
            try {
                PackageManager pm = getPackageManager();
                Intent intent = new Intent(Intent.ACTION_MAIN, null);
                intent.addCategory(Intent.CATEGORY_LAUNCHER);
                List<ResolveInfo> apps = pm.queryIntentActivities(intent, 0);
                Collections.sort(apps, new ResolveInfo.DisplayNameComparator(pm));

                for (ResolveInfo app : apps) {
                    String pkg = app.activityInfo.packageName;
                    if (pkg.equals(getPackageName())) continue;

                    JSONObject obj = new JSONObject();
                    obj.put("name", app.loadLabel(pm).toString());
                    obj.put("package", pkg);
                    obj.put("icon", drawableToBase64(app.loadIcon(pm)));
                    appsArray.put(obj);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return appsArray.toString();
        }

        @JavascriptInterface
        public void launchApp(String packageName) {
            Intent launchIntent = getPackageManager().getLaunchIntentForPackage(packageName);
            if (launchIntent != null) {
                startActivity(launchIntent);
            }
        }

        @JavascriptInterface
        public void openWifiSettings() {
            startActivity(new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS));
        }

        private String drawableToBase64(Drawable drawable) {
            Bitmap bitmap;
            if (drawable instanceof BitmapDrawable) {
                bitmap = ((BitmapDrawable) drawable).getBitmap();
            } else {
                int w = drawable.getIntrinsicWidth() > 0 ? drawable.getIntrinsicWidth() : 96;
                int h = drawable.getIntrinsicHeight() > 0 ? drawable.getIntrinsicHeight() : 96;
                bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                drawable.draw(canvas);
            }
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, stream);
            return "data:image/png;base64," + Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP);
        }
    }
}
