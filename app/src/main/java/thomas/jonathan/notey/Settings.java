package thomas.jonathan.notey;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;
import uk.co.chrisjenx.calligraphy.CalligraphyTypefaceSpan;

public class Settings extends PreferenceActivity {
    private boolean impossible_to_delete = false;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (MainActivity.CURRENT_ANDROID_VERSION >= Build.VERSION_CODES.JELLY_BEAN) {
            addPreferencesFromResource(R.xml.settings);
        } else addPreferencesFromResource(R.xml.settings_ics);

        //show action bar
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
        getActionBar().setIcon(new ColorDrawable(getResources().getColor(android.R.color.transparent)));

        //default font
        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                        .setDefaultFontPath("assets/ROBOTO-REGULAR.ttf")
                        .setFontAttrId(R.attr.fontPath)
                        .build()
        );

        setUpFonts();

        initializeSettings();

        //enable pro feature options
        if (MainActivity.proVersionEnabled) {
            CheckBoxPreference pref_shortcut = (CheckBoxPreference) findPreference("pref_shortcut");
            pref_shortcut.setEnabled(true);
        }

        //listen for changes to preferences. Need to make sure users make it possible to delete notifications.
        SharedPreferences.OnSharedPreferenceChangeListener prefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                String temp_clickNotif = prefs.getString("clickNotif", "info");
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

        final SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();

        //notification click action dialog pop-up
        new AlertDialog.Builder(this);
        findPreference("pref_click_notif").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                String temp_clickNotif = PreferenceManager.getDefaultSharedPreferences(Settings.this).getString("clickNotif", "info");
                int i; //set the value of the selected choice when the dialog launches
                switch (temp_clickNotif) {
                    case "edit":
                        i = 1;
                        break;
                    case "remove":
                        i = 2;
                        break;
                    case "nothing":
                        i = 3;
                        break;
                    default:
                        i = 0;
                        break;
                }
                MaterialDialog md = new MaterialDialog.Builder(Settings.this)
                        .title(getResources().getString(R.string.notifClick))
                        .items(getResources().getStringArray(R.array.listArray))
                        .itemsCallbackSingleChoice(i, new MaterialDialog.ListCallbackSingleChoice() {

                            @Override
                            public boolean onSelection(MaterialDialog materialDialog, View view, int i, CharSequence charSequence) {
                                String s = charSequence.toString().toLowerCase();
                                if (s.equals("show info screen")) s = "info";
                                if (s.equals("do nothing")) s = "nothing";

                                editor.putString("clickNotif", s);
                                editor.apply();
                                return true;
                            }
                        })
                        .typeface(Typeface.createFromAsset(getAssets(), "ROBOTO-REGULAR.ttf"), Typeface.createFromAsset(getAssets(), "ROBOTO-REGULAR.ttf"))
                        .build();
                md.show();

                return false;
            }
        });

        //priority preference dialog
        new AlertDialog.Builder(this);
        findPreference("pref_priority").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                String temp_priority = PreferenceManager.getDefaultSharedPreferences(Settings.this).getString("pref_priority", "normal");
                int i; //set the value of the selected choice when the dialog launches

                switch (temp_priority) {
                    case "high":
                        i = 0;
                        break;
                    case "low":
                        i = 2;
                        break;
                    case "minimum":
                        i = 3;
                        break;
                    default:
                        i = 1;  //else normal
                        break;
                }

                MaterialDialog md = new MaterialDialog.Builder(Settings.this)
                        .title(getResources().getString(R.string.priority))
                        .items(getResources().getStringArray(R.array.priorityArray))
                        .itemsCallbackSingleChoice(i, new MaterialDialog.ListCallbackSingleChoice() {

                            @Override
                            public boolean onSelection(MaterialDialog materialDialog, View view, int i, CharSequence charSequence) {
                                String s = charSequence.toString().toLowerCase();

                                if (s.contains("high")) s = "high";
                                else if (s.contains("minimum")) s = "minimum";
                                else if (s.contains("low")) s = "low";
                                else s = "normal";

                                editor.putString("pref_priority", s);
                                editor.apply();
                                return true;
                            }
                        })
                        .typeface(Typeface.createFromAsset(getAssets(), "ROBOTO-REGULAR.ttf"), Typeface.createFromAsset(getAssets(), "ROBOTO-REGULAR.ttf"))
                        .build();
                md.show();

                return false;
            }
        });
    }

    private void setUpFonts() {
        SpannableString s = new SpannableString(getString(R.string.notey_settings));
        s.setSpan(new CalligraphyTypefaceSpan(Typeface.createFromAsset(getAssets(), "ROBOTO-LIGHT.TTF")), 0, s.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        getActionBar().setTitle(s);

        PreferenceCategory notif_cat = (PreferenceCategory) findPreference("pref_key_general_settings");
        SpannableString s2 = new SpannableString(getString(R.string.notifications));
        s2.setSpan(new CalligraphyTypefaceSpan(Typeface.createFromAsset(getAssets(), "ROBOTO-BOLD.ttf")), 0, s2.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        notif_cat.setTitle(s2);

        PreferenceCategory notif_other = (PreferenceCategory) findPreference("pref_key_other");
        SpannableString s3 = new SpannableString(getString(R.string.other));
        s3.setSpan(new CalligraphyTypefaceSpan(Typeface.createFromAsset(getAssets(), "ROBOTO-BOLD.ttf")), 0, s3.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        notif_other.setTitle(s3);

        PreferenceCategory notif_pro = (PreferenceCategory) findPreference("pref_key_pro");
        SpannableString s4 = new SpannableString(getString(R.string.pro_options));
        s4.setSpan(new CalligraphyTypefaceSpan(Typeface.createFromAsset(getAssets(), "ROBOTO-BOLD.ttf")), 0, s4.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        notif_pro.setTitle(s4);
    }

    private void initializeSettings() {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
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

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }
}
