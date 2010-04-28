package edu.ucla.cens.truckstop.services;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import edu.ucla.cens.truckstop.survey.CreateRoute;
import edu.ucla.cens.truckstop.utils.*;
import edu.ucla.cens.truckstop.R;

public class RecordPath extends Service {
    private static final String TAG = "RecordPath";

    private SurveyDB sdb;
    private CreateRoute survey;
    private Service ctx;

    private long PATH_PERIOD_MSECS, PATH_DURATION_MSECS;
    private static final long PATH_DURATION_DEFAULT = 3 * 60 * 60 * 1000; // Default to 3 hours
    private static final long PATH_PERIOD_DEFAULT = 5 * 60 * 1000;  // Default to 5 minute

    private RequestLocation requestLoc;
    private Timer endTimer = null;


    @Override
    public void onCreate() {
        Log.d(TAG, "Started");
        ctx = this;

        // Set up the database for the route survey.
        survey = new CreateRoute(ctx);
        sdb = new SurveyDB(survey.databaseTable(), survey.uploadURL(),
                survey.getDBKeys());

        // Bind to the location service so we can get location.
        Log.d(TAG, "LightLocation service started");
        requestLoc = new RequestLocation(this, new locationListener());

        try {
            PATH_PERIOD_MSECS = Long.parseLong(getString(R.string.pathRequestPeriod));
        } catch (NumberFormatException nfe) {
            PATH_PERIOD_MSECS = PATH_PERIOD_DEFAULT;
        }

        try {
            PATH_DURATION_MSECS = Long.parseLong(getString(R.string.pathRequestDuration).trim());
        } catch (NumberFormatException nfe) {
            PATH_DURATION_MSECS = PATH_DURATION_DEFAULT;
        }

        Log.d(TAG, "Request location every secs: " + PATH_PERIOD_MSECS);
        requestLoc.startLocation(PATH_PERIOD_MSECS, "");

        // Start the timer to kill this service
        endTimer = new Timer();
        endTimer.schedule(endProcess, PATH_DURATION_MSECS, PATH_DURATION_MSECS);
    }

    private void saveRow(Location loc) {
        // Open the database and insert the row
        DBRow row = new DBRow(ctx, loc);
        sdb.openWriteable(ctx, survey.databaseTable());
        sdb.insertEntry(ctx, row);
        sdb.close();
    }

    TimerTask endProcess = new TimerTask() {

        public void run() {
                Log.d(TAG, "Ending process!");
                ctx.stopSelf();
        }

        public boolean cancel() {
                Log.d(TAG, "Cancelling the endprocess thread");
                return true;
        }
    };

    public class locationListener implements LocationListener {

        public void onLocationChanged (Location loc) {
            Log.d(TAG, "Received location: " + loc.toString());
            saveRow(loc);
        }

       public void onProviderDisabled (String provider) {}

       public void onProviderEnabled (String provider) {}

       public void onStatusChanged (String provider, int status, Bundle extras) {}
    };


    @Override
    public IBinder onBind(Intent arg0) {
        Log.d(TAG, "Somebody is binding to this service");
        return null;
    }

    @Override
    public void onDestroy() {

        // Cancel the timer, in case it is still running
        endProcess.cancel();

        // Unbind from the location service
        requestLoc.destroy();
    }
}
