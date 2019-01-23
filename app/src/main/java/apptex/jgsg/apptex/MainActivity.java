package apptex.jgsg.apptex;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private String doubleEscapeTeX(String s) {
        String t="";
        for (int i=0; i < s.length(); i++) {
            if (s.charAt(i) == '\'') t += '\\';
            if (s.charAt(i) != '\n') t += s.charAt(i);
            if (s.charAt(i) == '\\') t += "\\";
        }
        return t;
    }

    private int exampleIndex = 0;
    private WebView view;
    private String javascript = "javascript:MathJax.Hub.Queue(['Typeset',MathJax.Hub]);";

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

    public void onClick(View v) {
        if (v == findViewById(R.id.btn_go)) {
            EditText e = (EditText) findViewById(R.id.latex_editText);
            loadURL("javascript:document.getElementById('math').innerHTML='$"
                +doubleEscapeTeX(e.getText().toString())+"$';");
            loadURL(javascript);
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

        String url = "</style>"
                + "<script type='text/x-mathjax-config'>"
                + "  MathJax.Hub.Config({" + "    showMathMenu: false,"
                + "             jax: ['input/TeX','output/HTML-CSS', 'output/CommonHTML'],"
                + "      extensions: ['tex2jax.js','MathMenu.js','MathZoom.js', 'CHTML-preview.js'],"
                + "         tex2jax: { inlineMath: [ ['$','$'] ], processEscapes: true },"
                + "             TeX: {" + "               extensions:['AMSmath.js','AMSsymbols.js',"
                + "                           'noUndefined.js']" + "             }"
                + "  });"
                + "</script>"
                + "<script type='text/javascript' src='file:///android_asset/MathJax/MathJax.js'>"
                + "</script>"
                + "<p style=\"line-height:1.5; padding: 16 16\" align=\"justify\">"
                + "<span id='math'>";

        // Demo display equation
        url += "This is a display equation: $$P=\\frac{F}{A}$$";

        url += "This is also an identical display equation with different format:\\[P=\\frac{F}{A}\\]";

        // equations aligned at equal sign
        url += "You can also put aligned equations just like Latex:";
        String align = "\\begin{aligned}"
                + "F\\; &= P \\times A \\\\ "
                + "&= 4000 \\times 0.2\\\\"
                + "&= 800\\; \\text{N}\\end{aligned}";
        url += align;

        url += "This is an inline equation $\\sqrt{b^2-4ac}.$";

        // Finally, must enclose the brackets
        url += "</span></p>";

        view.loadDataWithBaseURL("http://bar", url, "text/html", "utf-8", "");
        view.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (!url.startsWith("http://bar")) return;
                loadURL(javascript);
            }
        });

        EditText e = (EditText) findViewById(R.id.latex_editText);
        e.setBackgroundColor(Color.LTGRAY);
        e.setTextColor(Color.BLACK);
        e.setText("");
        Button b = (Button) findViewById(R.id.btn_go);
        b.setOnClickListener(this);
    }
}
