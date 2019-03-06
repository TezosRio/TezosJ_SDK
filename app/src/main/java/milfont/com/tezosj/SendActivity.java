package milfont.com.tezosj;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.math.BigDecimal;

import milfont.com.tezosj_android.domain.Crypto;

public class SendActivity extends AppCompatActivity
{
    private EditText editTextDestinationAddress = null;
    private EditText editTextAmount = null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send);

        // Requests for camera use permission.
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 3);

        // Keeps keyboard hidden when needed.
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        // Gets screen element references.
        editTextDestinationAddress = (EditText) findViewById(R.id.editTextDestinationAddress);
        editTextAmount = (EditText) findViewById(R.id.editTextAmount);
        Button buttonConfirm = (Button) findViewById(R.id.btn_confirm);
        Button buttonCancel = (Button) findViewById(R.id.btn_cancel);
        ImageButton imageButtonScan = (ImageButton) findViewById(R.id.imageButton_scan);

        // Add listener.
        buttonConfirm.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                Boolean isValidAddress = false;

                try
                {
                    Crypto crypto = new Crypto();
                    isValidAddress = crypto.checkAddress(editTextDestinationAddress.getText().toString());
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }

                if (isValidAddress)
                {
                    if (editTextAmount.getText().toString().length() > 0)
                    {
                        BigDecimal amount = new BigDecimal(editTextAmount.getText().toString());

                        if (amount.compareTo(BigDecimal.ZERO) > 0)
                        {
                            Intent resultIntent = new Intent();
                            resultIntent.putExtra("dest_address", editTextDestinationAddress.getText().toString());
                            resultIntent.putExtra("amount", editTextAmount.getText().toString());
                            setResult(SendActivity.RESULT_OK, resultIntent);
                            finish();
                        }
                        else
                        {
                            Toast toast = Toast.makeText(getApplicationContext(), "Amount must be greater than zero", Toast.LENGTH_SHORT);
                            toast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL, 0, 0);
                            toast.show();
                        }
                    }
                    else
                    {
                        Toast toast = Toast.makeText(getApplicationContext(), "Please enter an amount", Toast.LENGTH_SHORT);
                        toast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL, 0, 0);
                        toast.show();
                    }
                }
                else
                {
                    Toast toast = Toast.makeText(getApplicationContext(), "Invalid destination address", Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL, 0, 0);
                    toast.show();
                }
            }
        });

        // Add listener.
        buttonCancel.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                finish();
            }
        });


        // Add listener to image button scan.
        imageButtonScan.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                IntentIntegrator integrator = new IntentIntegrator(SendActivity.this);
                integrator.setPrompt("Scan destination address barcode");
                integrator.initiateScan();
            }
        });

    }


    private void hideKeyboard()
    {

        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        View view = this.getCurrentFocus();
        if (view != null)
        {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (scanResult != null)
        {
            editTextDestinationAddress.setText(scanResult.getContents());
        }


    }
}
