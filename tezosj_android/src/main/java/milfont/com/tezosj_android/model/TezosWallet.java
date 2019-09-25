package milfont.com.tezosj_android.model;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import org.bitcoinj.crypto.MnemonicCode;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.libsodium.jni.NaCl;

import java.math.BigDecimal;

import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import milfont.com.tezosj_android.data.TezosNetwork;
import milfont.com.tezosj_android.domain.Crypto;
import milfont.com.tezosj_android.domain.Rpc;
import milfont.com.tezosj_android.helper.Base58;
import milfont.com.tezosj_android.helper.Sha256Hash;
import milfont.com.tezosj_android.helper.SharedPreferencesHelper;

import static milfont.com.tezosj_android.helper.Constants.TEZOS_SYMBOL;
import static milfont.com.tezosj_android.helper.Constants.TZJ_KEY_ALIAS;
import static milfont.com.tezosj_android.helper.Constants.UTEZ;
import static org.apache.commons.lang3.StringUtils.isNumeric;

/**
 * Created by Milfont on 21/07/2018.
 */

public class TezosWallet
{

    private String alias = "";
    private byte[] publicKey;
    private byte[] publicKeyHash;
    private byte[] privateKey;
    private byte[] mnemonicWords;
    private String balance = "";
    private ArrayList<Transaction> transactions = null;

    private String encPass, encIv;

    private Rpc rpc = null;
    private Crypto crypto = null;

    // Constuctor with passPhrase.
    // This will create a new wallet and generate new keys and mnemonic words.
    public TezosWallet(String passPhrase) throws Exception
    {
        if (passPhrase != null)
        {
            if (passPhrase.length() > 0)
            {

                // Converts passPhrase String to a byte array, respecting char values.
                byte[] c = new byte[passPhrase.length()];
                for (int i = 0; i < passPhrase.length(); i++)
                {
                    c[i] = (byte) passPhrase.charAt(i);
                }

                initStore(c);

                generateMnemonic();
                generateKeys(passPhrase);

                initDomainClasses();
            }
            else
            {
                throw new java.lang.RuntimeException("A passphrase is mandatory.");
            }
        }
        else
        {
            throw new java.lang.RuntimeException("Null passphrase.");
        }
    }

    // Uses Pocket RPC
    // Constuctor with passPhrase.
    // This will create a new wallet and generate new keys and mnemonic words.
    public TezosWallet(String passPhrase, @NotNull String pocketDevID, @NotNull TezosNetwork pocketNetID, int pocketTimeout) throws Exception
    {
        if (passPhrase != null)
        {
            if (passPhrase.length() > 0)
            {

                // Converts passPhrase String to a byte array, respecting char values.
                byte[] c = new byte[passPhrase.length()];
                for (int i = 0; i < passPhrase.length(); i++)
                {
                    c[i] = (byte) passPhrase.charAt(i);
                }

                initStore(c);

                generateMnemonic();
                generateKeys(passPhrase);

                initDomainClasses(pocketDevID, pocketNetID, pocketTimeout);
            }
            else
            {
                throw new java.lang.RuntimeException("A passphrase is mandatory.");
            }
        }
        else
        {
            throw new java.lang.RuntimeException("Null passphrase.");
        }
    }

