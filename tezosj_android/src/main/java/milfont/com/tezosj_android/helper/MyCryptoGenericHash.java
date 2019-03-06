package milfont.com.tezosj_android.helper;

import org.libsodium.jni.Sodium;

/**
 * Created by Milfont on 09/03/2018.
 */

public class MyCryptoGenericHash extends Sodium
{
    public static byte[] cryptoGenericHash(byte[] input, int outputLength) throws Exception
    {
        byte[] genericHash = new byte[outputLength];

        int rc = Sodium.crypto_generichash(genericHash, genericHash.length, input, input.length, input, 0);

        if (rc != 0)
        {
            throw new Exception("cryptoGenericHash libsodium crypto_generichash failed, returned " + rc + ", expected 0");
        }

        return genericHash;
    }


    public static int cryptoSignDetached(byte[] input, int[] signature_len, byte[] src_msg, int msg_len, byte[] local_private_key) throws Exception
    {
        byte[] dst_signature = new byte[msg_len];

        int rc = Sodium.crypto_sign_detached(dst_signature, signature_len, src_msg, msg_len, local_private_key);

        if (rc != 0)
        {
            throw new Exception("cryptoSignDetached libsodium crypto_sign_detached failed, returned " + rc + ", expected 0");
        }

        return rc;
    }


}
