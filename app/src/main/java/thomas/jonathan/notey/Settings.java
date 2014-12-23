package thomas.jonathan.notey;

import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.widget.Toast;

public class Settings extends PreferenceActivity {

    final int CURRENT_ANDROID_VERSION = Build.VERSION.SDK_INT;
    private SharedPreferences.OnSharedPreferenceChangeListener prefListener;
    private String clickNotif;
    private boolean pref_swipe, pref_expand, impossible_to_delete = false;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (CURRENT_ANDROID_VERSION >= Build.VERSION_CODES.JELLY_BEAN) {
            addPreferencesFromResource(R.xml.settings);
        } else addPreferencesFromResource(R.xml.settings_ics);

        //show action bar
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
        getActionBar().setIcon(new ColorDrawable(getResources().getColor(android.R.color.transparent)));

        initializeSettings();

        //listen for changes to preferences. Need to make sure users make it possible to delete notifications.
        prefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                String temp_clickNotif = prefs.getString("clickNotif", "edit");
                boolean temp_pref_swipe = prefs.getBoolean("pref_swipe", false);
                boolean temp_pref_expand = prefs.getBoolean("pref_expand", true);

                //if all three settings are set like this, notify user and don't allow them to close out the settings
                // until they make it possible to delete notifications
                if ((!temp_clickNotif.equals("remove") && !temp_clickNotif.equals("info")) && !temp_pref_swipe && !temp_pref_expand) {
                    Toast.makeText(getApplicationContext(), getString(R.string.impossibleToDelete), Toast.LENGTH_LONG).show();
                    getActionBar().setHomeButtonEnabled(false);
                    impossible_to_delete = true;

                } else {
                    getActionBar().setHomeButtonEnabled(true);
                    impossible_to_delete = false;
                }
            }
        };
        sharedPreferences.registerOnSharedPreferenceChangeListener(prefListener);
    }

    private void initializeSettings() {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        if (CURRENT_ANDROID_VERSION >= 16) { //Jelly Bean or above can have expandable notifications
            pref_expand = sharedPreferences.getBoolean("pref_expand", true);
            pref_swipe = sharedPreferences.getBoolean("pref_swipe", false);
        } else {
            pref_expand = false;
            pref_swipe = sharedPreferences.getBoolean("pref_swipe", true);
        }
        clickNotif = sharedPreferences.getString("clickNotif", "edit");
    }

    @Override //back button in action bar to go close settings
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            this.finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() { //hardware or software back button on the phone
        if (!impossible_to_delete) super.onBackPressed(); //allow back button
    }
}
