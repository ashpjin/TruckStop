package edu.ucla.cens.truckstop.ui;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.MenuItem;
import android.content.Intent;
import android.content.Context;

import edu.ucla.cens.truckstop.R;

public class Instructions extends Activity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate (b);
        setContentView (R.layout.instructions);
    }

    @Override
    public boolean onCreateOptionsMenu (Menu m) {
        super.onCreateOptionsMenu (m);

        m.add (Menu.NONE, 0, Menu.NONE, "Home").setIcon (android.R.drawable.ic_menu_revert);
        m.add (Menu.NONE, 1, Menu.NONE, "Survey").setIcon (android.R.drawable.ic_menu_agenda);
        m.add (Menu.NONE, 2, Menu.NONE, "About").setIcon (android.R.drawable.ic_menu_info_details);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected (MenuItem index) {
        Context ctx = Instructions.this;
        Intent i;
        switch (index.getItemId()) {
            case 0:
                i = new Intent (ctx, Home.class);
                break;
            case 1:
                i = new Intent (ctx, Survey.class);
                break;
            case 2:
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
