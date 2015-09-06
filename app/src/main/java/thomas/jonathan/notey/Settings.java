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
import com.crashlytics.android.Crashlytics;
import com.jenzz.materialpreference.Preference;
import com.jenzz.materialpreference.SwitchPreference;
import com.rey.material.widget.CheckBox;
import com.rey.material.widget.FloatingActionButton;

import cyanogenmod.app.CMStatusBarManager;
import cyanogenmod.app.CustomTile;

public class Settings extends ActionBarActivity {
    private static boolean impossible_to_delete = false;
    private static SharedPreferences sharedPreferences;
    private static SharedPreferences.Editor editor;

    //theme dialog variables
    private static String selectedFab;
    private static String selectedAccentFab;
    private static String selectedDefaultColorFab;
    private static String selectedDefaultLEDColorFab;
    private static String tempTheme;
    private static CheckBox darkCheckBox;

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        themeStuffBeforeSetContentView();
        super.onCreate(savedInstanceState);

        editor = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(android.R.id.content, new SettingsFragment())
                    .commit();
        }
    }

    private void themeStuffBeforeSetContentView() {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        MainActivity.themeColor = sharedPreferences.getString("pref_theme_color", "md_blue_500");
        MainActivity.darkTheme = sharedPreferences.getBoolean("pref_theme_dark", false);
        MainActivity.accentColor = sharedPreferences.getString("pref_accent_color", "md_blue_500");

        //set light/dark theme
        if (MainActivity.darkTheme) {
            super.setTheme(getResources().getIdentifier("MySettingsThemeDark_" + MainActivity.themeColor + "_Accent_" + MainActivity.accentColor, "style", getPackageName()));
        } else {
            super.setTheme(getResources().getIdentifier("MySettingsTheme_" + MainActivity.themeColor + "_Accent_" + MainActivity.accentColor, "style", getPackageName()));
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

    public static class SettingsFragment extends PreferenceFragment {
        private NotificationManager nm;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if (MainActivity.CURRENT_ANDROID_VERSION >= Build.VERSION_CODES.LOLLIPOP && cyanogenmod.os.Build.CM_VERSION.SDK_INT > 0)
                addPreferencesFromResource(R.xml.settings_lollipop_cm);  //show Cyanogenmod quick tile if user is running CM
            else if (MainActivity.CURRENT_ANDROID_VERSION >= Build.VERSION_CODES.LOLLIPOP)
                addPreferencesFromResource(R.xml.settings_lollipop);
            else if (MainActivity.CURRENT_ANDROID_VERSION >= Build.VERSION_CODES.JELLY_BEAN)
                addPreferencesFromResource(R.xml.settings_jb_kk);
            else addPreferencesFromResource(R.xml.settings_ics);

            final SwitchPreference pref_shortcut = (SwitchPreference) findPreference("pref_shortcut");
            //enable pro feature options
//            if (MainActivity.proVersionEnabled) {
            final android.preference.Preference pref_accent = findPreference("pref_accent");
            final android.preference.Preference pref_default_icon_color = findPreference("pref_default_icon_color");
            final android.preference.Preference pref_default_led_color = findPreference("pref_default_led_color");

            // Create new note shortcut in the notification tray
            pref_shortcut.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(android.preference.Preference preference) {
                    nm = (NotificationManager) getActivity().getSystemService(NOTIFICATION_SERVICE);
                    if (pref_shortcut.isChecked()) buildShortcutNotification();
                    else nm.cancel(MainActivity.SHORTCUT_NOTIF_ID);
                    return false;
                }
            });

            //CM quick tile build
            if (cyanogenmod.os.Build.CM_VERSION.SDK_INT > 0) {
                final com.jenzz.materialpreference.SwitchPreference pref_cm_tile = (com.jenzz.materialpreference.SwitchPreference) findPreference("pref_cm_quick_tile");
                pref_cm_tile.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(android.preference.Preference preference) {
                        try {
                            if (sharedPreferences.getBoolean("pref_cm_quick_tile", false)) {
                                CustomTile customTile = new CustomTile.Builder(getActivity())
                                        .setOnClickIntent(PendingIntent.getActivity(getActivity(), MainActivity.SHORTCUT_NOTIF_ID, new Intent(getActivity(), MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT))
                                        .setLabel("Notey")
                                        .setIcon(R.drawable.ic_new_note_white)
                                        .build();
                                CMStatusBarManager.getInstance(getActivity()).publishTile("notey_cm_quick_tile", 1, customTile); //tag, id, tile
                            } else { //remove quick tile
                                CMStatusBarManager.getInstance(getActivity()).removeTile("notey_cm_quick_tile", 1); //tag, id
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            Crashlytics.logException(e);
                        }
                        return false;
                    }
                });
            }

            //accent color picker dialog
            findPreference("pref_accent").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(final android.preference.Preference preference) {
                    selectedFab = sharedPreferences.getString("pref_theme_fab", "button_bt_float6"); //fab6 is the default blue color for notey
                    selectedAccentFab = sharedPreferences.getString("pref_accent_fab", selectedFab);
                    int accentId = getResources().getIdentifier(MainActivity.accentColor, "color", getActivity().getPackageName());

                    final MaterialDialog md = new MaterialDialog.Builder(getActivity())
                            .customView(R.layout.theme_color_chooser_dialog_pro, false)
                            .autoDismiss(false)
                            .typeface(Typeface.createFromAsset(getActivity().getAssets(), "ROBOTO-REGULAR.ttf"), Typeface.createFromAsset(getActivity().getAssets(), "ROBOTO-REGULAR.ttf"))
                            .positiveText(R.string.set)
                            .negativeText(R.string.cancel)
                            .callback(new MaterialDialog.ButtonCallback() {
                                @Override
                                public void onPositive(MaterialDialog dialog) {
                                    editor.putString("pref_accent_fab", selectedAccentFab).apply();
                                    editor.putString("pref_accent_color", MainActivity.accentColor).apply();

                                    restartNotey(); //restart notey. re-launching the main activity along with the settings_jb_kk. that way the user returns to the settings_jb_kk screen
                                    getActivity().finish();
                                }

                                @Override
                                public void onNegative(MaterialDialog dialog) {
                                    dialog.dismiss();
                                }
                            })
                            .positiveColorRes(accentId)
                            .negativeColorRes(accentId)
                            .build();

                    // hide the dark theme check box, we dont need it here
                    TextView darkTextView = (TextView) md.findViewById(R.id.dark_theme_tv);
                    darkCheckBox = (CheckBox) md.findViewById(R.id.dark_check_box);
                    darkTextView.setVisibility(View.GONE);
                    darkCheckBox.setVisibility(View.GONE);

                    md.show();

                    //set icon for initial selected fab
                    FloatingActionButton fab = (FloatingActionButton) md.findViewById(getResources().getIdentifier(selectedAccentFab, "id", getActivity().getPackageName()));
                    fab.setIcon(getResources().getDrawable(R.drawable.ic_check_white_36dp), false);


                    //create theme picker dialog
                    for (int i = 1; i <= 19; i++) {
                        //fab button ids are weird
                        int id = getResources().getIdentifier("button_bt_float" + Integer.toString(i), "id", getActivity().getPackageName());
                        final FloatingActionButton newFab = (FloatingActionButton) md.findViewById(id);
                        final int finalI = i;
                        newFab.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                MainActivity.accentColor = v.getTag().toString();

                                //show checkmark
                                newFab.setIcon(getResources().getDrawable(R.drawable.ic_check_white_36dp), false);

                                //remove previous selected fab's checkmark
                                FloatingActionButton oldFab = (FloatingActionButton) md.findViewById(getResources().getIdentifier(selectedAccentFab, "id", getActivity().getPackageName()));
                                oldFab.setIcon(getResources().getDrawable(R.drawable.md_transparent), false);
                                selectedAccentFab = "button_bt_float" + Integer.toString(finalI);
                            }
                        });
                    }

                    return false;
                }
            });

            //default icon color chooser dialog
            pref_default_icon_color.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(android.preference.Preference preference) {
                    selectedDefaultColorFab = sharedPreferences.getString("pref_default_color_fab", "icon_button_bt_float0"); //default is white

                    final MaterialDialog md = new MaterialDialog.Builder(getActivity())
                            .customView(R.layout.icon_color_chooser_dialog_pro, false)
                            .build();

                    md.show();

                    //set icon for initial selected fab
                    FloatingActionButton fab = (FloatingActionButton) md.findViewById(getResources().getIdentifier(selectedDefaultColorFab, "id", getActivity().getPackageName()));
                    fab.setIcon(getResources().getDrawable(R.drawable.ic_check_white_36dp), false);

                    String colors[] = getResources().getStringArray(R.array.icon_colors_array_pro);
                    for (int i = 0; i < colors.length; i++) {
                        int id = getResources().getIdentifier("icon_button_bt_float" + Integer.toString(i), "id", getActivity().getPackageName());
                        final FloatingActionButton newFab = (FloatingActionButton) md.findViewById(id);
                        final int finalI = i;

                        //color white fab a little grey, so we can see the checkmark. fab icon's are only white, so we can't change the checkmark color
                        if (i == 0)
                            newFab.setBackgroundColor(getResources().getColor(R.color.md_grey_200));

                        newFab.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                MainActivity.defaultIconsColor = v.getTag().toString();

                                //show checkmark
                                newFab.setIcon(getResources().getDrawable(R.drawable.ic_check_white_36dp), false);

                                //remove previous selected fab's checkmark
                                FloatingActionButton oldFab = (FloatingActionButton) md.findViewById(getResources().getIdentifier(selectedDefaultColorFab, "id", getActivity().getPackageName()));
                                oldFab.setIcon(getResources().getDrawable(R.drawable.md_transparent), false);
                                selectedDefaultColorFab = "icon_button_bt_float" + Integer.toString(finalI);

                                editor.putString("pref_default_color_fab", selectedDefaultColorFab).apply();
                                editor.putString("pref_default_icon_color", MainActivity.defaultIconsColor).apply();

                                md.dismiss();
                            }
                        });
                    }
                    return false;
                }
            });

            //default led color
            pref_default_led_color.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(android.preference.Preference preference) {
                    selectedDefaultLEDColorFab = sharedPreferences.getString("pref_default_led_color_fab", "icon_button_bt_float5"); //default is blue

                    final MaterialDialog md = new MaterialDialog.Builder(getActivity())
                        .customView(R.layout.led_chooser_dialog, false)
                        .title(R.string.default_led_color)
                        .build();

                    md.show();

                    //set icon for initial selected fab
                    FloatingActionButton fab = (FloatingActionButton) md.findViewById(getResources().getIdentifier(selectedDefaultLEDColorFab, "id", getActivity().getPackageName()));
                    fab.setIcon(getResources().getDrawable(R.drawable.ic_check_white_36dp), false);

                    String colors[] = getResources().getStringArray(R.array.icon_colors_array_pro);
                    for (int i = 0; i < colors.length; i++) {
                        int id = getResources().getIdentifier("icon_button_bt_float" + Integer.toString(i), "id", getActivity().getPackageName());
                        final FloatingActionButton newFab = (FloatingActionButton) md.findViewById(id);
                        final int finalI = i;

                        //color white fab a little grey, so we can see the checkmark. fab icon's are only white, so we can't change the checkmark color
                        if (i == 0)
                            newFab.setBackgroundColor(getResources().getColor(R.color.md_grey_200));


                        newFab.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                //show checkmark
                                newFab.setIcon(getResources().getDrawable(R.drawable.ic_check_white_36dp), false);

                                //remove previous selected fab's checkmark
                                FloatingActionButton oldFab = (FloatingActionButton) md.findViewById(getResources().getIdentifier(selectedDefaultLEDColorFab, "id", getActivity().getPackageName()));
                                oldFab.setIcon(getResources().getDrawable(R.drawable.md_transparent), false);
                                selectedDefaultLEDColorFab = "icon_button_bt_float" + Integer.toString(finalI);
                                editor.putString("pref_default_led_color_fab", selectedDefaultLEDColorFab).apply();

                                editor.putString("pref_default_led_color", v.getTag().toString()).apply();
                                md.dismiss();
                            }
                        });
                    }
                    return false;
                }
            });

