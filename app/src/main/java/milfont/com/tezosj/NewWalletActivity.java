package milfont.com.tezosj;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import milfont.com.tezosj_android.model.TezosWallet;

public class NewWalletActivity extends AppCompatActivity
{

    private EditText editTextMnemonics = null;
    private EditText editTextPassphrase = null;
    private TezosWallet myWallet = null;
    private Context ctx = null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_wallet);

        ctx = this;

        // Gets screen references.
        Button btnCreateWallet = (Button) findViewById(R.id.btn_create_new_wallet);
        Button btnNext = (Button) findViewById(R.id.btn_next);
        editTextMnemonics = (EditText) findViewById(R.id.editText_mnemonic);
        editTextPassphrase = (EditText) findViewById(R.id.editText_passphrase);

        // Keeps keyboard hidden when needed.
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        // Ads listeners.
        btnCreateWallet.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                // Creates a new wallet with a passPhrase.
                try
                {
                    hideKeyboard();

                    if (editTextPassphrase.getText().toString().length() > 0)
                    {
                        // Creates a new Tezos Wallet.
                        myWallet = new TezosWallet(editTextPassphrase.getText().toString());

                        // Displays mnemonic words to user.
                        editTextMnemonics.setText(myWallet.getMnemonicWords());

                    }
                    else
                    {
                        Toast toast = Toast.makeText(ctx, "Passphrase is required", Toast.LENGTH_SHORT);
                        toast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM, 0, 0);
                        toast.show();
                    }


                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }

            }
        });


        btnNext.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if ((editTextPassphrase.getText().toString().length() > 0) && (editTextMnemonics.getText().toString().length() > 0))
                {
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(ctx);
                    alertDialogBuilder.setTitle("Warning");
                    alertDialogBuilder
                            .setMessage("Did you really write down the passphrase and mnemonic words and now wish to continue to next step?")
                            .setCancelable(false)
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener()
                            {
                                public void onClick(DialogInterface dialog, int id)
                                {
                                    // Saves the created wallet.
                                    myWallet.save(ctx);

                                    // Erases wallet from memory.
                                    myWallet = null;

                                    // Opens wallet activity.
                                    Intent intent = new Intent(NewWalletActivity.this, WalletActivity.class);
                                    startActivity(intent);
                                    finish();

                                }
                            })
                            .setNegativeButton("No", new DialogInterface.OnClickListener()
                            {
                                public void onClick(DialogInterface dialog, int id)
                                {
                                    dialog.cancel();
                                }
                            });

                    AlertDialog alertDialog = alertDialogBuilder.create();
                    alertDialog.show();
                }
                else
                {
                    Toast.makeText(getApplicationContext(), "Passphrase and mnemonic words are required", Toast.LENGTH_SHORT).show();
                }

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


}
