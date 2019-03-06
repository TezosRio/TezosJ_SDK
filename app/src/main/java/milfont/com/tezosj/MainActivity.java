package milfont.com.tezosj;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import milfont.com.tezosj_android.helper.SharedPreferencesHelper;


import static milfont.com.tezosj_android.helper.Constants.TZJ_KEY_ALIAS;

public class MainActivity extends AppCompatActivity
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Context ctx = this;

        // Checks if there is a previously saved wallet.
        String myWalletString = "";
        SharedPreferencesHelper sp = new SharedPreferencesHelper();
        myWalletString = sp.getSharedPreferenceString(ctx, TZJ_KEY_ALIAS, "");

        if (myWalletString.length() > 0)
        {
            // Opens wallet activity.
            Intent intent = new Intent(MainActivity.this, WalletActivity.class);
            startActivity(intent);

            finish();

        }
        else
        {
            // Gets screen element references.
            Button btnNewWallet = (Button) findViewById(R.id.btn_new_wallet);
            Button btnImportWallet = (Button) findViewById(R.id.btn_import_wallet);

            // Adds listeners.
            btnNewWallet.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    // Opens new wallet activity.
                    Intent intent = new Intent(MainActivity.this, NewWalletActivity.class);
                    startActivity(intent);
                    finish();
                }
            });

            btnImportWallet.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    // Opens import wallet activity.
                    Intent intent = new Intent(MainActivity.this, ImportWalletActivity.class);
                    startActivity(intent);
                    finish();
                }
            });
        }

    }

}
