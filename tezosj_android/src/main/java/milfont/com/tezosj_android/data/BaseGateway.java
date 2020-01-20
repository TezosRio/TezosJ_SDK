package milfont.com.tezosj_android.data;

import android.security.keystore.KeyProperties;
import android.util.Base64;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.libsodium.jni.NaCl;

import java.math.BigDecimal;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import milfont.com.tezosj_android.helper.Base58Check;
import milfont.com.tezosj_android.model.EncKeys;
import milfont.com.tezosj_android.model.SignedOperationGroup;
import milfont.com.tezosj_android.model.TezosWallet;

import static milfont.com.tezosj_android.helper.Constants.OPERATION_KIND_DELEGATION;
import static milfont.com.tezosj_android.helper.Constants.OPERATION_KIND_ORIGINATION;
import static milfont.com.tezosj_android.helper.Constants.OPERATION_KIND_TRANSACTION;
import static milfont.com.tezosj_android.helper.Constants.TZJ_KEY_ALIAS;
import static milfont.com.tezosj_android.helper.Constants.UTEZ;
import static org.libsodium.jni.encoders.Encoder.HEX;

public abstract class BaseGateway {

    abstract Object query(String endpoint, String data) throws Exception;

    public JSONObject getHead() throws Exception
    {
        return (JSONObject) query("/chains/main/blocks/head", null);
    }

    public JSONObject getAccountManagerForBlock(String blockHash, String accountID) throws Exception
    {
        JSONObject result = (JSONObject) query("/chains/main/blocks/" + blockHash + "/context/contracts/" + accountID + "/manager_key", null);

        return result;
    }

    // Gets the balance for a given address.
    public JSONObject getBalance(String address) throws Exception
    {
        JSONObject result = (JSONObject) query("/chains/main/blocks/head/context/contracts/" + address + "/balance", null);

        return result;
    }

    // Prepares ans sends an operation to the Tezos node.
    private JSONObject sendOperation(JSONArray operations, EncKeys encKeys) throws Exception
    {
        JSONObject result = new JSONObject();

        JSONObject head = new JSONObject();
        String forgedOperationGroup = "";

        head = (JSONObject) query("/chains/main/blocks/head/header", null);
        forgedOperationGroup = forgeOperations(head, operations);

        SignedOperationGroup signedOpGroup = signOperationGroup(forgedOperationGroup, encKeys);
        String operationGroupHash = computeOperationHash(signedOpGroup);
        JSONObject appliedOp = applyOperation(head, operations, operationGroupHash, forgedOperationGroup, signedOpGroup);
        JSONObject opResult = checkAppliedOperationResults(appliedOp);

        if (opResult.get("result").toString().length() == 0)
        {
            JSONObject injectedOperation = injectOperation(signedOpGroup);
            if (isJSONArray(injectedOperation.toString()))
            {
                if (((JSONObject) ((JSONArray) injectedOperation.get("result")).get(0)).has("error"))
                {
                    String err = (String) ((JSONObject) ((JSONArray) injectedOperation.get("result")).get(0)).get("error");
                    String reason = "There were errors: '" + err + "'";

                    result.put("result", reason);
                }
                else
                {
                    result.put("result", "");
                }

            }
            else if (isJSONObject(injectedOperation.toString()))
            {
                if (injectedOperation.has("result"))
                {
                    if (isJSONArray(injectedOperation.get("result").toString()))
                    {
                        if (((JSONObject) ((JSONArray) injectedOperation.get("result")).get(0)).has("error"))
                        {
                            String err = (String) ((JSONObject) ((JSONArray) injectedOperation.get("result")).get(0)).get("error");
                            String reason = "There were errors: '" + err + "'";

                            result.put("result", reason);
                        }
                        else
                        {
                            result.put("result", "");
                        }

                    }
                    else
                    {
                        result.put("result", injectedOperation.get("result"));
                    }
                }
                else
                {
                    result.put("result", "There were errors.");
                }
            }


        }
        else
        {
            result.put("result", opResult.get("result").toString());
        }

        return result;
    }

