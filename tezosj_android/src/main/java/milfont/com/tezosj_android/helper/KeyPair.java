package milfont.com.tezosj_android.helper;

import org.libsodium.jni.crypto.Point;
import org.libsodium.jni.encoders.Encoder;
import org.libsodium.jni.keys.PublicKey;

import static org.libsodium.jni.NaCl.sodium;
import static org.libsodium.jni.SodiumConstants.PUBLICKEY_BYTES;
import static org.libsodium.jni.SodiumConstants.SECRETKEY_BYTES;
import static org.libsodium.jni.crypto.Util.zeros;

public class KeyPair
{

    private byte[] publicKey;
    private final byte[] secretKey;

    public KeyPair()
    {
        this.secretKey = zeros(SECRETKEY_BYTES * 2);
        this.publicKey = zeros(PUBLICKEY_BYTES);
        sodium().crypto_box_curve25519xsalsa20poly1305_keypair(publicKey, secretKey);
    }


    public KeyPair(byte[] seed)
    {
        //Util.checkLength(seed, SECRETKEY_BYTES);
        byte[] seed1 = seed;
        this.secretKey = zeros(SECRETKEY_BYTES * 2);
        this.publicKey = zeros(PUBLICKEY_BYTES);
        sodium().crypto_sign_seed_keypair(publicKey, secretKey, seed);
    }

    public KeyPair(String secretKey, Encoder encoder)
    {
        this(encoder.decode(secretKey));
    }

    public PublicKey getPublicKey()
    {
        Point point = new Point();
        byte[] key = publicKey != null ? publicKey : point.mult(secretKey).toBytes();
        return new PublicKey(key);
    }

    public PrivateKey getPrivateKey()
    {
        return new PrivateKey(secretKey);
    }

}