    // Constructor with previously owned mnemonic words and passPhrase.
    // This will import an existing wallet from blockchain.
    public TezosWallet(String mnemonicWords, String passPhrase) throws Exception
    {
        if (mnemonicWords != null)
        {
            if (mnemonicWords.length() > 0)
            {
                if (passPhrase != null)
                {
                    if (passPhrase.length() > 0)
                    {

                        // Converts passPhrase String to a byte array, respecting char values.
                        byte[] c = new byte[passPhrase.length()];
                        for (int i = 0; i < passPhrase.length(); i++)
                        {
                            c[i] = (byte) passPhrase.charAt(i);
                        }

                        initStore(c);

                        // Cleans undesired characters from mnemonic words.
                        String cleanMnemonic = mnemonicWords.replace("[", "");
                        cleanMnemonic = cleanMnemonic.replace("]", "");
                        cleanMnemonic = cleanMnemonic.replace(",", " ");
                        cleanMnemonic = cleanMnemonic.replace("  ", " ");

                        // Converts mnemonicWords String to a byte array, respecting char values.
                        byte[] b = new byte[cleanMnemonic.length()];
                        for (int i = 0; i < cleanMnemonic.length(); i++)
                        {
                            b[i] = (byte) cleanMnemonic.charAt(i);
                        }

                        // Stores encypted mnemonic words into wallet's field.
                        this.mnemonicWords = encryptBytes(b, getEncryptionKey());

                        generateKeys(passPhrase);

                        initDomainClasses();
                    }
                    else
                    {
                        throw new java.lang.RuntimeException("A passphrase is mandatory.");
                    }
                }
                else
                {
                    throw new java.lang.RuntimeException("Null passphrase.");
                }
            }
            else
            {
                throw new java.lang.RuntimeException("Mnemonic words are mandatory.");
            }
        }
        else
        {
            throw new java.lang.RuntimeException("Null mnemonic words.");
        }
    }

    // Uses Pocket RPC
    // Constructor with previously owned mnemonic words and passPhrase.
    // This will import an existing wallet from blockchain.
    public TezosWallet(String mnemonicWords, String passPhrase, @NotNull String pocketDevID, @NotNull TezosNetwork pocketNetID, int pocketTimeout) throws Exception
    {
        if (mnemonicWords != null)
        {
            if (mnemonicWords.length() > 0)
            {
                if (passPhrase != null)
                {
                    if (passPhrase.length() > 0)
                    {

                        // Converts passPhrase String to a byte array, respecting char values.
                        byte[] c = new byte[passPhrase.length()];
                        for (int i = 0; i < passPhrase.length(); i++)
                        {
                            c[i] = (byte) passPhrase.charAt(i);
                        }

                        initStore(c);

                        // Cleans undesired characters from mnemonic words.
                        String cleanMnemonic = mnemonicWords.replace("[", "");
                        cleanMnemonic = cleanMnemonic.replace("]", "");
                        cleanMnemonic = cleanMnemonic.replace(",", " ");
                        cleanMnemonic = cleanMnemonic.replace("  ", " ");

                        // Converts mnemonicWords String to a byte array, respecting char values.
                        byte[] b = new byte[cleanMnemonic.length()];
                        for (int i = 0; i < cleanMnemonic.length(); i++)
                        {
                            b[i] = (byte) cleanMnemonic.charAt(i);
                        }

                        // Stores encypted mnemonic words into wallet's field.
                        this.mnemonicWords = encryptBytes(b, getEncryptionKey());

                        generateKeys(passPhrase);

                        initDomainClasses(pocketDevID, pocketNetID, pocketTimeout);
                    }
                    else
                    {
                        throw new java.lang.RuntimeException("A passphrase is mandatory.");
                    }
                }
                else
                {
                    throw new java.lang.RuntimeException("Null passphrase.");
                }
            }
            else
            {
                throw new java.lang.RuntimeException("Mnemonic words are mandatory.");
            }
        }
        else
        {
            throw new java.lang.RuntimeException("Null mnemonic words.");
        }
    }

    // Constructor for previously media persisted (saved) wallet.
    // This will load an existing wallet from media.
    public TezosWallet(Context ctx, String p)
    {
        load(ctx, p);
    }

    private void initDomainClasses()
    {
        this.rpc = new Rpc();
        this.crypto = new Crypto();
    }

