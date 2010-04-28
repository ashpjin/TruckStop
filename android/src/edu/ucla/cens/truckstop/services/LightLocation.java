package edu.ucla.cens.truckstop.services;

// Reset CurrentLocation to every N minutes. Stale the location every M minutes, so that
//  we avoid providing old locations to applications.

import edu.ucla.cens.truckstop.utils.RequestLocation;
import edu.ucla.cens.truckstop.R;
import android.os.Bundle;
import android.os.IBinder;
import android.content.Intent;
import android.app.Service;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.util.Log;

public class LightLocation extends Service {
    // Configuration information
    private static final String TAG = "LIGHT_LOC";
    private long LOCATION_PERIOD_MSECS;
    private long LOCATION_PERIOD_DEFAULT = 10 * 60 * 1000;  // Default to 10 minute

    private RequestLocation requestLoc;

    /* Usual administrative set up for a Service */
    @Override
    public void onCreate () {
        super.onCreate ();
        Log.d(TAG, "LightLocation service started");

        requestLoc = new RequestLocation(this);
        LOCATION_PERIOD_MSECS = Long.getLong(getString(R.string.locationRequestPeriod),
                LOCATION_PERIOD_DEFAULT);

        // We want both locations to be obtained, if possible
        requestLoc.startLocation(LOCATION_PERIOD_MSECS, LocationManager.GPS_PROVIDER);
        requestLoc.startLocation(LOCATION_PERIOD_MSECS, LocationManager.NETWORK_PROVIDER);
    }

    @Override
    public IBinder onBind( Intent intent ) {
        Log.d(TAG, "Somebody is binding to this service");
        return null;
    }

    @Override
    public void onDestroy () {
        Log.d(TAG, "LightLocation service destroyed");
        requestLoc.destroy();
        super.onDestroy ();
    }
}
