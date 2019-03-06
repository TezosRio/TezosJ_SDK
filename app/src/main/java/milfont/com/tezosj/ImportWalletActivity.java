package milfont.com.tezosj;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import milfont.com.tezosj_android.model.TezosWallet;

public class ImportWalletActivity extends AppCompatActivity
{

    private EditText editTextPassphrase = null;
    private EditText editTextMnemonics = null;
    private Context ctx = null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import_wallet);

        ctx = this;

        // Gets screen references.
        editTextPassphrase = (EditText) findViewById(R.id.editText_passphrase);
        editTextMnemonics = (EditText) findViewById(R.id.editText_mnemonic);
        Button btnImport = (Button) findViewById(R.id.btn_import_wallet);

        // Ads listeners.
        btnImport.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if ((editTextPassphrase.getText().toString().length() > 0) && (editTextMnemonics.getText().toString().length() > 0))
                {

                    try
                    {
                        // Imports previously owned wallet.
                        TezosWallet myWallet = new TezosWallet(editTextMnemonics.getText().toString(), editTextPassphrase.getText().toString());

                        // Saves the created wallet.
                        myWallet.save(ctx);

                        // Erases wallet from memory.
                        myWallet = null;

                        // Opens wallet activity.
                        Intent intent = new Intent(ImportWalletActivity.this, WalletActivity.class);
                        startActivity(intent);
                        finish();


                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
                else
                {
                    Toast.makeText(getApplicationContext(), "Passphrase and mnemonic words are required", Toast.LENGTH_SHORT).show();
                }

            }
        });


    }
}
