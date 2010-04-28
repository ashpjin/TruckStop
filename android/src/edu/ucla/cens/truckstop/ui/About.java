package edu.ucla.cens.truckstop.ui;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.MenuItem;
import android.content.Intent;
import android.content.Context;
import edu.ucla.cens.truckstop.R;

public class About extends Activity {
	@Override
    protected void onCreate(Bundle b) {
        super.onCreate (b);
        setContentView (R.layout.about);
    }

    @Override
    public boolean onCreateOptionsMenu (Menu m) {
        super.onCreateOptionsMenu (m);

        m.add (Menu.NONE, 0, Menu.NONE, "Home").setIcon (android.R.drawable.ic_menu_revert);
        m.add (Menu.NONE, 1, Menu.NONE, "Survey").setIcon (android.R.drawable.ic_menu_agenda);
        m.add (Menu.NONE, 2, Menu.NONE, "Instructions").setIcon (android.R.drawable.ic_menu_help);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected (MenuItem index) {
        Context ctx = About.this;
        Intent i;
        switch (index.getItemId()) {
            case 0:
                i = new Intent (ctx, Home.class);
                break;
            case 1:
                i = new Intent (ctx, Survey.class);
                break;
            case 2:
                i = new Intent (ctx, Instructions.class);
                break;
            default:
                return false;
        }
        ctx.startActivity (i);
        this.finish();
        return true;
    }
}