    // Sends a transaction to the Tezos node.
    public JSONObject sendTransaction(String from, String to, BigDecimal amount, BigDecimal fee, String gasLimit, String storageLimit, EncKeys encKeys, JSONObject parameters) throws Exception
    {
        JSONObject result = new JSONObject();

        BigDecimal roundedAmount = amount.setScale(6, BigDecimal.ROUND_HALF_UP);
        BigDecimal roundedFee = fee.setScale(6, BigDecimal.ROUND_HALF_UP);
        JSONArray operations = new JSONArray();
        JSONObject revealOperation = new JSONObject();
        JSONObject transaction = new JSONObject();
        JSONObject head = new JSONObject();
        JSONObject account = new JSONObject();
        JSONObject param = new JSONObject();
        JSONArray argsArray = new JSONArray();
        Integer counter = 0;

        // Check if address has enough funds to do the transfer operation.
        JSONObject balance = getBalance(from);
        if (balance.has("result"))
        {
            BigDecimal bdAmount = amount.multiply(BigDecimal.valueOf(UTEZ));
            BigDecimal total = new BigDecimal(((balance.getString("result").replaceAll("\\n", "")).replaceAll("\"", "").replaceAll("'", "")));

            if (total.compareTo(bdAmount) < 0) // Returns -1 if value iss less than amount.
            {
                // Not enough funds to do the transfer.
                JSONObject returned = new JSONObject();
                returned.put("result", "{ \"result\":\"error\", \"kind\":\"TezosJ_SDK_exception\", \"id\": \"Not enough funds\" }");

                return returned;
            }
        }

        if (gasLimit == null)
        {
            gasLimit = "15400";
        }
        else
        {
            if ((gasLimit.length() == 0) || (gasLimit.equals("0")))
            {
                gasLimit = "15400";
            }
        }

        if (storageLimit == null)
        {
            storageLimit = "300";
        }
        else
        {
            if (storageLimit.length() == 0)
            {
                storageLimit = "300";
            }
        }

        head = new JSONObject(query("/chains/main/blocks/head/header", null).toString());
        account = getAccountForBlock(head.get("hash").toString(), from);
        counter = Integer.parseInt(account.get("counter").toString());

        // Append Reveal Operation if needed.
        revealOperation = appendRevealOperation(head, encKeys, from, (counter));

        if (revealOperation != null)
        {
            operations.put(revealOperation);
            counter = counter + 1;
        }

        transaction.put("destination", to);
        transaction.put("amount", (String.valueOf(roundedAmount.multiply(BigDecimal.valueOf(UTEZ)).toBigInteger())));
        transaction.put("storage_limit", storageLimit);
        transaction.put("gas_limit", gasLimit);
        transaction.put("counter", String.valueOf(counter + 1));
        transaction.put("fee", (String.valueOf(roundedFee.multiply(BigDecimal.valueOf(UTEZ)).toBigInteger())));
        transaction.put("source", from);
        transaction.put("kind", OPERATION_KIND_TRANSACTION);

        if ((parameters != null) && (parameters.length() > 0))
        {
            // User has passed some parameters. Add it to the transaction.
            transaction.put("parameters", parameters);
        }

        operations.put(transaction);

        result = (JSONObject) sendOperation(operations, encKeys);

        return result;
    }

    private SignedOperationGroup signOperationGroup(String forgedOperation, EncKeys encKeys) throws Exception
    {

        SignedOperationGroup signedOperationGroup = null;

        JSONObject signed = sign(HEX.decode(forgedOperation), encKeys, "03");

        // Prepares the object to be returned.
        byte[] workBytes = ArrayUtils.addAll(HEX.decode(forgedOperation), HEX.decode((String) signed.get("sig")));
        signedOperationGroup = new SignedOperationGroup(workBytes, (String) signed.get("edsig"), (String) signed.get("sbytes"));

        return signedOperationGroup;

    }

    private String forgeOperations(JSONObject blockHead, JSONArray operations) throws Exception
    {
        JSONObject result = new JSONObject();
        result.put("branch", blockHead.get("hash"));
        result.put("contents", operations);

        return nodeForgeOperations(result.toString());
    }


    private String nodeForgeOperations(String opGroup) throws Exception
    {
        JSONObject response = (JSONObject) query("/chains/main/blocks/head/helpers/forge/operations", opGroup);
        String forgedOperation = (String) response.get("result");

        return ((forgedOperation.replaceAll("\\n", "")).replaceAll("\"", "").replaceAll("'", ""));

    }

    private JSONObject getAccountForBlock(String blockHash, String accountID) throws Exception
    {
        JSONObject result = new JSONObject();

        result = (JSONObject) query("/chains/main/blocks/" + blockHash + "/context/contracts/" + accountID, null);

        return result;
    }

    private String computeOperationHash(SignedOperationGroup signedOpGroup) throws Exception
    {

        byte[] hash = new byte[32];
        int r = NaCl.sodium().crypto_generichash(hash, hash.length, signedOpGroup.getTheBytes(), signedOpGroup.getTheBytes().length, signedOpGroup.getTheBytes(), 0);

        return Base58Check.encode(hash);
    }

