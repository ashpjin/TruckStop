    package edu.ucla.cens.truckstop.ui;

    import android.app.Activity;
    import android.location.Location;
    import android.location.LocationListener;
    import android.location.LocationManager;
    import android.os.Bundle;
    import android.util.Log;
    import android.content.Context;
    import android.content.Intent;
    import android.view.View;
    import android.view.View.OnClickListener;
    import android.view.Menu;
    import android.view.MenuItem;
    import android.widget.Button;
    import android.widget.ScrollView;
    import android.widget.Toast;
    import java.util.ArrayList;

    import edu.ucla.cens.truckstop.utils.*;
    import edu.ucla.cens.truckstop.content.CreateQuestions;
    import edu.ucla.cens.truckstop.survey.*;
    import edu.ucla.cens.truckstop.R;

    public class Survey extends Activity
    {
        private String TAG = "Survey";
        private Button submit_button;
        private SurveyDB sdb;
        private Activity ctx = this;

        // Get the survey information
        private CreateQuestions survey;
        private ArrayList<Question> questions;

        // Variables to access saved instance state
        private final static String SAVED_STARTED = "started";
        private final static String SAVED_XPOS = "xpos";
        private final static String SAVED_YPOS = "ypos";
        private int xpos; // Latest recorded x-position
        private int ypos; // Latest recorded y-position

        // Location updater
        private RequestLocation locationUpdater;
        private static final long locationPeriod = 30 * 1000;

        @Override
        public void onDestroy() {
            super.onDestroy();
            Log.d(TAG, "Destroyed survey activity");
            locationUpdater.destroy();
        }

        /* TODO: Need to implement onPause/onResume to save to the database the state that
         * has been modified by the user.
         */
        @Override
        public void onPause() {
            super.onPause();
            Log.d(TAG, "onPause");
        }

        @Override
        public void onStart() {
            super.onStart();

            Log.d(TAG, " onStart: Now language is: " +
                    BHLanguage.getSystemLanguageSetting(ctx));
        }

        @Override
        public void onResume() {
            super.onResume();
            Log.d(TAG, "onResume: Now language is: " + BHLanguage.getSystemLanguageSetting(ctx));
        }

        // Save the state so that it can be restored if the Activity is killed or
        //   when the screen changes orientation.
        @Override
        public void onSaveInstanceState(Bundle savedInstanceState) {

            savedInstanceState.putBoolean(SAVED_STARTED, true);

            // Cycle through the questions to save the state for Question objects that
            //  require custom saving. Question objects that implement getRestoreState()
            //  fall into this category.

            for (int i = 0; i < questions.size(); i++) {
                String key = questions.get(i).getDBKey();
                String value = questions.get(i).getRestoreState();
                if (value != "") {
                    savedInstanceState.putString(key, value);
                    Log.d(TAG, "Storing state for key: " + key + " value = " + value);
                }
            }

            // Save current screen position
            ScrollView sv = CreateSurvey.getScrollViewId(this);
            xpos = sv.getScrollX();
            ypos = sv.getScrollY();
            savedInstanceState.putInt(SAVED_XPOS, xpos);
            savedInstanceState.putInt(SAVED_YPOS, ypos);
            Log.d(TAG, "Saving off ypos: " + ypos);

            super.onSaveInstanceState(savedInstanceState);
        }

        /** Called when the activity is first created. */
        @Override
        public void onCreate(Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);

            Log.d(TAG, "OnCreate");

            // Start a location listener at a short periodicity. This location listener supplements
            //  the location updates done by LightLocation. We do this to increase the accuracy
            //  of location updates, only while the user is taking the survey. This is killed
            //  once the survey goes away. We have to provide a location listener, even though
            //  it doesn't do anything.
            locationUpdater = new RequestLocation(this);
            locationUpdater.startLocation(locationPeriod, LocationManager.GPS_PROVIDER);

            // Restore language
            BHLanguage.setLanguage(ctx, BHLanguage.getLanguage(ctx));

            // Initialize datastructures and services
            survey = new CreateQuestions(this);
            sdb = new SurveyDB(survey.databaseTable(), survey.uploadURL(),
                    survey.getDBKeys());
            questions = survey.getQuestions();
            setContentView(R.layout.survey2);

            // Cycle through each question in the survey, to assign the View for that reseponse.
            // This has to be done from the calling activity, which is the only place that has access
            //  to the View pointer.
            for (int i = 0; i < questions.size(); i++) {
                questions.get(i).layoutQuestion(Survey.this);
            }

            // add submit button and listener
            submit_button = (Button) findViewById(R.id.upload_button);
            submit_button.setOnClickListener(submit_button_listener);

            // restore previous state (if available)
            if (savedInstanceState != null && savedInstanceState.getBoolean(SAVED_STARTED)) {
                for (int i = 0; i < questions.size(); i++) {
                    String value = savedInstanceState.getString(questions.get(i).getDBKey());
                    if (value != null) {
                        questions.get(i).setRestoreState(value);
                    }
                }

                // Restore screen position
                xpos = savedInstanceState.getInt(SAVED_XPOS);
                ypos = savedInstanceState.getInt(SAVED_YPOS);
                ScrollView sv = CreateSurvey.getScrollViewId(this);

                // This has to be delayed because layout has not yet happened.
                sv.post(new Runnable() {
                    public void run() {

                        // Restore screen position
                        ScrollView sv = CreateSurvey.getScrollViewId(ctx);
                        sv.scrollTo(xpos, ypos);
                    }});

                Log.d(TAG, "Restoring screen to ypos: " + ypos);
            }
        }

        // TODO: This needs to be centralized, and more general. Currently its copied
        //   in three different files!
        @Override
        public boolean onCreateOptionsMenu (Menu m) {
            super.onCreateOptionsMenu (m);
            m.add (Menu.NONE, 0, Menu.NONE, "Home").setIcon (android.R.drawable.ic_menu_revert);
            return true;
        }

        @Override
        public boolean onOptionsItemSelected (MenuItem index) {
            Intent i;
            switch (index.getItemId()) {
                case 0: i = new Intent (ctx, Home.class); break;
                default: return false;
            }
            ctx.startActivity (i);
            this.finish();
            return true;
        }

        protected void onActivityResult(int requestCode, int resultCode, Intent data) {
            BCustomPhoto q = (BCustomPhoto) survey.getQuestion(requestCode, Question.DType.BIMAGE);
            if (q == null) {
                Log.e(TAG, "onActivityResult received a requestCode of: " + requestCode +
                        "which does not correspond to a valid Question object");
            }

            else {
                q.imageTaken(resultCode, data);

                // Get the location
                Location location = RequestLocation.getBestLocation(ctx);
                if (location != null)
                    q.geotagImage(location);
            }
        }

        OnClickListener submit_button_listener = new OnClickListener() {

            public void onClick(View v) {
                int i;

                // Get the location
                Location location = RequestLocation.getBestLocation(ctx);
                if (location == null) Log.d(TAG, "Null location");

                DBRow row = new DBRow(ctx, location);

                for (i = 0; i < questions.size(); i++) {
                    Question q = questions.get(i);
                    if (Question.isImage(q.getType()))
                        row.imageFilenames.add(q.responseString());
                    else row.responses.add(q.responseString());
                }

                // Open the database and insert the row
                sdb.openWriteable(ctx, survey.databaseTable());
                sdb.insertEntry(ctx, row);
                sdb.close();

                // popup success toast and return to home page
                Toast.makeText(Survey.this, "Survey successfully submitted!", Toast.LENGTH_LONG).show();
                Survey.this.startActivity (new Intent(Survey.this, FurtherInstr.class));
                Survey.this.finish();
            }
        };
    }

