package edu.ucla.cens.truckstop.utils;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

public class RequestLocation {

	private static final String TAG = "RequestLocationUtil";

	private LocationManager lm;
    private LocationListener ll;

    // Location stamp can be taken no more than this many milliseconds ago
    private static final long STALE_TIME_MILLIS = 10 * 60 * 1000;


    // Determine if the location is stale based on the timestamp
    private static boolean isStale(Location loc) {
    	long nowMillis = System.currentTimeMillis();

    	// Location is stale if its timestamp is too old
    	if (nowMillis - loc.getTime() > STALE_TIME_MILLIS) {
       		Log.d(TAG, "Location is stale, based on timestamp");
       		return true;
    	}

    	return false;
    }

    // Returns a location, uses a common stale value to determine if the location
    //	is recent enough.
    public static Location getLocation(Context ctx, String provider) {
    	// Get location from GPS. If GPS is older than
    	//	STALE_MINUTES, than check Network and see if it was obtained more recently.
    	LocationManager lmTmp = (LocationManager) ctx.getSystemService (Context.LOCATION_SERVICE);
    	Location location = lmTmp.getLastKnownLocation(provider);

    	if (location == null) {
    		Log.d(TAG, "Location is null");
    		return null;
    	}

    	Log.d(TAG, "Provider: " + provider + " / Location was taken: " + location.getTime());

    	if (RequestLocation.isStale(location)) return null;
    	return location;
    }

    public static Location getBestLocation(Context ctx) {
    	Location location = getLocation(ctx, LocationManager.GPS_PROVIDER);

    	if (location == null) {
        	location = getLocation(ctx, LocationManager.NETWORK_PROVIDER);
    	}
    	return location;
    }


    public RequestLocation(Context ctx, LocationListener ll) {
    	Log.d(TAG, "New requestLocation");

    	// Request the provider
    	lm = (LocationManager) ctx.getSystemService (Context.LOCATION_SERVICE);
    	this.ll = ll;
    }

    public RequestLocation(Context ctx) {
    	Log.d(TAG, "New requestLocation");

    	// Request the provider
    	lm = (LocationManager) ctx.getSystemService (Context.LOCATION_SERVICE);
    	this.ll = new dummyLocationListener();
    }

    public void destroy() {
        if (null != lm && null != ll) {
            lm.removeUpdates (ll);
        }
        ll = null;
        lm = null;
    }

	/* Request updates from the location provider.
	 * If provider == "", then we request the best possible provider
	 */
    public void startLocation(long period, String provider) {

    	Log.d(TAG, "startLocation() with period: " + period + " and provider: " + provider);

    	if (provider.equals("")) {

        	// Get the best provider, based on accuracy. Only request enabled providers
        	Criteria c = new Criteria();
        	c.setAccuracy(Criteria.ACCURACY_FINE);

    		provider = lm.getBestProvider(c, true);
    		Log.d(TAG, "Null provider provided, to we are setting to : " + provider);
    	}

    	// Schedule the location provider to send regular location updates.
        try {
            if (null != lm && null != ll) {
                lm.removeUpdates (ll);
            }
        	lm.requestLocationUpdates (provider, period, 0, ll);
            Log.d(TAG, "started location service, with provider: " + provider +
            		"and period: " + period);
        } catch (Exception e) {
        	Log.e(TAG, "Could not request location updates from location provider");
        	e.printStackTrace();
        }
    }

    /* Gets updated location. We don't need to store it, we just need to update the
     * 	location so that any other services that request location using getLastKnownLocation()
     * 	will get an updated location. So we create this dummy class that doesn't do anything.
     */
    public class dummyLocationListener implements LocationListener {
        public void onLocationChanged (Location loc) {
        	Log.d(TAG, "Updated location to: " + loc.toString());
        }

        public void onProviderDisabled (String provider) {}

        public void onStatusChanged (String provider, int status, Bundle extras) {}

		public void onProviderEnabled(String provider) {}
    };
}
