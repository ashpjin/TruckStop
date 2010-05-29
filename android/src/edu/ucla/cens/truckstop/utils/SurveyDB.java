package edu.ucla.cens.truckstop.utils;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import edu.ucla.cens.truckstop.survey.CreateSurvey;
import edu.ucla.cens.truckstop.survey.Question;
import edu.ucla.cens.truckstop.survey.Question.DType;
import edu.ucla.cens.truckstop.ui.Authenticate;
import edu.ucla.cens.truckstop.R;

public class SurveyDB {

	public static final String KEY_ROWID = "_id";
	public static final int DATABASE_VERSION = 6;

	// Stores the database keys used to create and access this database
	private List<String> DBKeys;

	// Sets the information for the survey associated with this SurveyDB instance.
	private String databaseTable = "";

	private static boolean databaseOpen = false;
	private static Object dbLock = new Object();
	public static final String TAG = "SurveyDB";
	private DatabaseHelper dbHelper;
	private SQLiteDatabase db;

	// This is set once the database has been created. It should be the same across
	//	all opens, so we make this static, so that it can be accessed statically
	//	later by GetUploadData, to open the database.
	private static String DBPath = "";

	// @params
	//	table: Name of the table to be accessed. Cannot be ""
	//	uploadURL: URL to upload to.
	//	allKeys: cannot be null.
	public SurveyDB(String table, String uploadURL, List<String> allKeys)
	{
		Log.d(TAG, "Table: " + table);

		// Return if DBKeys are null, because this will prevent the database from being
		//	created if needed.
		if (allKeys == null) {
				Log.e(TAG, "Cannot supply null DBKeys to SurveyDB constructor, for table: "
						+ table);
				return;
		}

		this.databaseTable = table;
		this.DBKeys = allKeys;
	}

	// This function cannot return null. Otherwise calling functions cannot
	//	close the database.
	public SurveyDB openWriteable(Context ctx, String table) throws SQLException
    {
		synchronized(dbLock)
		{
			while (databaseOpen)
			{
				try
				{
					dbLock.wait();
				}
				catch (InterruptedException e){}
			}

			databaseOpen = true;
			dbHelper = new DatabaseHelper(ctx, table, DBKeys);
			db = dbHelper.getWritableDatabase();

			// db.getPath() returns the full path + databasename. We just want the
			//	path, so we need to strip the last component off. Should only need to
			// 	set this once. Seems like a HACK.
			if (DBPath.equals("")) {
				int end = db.getPath().lastIndexOf("/");
				DBPath = db.getPath().substring(0, end);
				Log.d(TAG, "Set DBPath to: " + DBPath);
			}

			return this;
		}
	}

	public void close()
	{
		synchronized(dbLock)
		{
			dbHelper.close();
			databaseOpen = false;
			dbLock.notify();
		}
	}

	// Params:
	// survey - the object CreateQuestions that holds all of the information for this survey
	// responses - struct that holds all of the user's responses for this entry. Accessed with
	//    		   the same index used to access survey.questions.
	// longitude, latitude, time, version, photo_filename - self explanatory
	public long insertEntry(Context ctx, DBRow row)
	{
		if (row == null) {
			Log.e(TAG, "SOFTWARE BUG: Row is null.");
			return -1;
		}

		// Store the data into the proper datastructure for writing to the database

		ContentValues vals = new ContentValues();
		for (int i = 0; i < row.responses.size(); i++) {
			vals.put(Question.createKey(DType.BRESPONSE, i), row.responses.get(i));
		}

		for (int i = 0; i < row.imageFilenames.size(); i++) {
			vals.put(Question.createKey(DType.BIMAGE, i), row.imageFilenames.get(i));
		}

		// Store the header and authentication information

		vals.put(CreateSurvey.getLongitudeKey(), row.longitude);
		vals.put(CreateSurvey.getLatitudeKey(), row.latitude);
		vals.put(CreateSurvey.getTimeKey(), row.time);
		vals.put(CreateSurvey.getVersionKey(), row.version);

		String access_token = "";
		try {
			access_token = PreferencesMgr.getString(ctx, Authenticate.ACCESS_TOKEN_STUB);

			if (access_token.equals("")) {
				Log.e(TAG, "access_token is an empty string! WIll not be able to upload data");
			}
		}

		catch (Exception e) {
			Log.e(TAG, "Unable to get access_token from the sharedPreferences");
			e.printStackTrace();
		}
		vals.put(CreateSurvey.getAuthKey(), access_token);

		// Insert the data into the database.

		long rowId = -1;
		try {
			rowId = db.insert(databaseTable, null, vals);
			Log.d(TAG, "Inserted data, rowid: " + rowId);
		}

		catch (Exception e) {
			Log.e(TAG, "Unable to insert " + vals + " into Database!");
			e.printStackTrace();
		}

		return rowId;
	}



