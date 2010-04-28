package edu.ucla.cens.truckstop.survey;

/*
 * @author Lorax Analytics, nithya ramanathan
 * This class adds support for editable text boxes.
 */
import android.util.Log;
import android.view.View;
import android.app.Activity;
import android.widget.EditText;

public class BEditText extends Question {
	private String TAG = "BEditText";
	private EditText text = null;

	// View id for the response element
	private int responseId = -1;
	private int visibility;

	/* @params:
	 * visibility = View.VISIBLE | View.INVISIBLE
	 */
	public BEditText(int responseId, int viewLabelId, int stringLabelId,
			int visibility) {
		super(viewLabelId, stringLabelId, DType.BRESPONSE);
		this.responseId = responseId;
		this.visibility = visibility;
	}

	public String responseString() {
		String answer = "";

		try {
			answer = text.getText().toString();
		}
		catch (Exception e) {
			Log.e(TAG, "Unable to return a response from BEditText.");
			return "";
		}

		if (answer == null | answer.equals("")) answer = NO_RESPONSE;
		return answer;
	}

	public void setVisible() {
		if (text != null) text.setVisibility(View.VISIBLE);
		else Log.w(TAG, "Unable to make Edit Text visible: It has not " +
				"been initialized");
	}

	@Override
	public void layoutQuestion(Activity ctx) {
		super.layoutQuestion(ctx);

		text = (EditText) ctx.findViewById(responseId);
		if (text == null) Log.e(TAG, "Could not get a BEdit text pointer!");
		else if (visibility == View.VISIBLE) this.setVisible();
	}


}
