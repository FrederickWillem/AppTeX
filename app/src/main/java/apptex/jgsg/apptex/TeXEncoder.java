package apptex.jgsg.apptex;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

public class TeXEncoder {

    public static Bitmap encodeInBitmap(Bitmap original, String tex) {
        int height = original.getHeight()+10;
        Bitmap bm = Bitmap.createBitmap(original.getWidth(), height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bm);
        canvas.drawBitmap(original, 0, 0, null);

        Paint p = new Paint();
        for(int i = 0; i < tex.length(); i++) {
            int c = tex.charAt(i);
            float factor = i/(tex.length()+1)*2;
            p.setColor(Color.rgb((int)(c*(i%3==0 ? factor : 1.5)),(int)(c*(i%3==1 ? factor : 1.5)), (int)(c*(i%3==2 ? factor : 1.5))));
            canvas.drawPoint(i, height-2, p);
            canvas.drawPoint(i, height-1, p);
        }
        return bm;
    }
}
