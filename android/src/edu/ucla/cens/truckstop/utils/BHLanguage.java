package edu.ucla.cens.truckstop.utils;

import java.util.Locale;

import edu.ucla.cens.truckstop.R;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.util.Log;

public class BHLanguage {
	private static final String TAG = "BHLanguage";

	// Variables to track the state for a user's preference for language
	public static final String ENGLISH = "en";
	public static final String SPANISH = "es";

	// Returns the language currently set in preferences
	public static String getLanguage(Context ctx) {
		return PreferencesMgr.getString(ctx, PreferencesMgr.LANGUAGE, ENGLISH);
	}

	// Returns the language set in the system
	public static String getSystemLanguageSetting(Context ctx) {
        Resources res = ctx.getResources();
        Configuration conf = res.getConfiguration();
        return conf.locale.toString();
	}
	// Sets the new language, and saves the information in preferences
	public static void setLanguage(Context ctx, String language) {
		Log.d(TAG, "Setting language to: " + language);

    	// Set the locale
        Resources res = ctx.getResources();
        DisplayMetrics dm = res.getDisplayMetrics();
        Configuration conf = res.getConfiguration();
        conf.locale = new Locale(language);
        res.updateConfiguration(conf, dm);

        Log.d(TAG, "Now language is: " + getSystemLanguageSetting(ctx));

        // Save the language
		PreferencesMgr.setString(ctx, PreferencesMgr.LANGUAGE, language);
	}
}
