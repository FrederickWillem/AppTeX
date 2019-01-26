package apptex.jgsg.apptex;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Base64;
import android.view.MenuItem;
import android.view.View;
import android.webkit.DownloadListener;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private WebView view;
    private String javascript = "javascript:MathJax.Hub.Queue(['Typeset',MathJax.Hub]);";
    EditText editText;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                default:
                    return true;
            }
            //return false;
        }
    };

    @Override
    public void onClick(View v) {
        if (v == findViewById(R.id.btn_go)) {
            EditText e = (EditText) findViewById(R.id.latex_editText);
            loadURL("javascript:setTeX('" + Parser.doubleEscapeTeX(e.getText().toString()) + "');");
        } else if (v == findViewById(R.id.btn_save)) {
            PermissionHandler.doWithPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE, new PermissionHandler.PermissionRequestListener() {
                @Override
                public void onPermissionPreviouslyDenied() {
                    //TODO: show a dialog explaining the permission.
                }

                @Override
                public void onPermissionDisabled() {
                    Toast.makeText(view.getContext(), "Please provide permission to write to the external storage.", Toast.LENGTH_LONG).show();
                }

                @Override
                public void onPermissionGranted() {
                    loadURL("javascript:save();");
                }
            });
        } else if (v == findViewById(R.id.btn_share)) {
            share();
        }
        /*else if (v == findViewById(R.id.button3)) {
            WebView w = (WebView) findViewById(R.id.webview);
            EditText e = (EditText) findViewById(R.id.edit);
            e.setText("");
            w.loadUrl("javascript:document.getElementById('math').innerHTML='';");
            w.loadUrl("javascript:MathJax.Hub.Queue(['Typeset',MathJax.Hub]);");
        }
        else if (v == findViewById(R.id.button4)) {
            WebView w = (WebView) findViewById(R.id.webview);
            EditText e = (EditText) findViewById(R.id.edit);
            e.setText(getExample(exampleIndex++));
            if (exampleIndex > getResources().getStringArray(R.array.tex_examples).length-1)
                exampleIndex=0;
            w.loadUrl("javascript:document.getElementById('math').innerHTML='\\\\["
                    +doubleEscapeTeX(e.getText().toString())
                    +"\\\\]';");
            w.loadUrl("javascript:MathJax.Hub.Queue(['Typeset',MathJax.Hub]);");
        }*/
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        PermissionHandler.handlePermissionRequestResult(requestCode, grantResults);
    }

    public void loadURL(String str) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            view.loadUrl(str);
        } else {
            view.evaluateJavascript(str, null);
        }
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        view = (WebView) findViewById(R.id.webview);
        view.getSettings().setJavaScriptEnabled(true);
        view.getSettings().setBuiltInZoomControls(true);

        String url = "<html><head>"
                + "<script type='text/x-mathjax-config'>"
                + "  MathJax.Hub.Config({showMathMenu: false,\n" +
                "                jax: ['input/TeX','output/SVG'],\n" +
                "                extensions: ['tex2jax.js','MathMenu.js','MathZoom.js', 'CHTML-preview.js'],\n" +
                "                tex2jax: { inlineMath: [ ['$','$'] ], processEscapes: true },\n" +
                "                TeX: {extensions:['AMSmath.js','AMSsymbols.js', 'noUndefined.js']}\n" +
                "        });"
                + "</script>"
                + "<script type='text/javascript' src='file:///android_asset/MathJax/MathJax.js'></script>"
                + "<script type='text/javascript' src='file:///android_asset/script.js'></script>"
                + "<script type='text/javascript' src='file:///android_asset/rgbcolor.min.js'></script>"
                + "<script type='text/javascript' src='file:///android_asset/canvg.min.js'></script>"
                + "</head><body>"
                + "<p style=\"line-height:1.5; padding: 16 16\" align=\"justify\">"
                + "<span id='math'>";

        // Demo display equation
        url += "$$P=\\frac{F}{A}$$";

        //close tags and add canvas
        url += "</span></p><div style='display:none;'><a id='link'>link</a><canvas id='canvas' style='width:100%; height:50%'></canvas></div>";

        //end document
        url += "</body></html>";

        view.loadDataWithBaseURL("http://bar", url, "text/html", "utf-8", "");
        view.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (!url.startsWith("http://bar")) return;
                loadURL(javascript);
            }
        });

        view.setDownloadListener(new DownloadListener() {

            @Override
            public void onDownloadStart(String url, String userAgent,
                                        String contentDisposition, String mimetype,
                                        long contentLength) {
                String base64 = url.substring(url.indexOf(","));
                final byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
                Bitmap bm  = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                bm = TeXEncoder.encodeInBitmap(bm, editText.getText().toString());
                try {
                    System.out.println(Environment.getExternalStorageDirectory().getAbsolutePath() + " : " + getFilesDir());
                    //bm.compress(Bitmap.CompressFormat.PNG, 100, new FileOutputStream(new File(getFilesDir(), "test.png")));
                    bm.compress(Bitmap.CompressFormat.PNG, 100, new FileOutputStream(new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "test.png")));
                } catch (Exception e) {
                    e.printStackTrace();
                }

                //((ImageView) findViewById(R.id.imgview)).setImageBitmap(BitmapFactory.decodeFile(getFilesDir() + "/test.png"));
                ((ImageView) findViewById(R.id.imgview)).setImageBitmap(BitmapFactory.decodeFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/test.png"));
            }
        });

        editText = (EditText) findViewById(R.id.latex_editText);
        editText.setBackgroundColor(Color.LTGRAY);
        editText.setTextColor(Color.BLACK);
        editText.setText("");
        ((Button) findViewById(R.id.btn_go)).setOnClickListener(this);
        ((Button) findViewById(R.id.btn_save)).setOnClickListener(this);
    }

    public void share() {
        final Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("image/png");
        final File pic = new File(getFilesDir(), "test.png");
        sharingIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(pic));
        startActivity(Intent.createChooser(sharingIntent, "Share via"));
    }
}
