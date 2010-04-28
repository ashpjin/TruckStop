package edu.ucla.cens.truckstop.utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import edu.ucla.cens.truckstop.R;

public class PreferencesMgr {
	private static final String TAG = "PreferencesMgr";

    // Strings to access the user and password from the preferences menu.
    public static final String USER = "user";
    public static final String PASSWORD = "pass";
    public static final String SAVE_LOGIN = "save_login";
    public static final String PASS_HASHED = "_un";
    public static final String AUTHENTICATED = "authenticated";
    public static final String REGISTERED = "registered";

    // Set language
	public static final String LANGUAGE = "language";

    // Path data
	public static final String PATH_RUNNING = "pathServiceRunning";

	// The path service should only be run the first time a user log in.
	//	after that, the service should not be launched again, unless the
	//	user specifically requests it.
	public static final String PATH_RUN_ONCE = "pathServiceRun";

    // Default return value is empty string
    private static String DEFAULT_STRING = "";
    private static boolean DEFAULT_BOOLEAN = false;

    private static SharedPreferences getPrefs (Context ctx) {
    	String pathPrefs = ctx.getString(R.string.preferences);
    	return ctx.getSharedPreferences(pathPrefs, Activity.MODE_PRIVATE);
    }

    // Each key should be accessed by the username, to prevent any issues
    //	when a new user registers. So this function will return the <username>+_key;
    // 	If the requested key IS for the username, then this function just returns
    //	key.
    private static String getKey(Context ctx, String key) {
    	if (key.equals(USER))
    		return key;

    	String user = getString(ctx, USER, "");

    	// Return the empty string if there is no user
    	if (user.equals(""))
    		return "";

    	// Otherwise return the aggregate key
    	return user + "_" + key;
    }

    public static String getString(Context ctx, String key) {
    	return getString(ctx, key, DEFAULT_STRING);
    }

    public static String getString(Context ctx, String key, String defaultVal) {
    	String localKey = getKey(ctx, key);
    	Log.d(TAG, "getString key: " + key + " and localKey:: " + localKey);

    	if (localKey.equals(""))
    		return defaultVal;
    	return getPrefs(ctx).getString(localKey, defaultVal);
    }

    public static boolean getBoolean(Context ctx, String key) {
    	return getBoolean(ctx, key, DEFAULT_BOOLEAN);
    }

    public static boolean getBoolean(Context ctx, String key, boolean defaultVal) {
    	String localKey = getKey(ctx, key);

    	if (localKey.equals(""))
    		return defaultVal;
    	return getPrefs(ctx).getBoolean(localKey, defaultVal);
    }

    public static void setString(Context ctx, String key, String value) {
    	String localKey = getKey(ctx, key);
    	if (localKey.equals("")) {
    		Log.w(TAG, "Not setting preferences for key: " + key
    				+ " because no user has been set yet!");
    		return;
    	}

    	getPrefs(ctx).edit()
			.putString(localKey, value)
			.commit();
    }

    public static void setBoolean(Context ctx, String key, boolean value) {
    	String localKey = getKey(ctx, key);
    	if (localKey.equals("")) {
    		Log.w(TAG, "Not setting preferences for key: " + key
    				+ " because no user has been set yet!");
    		return;
    	}

    	getPrefs(ctx).edit()
			.putBoolean(localKey, value)
			.commit();
    }
}