    private JSONObject nodeApplyOperation(JSONArray payload) throws Exception
    {
        JSONObject response = (JSONObject) query("/chains/main/blocks/head/helpers/preapply/operations", payload.toString());

        return response;
    }

    private JSONObject applyOperation(JSONObject head, JSONArray operations, String operationGroupHash, String forgedOperationGroup, SignedOperationGroup signedOpGroup) throws Exception
    {
        JSONArray payload = new JSONArray();
        JSONObject jsonObject = new JSONObject();

        jsonObject.put("protocol", head.get("protocol"));
        jsonObject.put("branch", head.get("hash"));
        jsonObject.put("contents", operations);
        jsonObject.put("signature", signedOpGroup.getSignature());
        payload.put(jsonObject);

        return nodeApplyOperation(payload);
    }

    private JSONObject checkAppliedOperationResults(JSONObject appliedOp) throws Exception
    {
        JSONObject returned = new JSONObject();
        String error = "", status = "";
        Boolean errors = false;
        String reason = "";

        String[] validAppliedKinds = new String[]{"activate_account", "reveal", "transaction", "origination", "delegation"};
        JSONObject firstAppliedOp = new JSONObject();
        firstAppliedOp = appliedOp;

        String firstApplyed = firstAppliedOp.toString().replaceAll("\\\\n", "").replaceAll("\\\\", "");
        JSONArray result = new JSONArray(new JSONObject(firstApplyed).get("result").toString());
        JSONObject first = (JSONObject) result.get(0);

        if (isJSONObject(first.toString()))
        {
            // Check for error.
            if (first.has("kind") && first.has("id"))
            {
                errors = true;
                reason = "There were errors: kind '" + first.getString("kind") + "' id '" + first.getString("id") + "'";
            }

        }
        else if (isJSONArray(first.toString()))
        {
            // Loop through contents and check for errors.
            Integer elements = ((JSONArray) first.get("contents")).length();
            String element = "";
            for (Integer i = 0; i < elements; i++)
            {
                JSONObject operation_result = ((JSONObject) ((JSONObject) (((JSONObject) (((JSONArray) first.get("contents")).get(i))).get("metadata"))).get("operation_result"));
                element = ((JSONObject) operation_result).getString("status");
                if (element.equals("failed") == true)
                {
                    errors = true;
                    if (operation_result.has("errors"))
                    {
                        JSONObject err = (JSONObject) ((JSONArray) operation_result.get("errors")).get(0);
                        reason = "There were errors: kind '" + err.getString("kind") + "' id '" + err.getString("id") + "'";
                    }
                    break;
                }
            }
        }

        if (errors)
        {
            returned.put("result", reason);
        }
        else
        {
            // Success.
            returned.put("result", "");
        }
        return returned;
    }

    private JSONObject appendRevealOperation(JSONObject blockHead, EncKeys encKeys, String pkh, Integer counter) throws Exception
    {

        // Create new JSON object for the reveal operation.
        JSONObject revealOp = new JSONObject();

        // Get public key from encKeys.
        byte[] bytePk = encKeys.getEncPublicKey();
        byte[] decPkBytes = decryptBytes(bytePk, TezosWallet.getEncryptionKey(encKeys));

        StringBuilder builder2 = new StringBuilder();
        for (byte decPkByte : decPkBytes)
        {
            builder2.append((char) (decPkByte));
        }
        String publicKey = builder2.toString();


        // If Manager key is not revealed for account...
        if (!isManagerKeyRevealedForAccount(blockHead, pkh))
        {
            BigDecimal fee = new BigDecimal("0.001300");
            BigDecimal roundedFee = fee.setScale(6, BigDecimal.ROUND_HALF_UP);
            revealOp.put("kind", "reveal");
            revealOp.put("source", pkh);
            revealOp.put("fee", (String.valueOf(roundedFee.multiply(BigDecimal.valueOf(UTEZ)).toBigInteger())));
            revealOp.put("counter", String.valueOf(counter + 1));
            revealOp.put("gas_limit", "10000");
            revealOp.put("storage_limit", "0");
            revealOp.put("public_key", publicKey);

        }
        else
        {
            revealOp = null;
        }

        return revealOp;
    }

