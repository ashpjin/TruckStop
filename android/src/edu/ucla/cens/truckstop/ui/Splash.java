package edu.ucla.cens.truckstop.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import edu.ucla.cens.truckstop.R;

public class Splash extends Activity {

	/* Fields */
	private final int SPLASH_DISPLAY_LENGHT = 1;
    private String TAG = "SPLASH";

	/* Called when the activity is first created. */

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.splash);

        Log.d(TAG, "started splash");

		/* only start authenticateIntent the first time onCreate is called */
		if(savedInstanceState == null || !savedInstanceState.getBoolean("started"))
			new Handler().postDelayed(new Runnable(){
				public void run() {
					// Create an Intent that will start the Authenticate-Activity. //
					startActivity(new Intent(Splash.this, Authenticate.class));
                    Log.d(TAG, "started authentication intent");
					Splash.this.finish();
				}
			}, SPLASH_DISPLAY_LENGHT);
	}

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		savedInstanceState.putBoolean("started", true);
		super.onSaveInstanceState(savedInstanceState);
	}
}
