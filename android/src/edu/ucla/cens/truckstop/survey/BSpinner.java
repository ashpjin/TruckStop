package edu.ucla.cens.truckstop.survey;

/* Author: Nithya Ramanathan, Lorax Analytics
 * This class creates a SPinner object. Additionally, the calling class can pass a
 * 	ViewId for an edit text box. If the calling class does this, the spinner will
 * 	automatically add a final option called "Other" to the responses, and if "other"
 * 	is selected, the editable text box will pop up.
 */

import edu.ucla.cens.truckstop.R;
import android.app.Activity;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.AdapterView.OnItemSelectedListener;
import android.util.Log;

public class BSpinner extends Question {
	private final String TAG = "BSpinner";

	private String[] responses;

	// View id for the response element
	private int viewResponseId = -1;

	// Resource id for the array that contains the responses for this element
	private int arrayResponseId = -1;

	// View for the editable text box that should appear if the user wants to
	//	expand on their response.
	private int editTextId = -1;
	private BEditText editText = null;

	private Spinner spinner;

	// @params
	//	* editTextId: If set to a View Id for a text box, then this class will
	//	  	insert a DefaultResponse (usually its "other") to the list of
	//		possible resopnses, and if the user selects this response, the
	//		text box will pop up.
	public BSpinner(int viewResponseId, int viewLabelId, int stringLabelId,
			int arrayResponseId, int editTextId) {

		super(viewLabelId, stringLabelId, DType.BRESPONSE);

		// Save off state internal to this class
		this.viewResponseId = viewResponseId;
		this.arrayResponseId = arrayResponseId;
		this.editTextId = editTextId;

		// Initialize an EditText box if its specified
		if (this.editTextId > -1)
			editText = new BEditText(this.editTextId, -1, -1, View.INVISIBLE);
	}

	@Override
	public String responseString() {
		String answer = "";

		// Get the response selected by the user
		try {
			answer = (String) spinner.getSelectedItem();
		}

		catch (Exception e) {
			Log.e(TAG, "Unable to get response from spinner");
			e.printStackTrace();
		}

		// Edit the response and layout, based on our rules:
		//	1) If the user selects "Other" then the textbox will show up
		//	2) If the user leaves the default response on the spinner, then we send NO RESPONSE
		if (answer == null | answer.equals("")
				| answer.equals(ctx.getString(R.string.defaultResponse)))
			answer = NO_RESPONSE;

		// Grab text from the edit box if the user clicked other
		else if (answer.equals(ctx.getString(R.string.otherResponse))) {
			if (editText != null)
				answer = answer + " | " + editText.responseString();
		}

		return answer;
	}

	@Override
	public void layoutQuestion(Activity ctx) {
		super.layoutQuestion(ctx);

		// Get the view for this element
		spinner = (Spinner) ctx.findViewById(viewResponseId);

		if (null == spinner) {
			Log.e(TAG, "Could not find a spinner for id: " + viewResponseId);
			return;
		}

		// Need to call layout for all subcomponents of this class. Only create the "Other"
		//	response option if we will have an edit text box accompanying it.
		String[] r = ctx.getResources().getStringArray(arrayResponseId);
		if (editText != null) {
			editText.layoutQuestion(ctx);
			int size = r.length + 2;
			this.responses = new String[size];
			this.responses[size - 1] = ctx.getString(R.string.otherResponse);
		}

		// Otherwise, just insert the default response, so we need to allocate a slightly
		//	smaller array.
		else {
			int size = r.length + 1;
			this.responses = new String[size];
		}

		// Insert the default response in the 0th slot, and set up the response array
		this.responses[0] = ctx.getString(R.string.defaultResponse);
        System.arraycopy(r, 0, this.responses, 1, r.length);

		// Assign the responses in the array element to the drop down for the spinner
		ArrayAdapter adapter = new ArrayAdapter(ctx, android.R.layout.simple_spinner_item, this.responses);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(clickListener);
	}

	// If button is clicked, and response is "Other", then show the text box.
    private Spinner.OnItemSelectedListener clickListener =

    	new Spinner.OnItemSelectedListener() {

    		public void onItemSelected(AdapterView parent, View v, int position, long id) {
    			String response = (String) spinner.getSelectedItem();
    			if (response != null && ctx != null && editText != null) {
    				if (response.equals(ctx.getString(R.string.otherResponse)))
    					editText.setVisible();
    			}

    			// If the view is null, then we don't need to clear other element focus because
    			//	the survey is likely just getting started
    			if (v == null) {
    				Log.d(TAG, "View is null");
    				return;
    			}

        		// Clear other elements that are retaining focus, so that we can properly scroll
    			//	back to the original position on the screen after the user is done.
            	clearOtherElementFocus(spinner.getId());
        		// restoreScreen();
    		}

            public void onNothingSelected(AdapterView parent) {
                Log.d(TAG, "Nothing selected.");

                // Clear other elements that are retaining focus, so that we can properly scroll
    			//	back to the original position on the screen after the user is done.
            	clearOtherElementFocus(spinner.getId());
        		// restoreScreen();
            }

        };
}