//            }

            //listen for changes to preferences. Need to make sure users make it possible to delete notifications.
            SharedPreferences.OnSharedPreferenceChangeListener prefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
                public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                    String temp_clickNotif = prefs.getString("clickNotif", "info");
                    boolean temp_pref_swipe = prefs.getBoolean("pref_swipe", false);
                    boolean temp_pref_expand = prefs.getBoolean("pref_expand", true);

                    //if all three settings_jb_kk are set like this, notify user and don't allow them to close out the settings_jb_kk
                    // until they make it possible to delete notifications
                    if ((!temp_clickNotif.equals("remove") && !temp_clickNotif.equals("info")) && !temp_pref_swipe && !temp_pref_expand) {
                        Toast.makeText(getActivity(), getString(R.string.impossibleToDelete), Toast.LENGTH_LONG).show();
                        impossible_to_delete = true;

                    } else {
                        impossible_to_delete = false;
                    }
                }
            };
            sharedPreferences.registerOnSharedPreferenceChangeListener(prefListener);

            //notification click action dialog pop-up
            new AlertDialog.Builder(getActivity());
            findPreference("pref_click_notif").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(android.preference.Preference preference) {
                    String temp_clickNotif = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString("clickNotif", "info");
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
                    MaterialDialog md = new MaterialDialog.Builder(getActivity())
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
                            .typeface(Typeface.createFromAsset(getActivity().getAssets(), "ROBOTO-REGULAR.ttf"), Typeface.createFromAsset(getActivity().getAssets(), "ROBOTO-REGULAR.ttf"))
                            .build();
                    md.show();

                    return false;
                }
            });

            //priority preference dialog
            new AlertDialog.Builder(getActivity());
            findPreference("pref_priority").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(android.preference.Preference preference) {
                    String temp_priority = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString("pref_priority", "normal");
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

                    MaterialDialog md = new MaterialDialog.Builder(getActivity())
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
                            .typeface(Typeface.createFromAsset(getActivity().getAssets(), "ROBOTO-REGULAR.ttf"), Typeface.createFromAsset(getActivity().getAssets(), "ROBOTO-REGULAR.ttf"))
                            .build();
                    md.show();

                    return false;
                }
            });

            //theme preference dialog
            new AlertDialog.Builder(getActivity());
            findPreference("pref_theme").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(final android.preference.Preference preference) {
                    selectedFab = sharedPreferences.getString("pref_theme_fab", "button_bt_float6"); //fab6 is the default blue color for notey
                    int accentId = getResources().getIdentifier(MainActivity.themeColor.replace("500", "300"), "color", getActivity().getPackageName());

                    int view; //layout for theme choose
//                    if(MainActivity.proVersionEnabled)
                    view = R.layout.theme_color_chooser_dialog_pro;
//                    else view = R.layout.theme_color_chooser_dialog;

                    final MaterialDialog md = new MaterialDialog.Builder(getActivity())
                            .customView(view, false)
                            .autoDismiss(false)
                            .typeface(Typeface.createFromAsset(getActivity().getAssets(), "ROBOTO-REGULAR.ttf"), Typeface.createFromAsset(getActivity().getAssets(), "ROBOTO-REGULAR.ttf"))
                            .positiveText(R.string.set)
                            .negativeText(R.string.cancel)
                            .callback(new MaterialDialog.ButtonCallback() {
                                @Override
                                public void onPositive(MaterialDialog dialog) {
                                    MainActivity.themeColor = tempTheme;
                                    MainActivity.accentColor = tempTheme;
                                    if (darkCheckBox.isChecked()) {
                                        editor.putBoolean("pref_theme_dark", true).apply();
                                    } else {
                                        editor.putBoolean("pref_theme_dark", false).apply();
                                    }
                                    editor.putString("pref_theme_fab", selectedFab).apply();
                                    editor.putString("pref_theme_color", MainActivity.themeColor).apply();
                                    //set the accent color to the same thing as the theme color
                                    editor.putString("pref_accent_fab", selectedFab).apply();
                                    editor.putString("pref_accent_color", MainActivity.themeColor).apply();

                                    restartNotey(); //restart notey. re-launching the main activity along with the settings. that way the user returns to the settings screen
                                    getActivity().finish();
                                }

                                @Override
                                public void onNegative(MaterialDialog dialog) {
                                    dialog.dismiss();
                                }
                            })
                            .positiveColorRes(accentId)
                            .negativeColorRes(accentId)
                            .build();

                    //allow use to click on the text next to the checkbox to (de)select the checkbox
                    TextView darkTextView = (TextView) md.findViewById(R.id.dark_theme_tv);
                    darkTextView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (darkCheckBox.isChecked()) {
                                darkCheckBox.setChecked(false);
                            } else {
                                darkCheckBox.setChecked(true);
                            }
                        }
                    });

                    md.show();

                    //set icon for initial selected fab
                    FloatingActionButton fab = (FloatingActionButton) md.findViewById(getResources().getIdentifier(selectedFab, "id", getActivity().getPackageName()));
                    fab.setIcon(getResources().getDrawable(R.drawable.ic_check_white_36dp), false);

                    //dark theme checkbox
                    darkCheckBox = (CheckBox) md.findViewById(R.id.dark_check_box);
                    darkCheckBox.setChecked(MainActivity.darkTheme);

                    //enable/disable the checkbox and textview for dark theme based on pro status.
