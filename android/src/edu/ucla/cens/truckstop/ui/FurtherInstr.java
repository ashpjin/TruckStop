package edu.ucla.cens.truckstop.ui;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.content.Intent;
import android.content.Context;

import edu.ucla.cens.truckstop.R;

public class FurtherInstr extends Activity {

    //private String idlingTruck = "Please return after 6 minutes to complete a returning survey. Thank you!";
    //private String parkedTruck = "Please return after 72 hours to complete a returning survey. Thank you!";

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate (b);
        setContentView (R.layout.further);

        Button button = (Button) findViewById(R.id.home_button);
        button.setOnClickListener(home_button);
    }

    View.OnClickListener home_button = new View.OnClickListener () {
        public void onClick(View v) {
            FurtherInstr.this.startActivity (new Intent (FurtherInstr.this, Home.class));
            FurtherInstr.this.finish ();
        }
    };

    @Override
    public boolean onCreateOptionsMenu (Menu m) {
        super.onCreateOptionsMenu (m);

        m.add (Menu.NONE, 0, Menu.NONE, "Survey").setIcon (android.R.drawable.ic_menu_agenda);
        m.add (Menu.NONE, 1, Menu.NONE, "About").setIcon (android.R.drawable.ic_menu_info_details);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected (MenuItem index) {
        Context ctx = FurtherInstr.this;
        Intent i;
        switch (index.getItemId()) {
            case 0:
                i = new Intent (ctx, Survey.class);
                break;
            case 1:
                i = new Intent (ctx, About.class);
                break;
            default:
                return false;
        }
        ctx.startActivity (i);
        this.finish();
        return true;
    }
}
