package edu.ucla.cens.truckstop.utils;

import java.io.IOException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import android.util.Log;

public class Upload {

	// TODO: Make this be able to upload multiple rows at a time!

	// Parameters:
	// url - the url to upload the data
	// survey - contains the information about this survey. This field is used to
	//          retrieve the keys associated with the responses. The order of this
	//          field is the same as the order in responses.
	// responses - contains the responses for the questions. Order is the same as
	//  			in survey.
	public static boolean doPost(String url, MultipartEntity entity) throws IOException
    {
    	String TAG = "Upload";

    	HttpClient httpClient = new DefaultHttpClient();
    	HttpPost request = new HttpPost(url.toString());
        if (null == request) return false;

    	request.setEntity(entity);
    	HttpResponse response = httpClient.execute(request);
    	int status = response.getStatusLine().getStatusCode();
    	if(status == HttpStatus.SC_OK) {
    		Log.d(TAG, "Received HTTP OK");
    		return true;
    	}
    	else {
	    	Log.e(TAG, "Data not uploaded!");
    		return false;
    	}
    }
}
