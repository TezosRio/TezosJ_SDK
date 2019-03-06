package milfont.com.tezosj_android.helper;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPreferencesHelper
{
    private final static String PREF_FILE = "PREF";


    public void setSharedPreferenceString(Context context, String key, String value)
    {
        SharedPreferences settings = context.getSharedPreferences(PREF_FILE, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(key, value);
        editor.apply();
    }


    public void setSharedPreferenceInt(Context context, String key, int value)
    {
        SharedPreferences settings = context.getSharedPreferences(PREF_FILE, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(key, value);
        editor.apply();
    }


    public void setSharedPreferenceBoolean(Context context, String key, boolean value)
    {
        SharedPreferences settings = context.getSharedPreferences(PREF_FILE, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }


    public String getSharedPreferenceString(Context context, String key, String defValue)
    {
        SharedPreferences settings = context.getSharedPreferences(PREF_FILE, 0);
        return settings.getString(key, defValue);
    }


    public int getSharedPreferenceInt(Context context, String key, int defValue)
    {
        SharedPreferences settings = context.getSharedPreferences(PREF_FILE, 0);
        return settings.getInt(key, defValue);
    }


    public boolean getSharedPreferenceBoolean(Context context, String key, boolean defValue)
    {
        SharedPreferences settings = context.getSharedPreferences(PREF_FILE, 0);
        return settings.getBoolean(key, defValue);
    }
}