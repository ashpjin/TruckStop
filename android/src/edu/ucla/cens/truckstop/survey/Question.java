package edu.ucla.cens.truckstop.survey;

import java.util.ArrayList;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * @author Lorax Analytics, Nithya Ramanathan
 * This class holds all of the information we need to create and author each question.
 * Additionally this class is used throughout the code to hold state for each question.
 *
 * NOTE: The entire structure of RCP depends on the naming of the database columns.
 * 	Data of type images, are inserted into columns labeled "i_*", and data of type
 * 	text is inserted into columns labeled "r_*". This is needed so that the upload
 * 	service, knows what type of data is located in each.
 *
 * TODO: Another way to do the above is just to see if an image exists for the specified
 * 	response. if it does, then we can assume its an image. I should be able to get
 * 	rid of the whole DType, and isImage() calls.
 */

public class Question {
	private String TAG = "Question";

	// android resource ID to access the label in the view.
	protected int viewLabelId = -1;

	// android resource ID to access the text label in the strings
	protected int stringLabelId;

	// Response structs, used by the children of this class to access the response views.
	protected ArrayList<View> responseViews = new ArrayList<View>();

	private boolean setUpResponseCalled = false;

    // The id for this object. The DType + id is unique for each question. The id
	//	needs to be an integer so that it is easy to pass back and forth through intents.
    private int id = -1;

    private String dbKey = "";
	protected Activity ctx;

	// Used to save the position on the screen, and go back to that position once the
	//	user has made their selection
	int xpos = -1; // Last x-position on screen
	int ypos = -1; // Last y-position on screen

    // NOTE: Header keys should not start with these pre-fixes!
    public static final String KEY_IMAGE = "i_";
    public static final String KEY_RESPONSE = "r_";

    public static final String NO_RESPONSE = "NO RESPONSE";

    // For now, the code just cares if an object is an image or not, when storing data
    //	to the database. Later, we may want to distinguish between other types, which
    //	is why I added a Type field.
    private DType type = DType.BUNKNOWN;
    public enum DType {
    	BUNKNOWN, BRESPONSE, BIMAGE
    }

	public Question(int viewLabelId, int stringLabelId, DType type) {
		this.viewLabelId = viewLabelId;
		this.stringLabelId = stringLabelId;
		this.type = type;
	}

	// Return the string format of the label. Putting it into a method call ensures
	//	that if the locale changes, we will always return the correct translation.
	public String label(Activity ctx) {
		return ctx.getString(this.stringLabelId);
	}

	// Used to create the database key and upload key based on this question objects
	//	unique Id and the type.
	public static String createKey(DType t, int id) {
		if (isImage(t)) {
			return Question.KEY_IMAGE + String.valueOf(id);
		}
		else return Question.KEY_RESPONSE + String.valueOf(id);
	}

	public static boolean isImage(DType t) {
		return (t == DType.BIMAGE);
	}

	/*
	 * Returns the response stored for this question, as a stirng. Create an if clause
	 * for every type of question we support. The only question types that need to be
	 * handled here are types that dont have their own class (e.g. seekBar)
	 */
	public String responseString() {
		if (!setUpResponseCalled) {
			Log.e(TAG, "SOFTWARE BUG: Please call setUpResponseViews() before trying to " +
					"get the repsonse string.");
			return (String) "";
		}
		return "USER SHOULD NOT SEE THIS";
	}

	/* The children class use this method to set up their specific structs and
	 * listeners. This has to be called before the caller can get responses,
	 * so we ensure that it is called using a setUpResponseCalled boolean variable.
	 */
	public void layoutQuestion(Activity ctx) {
		this.setUpResponseCalled = true;
		this.ctx = ctx;

		if (viewLabelId > -1) {
			TextView labelView = (TextView) ctx.findViewById(viewLabelId);
			labelView.setText(label(ctx));
			labelView.setPadding(0, 15, 0, 15);
			labelView.setTextSize(20);
			Log.d(TAG, "Found viewLabelId: " + viewLabelId);
		}


	}

	// Restore the screen to the previously stored location
	protected void restoreScreen() {
    	ScrollView sv = CreateSurvey.getScrollViewId(ctx);
    	sv.scrollTo(xpos, ypos);
    	Log.d(TAG, "Screen restored to ypos: " + ypos);
	}

	// Called when the child view is clicked. Method
	//	determines if another view has focus. For example, if another text box element
	//	still has a cursor in it, it will retain focus. This becomes a problem when we are
	//	trying to scroll back to the original view, after this button has been clicked. The
	//	scrollView will instead automatically scroll back to the View that retained focus
	//	before the button click. If we clear the focus, then we can force to scroll back
	//	to the original position, which is the desired functionality.
	protected void clearOtherElementFocus(int elementId) {
		ScrollView sv = CreateSurvey.getScrollViewId(ctx);

		if (sv == null) Log.e(TAG, "Why is scroll view null?");

		try {
			View viewF = sv.findFocus();

			// If the element that currently has focus is not this element, then
			//	clear the focus.
			if (elementId != viewF.getId()) {
		    	Log.d(TAG, "About to take picture; View with focus currently is: " + viewF.getId());
		    	viewF.clearFocus();
			}
		}

		catch (Exception e) {
			Log.d(TAG, "Unable to access current view, perhaps its null");
		}

		// Save the location of where we are on the screen, so we
		//	can jump back here.
	    xpos = sv.getScrollX();
	    ypos = sv.getScrollY();
	    Log.d(TAG, "ypos set to : " + ypos);
	    Log.d(TAG, "Button has been clicked. Will try to clear focus of other elements");
	}

	// Called by the calling activity when state is restored.
	public void setRestoreState(String filename) {	}

	// Specifies what the calling activity should save, that can be reinstated
	//  when setRestoreState() is called. Only required to be implemented by those
	//  Question objects that require custom saving of state.
	public String getRestoreState() {
		return "";
	}

	public void setId(int id) {
		if (this.id != -1) Log.w(TAG, "Id has already been set to: " + this.id);
		this.id = id;

		// Need to set the database key as well:
		this.dbKey = createKey(type, this.id);
	}

	public int getId() {
		if (id == -1) Log.w(TAG, "WARN: id has not been set");
		return id;
	}

	public String getDBKey() {
		return dbKey;
	}

	public DType getType() {
		return type;
	}
}