//                    darkCheckBox.setEnabled(MainActivity.proVersionEnabled);
//                    if(!MainActivity.proVersionEnabled) {
//                        darkCheckBox.setAlpha(0.15f); //fade the checkbox
//                        darkTextView.setEnabled(false);
//                    }

                    int count; //number of theme choices
//                    if(MainActivity.proVersionEnabled)
                    count = 19;
//                    else count = 4;

                    //create theme picker dialog
                    for (int i = 1; i <= count; i++) {
                        //fab button ids are weird
                        final int val;
//                        if(count == 4) {
//                            if (i == 1) val = 1;
//                            else if (i == 2) val = 6;
//                            else if (i == 3) val = 10;
//                            else val = 18;
//                        }
//                        else
                        val = i;

                        int id = getResources().getIdentifier("button_bt_float" + Integer.toString(val), "id", getActivity().getPackageName());
                        final FloatingActionButton newFab = (FloatingActionButton) md.findViewById(id);
                        newFab.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                tempTheme = v.getTag().toString();

                                //show checkmark
                                newFab.setIcon(getResources().getDrawable(R.drawable.ic_check_white_36dp), false);

                                //remove previous selected fab's checkmark
                                FloatingActionButton oldFab = (FloatingActionButton) md.findViewById(getResources().getIdentifier(selectedFab, "id", getActivity().getPackageName()));
                                oldFab.setIcon(getResources().getDrawable(R.drawable.md_transparent), false);
                                selectedFab = "button_bt_float" + Integer.toString(val);
                            }
                        });
                    }

                    return false;
                }
            });

            //update theme
            new AlertDialog.Builder(getActivity());
            findPreference("pref_update_icon").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(android.preference.Preference preference) {
                    int accentId = getResources().getIdentifier(MainActivity.themeColor.replace("500", "300"), "color", getActivity().getPackageName());

                    final MaterialDialog md = new MaterialDialog.Builder(getActivity())
                            .customView(R.layout.update_icon_dialog, false)
                            .title(getString(R.string.your_new_icon))
                            .typeface(Typeface.createFromAsset(getActivity().getAssets(), "ROBOTO-REGULAR.ttf"), Typeface.createFromAsset(getActivity().getAssets(), "ROBOTO-REGULAR.ttf"))
                            .positiveText(R.string.set)
                            .negativeText(R.string.cancel)
                            .callback(new MaterialDialog.ButtonCallback() {
                                @Override
                                public void onPositive(MaterialDialog dialog) {
                                    setLauncherIcon();
                                    getActivity().finish();
                                }

                                @Override
                                public void onNegative(MaterialDialog dialog) {
                                    dialog.dismiss();
                                }
                            })
                            .positiveColorRes(accentId)
                            .negativeColorRes(accentId)
                            .build();
                    md.show();

                    ImageView iconImage = (ImageView) md.findViewById(R.id.update_icon_iv);
                    if (MainActivity.darkTheme)
                        iconImage.setImageResource(getResources().getIdentifier("ic_launcher_" + MainActivity.themeColor + "_dark", "drawable", getActivity().getPackageName()));
                    else
                        iconImage.setImageResource(getResources().getIdentifier("ic_launcher_" + MainActivity.themeColor, "drawable", getActivity().getPackageName()));

                    return false;
                }
            });

            //default note type
            findPreference("pref_default_note_type").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(android.preference.Preference preference) {
                    String temp_noteType = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString("defaultNoteType", "plain");
                    int i; //set the value of the selected choice when the dialog launches
                    switch (temp_noteType) {
                        case "bullet":
                            i = 1;
                            break;
                        case "number":
                            i = 2;
                            break;
                        default: //plain
                            i = 0;
                            break;
                    }
                    MaterialDialog md = new MaterialDialog.Builder(getActivity())
                            .title(getResources().getString(R.string.default_note_type))
                            .items(getResources().getStringArray(R.array.defaultNoteArray))
                            .itemsCallbackSingleChoice(i, new MaterialDialog.ListCallbackSingleChoice() {

                                @Override
                                public boolean onSelection(MaterialDialog materialDialog, View view, int i, CharSequence charSequence) {
                                    String s = charSequence.toString().toLowerCase();
                                    if (s.contains("plain")) s = "plain";
                                    else if (s.contains("bullet")) s = "bullet";
                                    else if (s.contains("number")) s = "number";

                                    editor.putString("defaultNoteType", s);
                                    editor.apply();
                                    return true;
                                }
                            })
                            .typeface(Typeface.createFromAsset(getActivity().getAssets(), "ROBOTO-REGULAR.ttf"), Typeface.createFromAsset(getActivity().getAssets(), "ROBOTO-REGULAR.ttf"))
                            .build();
                    md.show();

                    return false;
                }
            });
        }

        private void buildShortcutNotification() {
            Notification n;

            if (MainActivity.CURRENT_ANDROID_VERSION >= 21) { //if > lollipop
                n = new NotificationCompat.Builder(getActivity())
                        .setContentTitle(getString(R.string.app_name))
                        .setContentText(getString(R.string.quick_note))
                        .setSmallIcon(R.drawable.ic_new_note_white)
                        .setColor(getResources().getColor(R.color.grey_500))
                        .setOngoing(true)
                        .setContentIntent(PendingIntent.getActivity(getActivity(), MainActivity.SHORTCUT_NOTIF_ID, new Intent(getActivity(), MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT))
                        .setAutoCancel(false)
                        .setPriority(Notification.PRIORITY_MIN)
                        .build();
            } else {
                n = new NotificationCompat.Builder(getActivity())
                        .setContentTitle(getString(R.string.app_name))
                        .setContentText(getString(R.string.quick_note))
                        .setSmallIcon(R.drawable.ic_launcher_dashclock)
                        .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_new_note))
                        .setOngoing(true)
                        .setContentIntent(PendingIntent.getActivity(getActivity(), MainActivity.SHORTCUT_NOTIF_ID, new Intent(getActivity(), MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT))
                        .setAutoCancel(false)
                        .setPriority(Notification.PRIORITY_MIN)
                        .build();
            }
            nm.notify(MainActivity.SHORTCUT_NOTIF_ID, n);
        }

        private void restartNotey() {
            Intent i = getActivity().getPackageManager().getLaunchIntentForPackage(getActivity().getPackageName());
            Intent i2 = new Intent(getActivity(), Settings.class);
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            i2.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            startActivity(i2);
        }

        private void setLauncherIcon() {
            //loop through all 38 activity alias and disable them
            String[] activityArray = getResources().getStringArray(R.array.mainactivity_icon_alias_array_names);
            for (String s : activityArray) {
                getActivity().getPackageManager().setComponentEnabledSetting(
                        new ComponentName("thomas.jonathan.notey", s),
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
            }

            //get which activity to enable
            String enabledActivity;
            if (MainActivity.darkTheme)
                enabledActivity = "thomas.jonathan.notey.MainActivity-" + MainActivity.themeColor + "_dark";
            else enabledActivity = "thomas.jonathan.notey.MainActivity-" + MainActivity.themeColor;

            //enable it
            getActivity().getPackageManager().setComponentEnabledSetting(
                    new ComponentName("thomas.jonathan.notey", enabledActivity),
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);

            //restart the app
            restartNotey();
        }
    }
}