	/* Function for accessing the readable database. I dont think this requires
	 * synchronization, so I can make it static.
	 */
	public static String dbName(String table) {
		return table + "db";
	}

	private static String dbPath(String table) {
		Log.d(TAG, "Test DBPath: " + DBPath);
		if (DBPath.equals("")) {
			Log.w(TAG, "The full path for the database has not been set yet, likely " +
					"because the database has not yet been opened. This should resolve" +
					"once the database has been opened. ");
			return "";
		}
		Log.d(TAG, "dbName(table) for table: " + table + " returns: " + dbName(table));
		return DBPath + "/" + dbName(table);
	}

	private static SQLiteDatabase getDB(String table, int mode) {
	   	SQLiteDatabase checkDB = null;
    	String databaseName = SurveyDB.dbPath(table);

    	Log.d(TAG, "For table: " + table + "databasename: " + databaseName);
    	if (databaseName.equals("")) {
    		Log.w(TAG, "No databasename returned, it is likely not open yet");
    		return checkDB;
    	}

    	// Open the database

    	try {
    		checkDB = SQLiteDatabase.openDatabase(databaseName, null, mode);
    	}

    	catch(SQLiteException e) {
    		Log.e(TAG, "Database: " + databaseName + " does not exist");
    	}

    	return checkDB;
	}

	public static boolean delete(String table, long rowId) {
		Log.d(TAG, "Deleting rowId: " + rowId + "from table: " + table);
		SQLiteDatabase db = getDB(table, SQLiteDatabase.OPEN_READWRITE);
		if (db == null) return false;

    	// Delete the rows
    	int count = 0;
    	count = db.delete(table, KEY_ROWID+"="+rowId, null);
    	db.close();

		return count > 0 ? true : false;
	}

    private static SQLiteDatabase openReadable(String table) {
    	Log.d(TAG, "Opening readable version of table: " + table);
    	return getDB(table, SQLiteDatabase.OPEN_READONLY);
    }

	public static List<DBRow> fetchData(String table) {
		Log.d(TAG, "Fetching data for table: " + table);
		SQLiteDatabase db = SurveyDB.openReadable(table);
		List<DBRow> rows = new ArrayList<DBRow>();

		if (db == null) {
			Log.w(TAG, "Cannot open database: " + table);
			return null;
		}

		Log.d(TAG, "Opened the database");

		// Fetch the rows

		Cursor c = null;
        try {
            c = db.query(table, null, null, null, null, null, null);
        }

        catch (SQLiteException e) {
        	Log.e(TAG, "Unable to retrieve cursor from database");
        }

		if (c == null) return null;

        Log.d(TAG, "Got rows");

        // Store the rows in the datastructure

		c.moveToFirst();
		Log.d(TAG, "DB returned rows: " + c.getCount() + "with "
    		+ c.getColumnCount() + "columns");

		// Iterate through each row in the database, and save to the DBRow structure

		for (int i =0; i < c.getCount(); i++) {
			DBRow r = new DBRow();

			r.version = c.getString(c.getColumnIndex(CreateSurvey.getVersionKey()));
			r.latitude = c.getString(c.getColumnIndex(CreateSurvey.getLatitudeKey()));
			r.longitude = c.getString(c.getColumnIndex(CreateSurvey.getLongitudeKey()));
			r.time = c.getString(c.getColumnIndex(CreateSurvey.getTimeKey()));
			r.oauthToken = c.getString(c.getColumnIndex(CreateSurvey.getAuthKey()));
	        r.rowId = c.getLong(c.getColumnIndex(SurveyDB.KEY_ROWID));

			for (int j = 0; j < c.getColumnCount(); j++) {
				String columnName = c.getColumnName(j);
				if (columnName.startsWith(Question.KEY_IMAGE)) {
					r.imageFilenames.add(c.getString(j));
				}

				else if (columnName.startsWith(Question.KEY_RESPONSE)) {
					r.responses.add(c.getString(j));
				}
			}
			rows.add(r);
			c.moveToNext();
		}
		c.close();
		db.close();

		Log.d(TAG, "Returning rows of size: " + rows.size());
        return rows;
	}
}
