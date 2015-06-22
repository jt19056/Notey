package thomas.jonathan.notey;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.jenzz.materialpreference.Preference;
import com.jenzz.materialpreference.SwitchPreference;
import com.rey.material.widget.CheckBox;
import com.rey.material.widget.FloatingActionButton;

public class Settings extends ActionBarActivity{
    private boolean impossible_to_delete = false;
    private SharedPreferences sharedPreferences;

    //theme dialog variables
    private String selectedFab;
    private CheckBox darkCheckBox;

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        themeStuffBeforeSetContentView();
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(android.R.id.content, new SettingsFragment())
                    .commit();
        }
    }

    private void themeStuffBeforeSetContentView(){
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        MainActivity.themeColor = sharedPreferences.getString("pref_theme_color", "md_blue_500");
        MainActivity.darkTheme = sharedPreferences.getBoolean("pref_theme_dark", false);

        //set light/dark theme
        if(MainActivity.darkTheme) {
            super.setTheme(getResources().getIdentifier("MySettingsThemeDark_" + MainActivity.themeColor, "style", getPackageName()));
        }
        else {
            super.setTheme(getResources().getIdentifier("MySettingsTheme_" + MainActivity.themeColor, "style", getPackageName()));
        }
    }

    @Override //back button in action bar to go close settings_jb_kk
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

    public class SettingsFragment extends PreferenceFragment {
        private NotificationManager nm;

        @Override public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if (MainActivity.CURRENT_ANDROID_VERSION >= Build.VERSION_CODES.LOLLIPOP)
                addPreferencesFromResource(R.xml.settings_lollipop);
            else if(MainActivity.CURRENT_ANDROID_VERSION >= Build.VERSION_CODES.JELLY_BEAN)
                addPreferencesFromResource(R.xml.settings_jb_kk);
            else addPreferencesFromResource(R.xml.settings_ics);

            //enable pro feature options
            if (MainActivity.proVersionEnabled) {
                final SwitchPreference pref_shortcut = (SwitchPreference) findPreference("pref_shortcut");
                pref_shortcut.setEnabled(true);
                // Create new note shortcut in the notification tray
                pref_shortcut.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(android.preference.Preference preference) {
                        nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                        if(pref_shortcut.isChecked()) buildShortcutNotification();
                        else nm.cancel(MainActivity.SHORTCUT_NOTIF_ID);
                        return false;
                    }
                });
            }

            //listen for changes to preferences. Need to make sure users make it possible to delete notifications.
            SharedPreferences.OnSharedPreferenceChangeListener prefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
                public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                    String temp_clickNotif = prefs.getString("clickNotif", "info");
                    boolean temp_pref_swipe = prefs.getBoolean("pref_swipe", false);
                    boolean temp_pref_expand = prefs.getBoolean("pref_expand", true);

                    //if all three settings_jb_kk are set like this, notify user and don't allow them to close out the settings_jb_kk
                    // until they make it possible to delete notifications
                    if ((!temp_clickNotif.equals("remove") && !temp_clickNotif.equals("info")) && !temp_pref_swipe && !temp_pref_expand) {
                        Toast.makeText(getApplicationContext(), getString(R.string.impossibleToDelete), Toast.LENGTH_LONG).show();
                        impossible_to_delete = true;

                    } else {
                        impossible_to_delete = false;
                    }
                }
            };
            sharedPreferences.registerOnSharedPreferenceChangeListener(prefListener);

            final SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();

            //notification click action dialog pop-up
            new AlertDialog.Builder(getApplicationContext());
            findPreference("pref_click_notif").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(android.preference.Preference preference) {
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
            new AlertDialog.Builder(getApplicationContext());
            findPreference("pref_priority").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(android.preference.Preference preference) {
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

            //theme preference dialog
            new AlertDialog.Builder(getApplicationContext());
            findPreference("pref_theme").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(final android.preference.Preference preference) {
                    selectedFab = sharedPreferences.getString("pref_theme_fab", "button_bt_float6"); //fab6 is the default blue color for notey
                    int colorID = getResources().getIdentifier(MainActivity.themeColor, "color", getPackageName());

                    int view; //layout for theme choose
                    if(MainActivity.proVersionEnabled) view = R.layout.theme_color_chooser_dialog_pro;
                    else view = R.layout.theme_color_chooser_dialog;

                    final MaterialDialog md = new MaterialDialog.Builder(Settings.this)
                            .customView(view, false)
                            .autoDismiss(false)
                            .typeface(Typeface.createFromAsset(getAssets(), "ROBOTO-REGULAR.ttf"), Typeface.createFromAsset(getAssets(), "ROBOTO-REGULAR.ttf"))
                            .positiveText(R.string.set)
                            .negativeText(R.string.cancel)
                            .callback(new MaterialDialog.ButtonCallback() {
                                @Override
                                public void onPositive(MaterialDialog dialog) {
                                    if(darkCheckBox.isChecked()){
                                        editor.putBoolean("pref_theme_dark", true).apply();
                                    } else{
                                        editor.putBoolean("pref_theme_dark", false).apply();
                                    }
                                    editor.putString("pref_theme_fab", selectedFab).apply();
                                    editor.putString("pref_theme_color", MainActivity.themeColor).apply();

                                    restartNotey(); //restart notey. re-launching the main activity along with the settings_jb_kk. that way the user returns to the settings_jb_kk screen
                                    finish();
                                }
                                @Override
                                public void onNegative(MaterialDialog dialog) {
                                    dialog.dismiss();
                                }
                            })
                            .positiveColorRes(colorID)
                            .negativeColorRes(colorID)
                            .build();
                    md.show();

                    //set icon for initial selected fab
                    FloatingActionButton fab = (FloatingActionButton) md.findViewById(getResources().getIdentifier(selectedFab, "id", getPackageName()));
                    fab.setIcon(getResources().getDrawable(R.drawable.ic_check_white_36dp), false);

                    //dark theme checkbox
                    darkCheckBox = (CheckBox) md.findViewById(R.id.dark_check_box);
                    darkCheckBox.setChecked(MainActivity.darkTheme);

                    //enable/disable the checkbox and textview for dark theme based on pro status.
                    darkCheckBox.setEnabled(MainActivity.proVersionEnabled);
                    if(!MainActivity.proVersionEnabled) {
                        darkCheckBox.setAlpha(0.15f); //fade the checkbox
                        TextView darkTextView = (TextView) md.findViewById(R.id.dark_theme_tv);
                        darkTextView.setEnabled(false);
                    }

                    int count; //number of theme choices
                    if(MainActivity.proVersionEnabled) count = 18;
                    else count = 4;

                    //create theme picker dialog
                    for (int i = 1; i <= count; i++) {
                        //fab button ids are weird
                        final int val;
                        if(i==1) val = 1;
                        else if(i==2) val = 6;
                        else if(i==3) val = 10;
                        else val = 18;

                        int id = getResources().getIdentifier("button_bt_float" + Integer.toString(val), "id", getPackageName());
                        final FloatingActionButton newFab = (FloatingActionButton) md.findViewById(id);
                        newFab.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                MainActivity.themeColor = v.getTag().toString();

                                //show checkmark
                                newFab.setIcon(getResources().getDrawable(R.drawable.ic_check_white_36dp), false);

                                //remove previous selected fab's checkmark
                                FloatingActionButton oldFab = (FloatingActionButton) md.findViewById(getResources().getIdentifier(selectedFab, "id", getPackageName()));
                                oldFab.setIcon(getResources().getDrawable(R.drawable.md_transparent), false);
                                selectedFab = "button_bt_float" + Integer.toString(val);
                            }
                        });
                    }

                    return false;
                }
            });

            //update theme
            new AlertDialog.Builder(getApplicationContext());
            findPreference("pref_update_icon").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(android.preference.Preference preference) {
                    int colorID = getResources().getIdentifier(MainActivity.themeColor, "color", getPackageName());

                    final MaterialDialog md = new MaterialDialog.Builder(Settings.this)
                            .customView(R.layout.update_icon_dialog, false)
                            .title(getString(R.string.your_new_icon))
                            .typeface(Typeface.createFromAsset(getAssets(), "ROBOTO-REGULAR.ttf"), Typeface.createFromAsset(getAssets(), "ROBOTO-REGULAR.ttf"))
                            .positiveText(R.string.set)
                            .negativeText(R.string.cancel)
                            .callback(new MaterialDialog.ButtonCallback() {
                                @Override
                                public void onPositive(MaterialDialog dialog) {
                                    setLauncherIcon();
                                    finish();
                                }
                                @Override
                                public void onNegative(MaterialDialog dialog) {
                                    dialog.dismiss();
                                }
                            })
                            .positiveColorRes(colorID)
                            .negativeColorRes(colorID)
                            .build();
                    md.show();

                    ImageView iconImage = (ImageView) md.findViewById(R.id.update_icon_iv);
                    if(MainActivity.darkTheme) iconImage.setImageResource(getResources().getIdentifier("ic_launcher_" + MainActivity.themeColor + "_dark", "drawable", getPackageName()));
                    else iconImage.setImageResource(getResources().getIdentifier("ic_launcher_" + MainActivity.themeColor, "drawable", getPackageName()));

                    return false;
                }
            });
        }

        private void buildShortcutNotification() {
            Notification n;

            if (MainActivity.CURRENT_ANDROID_VERSION >= 21) { //if > lollipop
                n = new NotificationCompat.Builder(getApplicationContext())
                        .setContentTitle(getString(R.string.app_name))
                        .setContentText(getString(R.string.quick_note))
                        .setSmallIcon(R.drawable.ic_new_note_white)
                        .setColor(getResources().getColor(R.color.grey_500))
                        .setOngoing(true)
                        .setContentIntent(PendingIntent.getActivity(getApplicationContext(), MainActivity.SHORTCUT_NOTIF_ID, new Intent(getApplicationContext(), MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT))
                        .setAutoCancel(false)
                        .setPriority(Notification.PRIORITY_MIN)
                        .build();
            } else {
                n = new NotificationCompat.Builder(getApplicationContext())
                        .setContentTitle(getString(R.string.app_name))
                        .setContentText(getString(R.string.quick_note))
                        .setSmallIcon(R.drawable.ic_launcher_dashclock)
                        .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_new_note))
                        .setOngoing(true)
                        .setContentIntent(PendingIntent.getActivity(getApplicationContext(), MainActivity.SHORTCUT_NOTIF_ID, new Intent(getApplicationContext(), MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT))
                        .setAutoCancel(false)
                        .setPriority(Notification.PRIORITY_MIN)
                        .build();
            }
            nm.notify(MainActivity.SHORTCUT_NOTIF_ID, n);
        }

        private void restartNotey(){
            Intent i = getBaseContext().getPackageManager().getLaunchIntentForPackage(getBaseContext().getPackageName() );
            Intent i2 = new Intent(getApplicationContext(), Settings.class);
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            i2.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            startActivity(i2);
        }

        private void setLauncherIcon() {
            //loop through all 38 activity alias and disable them
            String[] activityArray = getResources().getStringArray(R.array.mainactivity_icon_alias_array_names);
            for(String s : activityArray) {
                getPackageManager().setComponentEnabledSetting(
                        new ComponentName("thomas.jonathan.notey", s),
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
            }

            //get which activity to enable
            String enabledActivity;
            if(MainActivity.darkTheme) enabledActivity = "thomas.jonathan.notey.MainActivity-" + MainActivity.themeColor + "_dark";
            else enabledActivity = "thomas.jonathan.notey.MainActivity-" + MainActivity.themeColor;

            //enable it
            getPackageManager().setComponentEnabledSetting(
                    new ComponentName("thomas.jonathan.notey", enabledActivity),
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);

            //restart the app
            restartNotey();
        }
    }
}
