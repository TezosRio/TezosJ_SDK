////////////////////////////////////////////////////////////////////
// WARNING - This software uses the real TezosGateway Betanet blockchain.
//           Use it with caution.
////////////////////////////////////////////////////////////////////

package milfont.com.tezosj_android.data;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.concurrent.TimeUnit;
import java.lang.Object;
import milfont.com.tezosj_android.helper.Global;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class TezosGateway extends BaseGateway
{

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final MediaType textPlainMT = MediaType.parse("text/plain; charset=utf-8");
    private static final Integer HTTP_TIMEOUT = 20;


    // Sends request for Tezos node.
    Object query(String endpoint, String data) throws Exception {

        JSONObject result = null;
        Boolean methodPost = false;
        Request request = null;


        final MediaType MEDIA_PLAIN_TEXT_JSON = MediaType.parse("application/json");
        String DEFAULT_PROVIDER = Global.defaultProvider;
        RequestBody body = RequestBody.create(textPlainMT, DEFAULT_PROVIDER + endpoint);

        if (data != null)
        {
            methodPost = true;
            body = RequestBody.create(MEDIA_PLAIN_TEXT_JSON, data.getBytes());
        }

        if (methodPost == false)
        {
            request = new Request.Builder()
                    .url(DEFAULT_PROVIDER + endpoint)
                    .build();
        }
        else
        {

            request = new Request.Builder()
                    .url(DEFAULT_PROVIDER + endpoint)
                    .addHeader("Content-Type", "text/plain")
                    .post(body)
                    .build();
        }

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(HTTP_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(HTTP_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(HTTP_TIMEOUT, TimeUnit.SECONDS)
                .build();

        try
        {

            Response response = client.newCall(request).execute();
            String strResponse = response.body().string();

            if (isJSONObject(strResponse))
            {
                result = new JSONObject(strResponse);
            }
            else
            {
                if (isJSONArray(strResponse))
                {
                    JSONArray myJSONArray = new JSONArray(strResponse);
                    result = new JSONObject();
                    result.put("result", myJSONArray);
                }
                else
                {
                    // If response is not a JSONObject nor JSONArray...
                    // (can be a primitive).
                    result = new JSONObject();
                    result.put("result", strResponse);
                }
            }
        }
        catch (Exception e)
        {
            // If there is a real error...
            e.printStackTrace();
            result = new JSONObject();
            result.put("result", e.toString());
        }

        return result;
    }
}
