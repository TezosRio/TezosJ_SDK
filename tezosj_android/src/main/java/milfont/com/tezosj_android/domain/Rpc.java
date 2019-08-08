package milfont.com.tezosj_android.domain;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import java.math.BigDecimal;

import milfont.com.tezosj_android.data.BaseGateway;
import milfont.com.tezosj_android.data.PocketGateway;
import milfont.com.tezosj_android.data.TezosGateway;
import milfont.com.tezosj_android.data.TezosNetwork;
import milfont.com.tezosj_android.model.EncKeys;


public class Rpc
{

    private BaseGateway gateway = null;


    // Uses default TezosGateway
    public Rpc()
    {
        this.gateway = new TezosGateway();
    }

    // Uses PocketGateway
    public Rpc(@NotNull String pocketDevID, @NotNull TezosNetwork pocketNetID, int pocketTimeout) {
        this.gateway = new PocketGateway(pocketDevID, pocketNetID, pocketTimeout);
    }


    public String getHead()
    {
        JSONObject result = new JSONObject();
        String response = "";

        try
        {
            response = (String) gateway.getHead().get("result");
            result.put("result", response);

        }
        catch (Exception e)
        {
            e.printStackTrace();
            try
            {
                result.put("result", "An error occured when trying to do getHead operation. See stacktrace for more info.");
            }
            catch (Exception f)
            {
                f.printStackTrace();
            }
        }

        return response;
    }

    public JSONObject getBalance(String address)
    {
        JSONObject result = new JSONObject();
        String response = "";

        try
        {
            response = (String) gateway.getBalance(address).get("result");
            result.put("result", response);

        }
        catch (Exception e)
        {
            e.printStackTrace();
            try
            {
                result.put("result", e.toString());
            }
            catch (Exception f)
            {
                f.printStackTrace();
            }
        }

        return result;

    }

    public JSONObject transfer(String from, String to, BigDecimal amount, BigDecimal fee, String gasLimit, String storageLimit, EncKeys encKeys, JSONObject parameters)
    {
        JSONObject result = new JSONObject();

        try
        {
            result = (JSONObject) gateway.sendTransaction(from, to, amount, fee, gasLimit, storageLimit, encKeys, parameters);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new java.lang.RuntimeException("An error occured while trying to do perform an operation. See stacktrace for more info.");
        }

        return result;

    }

}
