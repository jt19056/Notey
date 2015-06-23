package thomas.jonathan.notey;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.android.apps.dashclock.api.DashClockExtension;
import com.google.android.apps.dashclock.api.ExtensionData;

import java.util.List;

public class DashClock extends DashClockExtension {
    private MySQLiteHelper db = new MySQLiteHelper(this);

    protected void onUpdateData(int reason) {
        List<NoteyNote> allNoteys = db.getAllNoteys();
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        String dashclock_icon;
        int icon = R.drawable.ic_check_white_36dp; //default icon is white check

        // get icon from preferences
        dashclock_icon = sharedPref.getString("pref_dashclock_icon", "topIcon");

        // set icon
        //this is the new way of finding icons
        if(!dashclock_icon.equals("topIcon") && !dashclock_icon.equals("noteyLogo"))
            icon = getResources().getIdentifier("ic_" + dashclock_icon + "_white_36dp", "drawable", getPackageName());
        else if(dashclock_icon.equals("noteyLogo"))
            icon = R.drawable.ic_launcher_dashclock;

        //legacy checks becuase I changed the way icons are read in v2.4
        if (dashclock_icon.equals("heart")) icon = R.drawable.ic_favorite_white_36dp;
        else if (dashclock_icon.equals("note")) icon = R.drawable.ic_note_add_white_36dp;
        else if (dashclock_icon.equals("smile")) icon = R.drawable.ic_mood_white_36dp;

        if (allNoteys.size() > 0) {
            String allNoteysString = "";
            for (NoteyNote n : allNoteys) {
                allNoteysString += n.getNote() + "\n";

                if (dashclock_icon.equals("topIcon")) icon = n.getIcon();
            }

            publishUpdate(new ExtensionData()
                    .visible(true)
                    .icon(icon)
                    .status(allNoteys.size() + "")
                    .expandedTitle(allNoteys.size() + " " + getResources().getString(R.string.reminders))
                    .expandedBody(allNoteysString)
                    .clickIntent(new Intent(this, MainActivity.class)));
        } else publishUpdate(null);

    }


}
