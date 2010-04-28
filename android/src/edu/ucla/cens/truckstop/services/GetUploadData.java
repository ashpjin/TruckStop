package edu.ucla.cens.truckstop.services;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import edu.ucla.cens.truckstop.R;
import edu.ucla.cens.truckstop.content.CreateQuestions;
import edu.ucla.cens.truckstop.survey.BCustomPhoto;
import edu.ucla.cens.truckstop.survey.CreateRoute;
import edu.ucla.cens.truckstop.survey.CreateSurvey;
import edu.ucla.cens.truckstop.survey.Question;
import edu.ucla.cens.truckstop.survey.Question.DType;
import edu.ucla.cens.truckstop.utils.*;

public class GetUploadData extends Service {
    private static final String TAG = "GetUploadData";

    // HACK For now I manually create these lists. Eventually should have the services
    //  register with this service to indicate which tables will be accessed.
    List<String> tableNames = new ArrayList<String>();
    List<String> tableURL = new ArrayList<String>();

    Service ctx = this; //EDIT?

    // TODO: Eventually use AndWellness3.2/UploadReceiver instead of this wakelock
    // We need this wake lock so that the device stays awake when the thread is launched.
    PowerManager.WakeLock wl;

    private String IMEI, appName;

    @Override
    public void onCreate() {
        Log.d(TAG, "Starting upload service");

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        wl.acquire();

        // HACK: Instead of manually doing this, I should be having Create* send Intents to register
        //  these tables with this service.
        tableNames.add(CreateQuestions.getTable(this));
        tableURL.add(CreateQuestions.getURL(this));

        tableNames.add(CreateRoute.getTable(this));
        tableURL.add(CreateRoute.getURL(this));

        // Get header information
        TelephonyManager t =
            (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        IMEI = t.getDeviceId();
        Log.d(TAG, "IMEI is: " + IMEI);

        appName = getString(R.string.app_name);

        new PostThread().start();
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onDestroy() {
        wl.release();
    }

    public class PostThread extends Thread{

        public void run() {

            for (int i = 0; i < tableNames.size(); i++) {
                String table = tableNames.get(i);
                String uploadURL = tableURL.get(i);

                Log.d(TAG, "Table name: " + table + " / uploadURL: " + uploadURL);

                // Get the rows from the database
                List<DBRow> rows = SurveyDB.fetchData(table);
                if (rows == null) continue;

                // Iterate through each row, and upload the data
                for (DBRow row : rows) {

                    MultipartEntity entity = encodeRow(row);

                    try {
                        if(Upload.doPost(uploadURL, entity))
                            deleteRow(table, row);
                    }

                    catch (IOException e) {
                        Log.d(TAG, "threw an IOException for sending file.");
                        e.printStackTrace();
                    }
                }
            }

            // Done with our work...  stop the service!
            GetUploadData.this.stopSelf();
        }

        private void deleteRow(String table, DBRow row) {
            if (SurveyDB.delete(table, row.rowId)) {
                Log.d(TAG, "Uploaded and deleted row: " + row.rowId);

                // If this is an image, then delete the file on disk as well
                for (int i = 0; i < row.imageFilenames.size(); i++) {
                    BCustomPhoto.deleteImage(row.imageFilenames.get(i));
                }
            }

            else
                Log.e(TAG, "Could not open the database to delete row: " + row.rowId);
        }

        private MultipartEntity encodeRow(DBRow row) {
            MultipartEntity entity = new MultipartEntity();

            // The upload key is based on the order of data
            //  stored, and whether the data is an image or a response
            try {

                // Add the responses
                for (int i = 0; i < row.responses.size(); i++) {
                    entity.addPart(Question.createKey(DType.BRESPONSE, i),
                            new StringBody(row.responses.get(i)));
                }

                // Add the images
                for (int i = 0; i < row.imageFilenames.size(); i++) {
                    String key = Question.createKey(DType.BIMAGE, i);
                    String photoFilename = row.imageFilenames.get(i);

                    if (photoFilename == null || photoFilename.equals("")) {
                        entity.addPart(key, new StringBody(""));
                    } else {
                        File file = new File(photoFilename);
                        entity.addPart(key, new FileBody(file));
                    }
                }

                // Add the header information.
                entity.addPart(CreateSurvey.getLatitudeKey(), new StringBody(row.latitude));
                entity.addPart(CreateSurvey.getLongitudeKey(), new StringBody(row.longitude));
                entity.addPart(CreateSurvey.getVersionKey(), new StringBody(row.version));
                entity.addPart(CreateSurvey.getAuthKey(), new StringBody(row.oauthToken));
                entity.addPart(CreateSurvey.getTimeKey(), new StringBody(row.time));
                entity.addPart(CreateSurvey.getIMEIKey(), new StringBody(IMEI));
                entity.addPart(CreateSurvey.getAppNameKey(), new StringBody(appName));
            }

            catch (UnsupportedEncodingException e) {
                Log.e(TAG, "Unable to encode the image data");
                e.printStackTrace();
            }
            return entity;
        }
    }
}
