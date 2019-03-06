package milfont.com.tezosj_android.domain;

import milfont.com.tezosj_android.helper.Base58Check;

public class Crypto
{

    public Boolean checkAddress(String address) throws Exception
    {
        try
        {
            Base58Check base58Check = new Base58Check();

            byte[] result = base58Check.decode(address);
            return true;
        }
        catch (Exception e)
        {
            return  false;
        }

    }

}