    private boolean isManagerKeyRevealedForAccount(JSONObject blockHead, String pkh) throws Exception
    {
        Boolean result = false;
        String blockHeadHash = blockHead.getString("hash");
        String r = "";

        Boolean hasResult = getAccountManagerForBlock(blockHeadHash, pkh).has("result");

        if (hasResult)
        {
            r = (String) getAccountManagerForBlock(blockHeadHash, pkh).get("result");

            // Do some cleaning.
            r = r.replace("\"", "");
            r = r.replace("\n", "");
            r = r.trim();

            if (r.equals("null") == true)
            {
                result = false;
            }
            else
            {
                // Account already revealed.
                result = true;
            }
        }

        return result;
    }

    private JSONObject injectOperation(SignedOperationGroup signedOpGroup) throws Exception
    {
        String payload = signedOpGroup.getSbytes();
        return nodeInjectOperation("\"" + payload + "\"");
    }

    private JSONObject nodeInjectOperation(String payload) throws Exception
    {
        JSONObject result = (JSONObject) query("/injection/operation?chain=main", payload);

        return result;
    }

    public JSONObject sign(byte[] bytes, EncKeys keys, String watermark) throws Exception
    {
        // Access wallet keys to have authorization to perform the operation.
        byte[] byteSk = keys.getEncPrivateKey();
        byte[] decSkBytes = decryptBytes(byteSk, getEncryptionKey(keys));

        StringBuilder builder = new StringBuilder();
        for (byte decSkByte : decSkBytes)
        {
            builder.append((char) (decSkByte));
        }

        // First, we remove the edsk prefix from the decoded private key bytes.
        byte[] edskPrefix = {(byte) 43, (byte) 246, (byte) 78, (byte) 7};
        byte[] decodedSk = Base58Check.decode(builder.toString());
        byte[] privateKeyBytes = Arrays.copyOfRange(decodedSk, edskPrefix.length, decodedSk.length);

        // Then we create a work array and check if the watermark parameter has been passed.
        byte[] workBytes = ArrayUtils.addAll(bytes);

        if (watermark != null)
        {
            byte[] wmBytes = HEX.decode(watermark);
            workBytes = ArrayUtils.addAll(wmBytes, workBytes);
        }

        // Now we hash the combination of: watermark (if exists) + the bytes passed in parameters.
        // The result will end up in the sig variable.
        byte[] hashedWorkBytes = new byte[32];
        int rc = NaCl.sodium().crypto_generichash(hashedWorkBytes, hashedWorkBytes.length, workBytes, workBytes.length, workBytes, 0);

        int[] lengths = {64};
        byte[] sig = new byte[64];
        int r = NaCl.sodium().crypto_sign_detached(sig, lengths, hashedWorkBytes, hashedWorkBytes.length, privateKeyBytes);

        // To create the edsig, we need to concatenate the edsig prefix with the sig and then encode it.
        // The sbytes will be the concatenation of bytes (in hex) + sig (in hex).
        byte[] edsigPrefix = {9, (byte) 245, (byte) 205, (byte) 134, 18};
        byte[] edsigPrefixedSig = new byte[edsigPrefix.length + sig.length];
        edsigPrefixedSig = ArrayUtils.addAll(edsigPrefix, sig);
        String edsig = Base58Check.encode(edsigPrefixedSig);
        String sbytes = HEX.encode(bytes) + HEX.encode(sig);

        // Now, with all needed values ready, we create and deliver the response.
        JSONObject response = new JSONObject();
        response.put("bytes", HEX.encode(bytes));
        response.put("sig", HEX.encode(sig));
        response.put("edsig", edsig);
        response.put("sbytes", sbytes);

        return response;

    }

    // Tests if a string is a valid JSON.
    Boolean isJSONObject(String myStr)
    {
        try
        {
            JSONObject testJSON = new JSONObject(myStr);
            testJSON = null;
            return true;
        }
        catch (JSONException e)
        {
            return false;
        }
    }

    // Tests if s string is a valid JSON Array.
    Boolean isJSONArray(String myStr)
    {
        try
        {
            JSONArray testJSONArray = new JSONArray(myStr);
            testJSONArray = null;
            return true;
        }
        catch (JSONException e)
        {
            return false;
        }
    }


