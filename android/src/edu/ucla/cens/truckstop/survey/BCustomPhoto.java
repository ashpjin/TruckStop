package edu.ucla.cens.truckstop.survey;

import java.io.File;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
//import android.media.ExifInterface;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.RelativeLayout;
import android.widget.ImageView;
import android.app.Activity;

import edu.ucla.cens.truckstop.ui.Photo;

public class BCustomPhoto extends Question {

    private static String TAG = "BCustomPhoto";
    private RelativeLayout take_picture;
    private ImageView image_thumbnail;

    // View Ids for the elements in this view
    private int photobuttonId = -1;
    private int thumbnailId = -1;

    // This is the name of the file that is created. It is set once the image is taken,
    //  and the calling class calls q.imageTaken() below. The bitmap is also stored once
    //	the image is taken. This way, we don't have to keep recreating it every time the
    //	screen is opened or closed.
    private String filename = "";
	private Bitmap bm = null;

    /* Order of Android resource Ids is specified by RESPONSE*IDX above */
	public BCustomPhoto(int photobuttonId, int thumbnailId, int viewLabelId, int stringLabelId) {
		super(viewLabelId, stringLabelId, DType.BIMAGE);
		this.photobuttonId = photobuttonId;
		this.thumbnailId = thumbnailId;
	}

	/* Method to assign views to the objects created in this class. Necessary to
	 * obtain responses from each button correctly.
	 */
	@Override
	public void layoutQuestion(Activity ctx) {
		super.layoutQuestion(ctx);

        // add picture button
        take_picture = (RelativeLayout) ctx.findViewById(photobuttonId);

        // add take picture button listener
        take_picture.setOnClickListener(take_picture_listener);

        // add image thumbnail view
        image_thumbnail = (ImageView) ctx.findViewById(thumbnailId);
	}

	// Returns the response stored by this Question object.
	public String responseString() {
		return filename;
	}

	// Specifies what the calling activity should save, that can be reinstated
	//  when setRestoreState() is called.
	@Override
	public String getRestoreState() {
		Log.d(TAG, "getRestrestate returns: " + filename);
		return filename;
	}

	@Override
	public void setRestoreState(String StoredFile) {
		Log.d(TAG, "setRestorestate in BCustomPhoto, with filename: " + StoredFile);
		this.filename = StoredFile;
		setImageTakenState();
	}

	// Only works for Android 2.0!
	public void geotagImage(Location loc) {
	/*	if (loc == null) return;

		Log.d(TAG, "Try to geotag image");
//DEL
		try {
			ExifInterface exif = new ExifInterface (filename);
			exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE,
					"118");
					// String.valueOf(loc.getLatitude()));
			exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE,
					String.valueOf(loc.getLongitude()));
			Log.d(TAG, "Setting exif header location for image: "
					+ filename + " / loc: " + loc.toString());
			Log.d(TAG, "1TEST longitude set to: "
					+ exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE));
			exif.saveAttributes();
			Log.d(TAG, "2TEST longitude set to: "
					+ exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE));
		}

		catch (Exception e) {
			Log.e(TAG, "Unable to get ExifInterface to geotag image." +
					"Probably incorrect version of Android (Need at least Android 2.0)");
		}*/
	}

	public static void deleteImage(String imageFilename) {

		// Can't delete an empty or non-existent filename
        if ((imageFilename == null || imageFilename.equals(""))) return;

		File file = new File(imageFilename);
        if(file != null) {
            Log.d(TAG, "Deleting file: " + file.getName());
            file.delete();
        }
	}

	// f: name of the image file that should be used for the thumbnail
	private void setImageTakenState() {
		BitmapFactory.Options o = new BitmapFactory.Options();

		// Reduce the size of the image by 16 (4^2) times to save memory. If this is
		//	set to 1, it uses the original image size, and its taking up too much
		//	memoryk, eventually crashing dalvik
		o.inSampleSize = 4;
		bm = BitmapFactory.decodeFile(filename, o);

		if (bm != null) {
			image_thumbnail.setVisibility(View.VISIBLE);
			image_thumbnail.setImageBitmap(bm);
			Log.d(TAG, "bitmap height: " + bm.getHeight() +
						" bitmap width : " + bm.getWidth());
        }
	}

	// Called by the onActivity listener, once the image has been taken.
	public void imageTaken(int resultCode, Intent data) {
		if (Activity.RESULT_CANCELED != resultCode) {

			// Delete the old file if it exists
			deleteImage(filename);

			// Grab the new image filename, and decode the image
			filename = data.getAction().toString();
			// Then set the new image
			if ((null != filename) && (!filename.toString().equals("")))
				setImageTakenState();
        }

		// Restore the screen to the original position before taking the image
		restoreScreen();
	}

	// Listener class for take_picture button
    OnClickListener take_picture_listener = new OnClickListener() {
    	public void onClick(View v) {
    		if (ctx == null) {
        		Log.e(TAG, "Major problem, ctx is not set here, so function is not doing anything");
        		return;
        	}

			clearOtherElementFocus(v.getId());

			/*
    		// Save the location of where we are on the screen, so we
    		//	can jump back here.
    		ScrollView sv = CreateSurvey.getScrollViewId(ctx);
    	    xpos = sv.getScrollX();
    	    ypos = sv.getScrollY();
    	    */
    		// Send the intent, with the id for the image_thumbnail, so that the suryve
    		// activity can set the thumbnail to be visible
            Intent photo_intent = new Intent(ctx, Photo.class);

            // Send the uniqueId for this object as the id in the intent, so that when
            //  the Activity gets the call, it can retrieve the question object.
            ctx.startActivityForResult(photo_intent, getId());
        }
    };
}