    public TezosWallet(String privateKey, String publicKey, String publicKeyHash, String passPhrase) throws Exception
    {
        // Imports an existing wallet from its keys.

        resetWallet();
        this.alias="";
        this.mnemonicWords = null;

        // Converts passPhrase String to a byte array, respecting char values.
        byte[] z = new byte[passPhrase.length()];
        for (int i = 0; i < passPhrase.length(); i++)
        {
            z[i] = (byte) passPhrase.charAt(i);
        }

        initStore(z);
        initDomainClasses();

        // Converts privateKey String to a byte array, respecting char values.
        byte[] c = new byte[privateKey.length()];
        for (int i = 0; i < privateKey.length(); i++)
        {
            c[i] = (byte) privateKey.charAt(i);
        }
        this.privateKey = encryptBytes(c, getEncryptionKey());

        // Converts publicKey String to a byte array, respecting char values.
        byte[] d = new byte[publicKey.length()];
        for (int i = 0; i < publicKey.length(); i++)
        {
            d[i] = (byte) publicKey.charAt(i);
        }
        this.publicKey = encryptBytes(d, getEncryptionKey());

        // Converts publicKeyHash String to a byte array, respecting char values.
        byte[] e = new byte[publicKeyHash.length()];
        for (int i = 0; i < publicKeyHash.length(); i++)
        {
            e[i] = (byte) publicKeyHash.charAt(i);
        }
        this.publicKeyHash = encryptBytes(e, getEncryptionKey());

    }

    private void initDomainClasses(@NotNull String devID, @NotNull TezosNetwork netID, int timeout)
    {
        this.rpc = new Rpc(devID, netID, timeout);
        this.crypto = new Crypto();
    }

    // This method generates the Private Key, Public Key and Public Key hash (Tezos address).
    private void generateKeys(String passphrase) throws Exception
    {

        // Decrypts the mnemonic words stored in class properties.
        byte[] input = decryptBytes(this.mnemonicWords, getEncryptionKey());

        // Converts mnemonics back into String.
        StringBuilder builder = new StringBuilder();
        for (byte anInput : input)
        {
            builder.append((char) (anInput));
        }

        MnemonicCode mc = new MnemonicCode();
        List<String> items = Arrays.asList((builder.toString()).split(" "));
        byte[] src_seed = mc.toSeed(items, passphrase);
        byte[] seed = Arrays.copyOfRange(src_seed, 0, 32);

        milfont.com.tezosj_android.helper.KeyPair key = new milfont.com.tezosj_android.helper.KeyPair(seed);
        byte[] sodiumPublicKey = key.getPublicKey().toBytes();
        byte[] sodiumPrivateKey = key.getPrivateKey().toBytes();

        // These are our prefixes.
        byte[] edpkPrefix = {(byte) 13, (byte) 15, (byte) 37, (byte) 217};
        byte[] edskPrefix = {(byte) 43, (byte) 246, (byte) 78, (byte) 7};
        byte[] tz1Prefix = {(byte) 6, (byte) 161, (byte) 159};

        // Creates Tezos Public Key.
        byte[] prefixedPubKey = new byte[36];
        System.arraycopy(edpkPrefix, 0, prefixedPubKey, 0, 4);
        System.arraycopy(sodiumPublicKey, 0, prefixedPubKey, 4, 32);

        byte[] firstFourOfDoubleChecksum = Sha256Hash.hashTwiceThenFirstFourOnly(prefixedPubKey);
        byte[] prefixedPubKeyWithChecksum = new byte[40];
        System.arraycopy(prefixedPubKey, 0, prefixedPubKeyWithChecksum, 0, 36);
        System.arraycopy(firstFourOfDoubleChecksum, 0, prefixedPubKeyWithChecksum, 36, 4);

        // Encrypts and stores Public Key into wallet's class property.
        this.publicKey = encryptBytes(Base58.encode(prefixedPubKeyWithChecksum).getBytes(), getEncryptionKey());

        // Creates Tezos Private (secret) Key.
        byte[] prefixedSecKey = new byte[68];
        System.arraycopy(edskPrefix, 0, prefixedSecKey, 0, 4);
        System.arraycopy(sodiumPrivateKey, 0, prefixedSecKey, 4, 64);

        firstFourOfDoubleChecksum = Sha256Hash.hashTwiceThenFirstFourOnly(prefixedSecKey);
        byte[] prefixedSecKeyWithChecksum = new byte[72];
        System.arraycopy(prefixedSecKey, 0, prefixedSecKeyWithChecksum, 0, 68);
        System.arraycopy(firstFourOfDoubleChecksum, 0, prefixedSecKeyWithChecksum, 68, 4);

        // Encrypts and stores Private Key into wallet's class property.
        this.privateKey = encryptBytes(Base58.encode(prefixedSecKeyWithChecksum).getBytes(), getEncryptionKey());

        // Creates Tezos Public Key Hash (Tezos address).
        byte[] genericHash = new byte[20];
        int r = NaCl.sodium().crypto_generichash(genericHash, genericHash.length, sodiumPublicKey, sodiumPublicKey.length, sodiumPublicKey, 0);

        byte[] prefixedGenericHash = new byte[23];
        System.arraycopy(tz1Prefix, 0, prefixedGenericHash, 0, 3);
        System.arraycopy(genericHash, 0, prefixedGenericHash, 3, 20);

        firstFourOfDoubleChecksum = Sha256Hash.hashTwiceThenFirstFourOnly(prefixedGenericHash);
        byte[] prefixedPKhashWithChecksum = new byte[27];
        System.arraycopy(prefixedGenericHash, 0, prefixedPKhashWithChecksum, 0, 23);
        System.arraycopy(firstFourOfDoubleChecksum, 0, prefixedPKhashWithChecksum, 23, 4);

        String pkHash = Base58.encode(prefixedPKhashWithChecksum);

        // Encrypts and stores Public Key Hash into wallet's class property.
        this.publicKeyHash = encryptBytes(Base58.encode(prefixedPKhashWithChecksum).getBytes(), getEncryptionKey());

    }

