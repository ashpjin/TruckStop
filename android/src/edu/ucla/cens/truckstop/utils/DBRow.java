package edu.ucla.cens.truckstop.utils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import edu.ucla.cens.truckstop.R;

import android.content.Context;
import android.location.Location;

/* Author: Nithya Ramanathan, Lorax Analytics
 * Class used to represent survey data stored in the database. Used to insert data
 * and receive data to/from the database.
 */
public class DBRow {
	public List<String> responses;
	public List<String> imageFilenames;
	public String longitude;
	public String latitude;
	public String time;
	public String version;
	public String oauthToken;
	public long rowId;

	public DBRow() {
   		longitude = "";
   		latitude = "";
   		time = "";
   		version = "";
   		rowId = -1;
   		oauthToken = "";

        responses=new ArrayList<String>();
        imageFilenames=new ArrayList<String>();
	}

	public DBRow(Context ctx, Location loc) {
   		longitude = "";
   		latitude = "";
   		oauthToken = "";

		if (loc != null) {
			longitude = String.valueOf(loc.getLongitude());
			latitude = String.valueOf(loc.getLatitude());
		}

        Date d = new Date();
		time =  Long.toString(d.getTime());
		version = ctx.getString(R.string.version);
    	rowId = -1;

        responses=new ArrayList<String>();
        imageFilenames=new ArrayList<String>();
	}
}
