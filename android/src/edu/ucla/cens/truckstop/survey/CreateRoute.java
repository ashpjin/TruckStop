package edu.ucla.cens.truckstop.survey;

/* @author Lorax Analytics, nithya ramanathan
 *
 * This class creates the route schema.
 */

import android.content.Context;

public class CreateRoute extends CreateSurvey {

	// HACK : This is a hack until I can do this the right way.
	private static final String TABLE = "_route";
	private static final String URL = "protected_upload_pathtrace";

	public static String getTable(Context ctx) {
		return getDatabaseTable(ctx, TABLE);
	}

	public static String getURL(Context ctx) {
		return getUploadURL(ctx, URL);
	}

    public CreateRoute(Context ctx) {
		super (ctx);
    	this.databaseTableTag = TABLE;
		this.uploadURLTag = URL;
	}
}