    // Generates the mnemonic words.
    private void generateMnemonic() throws Exception
    {
        String result = "";

        MnemonicCode mc = new MnemonicCode();
        byte[] bytes = new byte[20];
        new java.util.Random().nextBytes(bytes);
        ArrayList<String> code = (ArrayList<String>) mc.toMnemonic(bytes);
        result = code.toString();

        // Converts the string with the words to a byte array, respecting char values.
        String strMessage = "";
        strMessage = (String) code.toString();

        // Cleans undesired characters from mnemonic words.
        String cleanMnemonic = strMessage.replace("[", "");
        cleanMnemonic = cleanMnemonic.replace("]", "");
        cleanMnemonic = cleanMnemonic.replace(",", " ");
        cleanMnemonic = cleanMnemonic.replace("  ", " ");

        byte[] b = new byte[cleanMnemonic.length()];
        for (int i = 0; i < cleanMnemonic.length(); i++)
        {
            b[i] = (byte) cleanMnemonic.charAt(i);
        }

        // Stores encypted mnemonic words into wallet's field.
        this.mnemonicWords = encryptBytes(b, getEncryptionKey());

    }

    // Encryption routine.
    // Uses AES encryption.
    private static byte[] encryptBytes(byte[] original, byte[] key)
    {
        try
        {
            SecretKeySpec keySpec = null;
            Cipher cipher = null;
            keySpec = new SecretKeySpec(key, "AES/ECB/PKCS7Padding");
            cipher = Cipher.getInstance("AES/ECB/PKCS7Padding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);

            return cipher.doFinal(original);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return null;
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

    // Retieves mnemonic words upon user request.
    public String getMnemonicWords()
    {

        byte[] decrypted = decryptBytes(this.mnemonicWords, getEncryptionKey());

        StringBuilder builder = new StringBuilder();
        for (byte aDecrypted : decrypted)
        {
            builder.append((char) (aDecrypted));
        }
        return builder.toString();

    }

    // Retrieves the Public Key Hash (Tezos user address) upon user request.
    public String getPublicKeyHash()
    {
        if (this.publicKeyHash != null)
        {
            if (this.publicKeyHash.length > 0)
            {

                byte[] decrypted = decryptBytes(this.publicKeyHash, getEncryptionKey());

                StringBuilder builder = new StringBuilder();
                for (byte aDecrypted : decrypted)
                {
                    builder.append((char) (aDecrypted));
                }
                return builder.toString();
            }
            else
            {
                throw new java.lang.RuntimeException("Error getting public key hash.");
            }
        }
        else
        {
            throw new java.lang.RuntimeException("Error getting public key hash.");
        }

    }

    // Retrieves the account balance.
    public String getBalance() throws Exception
    {
        if (this.publicKeyHash != null)
        {
            if (this.publicKeyHash.length > 0)
            {
                if (this.crypto.checkAddress(this.getPublicKeyHash()))
                {

                    BigDecimal tezBalance = new BigDecimal(String.valueOf(BigDecimal.ZERO));

                    byte[] decrypted = decryptBytes(this.publicKeyHash, getEncryptionKey());

                    StringBuilder builder = new StringBuilder();
                    for (byte aDecrypted : decrypted)
                    {
                        builder.append((char) (aDecrypted));
                    }

                    // Get balance from Tezos blockchain.
                    String strBalance = (String) rpc.getBalance(builder.toString()).get("result");

                    // Test if is numeric;
                    if (isNumeric(strBalance.replaceAll("[^\\d.]", "")))
                    {
                        // Test if greater then zero.
                        if (Long.parseLong(strBalance.replaceAll("[^\\d.]", "")) > 0)
                        {
                            tezBalance = new BigDecimal(strBalance.replaceAll("[^\\d.]", "")).divide(BigDecimal.valueOf(UTEZ));
                        }

                        // Updates wallet´s balance property for retrieval.
                        this.balance = String.valueOf(tezBalance) + " " + TEZOS_SYMBOL;
                    }
                    else
                    {
                        throw new java.lang.RuntimeException(strBalance);
                    }

                    return this.balance;
                }
                else
                {
                    throw new java.lang.RuntimeException("Invalid address.");
                }
            }
            else
            {
                throw new java.lang.RuntimeException("A valid Tezos address is mandatory.");
            }
        }
        else
        {
            throw new java.lang.RuntimeException("No wallet found to get balance from.");
        }

    }

    // Retrieves wallet alias.
    public String getAlias()
    {
        return this.alias;
    }

    // Sets wallet alias.
    public void setAlias(String newAlias)
    {
        this.alias = newAlias;
    }

    // Transfers funds (XTZ) from this wallet to another one.
    // Returns to the user the operation results from Tezos node.
    public JSONObject send(String from, String to, BigDecimal amount, BigDecimal fee, String gasLimit, String storageLimit, JSONObject parameters) throws Exception
    {
        JSONObject result = new JSONObject();

        if ((from != null) && (to != null) && (amount != null))
        {
            if ((this.crypto.checkAddress(from) == true) && (this.crypto.checkAddress(to) == true))
            {

                if (from.length() > 0)
                {
                    if (to.length() > 0)
                    {
                        if (amount.compareTo(BigDecimal.ZERO) > 0)
                        {
                            if (fee.compareTo(BigDecimal.ZERO) > 0)
                            {

                                // Prepares keys.
                                EncKeys encKeys = new EncKeys(this.publicKey, this.privateKey, this.publicKeyHash);
                                encKeys.setEncIv(this.encIv);
                                encKeys.setEncP(this.encPass);

                                result = rpc.transfer(from, to, amount, fee, gasLimit, storageLimit, encKeys, parameters);
                            }
                            else
                            {
                                throw new java.lang.RuntimeException("Fee must be greater than zero.");
                            }

                        }
                        else
                        {
                            throw new java.lang.RuntimeException("Amount must be greater than zero.");
                        }
                    }
                    else
                    {
                        throw new java.lang.RuntimeException("Recipient (To field) is mandatory.");
                    }
                }
                else
                {
                    throw new java.lang.RuntimeException("Sender (From field) is mandatory.");
                }
            }
            else
            {
                throw new java.lang.RuntimeException("Valid Tezos addresses are required in From and To fields.");
            }
        }
        else
        {
            throw new java.lang.RuntimeException("The fields: From, To and Amount are required.");
        }

        return result;

    }

    private void initStore(byte[] toHash)
    {
        try
        {
            String pString = new String(toHash, "UTF-8");

            int hashedP = pString.hashCode();
            String strHash = String.valueOf(hashedP);
            while (strHash.length() < 16)
            {
                strHash = strHash + strHash;
            }
            strHash = strHash.substring(0, 16); // 16 bytes needed.
            pString = strHash;

            SecretKey secretKey = createKey();
            Cipher cipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/" + KeyProperties.BLOCK_MODE_CBC + "/" + KeyProperties.ENCRYPTION_PADDING_PKCS7);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);

            byte[] encryptionIv = cipher.getIV();
            byte[] pBytes = pString.getBytes("UTF-8");
            byte[] encPBytes = cipher.doFinal(pBytes);
            String encP = Base64.encodeToString(encPBytes, Base64.DEFAULT);
            String encryptedIv = Base64.encodeToString(encryptionIv, Base64.DEFAULT);

            this.encPass = encP;
            this.encIv = encryptedIv;

        }
        catch (Exception e)
        {
            throw new RuntimeException("Could not initialize Android KeyStore.", e);
        }
    }

    private SecretKey createKey()
    {
        try
        {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            keyGenerator.init(new KeyGenParameterSpec.Builder(
                    TZJ_KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    //.setUserAuthenticationRequired(true)
                    .setUserAuthenticationValidityDurationSeconds(5)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .build());

            return keyGenerator.generateKey();

        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed to create a symetric key", e);
        }

    }


    private byte[] getEncryptionKey()
    {
        try
        {
            String base64EncryptedPassword = this.encPass;
            String base64EncryptionIv = this.encIv;

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

    public static byte[] getEncryptionKey(EncKeys keys)
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

    public void save(Context ctx)
    {
        // Persists the wallet to media from memory.
        Boolean result;

        try
        {

            String myWalletData = Base64.encodeToString(this.alias.getBytes(), Base64.DEFAULT) + ";" +
                    Base64.encodeToString(this.publicKey, Base64.DEFAULT) + ";" +
                    Base64.encodeToString(this.publicKeyHash, Base64.DEFAULT) + ";" +
                    Base64.encodeToString(this.privateKey, Base64.DEFAULT) + ";" +
                    Base64.encodeToString(this.balance.getBytes(), Base64.DEFAULT) + ";" +
                    Base64.encodeToString(this.mnemonicWords, Base64.DEFAULT) + ";";

            SharedPreferencesHelper sp = new SharedPreferencesHelper();
            sp.setSharedPreferenceString(ctx, TZJ_KEY_ALIAS, myWalletData);

            result = true;

        }
        catch (Exception e)
        {
            throw new java.lang.RuntimeException("Error when trying to save the wallet to media.");
        }

    }

    public void load(Context ctx, String p)
    {
        // Loads a wallet from media to memory.

        try
        {
            String myWalletString = "";

            SharedPreferencesHelper sp = new SharedPreferencesHelper();
            myWalletString = sp.getSharedPreferenceString(ctx, TZJ_KEY_ALIAS, "");

            if (myWalletString.length() > 0)
            {
                resetWallet();

                String[] fields = myWalletString.split("\\;", -1);
                this.alias = new String(Base64.decode(fields[0], Base64.DEFAULT), "UTF-8");
                this.publicKey = Base64.decode(fields[1], Base64.DEFAULT);
                this.publicKeyHash = Base64.decode(fields[2], Base64.DEFAULT);
                this.privateKey = Base64.decode(fields[3], Base64.DEFAULT);
                this.balance = new String(Base64.decode(fields[4], Base64.DEFAULT), "UTF-8");
                this.mnemonicWords = Base64.decode(fields[5], Base64.DEFAULT);

                // Converts passPhrase String to a byte array, respecting char values.
                byte[] c = new byte[p.length()];
                for (int i = 0; i < p.length(); i++)
                {
                    c[i] = (byte) p.charAt(i);
                }

                initStore(c);
                initDomainClasses();
            }

        }
        catch (Exception e)
        {
            throw new java.lang.RuntimeException("Error when trying to load wallet from media.");
        }

    }

    private String buildStringFromByte(byte[] input)
    {
        StringBuilder builder = new StringBuilder();
        for (byte anInput : input)
        {
            builder.append((char) (anInput));
        }
        return builder.toString();
    }

    private byte[] buildByteFromString(String input)
    {
        byte[] d = new byte[input.length()];
        for (int i = 0; i < input.length(); i++)
        {
            d[i] = (byte) input.charAt(i);
        }

        return d;
    }

    // Removes the wallet data from memory.
    private void resetWallet()
    {
        this.privateKey = null;
        this.mnemonicWords = null;
        this.encPass = null;
        this.encIv = null;
        this.publicKeyHash = null;
        this.publicKey = null;
        this.balance = "";
        this.transactions = null;
        this.rpc = null;
    }

    // Checks if a give phrase is the correct wallet passphrase.
    public Boolean checkPhrase(String phrase)
    {
        Boolean result;

        try
        {
            MnemonicCode mc = new MnemonicCode();
            List<String> items = Arrays.asList((this.getMnemonicWords()).split(" "));
            byte[] src_seed = mc.toSeed(items, phrase);
            byte[] seed = Arrays.copyOfRange(src_seed, 0, 32);

            milfont.com.tezosj_android.helper.KeyPair key = new milfont.com.tezosj_android.helper.KeyPair(seed);
            byte[] sodiumPublicKey = key.getPublicKey().toBytes();
            byte[] sodiumPrivateKey = key.getPrivateKey().toBytes();

            // These are our prefixes.
            byte[] edpkPrefix = {(byte) 13, (byte) 15, (byte) 37, (byte) 217};

            // Creates Tezos Public Key.
            byte[] prefixedPubKey = new byte[36];
            System.arraycopy(edpkPrefix, 0, prefixedPubKey, 0, 4);
            System.arraycopy(sodiumPublicKey, 0, prefixedPubKey, 4, 32);

            byte[] firstFourOfDoubleChecksum = Sha256Hash.hashTwiceThenFirstFourOnly(prefixedPubKey);
            byte[] prefixedPubKeyWithChecksum = new byte[40];
            System.arraycopy(prefixedPubKey, 0, prefixedPubKeyWithChecksum, 0, 36);
            System.arraycopy(firstFourOfDoubleChecksum, 0, prefixedPubKeyWithChecksum, 36, 4);

            String publicKey = Base58.encode(prefixedPubKeyWithChecksum);

            // Converts this.publicKey into String.
            StringBuilder builder = new StringBuilder();
            byte[] input = decryptBytes(this.publicKey, getEncryptionKey());
            for (byte anInput : input)
            {
                builder.append((char) (anInput));
            }

            if (publicKey.equals(builder.toString()))
            {
                result = true;
            }
            else
            {
                result = false;
            }

        }
        catch (Exception e)
        {
            result = false;
        }

        return result;
    }


}

