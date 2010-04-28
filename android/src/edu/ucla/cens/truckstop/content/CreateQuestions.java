package edu.ucla.cens.truckstop.content;

/* @author Lorax Analytics, nithya ramanathan
 *
 * This class essentially authors the survey. It creates the questions by tying
 *   together the resources in the layout files, and the data structures in the
 *   code.
 *
 * To author a survey, a user needs to edit:
 * 1) CreateQuestions.java (this class) - to tie all the resources together. There
 *   are a couple of hacks here. First, the user needs to specify the button type
 *   for the resonses in the layout file (e.g. RadioButton, CheckBox), and then correctly
 *   specify that button type in the arguments to Question() below.
 * 2) survey.xml - to create the correct layout, including the types of
 *   buttons required, and the question and responses text.
 */

import android.content.Context;
import android.view.View;
import edu.ucla.cens.truckstop.R;
import edu.ucla.cens.truckstop.survey.*;

public class CreateQuestions extends CreateSurvey {

	private static final String TABLE = "_responses";
	private static final int uploadURLTagID = R.string.surveyuploadurltag;

	public static String getTable(Context ctx) {
		return getDatabaseTable(ctx, TABLE);
	}

	private static String getURLTag(Context ctx) {
		return ctx.getString(uploadURLTagID);
	}

	public static String getURL(Context ctx) {
		return getUploadURL(ctx, getURLTag(ctx));
	}

    public CreateQuestions(Context ctx) {
		super (ctx);
    	this.databaseTableTag = TABLE;
    	this.uploadURLTag = getURLTag(ctx);

		// Question
		addQuestion(new BEditText(R.id.r1_edit, R.id.r1_label,
                R.string.r1_label, View.VISIBLE));
	    addQuestion(new BEditText(R.id.r2_edit, R.id.r2_label,
	            R.string.r2_label, View.VISIBLE));

		// Question
		addQuestion(new BSpinner(R.id.r0, R.id.r0_label, R.string.r0_label,
				R.array.r0, -1));
		addQuestion(new BSpinner(R.id.r3, R.id.r3_label, R.string.r3_label, R.array.r3,
		        R.id.r3_edit));
		addQuestion(new BCustomPhoto(R.id.i0_picture, R.id.i0_thumbnail, R.id.i0_label,
				R.string.i0_label));

		// Question
		addQuestion(new BSpinner(R.id.r4, R.id.r4_label, R.string.r4_label,
				R.array.r4, R.id.r4_edit));
	}
}
