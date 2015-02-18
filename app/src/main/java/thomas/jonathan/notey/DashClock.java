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
        dashclock_icon = sharedPref.getString("pref_dashclock_icon", "check");

        // set icon
        if (dashclock_icon.equals("check")) icon = R.drawable.ic_check_white_36dp;
        else if (dashclock_icon.equals("warning")) icon = R.drawable.ic_warning_white_36dp;
        else if (dashclock_icon.equals("edit")) icon = R.drawable.ic_edit_white_36dp;
        else if (dashclock_icon.equals("star")) icon = R.drawable.ic_star_white_36dp;
        else if (dashclock_icon.equals("whatshot")) icon = R.drawable.ic_whatshot_white_36dp;

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
