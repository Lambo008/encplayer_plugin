package com.ramzi.chunkproject.utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class AppPreferences {
    private SharedPreferences appSharedPrefs;
    private Editor prefsEditor;
    public AppPreferences(Context context, String Preferncename) {
        this.appSharedPrefs = context.getSharedPreferences(Preferncename,
                Activity.MODE_PRIVATE);
        this.prefsEditor = appSharedPrefs.edit();
    }

    public void saveFloatData(String text, Float Tag) {
        prefsEditor.putFloat(text, Tag);
        prefsEditor.commit();
    }
    public Float getFloat(String key) {
        return appSharedPrefs.getFloat(key,-100f);

    }

}