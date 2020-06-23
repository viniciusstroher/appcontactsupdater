package br.rcx.updatercontacts.services;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferenceService {
    private SharedPreferences sharedPref;
    private SharedPreferences.Editor editor;

    public PreferenceService(SharedPreferences sharedPref){
        this.sharedPref = sharedPref;
        this.editor = sharedPref.edit();
    }

    public void setPreference(String key,String value){
        editor.putString(key, value);
        editor.commit();
    }

    public void setPreference(String key,Boolean value){
        editor.putBoolean(key, value);
        editor.commit();
    }

    public String getPreference(String key){
        return sharedPref.getString(key,null);
    }

    public boolean getPreferenceBoolean(String key){
        return sharedPref.getBoolean(key,false);
    }
}
