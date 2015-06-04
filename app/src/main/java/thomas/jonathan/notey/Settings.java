package thomas.jonathan.notey;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
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
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.rey.material.widget.FloatingActionButton;

import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;
import uk.co.chrisjenx.calligraphy.CalligraphyTypefaceSpan;

public class Settings extends PreferenceActivity{
    private boolean impossible_to_delete = false;
    private SharedPreferences sharedPreferences;

    //theme dialog variables
    private String selectedFab;
    private CheckBox darkCheckBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        themeStuffBeforeSetContentView();
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

        //theme preference dialog
        new AlertDialog.Builder(this);
        findPreference("pref_theme").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(final Preference preference) {
                selectedFab = sharedPreferences.getString("pref_theme_fab", "button_bt_float6"); //fab6 is the default blue color for notey
                int colorID = getResources().getIdentifier(MainActivity.themeColor, "color", getPackageName());

                int view; //layout for theme choose
                if(MainActivity.proVersionEnabled) view = R.layout.color_chooser_dialog_pro;
                else view = R.layout.color_chooser_dialog;

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

                                restartNotey(); //restart notey. re-launching the main activity along with the settings. that way the user returns to the settings screen
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
                TextView darkTextView = (TextView) md.findViewById(R.id.dark_theme_tv);
                if(!MainActivity.proVersionEnabled) darkTextView.setEnabled(false);

                if(MainActivity.proVersionEnabled) setProColorButtonOnClickListeners(md);
                else setColorButtonOnClickListeners(md);

                return false;
            }
        });

        //update theme
        new AlertDialog.Builder(this);
        findPreference("pref_update_icon").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
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

    private void setProColorButtonOnClickListeners(final MaterialDialog md){

        final FloatingActionButton fab1 = (FloatingActionButton) md.findViewById(R.id.button_bt_float1);
        fab1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!selectedFab.equals("button_bt_float1")) { //don't do anything if the user clicks on the same color twice in a row
                    MainActivity.themeColor = "md_red_500";

                    //show checkmark
                    fab1.setIcon(getResources().getDrawable(R.drawable.ic_check_white_36dp), false);

                    //remove previous selected fab's checkmark
                    FloatingActionButton fab = (FloatingActionButton) md.findViewById(getResources().getIdentifier(selectedFab, "id", getPackageName()));
                    fab.setIcon(getResources().getDrawable(R.drawable.md_transparent), false);
                    selectedFab = "button_bt_float1";
                }
            }
        });

        final FloatingActionButton fab2 = (FloatingActionButton) md.findViewById(R.id.button_bt_float2);
        fab2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!selectedFab.equals("button_bt_float2")) { //don't do anything if the user clicks on the same color twice in a row
                    MainActivity.themeColor = "md_pink_500";

                    //show checkmark
                    fab2.setIcon(getResources().getDrawable(R.drawable.ic_check_white_36dp), false);

                    //remove previous selected fab's checkmark
                    FloatingActionButton fab = (FloatingActionButton) md.findViewById(getResources().getIdentifier(selectedFab, "id", getPackageName()));
                    fab.setIcon(getResources().getDrawable(R.drawable.md_transparent), false);

                    selectedFab = "button_bt_float2";
                }
            }
        });
        final FloatingActionButton fab3 = (FloatingActionButton) md.findViewById(R.id.button_bt_float3);
        fab3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!selectedFab.equals("button_bt_float3")) { //don't do anything if the user clicks on the same color twice in a row
                    MainActivity.themeColor = "md_purple_500";

                    //show checkmark
                    fab3.setIcon(getResources().getDrawable(R.drawable.ic_check_white_36dp), false);

                    //remove previous selected fab's checkmark
                    FloatingActionButton fab = (FloatingActionButton) md.findViewById(getResources().getIdentifier(selectedFab, "id", getPackageName()));
                    fab.setIcon(getResources().getDrawable(R.drawable.md_transparent), false);

                    selectedFab = "button_bt_float3";
                }
            }
        });
        final FloatingActionButton fab4 = (FloatingActionButton) md.findViewById(R.id.button_bt_float4);
        fab4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!selectedFab.equals("button_bt_float4")) { //don't do anything if the user clicks on the same color twice in a row
                    MainActivity.themeColor = "md_deep_purple_500";

                    //show checkmark
                    fab4.setIcon(getResources().getDrawable(R.drawable.ic_check_white_36dp), false);

                    //remove previous selected fab's checkmark
                    FloatingActionButton fab = (FloatingActionButton) md.findViewById(getResources().getIdentifier(selectedFab, "id", getPackageName()));
                    fab.setIcon(getResources().getDrawable(R.drawable.md_transparent), false);

                    selectedFab = "button_bt_float4";
                }
            }
        });
        final FloatingActionButton fab5 = (FloatingActionButton) md.findViewById(R.id.button_bt_float5);
        fab5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!selectedFab.equals("button_bt_float5")) { //don't do anything if the user clicks on the same color twice in a row
                    MainActivity.themeColor = "md_indigo_500";

                    //show checkmark
                    fab5.setIcon(getResources().getDrawable(R.drawable.ic_check_white_36dp), false);

                    //remove previous selected fab's checkmark
                    FloatingActionButton fab = (FloatingActionButton) md.findViewById(getResources().getIdentifier(selectedFab, "id", getPackageName()));
                    fab.setIcon(getResources().getDrawable(R.drawable.md_transparent), false);

                    selectedFab = "button_bt_float5";
                }
            }
        });
        final FloatingActionButton fab6 = (FloatingActionButton) md.findViewById(R.id.button_bt_float6);
        fab6.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!selectedFab.equals("button_bt_float6")) { //don't do anything if the user clicks on the same color twice in a row
                    MainActivity.themeColor = "md_blue_500";

                    //show checkmark
                    fab6.setIcon(getResources().getDrawable(R.drawable.ic_check_white_36dp), false);

                    //remove previous selected fab's checkmark
                    FloatingActionButton fab = (FloatingActionButton) md.findViewById(getResources().getIdentifier(selectedFab, "id", getPackageName()));
                    fab.setIcon(getResources().getDrawable(R.drawable.md_transparent), false);

                    selectedFab = "button_bt_float6";
                }
            }
        });
        final FloatingActionButton fab7 = (FloatingActionButton) md.findViewById(R.id.button_bt_float7);
        fab7.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!selectedFab.equals("button_bt_float7")) { //don't do anything if the user clicks on the same color twice in a row
                    MainActivity.themeColor = "md_light_blue_500";

                    //show checkmark
                    fab7.setIcon(getResources().getDrawable(R.drawable.ic_check_white_36dp), false);

                    //remove previous selected fab's checkmark
                    FloatingActionButton fab = (FloatingActionButton) md.findViewById(getResources().getIdentifier(selectedFab, "id", getPackageName()));
                    fab.setIcon(getResources().getDrawable(R.drawable.md_transparent), false);

                    selectedFab = "button_bt_float7";
                }
            }
        });
        final FloatingActionButton fab8 = (FloatingActionButton) md.findViewById(R.id.button_bt_float8);
        fab8.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!selectedFab.equals("button_bt_float8")) { //don't do anything if the user clicks on the same color twice in a row
                    MainActivity.themeColor = "md_cyan_500";

                    //show checkmark
                    fab8.setIcon(getResources().getDrawable(R.drawable.ic_check_white_36dp), false);

                    //remove previous selected fab's checkmark
                    FloatingActionButton fab = (FloatingActionButton) md.findViewById(getResources().getIdentifier(selectedFab, "id", getPackageName()));
                    fab.setIcon(getResources().getDrawable(R.drawable.md_transparent), false);

                    selectedFab = "button_bt_float8";
                }
            }
        });
        final FloatingActionButton fab9 = (FloatingActionButton) md.findViewById(R.id.button_bt_float9);
        fab9.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!selectedFab.equals("button_bt_float9")) { //don't do anything if the user clicks on the same color twice in a row
                    MainActivity.themeColor = "md_teal_500";

                    //show checkmark
                    fab9.setIcon(getResources().getDrawable(R.drawable.ic_check_white_36dp), false);

                    //remove previous selected fab's checkmark
                    FloatingActionButton fab = (FloatingActionButton) md.findViewById(getResources().getIdentifier(selectedFab, "id", getPackageName()));
                    fab.setIcon(getResources().getDrawable(R.drawable.md_transparent), false);

                    selectedFab = "button_bt_float9";
                }
            }
        });
        final FloatingActionButton fab10 = (FloatingActionButton) md.findViewById(R.id.button_bt_float10);
        fab10.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!selectedFab.equals("button_bt_float10")) { //don't do anything if the user clicks on the same color twice in a row
                    MainActivity.themeColor = "md_green_500";

                    //show checkmark
                    fab10.setIcon(getResources().getDrawable(R.drawable.ic_check_white_36dp), false);

                    //remove previous selected fab's checkmark
                    FloatingActionButton fab = (FloatingActionButton) md.findViewById(getResources().getIdentifier(selectedFab, "id", getPackageName()));
                    fab.setIcon(getResources().getDrawable(R.drawable.md_transparent), false);

                    selectedFab = "button_bt_float10";
                }
            }
        });
        final FloatingActionButton fab11 = (FloatingActionButton) md.findViewById(R.id.button_bt_float11);
        fab11.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!selectedFab.equals("button_bt_float11")) { //don't do anything if the user clicks on the same color twice in a row
                    MainActivity.themeColor = "md_light_green_500";

                    //show checkmark
                    fab11.setIcon(getResources().getDrawable(R.drawable.ic_check_white_36dp), false);

                    //remove previous selected fab's checkmark
                    FloatingActionButton fab = (FloatingActionButton) md.findViewById(getResources().getIdentifier(selectedFab, "id", getPackageName()));
                    fab.setIcon(getResources().getDrawable(R.drawable.md_transparent), false);

                    selectedFab = "button_bt_float11";
                }
            }
        });
        final FloatingActionButton fab12 = (FloatingActionButton) md.findViewById(R.id.button_bt_float12);
        fab12.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!selectedFab.equals("button_bt_float12")) { //don't do anything if the user clicks on the same color twice in a row
                    MainActivity.themeColor = "md_lime_500";

                    //show checkmark
                    fab12.setIcon(getResources().getDrawable(R.drawable.ic_check_white_36dp), false);

                    //remove previous selected fab's checkmark
                    FloatingActionButton fab = (FloatingActionButton) md.findViewById(getResources().getIdentifier(selectedFab, "id", getPackageName()));
                    fab.setIcon(getResources().getDrawable(R.drawable.md_transparent), false);

                    selectedFab = "button_bt_float12";
                }
            }
        });
        final FloatingActionButton fab13 = (FloatingActionButton) md.findViewById(R.id.button_bt_float13);
        fab13.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!selectedFab.equals("button_bt_float13")) { //don't do anything if the user clicks on the same color twice in a row
                    MainActivity.themeColor = "md_yellow_500";

                    //show checkmark
                    fab13.setIcon(getResources().getDrawable(R.drawable.ic_check_white_36dp), false);

                    //remove previous selected fab's checkmark
                    FloatingActionButton fab = (FloatingActionButton) md.findViewById(getResources().getIdentifier(selectedFab, "id", getPackageName()));
                    fab.setIcon(getResources().getDrawable(R.drawable.md_transparent), false);

                    selectedFab = "button_bt_float13";
                }
            }
        });
        final FloatingActionButton fab14 = (FloatingActionButton) md.findViewById(R.id.button_bt_float14);
        fab14.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!selectedFab.equals("button_bt_float14")) { //don't do anything if the user clicks on the same color twice in a row
                    MainActivity.themeColor = "md_amber_500";

                    //show checkmark
                    fab14.setIcon(getResources().getDrawable(R.drawable.ic_check_white_36dp), false);

                    //remove previous selected fab's checkmark
                    FloatingActionButton fab = (FloatingActionButton) md.findViewById(getResources().getIdentifier(selectedFab, "id", getPackageName()));
                    fab.setIcon(getResources().getDrawable(R.drawable.md_transparent), false);

                    selectedFab = "button_bt_float14";
                }
            }
        });
        final FloatingActionButton fab15 = (FloatingActionButton) md.findViewById(R.id.button_bt_float15);
        fab15.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!selectedFab.equals("button_bt_float15")) { //don't do anything if the user clicks on the same color twice in a row
                    MainActivity.themeColor = "md_orange_500";

                    //show checkmark
                    fab15.setIcon(getResources().getDrawable(R.drawable.ic_check_white_36dp), false);

                    //remove previous selected fab's checkmark
                    FloatingActionButton fab = (FloatingActionButton) md.findViewById(getResources().getIdentifier(selectedFab, "id", getPackageName()));
                    fab.setIcon(getResources().getDrawable(R.drawable.md_transparent), false);

                    selectedFab = "button_bt_float15";
                }
            }
        });
        final FloatingActionButton fab16 = (FloatingActionButton) md.findViewById(R.id.button_bt_float16);
        fab16.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!selectedFab.equals("button_bt_float16")) { //don't do anything if the user clicks on the same color twice in a row
                    MainActivity.themeColor = "md_deep_orange_500";

                    //show checkmark
                    fab16.setIcon(getResources().getDrawable(R.drawable.ic_check_white_36dp), false);

                    //remove previous selected fab's checkmark
                    FloatingActionButton fab = (FloatingActionButton) md.findViewById(getResources().getIdentifier(selectedFab, "id", getPackageName()));
                    fab.setIcon(getResources().getDrawable(R.drawable.md_transparent), false);

                    selectedFab = "button_bt_float16";
                }
            }
        });
        final FloatingActionButton fab17 = (FloatingActionButton) md.findViewById(R.id.button_bt_float17);
        fab17.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!selectedFab.equals("button_bt_float17")) { //don't do anything if the user clicks on the same color twice in a row
                    MainActivity.themeColor = "md_brown_500";

                    //show checkmark
                    fab17.setIcon(getResources().getDrawable(R.drawable.ic_check_white_36dp), false);

                    //remove previous selected fab's checkmark
                    FloatingActionButton fab = (FloatingActionButton) md.findViewById(getResources().getIdentifier(selectedFab, "id", getPackageName()));
                    fab.setIcon(getResources().getDrawable(R.drawable.md_transparent), false);

                    selectedFab = "button_bt_float17";
                }
            }
        });
        final FloatingActionButton fab18 = (FloatingActionButton) md.findViewById(R.id.button_bt_float18);
        fab18.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!selectedFab.equals("button_bt_float18")) { //don't do anything if the user clicks on the same color twice in a row
                    MainActivity.themeColor = "md_grey_500";

                    //show checkmark
                    fab18.setIcon(getResources().getDrawable(R.drawable.ic_check_white_36dp), false);

                    //remove previous selected fab's checkmark
                    FloatingActionButton fab = (FloatingActionButton) md.findViewById(getResources().getIdentifier(selectedFab, "id", getPackageName()));
                    fab.setIcon(getResources().getDrawable(R.drawable.md_transparent), false);

                    selectedFab = "button_bt_float18";
                }
            }
        });
        final FloatingActionButton fab19 = (FloatingActionButton) md.findViewById(R.id.button_bt_float19);
        fab19.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!selectedFab.equals("button_bt_float19")) { //don't do anything if the user clicks on the same color twice in a row
                    MainActivity.themeColor = "md_blue_grey_500";

                    //show checkmark
                    fab19.setIcon(getResources().getDrawable(R.drawable.ic_check_white_36dp), false);

                    //remove previous selected fab's checkmark
                    FloatingActionButton fab = (FloatingActionButton) md.findViewById(getResources().getIdentifier(selectedFab, "id", getPackageName()));
                    fab.setIcon(getResources().getDrawable(R.drawable.md_transparent), false);

                    selectedFab = "button_bt_float19";
                }
            }
        });
    }

    //non pro version
    private void setColorButtonOnClickListeners(final MaterialDialog md){

        final FloatingActionButton fab1 = (FloatingActionButton) md.findViewById(R.id.button_bt_float1);
        fab1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!selectedFab.equals("button_bt_float1")) { //don't do anything if the user clicks on the same color twice in a row
                    MainActivity.themeColor = "md_red_500";

                    //show checkmark
                    fab1.setIcon(getResources().getDrawable(R.drawable.ic_check_white_36dp), false);

                    //remove previous selected fab's checkmark
                    FloatingActionButton fab = (FloatingActionButton) md.findViewById(getResources().getIdentifier(selectedFab, "id", getPackageName()));
                    fab.setIcon(getResources().getDrawable(R.drawable.md_transparent), false);
                    selectedFab = "button_bt_float1";
                }
            }
        });
        final FloatingActionButton fab6 = (FloatingActionButton) md.findViewById(R.id.button_bt_float6);
        fab6.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!selectedFab.equals("button_bt_float6")) { //don't do anything if the user clicks on the same color twice in a row
                    MainActivity.themeColor = "md_blue_500";

                    //show checkmark
                    fab6.setIcon(getResources().getDrawable(R.drawable.ic_check_white_36dp), false);

                    //remove previous selected fab's checkmark
                    FloatingActionButton fab = (FloatingActionButton) md.findViewById(getResources().getIdentifier(selectedFab, "id", getPackageName()));
                    fab.setIcon(getResources().getDrawable(R.drawable.md_transparent), false);

                    selectedFab = "button_bt_float6";
                }
            }
        });
        final FloatingActionButton fab10 = (FloatingActionButton) md.findViewById(R.id.button_bt_float10);
        fab10.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!selectedFab.equals("button_bt_float10")) { //don't do anything if the user clicks on the same color twice in a row
                    MainActivity.themeColor = "md_green_500";

                    //show checkmark
                    fab10.setIcon(getResources().getDrawable(R.drawable.ic_check_white_36dp), false);

                    //remove previous selected fab's checkmark
                    FloatingActionButton fab = (FloatingActionButton) md.findViewById(getResources().getIdentifier(selectedFab, "id", getPackageName()));
                    fab.setIcon(getResources().getDrawable(R.drawable.md_transparent), false);

                    selectedFab = "button_bt_float10";
                }
            }
        });
        final FloatingActionButton fab18 = (FloatingActionButton) md.findViewById(R.id.button_bt_float18);
        fab18.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!selectedFab.equals("button_bt_float18")) { //don't do anything if the user clicks on the same color twice in a row
                    MainActivity.themeColor = "md_grey_500";

                    //show checkmark
                    fab18.setIcon(getResources().getDrawable(R.drawable.ic_check_white_36dp), false);

                    //remove previous selected fab's checkmark
                    FloatingActionButton fab = (FloatingActionButton) md.findViewById(getResources().getIdentifier(selectedFab, "id", getPackageName()));
                    fab.setIcon(getResources().getDrawable(R.drawable.md_transparent), false);

                    selectedFab = "button_bt_float18";
                }
            }
        });
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

    private void setUpFonts() {
        SpannableString s = new SpannableString(getString(R.string.notey_settings));
        s.setSpan(new CalligraphyTypefaceSpan(Typeface.createFromAsset(getAssets(), "ROBOTO-BOLD.ttf")), 0, s.length(),
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

    private void restartNotey(){
        Intent i = getBaseContext().getPackageManager().getLaunchIntentForPackage(getBaseContext().getPackageName() );
        Intent i2 = new Intent(getApplicationContext(), Settings.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        i2.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
        startActivity(i2);
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
