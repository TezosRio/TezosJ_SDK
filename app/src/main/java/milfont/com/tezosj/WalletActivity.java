package milfont.com.tezosj;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.Timer;
import java.util.TimerTask;

import milfont.com.tezosj_android.domain.Crypto;
import milfont.com.tezosj_android.helper.SharedPreferencesHelper;
import milfont.com.tezosj_android.model.TezosWallet;

import static android.view.View.GONE;
import static milfont.com.tezosj_android.helper.Constants.TEZOS_SYMBOL;
import static milfont.com.tezosj_android.helper.Constants.TZJ_KEY_ALIAS;

public class WalletActivity extends AppCompatActivity
{
    private Context ctx = null;
    private TezosWallet myWallet = null;
    private TextView textViewBalance = null;
    private TextView textViewAddress = null;
    private EditText editTextAlias = null;
    private String myBalance = "0 " + TEZOS_SYMBOL;
    private Timer timer = new Timer();
    private final long DELAY = 3000;
    private Button buttonSend = null;
    private Button buttonReceive = null;
    private static final Integer RESPONSE_WALLET_ACTIVITY = 738;
    private JSONObject operationResult = new JSONObject();
    private ProgressBar progressBar = null;
    private EditText editTextUnlockPhrase = null;
    private Boolean locked = true;
    private LinearLayout linearLayoutMain = null;
    private LinearLayout linearLayoutLock = null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallet);

        ctx = this;

        // Keep keyboard hidden when needed.
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        // Gets screen element references.
        editTextUnlockPhrase = (EditText) findViewById(R.id.editTextUnlockPhrase);
        linearLayoutMain = (LinearLayout) findViewById(R.id.linearLayoutMain);
        linearLayoutLock = (LinearLayout) findViewById(R.id.linearLayoutLock);
        textViewBalance = (TextView) findViewById(R.id.textViewBalance);
        textViewAddress = (TextView) findViewById(R.id.textViewAddress);
        editTextAlias = (EditText) findViewById(R.id.editTextAlias);
        buttonSend = (Button) findViewById(R.id.btn_send);
        buttonReceive = (Button) findViewById(R.id.btn_receive);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setVisibility(GONE);
        progressBar.getIndeterminateDrawable().setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryDark), android.graphics.PorterDuff.Mode.MULTIPLY);
        Button buttonUnlock = (Button) findViewById(R.id.btn_unlock);

        // Verify is wallet is locked. And if it is, asks for passphrase.
        if (locked)
        {
            linearLayoutMain.setVisibility(GONE);
            linearLayoutLock.setVisibility(View.VISIBLE);
        }
        else
        {
            linearLayoutMain.setVisibility(View.VISIBLE);
            linearLayoutLock.setVisibility(GONE);
            locked = true;
        }


        // Adds listener on button unlock.
        buttonUnlock.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {

                if (editTextUnlockPhrase.getText().toString().length() > 0)
                {

                    // Brings encrypted wallet to memory.
                    loadEncryptedWallet();

                    // Verifies is the passphrase is right.
                    if (myWallet.checkPhrase(editTextUnlockPhrase.getText().toString()))
                    {
                        //Hide cursor and keyboard.
                        hideKeyboard();

                        editTextUnlockPhrase.setText("");
                        locked = false;
                        linearLayoutMain.setVisibility(View.VISIBLE);
                        linearLayoutLock.setVisibility(GONE);

                    }
                    else
                    {
                        //Hide cursor and keyboard.
                        hideKeyboard();

                        Toast.makeText(getApplicationContext(), "Wrong passphrase", Toast.LENGTH_SHORT).show();
                        locked = true;
                        linearLayoutLock.setVisibility(View.VISIBLE);
                        linearLayoutMain.setVisibility(GONE);
                    }

                }
            }
        });


    }

    // Brings previously saved encrypted wallet to memory.
    private void loadEncryptedWallet()
    {
        // Checks if there is a previously saved wallet.
        String myWalletString = "";
        SharedPreferencesHelper sp = new SharedPreferencesHelper();
        myWalletString = sp.getSharedPreferenceString(ctx, TZJ_KEY_ALIAS, "");

        if (myWalletString.length() > 0)
        {

            try
            {
                // Loads a wallet from media.
                myWallet = new TezosWallet(ctx, editTextUnlockPhrase.getText().toString());

                // Updates alias and address.
                try
                {
                    editTextAlias.setText(myWallet.getAlias());
                    textViewAddress.setText(myWallet.getPublicKeyHash());

                    // Adds listener to alias field.
                    editTextAlias.addTextChangedListener(new TextWatcher()
                    {
                        @Override
                        public void onTextChanged(CharSequence s, int start, int before, int count)
                        {
                        }

                        @Override
                        public void beforeTextChanged(CharSequence s, int start, int count, int after)
                        {
                        }

                        @Override
                        public void afterTextChanged(Editable s)
                        {
                            // Sets current wallet new alias.
                            myWallet.setAlias(editTextAlias.getText().toString());

                            timer.cancel();
                            timer = new Timer();
                            timer.schedule(
                                    new TimerTask()
                                    {
                                        @Override
                                        public void run()
                                        {
                                            runOnUiThread(new Runnable()
                                            {

                                                @Override
                                                public void run()
                                                {

                                                    // Saves current wallet after some seconds.
                                                    myWallet.save(ctx);

                                                    //Hide cursor and keyboard.
                                                    hideKeyboard();
                                                    editTextAlias.clearFocus();

                                                }
                                            });
                                        }
                                    },
                                    DELAY
                            );
                        }
                    });


                }
                catch (Exception e)
                {
                    textViewAddress.setText("Not available");
                }


                // Brings wallet balance.
                getWalletBalance();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            // Adds listener on button send.
            buttonSend.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    // Opens send activity.
                    Intent intent = new Intent(WalletActivity.this, SendActivity.class);
                    startActivityForResult(intent, RESPONSE_WALLET_ACTIVITY);
                }
            });

            // Adds listener on textView address to allow copy to clipboard.
            textViewAddress.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("tzAddress", textViewAddress.getText().toString());
                    clipboard.setPrimaryClip(clip);

                    Toast toast = Toast.makeText(getApplicationContext(), "Copied to clipboard!", Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL, 0, 0);
                    toast.show();
                }
            });

            // Adds listener on button receive.
            buttonReceive.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    // Opens receive activity.
                    try
                    {
                        Intent intent = new Intent(WalletActivity.this, ReceiveActivity.class);
                        intent.putExtra("receive_address", (String) myWallet.getPublicKeyHash());
                        startActivity(intent);
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }

                }
            });

        }

    }


    private void getWalletBalance()
    {

        try
        {

            new MyAsyncTask<Object, Object, Boolean>()
            {

                @Override
                protected void onPreExecute()
                {
                    progressBar.setVisibility(View.VISIBLE);
                }

                @Override
                protected Boolean doInBackground(Object... params)
                {

                    try
                    {
                        myBalance = myWallet.getBalance();
                    }
                    catch (Exception e)
                    {
                        return false;
                    }

                    return true;
                }

                @Override
                protected void onPostExecute(Boolean result)
                {

                    try
                    {
                        progressBar.setVisibility(GONE);

                        if (result == true)
                        {
                            textViewBalance.setText(myBalance);
                        }
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }

            }.executeContent("go");

        }
        catch (Exception e)
        {
        }


    }

    private void hideKeyboard()
    {

        // Soft keyboard configuration.
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        View view = this.getCurrentFocus();
        if (view != null)
        {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        // Unlocks wallet.
        this.locked = false;

        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode)
        {

            case 738:
            {
                if (resultCode == Activity.RESULT_OK)
                {

                    final String dest_address = data.getStringExtra("dest_address");
                    String amount = data.getStringExtra("amount");
                    Boolean isValidAddress = false;

                    try
                    {
                        Crypto crypto = new Crypto();
                        isValidAddress = crypto.checkAddress(dest_address);
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }

                    if (isValidAddress)
                    {
                        final BigDecimal bdAmount = new BigDecimal(amount);
                        if (bdAmount.compareTo(BigDecimal.ZERO) > 0)
                        {

                            // All clear to send. Confirm operation with user.
                            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(ctx);
                            alertDialogBuilder.setTitle("Confirm");
                            alertDialogBuilder
                                    .setMessage("Do you really want to send " + bdAmount + " " + TEZOS_SYMBOL + " to " + dest_address + " ?")
                                    .setCancelable(false)
                                    .setPositiveButton("Yes", new DialogInterface.OnClickListener()
                                    {
                                        public void onClick(DialogInterface dialog, int id)
                                        {

                                            new MyAsyncTask<Object, Object, Boolean>()
                                            {

                                                @Override
                                                protected void onPreExecute()
                                                {
                                                    progressBar.setVisibility(View.VISIBLE);
                                                }

                                                @Override
                                                protected Boolean doInBackground(Object... params)
                                                {

                                                    try
                                                    {
                                                        BigDecimal myFee = new BigDecimal("0.00294");
                                                        operationResult = myWallet.send(myWallet.getPublicKeyHash(), dest_address, bdAmount, myFee, null, null, null);
                                                    }
                                                    catch (Exception e)
                                                    {
                                                        return false;
                                                    }

                                                    return true;
                                                }

                                                @Override
                                                protected void onPostExecute(Boolean result)
                                                {

                                                    try
                                                    {
                                                        progressBar.setVisibility(GONE);

                                                        if (result == true)
                                                        {
                                                            String status = (String) operationResult.get("result");

                                                            // Verifies if there is any error indicator on the result. Otherwise, builts a success message.
                                                            // Note that TezosJ_SDK will append "error" word to the result JSON if anything was wrong.
                                                            // Otherwise, the result JSON will have the operation hash.
                                                            if (status.contains("error") == true)
                                                            {
                                                               status = "Sorry, funds could not be sent. There were errors. Operation was cancelled";
                                                            }
                                                            else
                                                            {
                                                                status = "Operation successful. Please wait until blockchain confirmation";
                                                            }

                                                            Toast.makeText(getApplicationContext(), status, Toast.LENGTH_LONG).show();
                                                        }
                                                    }
                                                    catch (Exception e)
                                                    {
                                                        e.printStackTrace();
                                                        Toast.makeText(getApplicationContext(), "An error ocurred while trying to send funds. Operation aborted", Toast.LENGTH_LONG).show();
                                                    }
                                                }

                                            }.executeContent("go");


                                        }
                                    })
                                    .setNegativeButton("No", new DialogInterface.OnClickListener()
                                    {
                                        public void onClick(DialogInterface dialog, int id)
                                        {
                                            Toast.makeText(getApplicationContext(), "Operation cancelled by user", Toast.LENGTH_SHORT).show();
                                            dialog.cancel();
                                        }
                                    });

                            AlertDialog alertDialog = alertDialogBuilder.create();
                            alertDialog.show();


                        }
                        else
                        {
                            Toast.makeText(getApplicationContext(), "Amount must be greater than zero", Toast.LENGTH_SHORT).show();
                        }

                    }
                    else
                    {
                        Toast.makeText(getApplicationContext(), "Invalid destination address", Toast.LENGTH_SHORT).show();
                    }


                }

                break;
            }
        }

    }

    @Override
    protected void onRestart()
    {
        super.onRestart();

        // Verify is wallet is locked. And if it is, asks for passphrase.
        if (locked)
        {
            linearLayoutMain.setVisibility(GONE);
            linearLayoutLock.setVisibility(View.VISIBLE);
        }
        else
        {
            linearLayoutMain.setVisibility(View.VISIBLE);
            linearLayoutLock.setVisibility(GONE);
        }

    }

    @Override
    protected void onPause()
    {
        super.onPause();
        this.locked = true;
    }
}
