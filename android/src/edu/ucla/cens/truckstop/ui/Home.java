package edu.ucla.cens.truckstop.ui;

/* Author: Nithya Ramanathan, Lorax Analytics
 * This class displays the initial control page. This activity also launches all of
 *  the background services: e.g. Upload, and Path recording.
 */

import java.util.Locale;

import android.os.Bundle;
import android.os.SystemClock;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.widget.Button;
import android.widget.ToggleButton;

import edu.ucla.cens.truckstop.R;
import edu.ucla.cens.truckstop.services.GetUploadData;
import edu.ucla.cens.truckstop.services.LightLocation;
import edu.ucla.cens.truckstop.services.RecordPath;
import edu.ucla.cens.truckstop.content.CreateQuestions;
import edu.ucla.cens.truckstop.utils.SurveyDB;
import edu.ucla.cens.truckstop.utils.BHLanguage;
import edu.ucla.cens.truckstop.utils.PreferencesMgr;

public class Home extends Activity {
    private static final String TAG = "Home";

    private static final boolean RECORD_PATH = false;

    // Variables to start the services
    private PendingIntent startLocationTrace;
    private AlarmManager am;
    private static final long LOCATION_SAMPLING_PERIOD =  1 * 60 * 1000; // milliseconds
    private static final long UPLOAD_PERIOD =  1 * 60 * 1000; // milliseconds

    // Variables to track the state for a user's preference of start/stop
    //  the path service
    private SharedPreferences preferences;
    private static final String PREFS_PATH_SERVICE_STATE = "pathServiceRunning";
    private boolean pathServiceRunning;
    private ToggleButton pathButton;

    // Variables to track the state for a user's preference for language
    private static final String PREFS_LANGUAGE = "language";
    private static final String ENGLISH = "en";
    private static final String SPANISH = "es";
    private String language;
    private Button translate;

    @Override
    public boolean onCreateOptionsMenu (Menu m) {
        super.onCreateOptionsMenu (m);

        m.add (Menu.NONE, 0, Menu.NONE, "About").setIcon (android.R.drawable.ic_menu_info_details);
        m.add (Menu.NONE, 1, Menu.NONE, "Instructions").setIcon (android.R.drawable.ic_menu_help);
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate (savedInstanceState);
        setContentView (R.layout.home);

        // Start the upload service alarm to go off UPLOAD_FREQUENCY mili-seconds from now
        am = (AlarmManager)getSystemService(ALARM_SERVICE);
        PendingIntent startUploadService = PendingIntent.getService(Home.this,
                0, new Intent(Home.this, GetUploadData.class), 0);
        am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime(), UPLOAD_PERIOD, startUploadService);

        // Get the preferences variable so that we can record state.
        preferences = this.getSharedPreferences(getString(R.string.preferences), Activity.MODE_PRIVATE);

        if (RECORD_PATH) {
            // Set the state for the pathServiceStarted variable. This needs to be
            //  correct, to determine what the button should do when clicked.
            // restore previous state (if available)

            pathServiceRunning = preferences.getBoolean(PREFS_PATH_SERVICE_STATE, true);

            // Create Intents that will be started by the alarm manager, and to start
            //  services.
            startLocationTrace = PendingIntent.getService(Home.this,
                0, new Intent(Home.this, RecordPath.class), 0);
            startService(new Intent(this, LightLocation.class));

            // The default state is true, because the path service should be started, unless the
            //  user expressly requested that it not be started.
            if (pathServiceRunning) startPathService();

            // Watch for button clicks.
            pathButton = (ToggleButton)findViewById(R.id.path);
            pathButton.setOnClickListener(mPathListener);
            pathButton.setChecked(pathServiceRunning);
            pathButton.setVisibility(View.VISIBLE);
        }

        // See what language to set: start with English
        language = preferences.getString(PREFS_LANGUAGE, ENGLISH);
        Button button_en = (Button) findViewById(R.id.start_survey_en);
        button_en.setOnClickListener(mEnglishListener);

        /*Button button_about = (Button) findViewById(R.id.about);
        button_about.setOnClickListener(mAboutListener);

        Button button_instr = (Button) findViewById(R.id.instructions);
        button_instr.setOnClickListener(mInstructionsListener);*/

        Button button_es = (Button) findViewById(R.id.start_survey_es);
        button_es.setOnClickListener(mSpanishListener);

        // Open database so that it creates the tables. Should only have to do this for one
        //  survey. This should force SurveyDB to initialize the path variables to the database.
        //  and create the database.
        CreateQuestions survey = new CreateQuestions(this);
        SurveyDB sdb = new SurveyDB(survey.databaseTable(), survey.uploadURL(),
                survey.getDBKeys());
        sdb.openWriteable(this, survey.databaseTable());
        sdb.close();

        Log.d(TAG, "onCreate");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "OnStart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "OnResume");

        if (RECORD_PATH) setPathButton(pathServiceRunning);
    }

    private void startPathService() {
        Log.d(TAG, "Start path service");

        // Schedule the alarm to go off SAMPLING_FREQUENCY mili-seconds from now
        am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime(), LOCATION_SAMPLING_PERIOD, startLocationTrace);
        pathServiceRunning = true;
    }

    private void startSurvey() {
        Home.this.startActivity (new Intent (Home.this, Survey.class));
        Home.this.finish ();
    }

    View.OnClickListener mEnglishListener = new View.OnClickListener () {
        public void onClick(View v) {
            setLanguage(BHLanguage.ENGLISH);
            startSurvey();
        }
    };

    View.OnClickListener mSpanishListener = new View.OnClickListener () {
        public void onClick(View v) {
            Log.d(TAG, "in Spanish Listener");
            setLanguage(BHLanguage.SPANISH);
            startSurvey();
        }
    };

    View.OnClickListener mAboutListener = new View.OnClickListener () {
        public void onClick(View v) {
            Home.this.startActivity (new Intent (Home.this, About.class));
            Home.this.finish ();
        }
    };

    View.OnClickListener mInstructionsListener = new View.OnClickListener () {
        public void onClick(View v) {
            Home.this.startActivity (new Intent (Home.this, Instructions.class));
            Home.this.finish ();
        }
    };

    private OnClickListener mPathListener = new OnClickListener() {

        public void onClick(View v) {

            // Start the path service
            if (!pathServiceRunning)
                startPathService();

            // Stop the path service
            else {
                AlarmManager am = (AlarmManager)getSystemService(ALARM_SERVICE);
                am.cancel(startLocationTrace);
                pathServiceRunning = false;
            }

            setPathButton(pathServiceRunning);
        }
    };

    private void setPathButton(boolean running) {
        String label;

        // If the service is now running, then set the button text to indicate
        //  that the next click will make it stop running.
        if (running) label = getString(R.string.stopTrace);
        else label = getString(R.string.startTrace);

        // Set the button text, and save this value to preferences
        pathButton.setText(label);
        preferences.edit().putBoolean(PREFS_PATH_SERVICE_STATE, running).commit();
    }

    private void setLanguage(String language) {
        Log.d(TAG, "Setting language to: " + language);
        BHLanguage.setLanguage(this, language);

        // Update the text for all of the buttons in this view
        if (RECORD_PATH) setPathButton(this.pathServiceRunning);
    }
}
