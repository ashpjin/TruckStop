package edu.ucla.cens.truckstop.survey;

import android.app.Activity;
import android.util.Log;
import android.widget.TimePicker;

public class BTimePicker extends Question {
	private String TAG = "BTimePicker";

	// View id for the response element
	private int responseId = -1;
	private TimePicker picker;

	public BTimePicker(int responseId, int viewLabelId, int stringLabelId) {
		super(viewLabelId, stringLabelId, DType.BRESPONSE);
		this.responseId = responseId;
	}

	public String responseString() {
		String answer = "";
		try {
            String hour = String.valueOf(picker.getCurrentHour());
            String minute = String.valueOf(picker.getCurrentMinute());
            answer = hour + ":" + minute;
		} catch (Exception e) {
			Log.e(TAG, "Unable to get response");
			e.printStackTrace();
		}

		if (answer == null | answer.equals("")) answer = NO_RESPONSE;
		return answer;
	}

	@Override
	public void layoutQuestion(Activity ctx) {
		super.layoutQuestion(ctx);

		// Get the view for this element
		picker = (TimePicker) ctx.findViewById(responseId);

		if (null == picker) {
			Log.e(TAG, "Could not find a spinner for id: " + responseId);
			return;
		}
	}
}
