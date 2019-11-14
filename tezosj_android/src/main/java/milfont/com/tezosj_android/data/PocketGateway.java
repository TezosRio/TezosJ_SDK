package milfont.com.tezosj_android.data;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;

import kotlin.Unit;
import kotlin.jvm.functions.Function2;
import milfont.com.tezosj_android.helper.SemaphoreUtil;
import network.pocket.core.errors.PocketError;

public class PocketGateway extends BaseGateway {

    private String devID;
    private TezosNetwork netID;
    private int timeout = 20000;
    private PocketProvider pocket;
    private final String TEZOS_NETWORK = "TEZOS";

    public PocketGateway(@NotNull String devID, @NotNull TezosNetwork netID, int timeout) {
        this.devID = devID;
        this.netID = netID;
        this.timeout = timeout;
        this.pocket = new PocketProvider(devID, TEZOS_NETWORK, new String[]{this.netID.name()}, 5, this.timeout);
    }


    @Override
    Object query(String endpoint, String data) throws Exception {
        PocketGatewaySemaphoreCallback callback = new PocketGatewaySemaphoreCallback(endpoint, data);
        SemaphoreUtil.executeSemaphoreCallback(callback);
        return callback.getResult();
    }

    class PocketGatewaySemaphoreCallback implements SemaphoreUtil.SemaphoreCallback, Function2<PocketError, String, Unit> {

        private String endpoint;
        private String data;
        private JSONObject result;
        private Semaphore semaphore;

        public PocketGatewaySemaphoreCallback(String endpoint, String data) {
            this.endpoint = endpoint;
            this.data = data;
        }

        public JSONObject getResult() {
            return result;
        }

        @Override
        public Unit invoke(PocketError pocketError, String response) {
            try {
                if (pocketError != null) {
                    if (isJSONObject(pocketError.getMessage())) {
                        result = new JSONObject(pocketError.getMessage());
                    } else {
                        result = new JSONObject();
                        result.put("result", pocketError.getMessage());
                    }
                } else if (isJSONObject(response)) {
                    result = new JSONObject(response);
                } else {
                    if (isJSONArray(response)) {
                        JSONArray myJSONArray = new JSONArray(response);
                        result = new JSONObject();
                        result.put("result", myJSONArray);
                    } else {
                        // If response is not a JSONObject nor JSONArray...
                        // (can be a primitive).
                        result = new JSONObject();
                        result.put("result", response);
                    }
                }
            } catch (Exception e) {
                // If there is a real error...
                e.printStackTrace();
                result = new JSONObject();
                try {
                    result.put("result", e.toString());
                } catch (JSONException jsonException) {
                    jsonException.printStackTrace();
                }
            }
            this.semaphore.release();
            return null;
        }

        @Override
        public void execute(Semaphore semaphore) {
            this.semaphore = semaphore;
            String httpMethod = "POST";
            if (this.data == null) {
                httpMethod = "GET";
            }

            PocketGateway.this.pocket.send(TEZOS_NETWORK, netID.name(), this.data, httpMethod, this.endpoint, null, null, this);
        }
    }
}
