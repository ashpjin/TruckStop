package edu.ucla.cens.truckstop.utils;

import java.util.List;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper
{
	private String table = "";
	private List<String> DBKeys;
	private String TAG = "DatabaseHelper";
	private String path = "";

	DatabaseHelper(Context ctx, String table, List<String> DBKeys)
	{
		super(ctx, SurveyDB.dbName(table), null, SurveyDB.DATABASE_VERSION);
		this.table = table;
		this.DBKeys = DBKeys;
	}

	// Needed by the SurveyDB class later in order to open this database
	public String getPath() {
		return path;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		int i;

		path = db.getPath();

		if (DBKeys == null) {
			Log.e(TAG, "Cannot create database, because we dont have keys!");
			return;
		}

		// Build the string to create the table, with the column names corresponding
		//  to the responses we are storing.
		String DATABASE_CREATE = "create table " + this.table
			+ " (" + SurveyDB.KEY_ROWID + " integer primary key autoincrement ";

		for (i = 0; i < DBKeys.size(); i++) {
			DATABASE_CREATE += ", " + DBKeys.get(i) + " text not null";
		}

	    DATABASE_CREATE += ");";
	    Log.d(TAG, "Created database create string: " + DATABASE_CREATE);
		db.execSQL(DATABASE_CREATE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS " + this.table);
		onCreate(db);
	}
}