    private byte[] getEncryptionKey(EncKeys keys)
    {
        try
        {
            String base64EncryptedPassword = keys.getEncP();
            String base64EncryptionIv = keys.getEncIv();

            byte[] encryptionIv = Base64.decode(base64EncryptionIv, Base64.DEFAULT);
            byte[] encryptionPassword = Base64.decode(base64EncryptedPassword, Base64.DEFAULT);

            KeyStore keystore = KeyStore.getInstance("AndroidKeyStore");
            keystore.load(null);

            SecretKey secretKey = (SecretKey) keystore.getKey(TZJ_KEY_ALIAS, null);
            Cipher cipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/" + KeyProperties.BLOCK_MODE_CBC + "/" + KeyProperties.ENCRYPTION_PADDING_PKCS7);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(encryptionIv));
            byte[] passwordBytes = cipher.doFinal(encryptionPassword);
            String password = new String(passwordBytes, "UTF-8");

            return passwordBytes;

        }
        catch (Exception e)
        {
            return null;
        }

    }

    // Decryption routine.
    private static byte[] decryptBytes(byte[] encrypted, byte[] key)
    {
        try
        {
            SecretKeySpec keySpec = null;
            Cipher cipher = null;
            keySpec = new SecretKeySpec(key, "AES/ECB/PKCS7Padding");
            cipher = Cipher.getInstance("AES/ECB/PKCS7Padding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec);

            return cipher.doFinal(encrypted);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }

    public JSONObject sendDelegationOperation(String delegator, String delegate, BigDecimal fee, String gasLimit, String storageLimit, EncKeys encKeys) throws Exception
    {
        JSONObject result = new JSONObject();

        BigDecimal roundedFee = fee.setScale(6, BigDecimal.ROUND_HALF_UP);
        JSONArray operations = new JSONArray();
        JSONObject revealOperation = new JSONObject();
        JSONObject transaction = new JSONObject();
        JSONObject head = new JSONObject();
        JSONObject account = new JSONObject();
        Integer counter = 0;

        if (gasLimit == null)
        {
            gasLimit = "10100";
        }
        else
        {
            if ((gasLimit.length() == 0) || (gasLimit.equals("0")))
            {
                gasLimit = "10100";
            }
        }

        if (storageLimit == null)
        {
            storageLimit = "0";
        }
        else
        {
            if (storageLimit.length() == 0)
            {
                storageLimit = "0";
            }
        }

        head = new JSONObject(query("/chains/main/blocks/head/header", null).toString());
        account = getAccountForBlock(head.get("hash").toString(), delegator);
        counter = Integer.parseInt(account.get("counter").toString());

        // Append Reveal Operation if needed.
        revealOperation = appendRevealOperation(head, encKeys, delegator, (counter));

        if (revealOperation != null)
        {
            operations.put(revealOperation);
            counter = counter + 1;
        }

        transaction.put("kind", OPERATION_KIND_DELEGATION);
        transaction.put("source", delegator);
        transaction.put("fee", (String.valueOf(roundedFee.multiply(BigDecimal.valueOf(UTEZ)).toBigInteger())));
        transaction.put("counter", String.valueOf(counter + 1));
        transaction.put("storage_limit", storageLimit);
        transaction.put("gas_limit", gasLimit);

        if (delegate.equals("undefined") == false)
        {
            transaction.put("delegate", delegate);
        }

        operations.put(transaction);

        result = (JSONObject) sendOperation(operations, encKeys);

        return result;


    }

    public JSONObject sendOriginationOperation(String from, Boolean spendable, Boolean delegatable, BigDecimal fee, String gasLimit, String storageLimit, BigDecimal amount, String code, String storage, EncKeys encKeys) throws Exception
    {
        JSONObject result = new JSONObject();

        BigDecimal roundedAmount = amount.setScale(6, BigDecimal.ROUND_HALF_UP);
        BigDecimal roundedFee = fee.setScale(6, BigDecimal.ROUND_HALF_UP);
        JSONArray operations = new JSONArray();
        JSONObject revealOperation = new JSONObject();
        JSONObject transaction = new JSONObject();
        JSONObject head = new JSONObject();
        JSONObject account = new JSONObject();
        Integer counter = 0;

        if (gasLimit == null)
        {
            gasLimit = "10100";
        }
        else
        {
            if ((gasLimit.length() == 0) || (gasLimit.equals("0")))
            {
                gasLimit = "10100";
            }
        }

        if (storageLimit == null)
        {
            storageLimit = "277";
        }
        else
        {
            if (storageLimit.length() == 0)
            {
                storageLimit = "277";
            }
        }

        head = new JSONObject(query("/chains/main/blocks/head/header", null).toString());
        account = getAccountForBlock(head.get("hash").toString(), from);
        counter = Integer.parseInt(account.get("counter").toString());

        // Append Reveal Operation if needed.
        revealOperation = appendRevealOperation(head, encKeys, from, (counter));

        if (revealOperation != null)
        {
            operations.put(revealOperation);
            counter = counter + 1;
        }

        transaction.put("kind", OPERATION_KIND_ORIGINATION);
        transaction.put("source", from);
        transaction.put("fee", (String.valueOf(roundedFee.multiply(BigDecimal.valueOf(UTEZ)).toBigInteger())));
        transaction.put("counter", String.valueOf(counter + 1));
        transaction.put("gas_limit", gasLimit);
        transaction.put("storage_limit", storageLimit);
        transaction.put("manager_pubkey", from);
        transaction.put("balance", (String.valueOf(roundedAmount.multiply(BigDecimal.valueOf(UTEZ)).toBigInteger())));
        transaction.put("spendable", spendable);
        transaction.put("delegatable", delegatable);
        operations.put(transaction);

        result = (JSONObject) sendOperation(operations, encKeys);

        return result;
    }

    public JSONObject sendUndelegationOperation(String delegator, BigDecimal fee, EncKeys encKeys) throws Exception
    {
        return sendDelegationOperation(delegator, "undefined", fee, "", "", encKeys);
    }


    // Calls a contract passing parameters.
    public JSONObject callContractEntryPoint(String from, String contract, BigDecimal amount, BigDecimal fee,
                                             String gasLimit, String storageLimit, EncKeys encKeys, String entrypoint,
                                             String[] parameters)
            throws Exception
    {
        JSONObject result = new JSONObject();

        BigDecimal roundedAmount = amount.setScale(6, BigDecimal.ROUND_HALF_UP);
        BigDecimal roundedFee = fee.setScale(6, BigDecimal.ROUND_HALF_UP);
        JSONArray operations = new JSONArray();
        JSONObject revealOperation = new JSONObject();
        JSONObject transaction = new JSONObject();
        JSONObject head = new JSONObject();
        JSONObject account = new JSONObject();
        Integer counter = 0;

        // Check if address has enough funds to do the transfer operation.
        JSONObject balance = getBalance(from);

        if(balance.has("result"))
        {
            BigDecimal bdAmount = amount.multiply(BigDecimal.valueOf(UTEZ));
            BigDecimal total = new BigDecimal(
                    ((balance.getString("result").replaceAll("\\n", "")).replaceAll("\"", "").replaceAll("'", "")));

            if(total.compareTo(bdAmount) < 0) // Returns -1 if value is less than amount.
            {
                // Not enough funds to do the transfer.
                JSONObject returned = new JSONObject();
                returned.put("result",
                        "{ \"result\":\"error\", \"kind\":\"TezosJ_SDK_exception\", \"id\": \"Not enough funds\" }");

                return returned;
            }
        }

        if(gasLimit == null)
        {
            gasLimit = "750000";
        }
        else
        {
            if((gasLimit.length() == 0) || (gasLimit.equals("0")))
            {
                gasLimit = "750000";
            }
        }

        if(storageLimit == null)
        {
            storageLimit = "1000";
        }
        else
        {
            if(storageLimit.length() == 0)
            {
                storageLimit = "1000";
            }
        }

        head = new JSONObject(query("/chains/main/blocks/head/header", null).toString());
        account = getAccountForBlock(head.get("hash").toString(), from);
        counter = Integer.parseInt(account.get("counter").toString());

        // Append Reveal Operation if needed.
        revealOperation = appendRevealOperation(head, encKeys, from, (counter));

        if(revealOperation != null)
        {
            operations.put(revealOperation);
            counter = counter + 1;
        }

        transaction.put("destination", contract);
        transaction.put("amount", (String.valueOf(roundedAmount.multiply(BigDecimal.valueOf(UTEZ)).toBigInteger())));
        transaction.put("storage_limit", storageLimit);
        transaction.put("gas_limit", gasLimit);
        transaction.put("counter", String.valueOf(counter + 1));
        transaction.put("fee", (String.valueOf(roundedFee.multiply(BigDecimal.valueOf(UTEZ)).toBigInteger())));
        transaction.put("source", from);
        transaction.put("kind", OPERATION_KIND_TRANSACTION);

        // Builds a Michelson-compatible set of parameters to pass to the smart
        // contract.
        JSONObject myparamJson = new JSONObject();

        String[] contractEntryPoints = getContractEntryPoints(contract);
        String[] contractEntryPointParameters = getContractEntryPointsParameters(contract, entrypoint, "names");
        String[] contractEntryPointParametersTypes = getContractEntryPointsParameters(contract, entrypoint, "types");

        myparamJson = paramValueBuilder(entrypoint, contractEntryPoints, parameters, contractEntryPointParameters,
                contractEntryPointParametersTypes);

        // Adds the smart contract parameters to the transaction.
        JSONObject myParams = new JSONObject();
        myParams.put("entrypoint", "default");
        myParams.put("value", myparamJson);

        transaction.put("parameters", myParams);

        operations.put(transaction);

        result = (JSONObject) sendOperation(operations, encKeys);

        return result;
    }

    private String[] getContractEntryPoints(String contractAddress) throws Exception
    {
        JSONObject response = (JSONObject) query(
                "/chains/main/blocks/head/context/contracts/" + contractAddress + "/entrypoints", null);

        JSONObject entryPointsJson = (JSONObject) response.get("entrypoints");

        JSONArray arr = entryPointsJson.names();
        ArrayList<String> list = new ArrayList<String>();
        for(int i = 0; i < arr.length(); i++)
        {
            list.add((String) arr.get(i));
        }

        String[] entrypoints  = new String[list.size()];
        entrypoints = list.toArray(entrypoints);

        // If there is a list of entrypoints, then sort its elements.
        // This is fundamental for the call to work, as the order of the entrypoints
        // matter.
        if(entrypoints != null)
        {
            if(entrypoints.length > 1)
            {
                Arrays.sort(entrypoints);
            }
        }
        else
        {
            // If there are no entrypoints declared in the contract, consider only the
            // "default" entrypoint.
            entrypoints = new String[]
                    { "default" };
        }

        return entrypoints;
    }

    private String[] getContractEntryPointsParameters(String contractAddress, String entrypoint, String namesOrTypes)
            throws Exception
    {
        ArrayList<String> parameters = new ArrayList<String>();

        // If no desired entrypoint was specified, use the "default" entrypoint.
        if((entrypoint == null) || (entrypoint.length() == 0))
        {
            entrypoint = "default";
        }

        JSONObject response = (JSONObject) query(
                "/chains/main/blocks/head/context/contracts/" + contractAddress + "/entrypoints/" + entrypoint, null);

        JSONArray paramArray = decodeMichelineParameters(response, null);

        JSONObject jsonObj = new JSONObject();

        parameters = new ArrayList<String>();

        if(namesOrTypes.equals("names"))
        {
            for(int i = paramArray.length() - 1; i >= 0; i--)
            {
                jsonObj = (JSONObject) paramArray.get(i);
                if(jsonObj.has("annots"))
                {
                    JSONArray annotsArray = (JSONArray) jsonObj.get("annots");
                    parameters.add((String) annotsArray.getString(0).replace("%", ""));
                }
            }
        }
        else if(namesOrTypes.equals("types"))
        {
            for(int i = paramArray.length() - 1; i >= 0; i--)
            {
                jsonObj = (JSONObject) paramArray.get(i);
                if(jsonObj.has("prim"))
                {
                    parameters.add((String) jsonObj.get("prim"));
                }
            }
        }

        String[] tempArray = new String[parameters.size()];
        tempArray = parameters.toArray(tempArray);

        return tempArray;
    }

    private JSONArray decodeMichelineParameters(JSONObject jsonObj, JSONArray builtArray) throws Exception
    {
        JSONObject left = new JSONObject();
        JSONObject right = new JSONObject();

        if((jsonObj.has("args") == true) || (jsonObj.has("prim") == true))
        {
            if(builtArray == null)
            {
                builtArray = new JSONArray();
            }

            if(jsonObj.has("args"))
            {
                JSONArray myArr = jsonObj.getJSONArray("args");
                left = myArr.getJSONObject(0);
                right = myArr.getJSONObject(1);
            }
            else
            {
                right = jsonObj;
            }

            builtArray.put(right);
            return decodeMichelineParameters(left, builtArray);

        }
        builtArray.put(left);

        return builtArray;
    }

    private JSONObject paramValueBuilder(String entrypoint, String[] contractEntrypoints, String[] parameters,
                                         String[] contractEntryPointParameters, String[] datatypes) throws Exception
    {

        // Creates a base Pair, so we can build our Michelson message parameter.
        Pair<Pair, String> basePair = null;

        // Creates the JSON object that will be returned by the methd.
        JSONObject myJsonObj = new JSONObject();

        List<String> parametersList = Arrays.asList(parameters);
        List<String> typesList = Arrays.asList(datatypes);
        List<String> entrypointsList = Arrays.asList(contractEntrypoints);

        // Corrects typeList.
        for(int i = 0; i < typesList.size(); i++)
        {
            switch (typesList.get(i))
            {
                case "int":
                    typesList.set(i, "int");
                    break;

                case "nat":
                    typesList.set(i, "int");
                    break;
                case "mutez":
                    typesList.set(i, "int");
                    break;
                case "tez":
                    typesList.set(i, "int");
                    break;
                case "bytes":
                    typesList.set(i, "int");
                    break;
                case "key_hash":
                    typesList.set(i, "string");
                    break;
                case "bool":
                    typesList.set(i, "string");
                    break;
                case "unit":
                    typesList.set(i, "string");
                    break;

                default:
                    typesList.set(i, "string");

            }

        }

        // Builds the parameters pairs and formats them as JSON.
        myJsonObj = buildParameterPairs(myJsonObj, basePair, parametersList, typesList, 0);

        // Builds the Micheline formatted JSON. Only if an entrypoint was specified.
        if(entrypoint != null)
        {
            if((entrypoint.length() > 0) && (entrypoint.equals("default") == false))
            {
                // Builds the Michelson message and formats it as JSON.
                myJsonObj = buildMessage(myJsonObj, entrypoint, entrypointsList);
            }
        }

        return myJsonObj;
    }

    private JSONObject buildParameterPairs(JSONObject jsonObj, Pair pair, List<String> parameters,
                                           List<String> datatypes, Integer firstElement) throws Exception
    {

        if(parameters.size() == 1)
        {
            jsonObj.put(datatypes.get(0), parameters.get(0));
        }
        else
        {

            for(int i = firstElement; i < parameters.size(); i++)
            {
                if(pair == null)
                {
                    pair = new MutablePair<>(parameters.get(i), parameters.get(i + 1));

                    // Build JSON object with "prim" and "args".
                    jsonObj.put("prim", "Pair");

                    JSONArray jsonArray = new JSONArray();

                    JSONObject newObj1 = new JSONObject();
                    newObj1.put((String) datatypes.get(i), (String) parameters.get(i));
                    jsonArray.put(newObj1);

                    JSONObject newObj2 = new JSONObject();
                    newObj2.put((String) datatypes.get(i + 1), (String) parameters.get(i + 1));
                    jsonArray.put(newObj2);

                    jsonObj.put("args", jsonArray);

                }
                else
                {
                    if((i + 1) < parameters.size())
                    {
                        pair = new MutablePair<>(pair, parameters.get(i + 1));

                        // Builds new JSON object with "prim" and "args".
                        JSONObject newJsonObj = new JSONObject();
                        newJsonObj.put("prim", "Pair");
                        JSONArray jsonArray = new JSONArray();
                        jsonArray.put(jsonObj);

                        JSONObject obj = new JSONObject();
                        obj.put((String) datatypes.get(i + 1), (String) parameters.get(i + 1));
                        jsonArray.put(obj);

                        newJsonObj.put("args", jsonArray);

                        return buildParameterPairs(newJsonObj, pair, parameters, datatypes, i + 1);

                    }
                }
            }
        }

        return jsonObj;

    }

    private JSONObject buildMessage(JSONObject jsonObj, String entrypoint, List<String> entrypoints) throws Exception
    {
        JSONObject messageJsonObj = new JSONObject();

        // Calculates the number of "Lefts".
        Integer entrypointPosition = entrypoints.indexOf(entrypoint) + 1;
        Integer numberOfLefts = (entrypoints.size() - entrypointPosition);

        if(entrypointPosition > 1)
        {
            // Build JSON object with "prim" and "args".
            messageJsonObj.put("prim", "Right");

            JSONArray jsonArray = new JSONArray();
            jsonArray.put(jsonObj);
            messageJsonObj.put("args", jsonArray);
        }

        for(int i = 0; i < numberOfLefts; i++)
        {
            if(messageJsonObj.length() == 0)
            {
                // Build JSON object with "prim" and "args".
                messageJsonObj.put("prim", "Left");

                JSONArray jsonArray = new JSONArray();
                jsonArray.put(jsonObj);
                messageJsonObj.put("args", jsonArray);
            }
            else
            {
                JSONObject newJsonObj = new JSONObject();

                // Build JSON object with "prim" and "args".
                if(entrypointPosition < entrypoints.size())
                {
                    newJsonObj.put("prim", "Left");
                }

                JSONArray jsonArray = new JSONArray();
                jsonArray.put(messageJsonObj);
                newJsonObj.put("args", jsonArray);
                messageJsonObj = newJsonObj;
            }
        }

        return messageJsonObj;

    }


}
