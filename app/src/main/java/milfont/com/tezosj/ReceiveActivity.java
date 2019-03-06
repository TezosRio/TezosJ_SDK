package milfont.com.tezosj;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import static android.graphics.Color.BLACK;
import static android.graphics.Color.WHITE;

public class ReceiveActivity extends AppCompatActivity
{
    private String myAddress = "";
    private int qrcode_width = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receive);

        // Keep keyboard hidden when needed.
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        // Gets screen element references.
        Button buttonOk = (Button) findViewById(R.id.btn_ok);
        ImageView imageViewQrCode = (ImageView) findViewById(R.id.imageViewQRCode);

        qrcode_width = 800;

        // Gets user's wallet address.
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();

        if (extras != null)
        {
            myAddress = extras.getString("receive_address");
        }

        // Generates QRCODE.
        try
        {
            Bitmap bitmap = encodeAsBitmap(myAddress);
            imageViewQrCode.setImageBitmap(bitmap);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }


        // Add listener.
        buttonOk.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                finish();
            }
        });


    }


    private Bitmap encodeAsBitmap(String str) throws WriterException
    {

        BitMatrix result;
        try
        {
            result = new MultiFormatWriter().encode(str,
                    BarcodeFormat.QR_CODE, qrcode_width, qrcode_width, null);
        }
        catch (IllegalArgumentException iae)
        {
            // Unsupported format
            return null;
        }
        int w = result.getWidth();
        int h = result.getHeight();
        int[] pixels = new int[w * h];
        for (int y = 0; y < h; y++)
        {
            int offset = y * w;
            for (int x = 0; x < w; x++)
            {
                pixels[offset + x] = result.get(x, y) ? BLACK : WHITE;
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, qrcode_width, 0, 0, w, h);
        return bitmap;
    }

}
