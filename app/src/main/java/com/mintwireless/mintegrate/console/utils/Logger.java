package com.mintwireless.mintegrate.console.utils;

import android.util.Log;

/**
 * Created by Jialian on 5/05/16.
 */
public class Logger {

    public static void logInfo(Class cls, String log){
        Log.i(cls.getSimpleName(),"[ logged INFO: ] " + log);
    }

    public static void logDebug(Class cls, String log){
        Log.d(cls.getSimpleName(), "[ logged DEBUG: ] " + log);
    }

}
