package milfont.com.tezosj_android;

import android.support.test.runner.AndroidJUnit4;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;

import milfont.com.tezosj_android.data.TezosNetwork;
import milfont.com.tezosj_android.model.TezosWallet;

/**
 * TezosWallet test
 * NOTE: All Pocket tests require a DeveloperID,
 * For more information visit: https://docs.pokt.network/docs/how-to-participate
 */
@RunWith(AndroidJUnit4.class)
public class TezosWalletTest {

    /**
     * Creates a new wallet and retrieves balance using Pocket
     */
    @Test
    public void getBalancePocket() throws Exception {
        String devID = "";
        TezosWallet pocketTezosWallet = new TezosWallet("1234", devID, TezosNetwork.ALPHANET, 60000);
        String balance = pocketTezosWallet.getBalance();
        Assert.assertNotNull(balance);
    }
}
