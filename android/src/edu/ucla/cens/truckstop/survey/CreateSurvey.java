package edu.ucla.cens.truckstop.survey;

/* @author Lorax Analytics, nithya ramanathan
 *
 * This class creates the survey. It creates the questions by tying
 *   together the resources in the layout files, and the data structures in the
 *   code.
 */

import java.util.*;
import edu.ucla.cens.truckstop.survey.Question;
import android.app.Activity;
import android.content.Context;
import android.widget.ScrollView;

import edu.ucla.cens.truckstop.R;

// This is a parent class, that children inherit from to create their own surveys.
public class CreateSurvey {

	private ArrayList<Question> questions;
	protected String databasename;

    // A counter used to assign a unique id to each Question object. Each object
    // needs a unique id, so that later it can be accessed individuall if needed.
	// Images get a separate counter from responses, so that its easier to access
	//	them later, in GetUploadData service.
    private int iCtr = 0;
    private int rCtr = 0;

    // This header map is used to store the keys for the header information. This
    //	is information that is stored in the local phone SQLite database, and then
    //	for the keys used to upload data to the URL. Don't add to this map unless
    //	the data will be stored in both places.
    protected static Map<HeaderKey, String> header = new HashMap<HeaderKey, String>();

    // This has to be unique across all instantiations of this class.
    protected String databaseTableTag = "";

    // The final path where this data is uploaded. This is tagged on to the base URL.
    protected String uploadURLTag = "";

    protected Context ctx;

    public enum HeaderKey {
    	BLATITUDE, BLONGITUDE, BTIME, BVERSION, BACCESS_TOKEN
    }

    private final static String IMEI_KEY = "IMEI";
    private final static String APPNAME_KEY = "appname";

    public CreateSurvey(Context ctx) {
    	this.ctx = ctx;
    	this.databasename = ctx.getString(R.string.databasename);
    	this.questions = new ArrayList<Question>();

		// Initialize the header information. These are the keys used to store this
    	//	data in the local database, and the keys used to upload the data to the
    	//	server.

		header.put(HeaderKey.BLATITUDE, "latitude");
		header.put(HeaderKey.BLONGITUDE, "longitude");
		header.put(HeaderKey.BTIME, "time");
		header.put(HeaderKey.BVERSION, "version");
		header.put(HeaderKey.BACCESS_TOKEN, "oauth_token");
	}

    protected void addQuestion(Question q) {

    	// Set the unique ID for the question, based on its type
    	if (Question.isImage(q.getType())) q.setId(iCtr++);
    	else q.setId(rCtr++);

    	// Add the question to our list of questions.
		questions.add(q);
    }

	// Returns the Question object with the provided id, matching the type t
	public Question getQuestion(int id, Question.DType t) {
		for (int i = 0; i < questions.size(); i++) {
			Question q = questions.get(i);
			if (q.getId() == id & q.getType() == t)
				return q;
		}
		return null;
	}

	public ArrayList<Question> getQuestions() {
		return questions;
	}

	/* Return all database keys, used in the local database,
	 * and in the upload string */
	public ArrayList<String> getDBKeys() {
		ArrayList <String> dbKeys = new ArrayList<String>();

		// First add all of the keys for the responses/images
		for (int i = 0; i < questions.size(); i++) {
			dbKeys.add(questions.get(i).getDBKey());
		}

		// Then add the keys for the header fields
	    Iterator<Map.Entry<HeaderKey, String>> it=header.entrySet().iterator();

        while(it.hasNext())
        {
            // key=value separator this by Map.Entry to get key and value
            Map.Entry<HeaderKey, String> m =(Map.Entry<HeaderKey, String>)it.next();
            dbKeys.add(m.getValue());
        }
		return dbKeys;
	}

	/* Return the database key for one of the header objects,
	 * associated with a specific data type, as
	 * accessed by the index variables specified above */
	public static String getHeaderDBKey(HeaderKey index) {
		return header.get(index);
	}

	public String databaseName() {
		return databasename;
	}

	public String databaseTable() {
		return databasename + databaseTableTag;
	}

	public String uploadURL() {
		return ctx.getString(R.string.baseurl) + uploadURLTag;
	}

	public static String getUploadURL(Context ctx, String URLTag) {
		return ctx.getString(R.string.baseurl) + URLTag;
	}

	public static String getDatabaseTable(Context ctx, String tableTag) {
		return ctx.getString(R.string.databasename) + tableTag;
	}

	public static String getAuthKey() {
		return getHeaderDBKey(HeaderKey.BACCESS_TOKEN);
	}

	public static String getVersionKey() {
		return getHeaderDBKey(HeaderKey.BVERSION);
	}

	public static String getTimeKey() {
		return getHeaderDBKey(HeaderKey.BTIME);
	}

	public static String getLongitudeKey() {
		return getHeaderDBKey(HeaderKey.BLONGITUDE);
	}

	public static String getLatitudeKey() {
		return getHeaderDBKey(HeaderKey.BLATITUDE);
	}

	public static String getIMEIKey() {
		return IMEI_KEY;
	}

	public static String getAppNameKey() {
		return APPNAME_KEY;
	}

	public static ScrollView getScrollViewId(Activity ctx) {
		return (ScrollView) ctx.findViewById(R.id.scroll_view);
	}
}
