package apptex.jgsg.apptex;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.FileProvider;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.DownloadListener;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;

public class CreateFragment extends Fragment {

    private static final int INPUT_MODE_TEX = 0, INPUT_MODE_NORMAL = 1;
    private WebView view;
    private String javascript = "javascript:MathJax.Hub.Queue(['Typeset',MathJax.Hub]);";
    private int inputMode = INPUT_MODE_NORMAL;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_create, container, false);
    }

    @Override
    public void onViewCreated(final View newView, Bundle savedInstanceState) {
        final Activity context = getActivity();

        view = (WebView) newView.findViewById(R.id.webview);
        view.getSettings().setJavaScriptEnabled(true);
        view.getSettings().setBuiltInZoomControls(true);

        Button goBtn = (Button) newView.findViewById(R.id.btn_go);
        goBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String str = ((EditText) newView.findViewById(R.id.latex_editText)).getText().toString();
                if(inputMode == INPUT_MODE_NORMAL)
                    str = new Parser(str).toLatex();
                loadURL("javascript:setTeX('" + Parser.doubleEscapeTeX(str) + "');");
                loadURL("javascript:document.getElementById('tex').innerHTML = '" + Parser.doubleEscapeTeX(str) + "'");
            }
        });

        Button saveBtn = (Button) newView.findViewById(R.id.btn_save);
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PermissionHandler.doWithPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE, new PermissionHandler.PermissionRequestListener() {
                    @Override
                    public void onPermissionPreviouslyDenied() {
                        //TODO: show a dialog explaining the permission.
                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        builder.setMessage(R.string.save_permission_text)
                                .setPositiveButton(R.string.settings, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Intent intent = new Intent();
                                        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                        intent.setData(Uri.fromParts("package", context.getPackageName(), null));
                                        context.startActivity(intent);
                                    }
                                }).setNegativeButton(R.string.dont_save, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Toast.makeText(newView.getContext(), "Saving cancelled.", Toast.LENGTH_SHORT).show();
                            }
                        }).create().show();
                    }

                    @Override
                    public void onPermissionDisabled() {
                        Toast.makeText(newView.getContext(), R.string.save_permission_disabled, Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onPermissionGranted() {
                        loadURL("javascript:save();");
                    }
                });
            }
        });

        Button shareBtn = newView.findViewById(R.id.btn_share);
        shareBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                share();
            }
        });

        ImageButton inputModeBtn = newView.findViewById(R.id.btn_switchMode);
        inputModeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                inputMode = (1+inputMode)%2;
                ((TextView) newView.findViewById(R.id.textView)).setText(inputMode == INPUT_MODE_NORMAL ? R.string.enter_normal : R.string.enter_latex);
            }
        });

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

        url += "<p id='tex'></p>";

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


        final EditText editText = (EditText) newView.findViewById(R.id.latex_editText);
        editText.setBackgroundColor(Color.LTGRAY);
        editText.setTextColor(Color.BLACK);
        editText.setText("");

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
                    System.out.println(Environment.getExternalStorageDirectory().getAbsolutePath() + " : " + context.getFilesDir());
                    //bm.compress(Bitmap.CompressFormat.PNG, 100, new FileOutputStream(new File(getFilesDir(), "test.png")));
                    bm.compress(Bitmap.CompressFormat.PNG, 100, new FileOutputStream(new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "test.png")));
                } catch (Exception e) {
                    e.printStackTrace();
                }

                System.out.println(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/test.png");
                //((ImageView) findViewById(R.id.imgview)).setImageBitmap(BitmapFactory.decodeFile(getFilesDir() + "/test.png"));
                ((ImageView) newView.findViewById(R.id.imgview)).setImageBitmap(BitmapFactory.decodeFile(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) + "/test.png"));
            }
        });

    }

    public void loadURL(String str) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            view.loadUrl(str);
        } else {
            view.evaluateJavascript(str, null);
        }
    }



    public void share() {
        final Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("image/png");
        File path = getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        System.out.println(path);
        File pic = new File(path, "test.png");

        Uri uri = FileProvider.getUriForFile(getContext(), getActivity().getApplicationContext().getPackageName() + ".provider", pic);
        sharingIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        sharingIntent.putExtra(Intent.EXTRA_STREAM, uri);
        startActivity(Intent.createChooser(sharingIntent, "Share via"));
    }

}
