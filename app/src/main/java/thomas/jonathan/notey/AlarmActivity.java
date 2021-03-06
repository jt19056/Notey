package thomas.jonathan.notey;


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.crashlytics.android.Crashlytics;
import com.rey.material.widget.FloatingActionButton;
import com.rey.material.widget.Spinner;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;
import com.wdullaer.materialdatetimepicker.time.RadialPickerLayout;
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog;

import org.adw.library.widgets.discreteseekbar.DiscreteSeekBar;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import io.fabric.sdk.android.Fabric;

public class AlarmActivity extends FragmentActivity implements View.OnClickListener, DatePickerDialog.OnDateSetListener, TimePickerDialog.OnTimeSetListener {
    public static final String DATEPICKER_TAG = "datepicker", TIMEPICKER_TAG = "timepicker";
    private final Calendar calendar = Calendar.getInstance();
    private TextView date_tv;
    private TextView time_tv;
    private TextView sound_tv;
    private ImageView sound_iv;
    private Spinner recurrenceSpinner;
    private int year, month, day, hour, minute, repeatTime = 0, spinnerSelectedValue;
    private PendingIntent alarmPendingIntent;
    private CheckBox vibrate_cb, wake_cb;
    SharedPreferences sharedPref;
    private int id;
    private Uri alarm_uri;
    private DiscreteSeekBar seekBar;
    private FloatingActionButton ledFab;
    private String ledColor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());

        themeStuffBeforeSetContentView();
        setContentView(R.layout.alarm_activity_dialog);
        themeStuffAfterSetContentView();

        Intent i = null;
        try {
            i = getIntent();
        }catch(NullPointerException npe){
            Crashlytics.logException(npe);
        }

        id = MainActivity.id;

        sharedPref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        if (savedInstanceState != null) {
            DatePickerDialog dpd = (DatePickerDialog) getFragmentManager().findFragmentByTag(DATEPICKER_TAG);
            if (dpd != null) {
                dpd.setOnDateSetListener(this);
            }

            TimePickerDialog tpd = (TimePickerDialog) getFragmentManager().findFragmentByTag(TIMEPICKER_TAG);
            if (tpd != null) {
                tpd.setOnTimeSetListener(this);
            }
        }

        year = calendar.get(Calendar.YEAR);
        month = calendar.get(Calendar.MONTH);
        day = calendar.get(Calendar.DATE);
        hour = calendar.get(Calendar.HOUR_OF_DAY);
        minute = calendar.get(Calendar.MINUTE);

        TextView alarm_set_tv = (TextView) findViewById(R.id.alarm_set_tv);
        date_tv = (TextView) findViewById(R.id.date_tv);
        time_tv = (TextView) findViewById(R.id.time_tv);
        TextView alarm_mainTitle = (TextView) findViewById(R.id.alarm_mainTitle);
        Button set_btn = (Button) findViewById(R.id.alarm_set_btn);
        Button cancel_btn = (Button) findViewById(R.id.alarm_cancel_btn);
        ImageButton alarm_delete = (ImageButton) findViewById(R.id.alarm_delete);
        vibrate_cb = (CheckBox) findViewById(R.id.alarm_vibrate);
        wake_cb = (CheckBox) findViewById(R.id.alarm_wake);
        sound_tv = (TextView) findViewById(R.id.alarm_sound);
        TextView led_tv = (TextView) findViewById(R.id.alarm_led_tv);
        sound_iv = (ImageView) findViewById(R.id.alarm_sound_btn);
        recurrenceSpinner = (Spinner) findViewById(R.id.reccurence_spinner);
        ledFab = (FloatingActionButton) findViewById(R.id.led_fab);

        //LED fab stuffs
        String defaultLEDColor = sharedPref.getString("pref_default_led_color", "md_blue_500"); //default is blue
        ledFab.setBackgroundColor(getResources().getColor(getResources().getIdentifier(defaultLEDColor, "color", getPackageName())));
        ledColor = defaultLEDColor; //initialize ledColor to the default color

        //set up seekbar
        setUpSeekbar();
        setUpRecurrenceSpinner();
        int color = getResources().getColor(getResources().getIdentifier(MainActivity.accentColor, "color", getPackageName()));
        seekBar.setScrubberColor(color);
        seekBar.setThumbColor(color, color);

        //dark theme stuffs
        if(MainActivity.darkTheme){
            wake_cb.setTextColor(getResources().getColor(R.color.md_grey_400));
            vibrate_cb.setTextColor(getResources().getColor(R.color.md_grey_400));
            findViewById(R.id.div).setBackgroundColor(getResources().getColor(R.color.md_divider_white));
        }
        else{
            findViewById(R.id.div).setBackgroundColor(getResources().getColor(R.color.md_divider_black));
        }

        //if a pro user, enable the sound and repeat option
//        if (MainActivity.proVersionEnabled) {
            sound_tv.setEnabled(true);
            sound_tv.setClickable(true);
            sound_tv.setOnClickListener(this);

            if(MainActivity.darkTheme) {
                sound_tv.setTextColor(getResources().getColor(R.color.md_grey_400));
                led_tv.setTextColor(getResources().getColor(R.color.md_grey_400));
            }
            else {
                sound_tv.setTextColor(Color.BLACK);
                led_tv.setTextColor(Color.BLACK);
            }
//        } else { //fade the icons if not pro
//            sound_iv.setAlpha(0.3f);
//            repeat_iv.setAlpha(0.3f);
//            seekBar.setAlpha(0.3f);
//            recurrenceSpinner.setEnabled(false);
//            recurrenceSpinner.setClickable(false);
//            recurrenceSpinner.setAlpha(0.3f);
//            seekBar.setEnabled(false);
//            repeat_iv.setClickable(false);
//            seekBar.setThumbColor(getResources().getColor(R.color.grey_600), getResources().getColor(R.color.grey_600));
//        }

        Typeface roboto_light = Typeface.createFromAsset(getAssets(), "ROBOTO-LIGHT.TTF");
        Typeface roboto_reg = Typeface.createFromAsset(getAssets(), "ROBOTO-REGULAR.ttf");
        Typeface roboto_bold = Typeface.createFromAsset(getAssets(), "ROBOTO-BOLD.ttf");

        alarm_set_tv.setTypeface(roboto_bold);
        date_tv.setTypeface(roboto_reg);
        time_tv.setTypeface(roboto_reg);
        alarm_mainTitle.setTypeface(roboto_reg);
        set_btn.setTypeface(roboto_light);
        cancel_btn.setTypeface(roboto_light);
        vibrate_cb.setTypeface(roboto_reg);
        wake_cb.setTypeface(roboto_reg);
        sound_tv.setTypeface(roboto_reg);
        led_tv.setTypeface(roboto_reg);

        date_tv.setOnClickListener(this);
        time_tv.setOnClickListener(this);
        set_btn.setOnClickListener(this);
        cancel_btn.setOnClickListener(this);
        alarm_delete.setOnClickListener(this);
        ledFab.setOnClickListener(this);

        //long click listener to delete icon. toast will appear saying what the button does
        View.OnLongClickListener listener = new View.OnLongClickListener() {
            public boolean onLongClick(View v) {
                Toast.makeText(getApplicationContext(), getString(R.string.delete) + " " + getString(R.string.alarm), Toast.LENGTH_SHORT).show();
                return true;
            }
        };
        alarm_delete.setOnLongClickListener(listener);

        // if id is valid. also checks the sharedprefs if the user is editing the alarm
        if (id != -1 && sharedPref.getBoolean("vibrate" + Integer.toString(id), true))
            vibrate_cb.setChecked(true);
        if (id != -1 && sharedPref.getBoolean("wake" + Integer.toString(id), true))
            wake_cb.setChecked(true);
        if(id != -1 && sharedPref.getString("led" + Integer.toString(id), null) != null){
            String led = sharedPref.getString("led" + Integer.toString(id), MainActivity.themeColor); //default is the theme color
            ledFab.setBackgroundColor(getResources().getColor(getResources().getIdentifier(led, "color", getPackageName())));
        }
        if (id != -1) {

            //alarm sounds
            String temp_string = sharedPref.getString("sound" + Integer.toString(id), getString(R.string.none));
            alarm_uri = Uri.parse(temp_string);

            if(temp_string.contains("notification")) {
                sound_tv.setText(getString(R.string.sound) + " " + getString(R.string.notification_sound));
                sound_iv.setImageResource(R.drawable.ic_volume_up_grey600_24dp);
            }
            else if(temp_string.contains("alarm")) {
                sound_tv.setText(getString(R.string.sound) + " " + getString(R.string.alarm_beep));
                sound_iv.setImageResource(R.drawable.ic_volume_up_grey600_24dp);
            }
            else {
                sound_tv.setText(getString(R.string.sound) + " " + getString(R.string.none));
                sound_iv.setImageResource(R.drawable.ic_volume_off_grey600_24dp);
            }
        }

        Date date = new Date();

        // if an alarm is already set, show the set alarm date/time. also show the delete button
        if (i != null && i.hasExtra("alarm_set_time")) {
            alarm_set_tv.setText(getString(R.string.alarm_set_for));
            date = new Date(Long.valueOf(i.getExtras().getString("alarm_set_time")));
            repeatTime = i.getExtras().getInt("repeat_set_time", 0);
            alarm_delete.setVisibility(View.VISIBLE);

            alarmPendingIntent = (PendingIntent) i.getExtras().get("alarmPendingIntent");

            // update the calendar and the year/mo/day/hour/min to the preset alarm time
            calendar.setTime(date);
            year = calendar.get(Calendar.YEAR);
            month = calendar.get(Calendar.MONTH);
            day = calendar.get(Calendar.DATE);
            hour = calendar.get(Calendar.HOUR_OF_DAY);
            minute = calendar.get(Calendar.MINUTE);

            //depending on what the repeating time is set to, set the seekbar and spinner values
            switch (repeatTime) {
                //minutes
                case 0: seekBar.setProgress(0); break;
                case 5: seekBar.setProgress(1); break;
                case 10: seekBar.setProgress(2); break;
                case 15: seekBar.setProgress(3); break;
                case 30: seekBar.setProgress(4); break;
                case 45: seekBar.setProgress(5); break;
                //hours
                case 60: seekBar.setProgress(1); recurrenceSpinner.setSelection(1); break;  //1hr
                case 120: seekBar.setProgress(2); recurrenceSpinner.setSelection(1); break; //2hrs
                case 300: seekBar.setProgress(3); recurrenceSpinner.setSelection(1); break; //5hrs
                case 600: seekBar.setProgress(4); recurrenceSpinner.setSelection(1); break; //10hrs
                case 720: seekBar.setProgress(5); recurrenceSpinner.setSelection(1); break; //12hrs
                //days
                case 1440: seekBar.setProgress(1); recurrenceSpinner.setSelection(2); break;  //1day
                case 10080: seekBar.setProgress(2); recurrenceSpinner.setSelection(2); break; //7days
                case 20160: seekBar.setProgress(3); recurrenceSpinner.setSelection(2); break; //14days
                case 30240: seekBar.setProgress(4); recurrenceSpinner.setSelection(2); break; //21days
                case 40320: seekBar.setProgress(5); recurrenceSpinner.setSelection(2); break; //28days
            }


        } else {
            alarm_delete.setVisibility(View.INVISIBLE); // no alarm? don't show delete button then
            if(minute == 59) hour += 1; //if it's 8:59, we'll want to show 9:00
            minute = minute + 1; //add one minute to the clock, so the default display time is one minute ahead of current time
        }

        date.setMinutes(minute);

        DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.DEFAULT, Locale.getDefault());
        DateFormat timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault());
        String formattedDate = dateFormat.format(date);
        String formattedTime = timeFormat.format(date);

        date_tv.setText(formattedDate);
        time_tv.setText(formattedTime);

        //hide the 'alarm set for' textview  if the user is in landscape mode.  if this weren't here, the cancel/set buttons wouldn't be visible on smaller screens
        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
            alarm_set_tv.setVisibility(View.GONE);
        }

    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.date_tv) { //show the date picker
            DatePickerDialog datePickerDialog = DatePickerDialog.newInstance(this, year, month, day);
            datePickerDialog.vibrate(false);
            datePickerDialog.setYearRange(1985, 2028);
            datePickerDialog.setAccentColor(getResources().getColor(getResources().getIdentifier(MainActivity.themeColor, "color", getPackageName())));
            if(MainActivity.darkTheme) datePickerDialog.setThemeDark(true);
            datePickerDialog.show(getFragmentManager(), DATEPICKER_TAG);
        } else if (view.getId() == R.id.time_tv) { //show the time picker
            TimePickerDialog timePickerDialog = TimePickerDialog.newInstance(this, hour, minute, android.text.format.DateFormat.is24HourFormat(this));
            timePickerDialog.vibrate(false);
            timePickerDialog.setAccentColor(getResources().getColor(getResources().getIdentifier(MainActivity.themeColor, "color", getPackageName())));
            if(MainActivity.darkTheme) timePickerDialog.setThemeDark(true);
            timePickerDialog.show(getFragmentManager(), TIMEPICKER_TAG);
        } else if (view.getId() == R.id.alarm_set_btn) {
            calendar.set(year, month, day, hour, minute); //set the calendar for the new alarm time

            // send the time (in milliseconds) back to MainActivity to set alarm if the user creates the notey
            if (System.currentTimeMillis() < calendar.getTimeInMillis()) { //if set time is greater than current time, then set the alarm
                Intent output = new Intent();
                output.putExtra("alarm_time", Long.toString(calendar.getTimeInMillis()));
                output.putExtra("repeat_time", repeatTime);
                setResult(RESULT_OK, output);

                saveSettings();
            } else
                Toast.makeText(getApplicationContext(), getString(R.string.alarm_not_set), Toast.LENGTH_SHORT).show();
            finish();
        } else if (view.getId() == R.id.alarm_cancel_btn) {
            finish();
        } else if (view.getId() == R.id.alarm_delete) {
            Intent output = new Intent();
            output.putExtra("alarm_time", "");
            output.putExtra("repeat_time", 0);
            setResult(RESULT_OK, output);

            // remove the no longer needed sharedprefs. to reset the UI if user clicks into this activity again
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
            editor.remove("vibrate" + Integer.toString(id)).apply();
            editor.remove("wake" + Integer.toString(id)).apply();
            editor.remove("sound" + Integer.toString(id)).apply();
            editor.remove("repeat" + Integer.toString(id)).apply();
            editor.remove("led" + Integer.toString(id)).apply();

            //cancel the alarm pending intent
            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            alarmManager.cancel(alarmPendingIntent);

            Toast.makeText(getApplicationContext(), getString(R.string.alarm_deleted), Toast.LENGTH_LONG).show();
            finish();
        } else if (view.getId() == R.id.alarm_sound) {
            int sound;

            //check if alarm sound is already set to have it pre-selected for the dialog
            if(alarm_uri == null) sound = 0;
            else if(alarm_uri.toString().contains("notification")) sound = 1;
            else if(alarm_uri.toString().contains("alarm")) sound = 2;
            else sound = 0;

            new MaterialDialog.Builder(this)
                    .title(R.string.choose_alarm_type)
                    .items(R.array.alarm_sound_picker_array)
                    .itemsCallbackSingleChoice(sound, new MaterialDialog.ListCallbackSingleChoice() {
                        @Override
                        public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                            //get which one the user selects and set the sound, text box, and button icon accordingly
                            if (which == 1) {
                                alarm_uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                                sound_tv.setText(getString(R.string.sound) + " " + getString(R.string.notification_sound));
                                sound_iv.setImageResource(R.drawable.ic_volume_up_grey600_24dp);
                            } else if (which == 2) {
                                alarm_uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                                sound_tv.setText(getString(R.string.sound) + " " + getString(R.string.alarm_beep));
                                sound_iv.setImageResource(R.drawable.ic_volume_up_grey600_24dp);
                            } else {
                                alarm_uri = null;
                                sound_tv.setText(getString(R.string.sound) + " " + getString(R.string.none));
                                sound_iv.setImageResource(R.drawable.ic_volume_off_grey600_24dp);
                            }
                            return true;
                        }
                    })
                    .typeface(Typeface.createFromAsset(getAssets(), "ROBOTO-REGULAR.ttf"), Typeface.createFromAsset(getAssets(), "ROBOTO-REGULAR.ttf"))
                    .show();
        } else if (view.getId() == R.id.alarm_repeat_iv) {
            switch(spinnerSelectedValue) {
                case 0:
                    Toast.makeText(getApplicationContext(), getString(R.string.repeat_every) + repeatTime + getString(R.string.minutes), Toast.LENGTH_SHORT).show();
                    break;
                case 1:
                    int hours = repeatTime/60;
                    //display "hour" or "hours" for the toast
                    if(hours == 1)
                        Toast.makeText(getApplicationContext(), getString(R.string.repeat_every) + hours + getString(R.string.hour), Toast.LENGTH_SHORT).show();
                    else Toast.makeText(getApplicationContext(), getString(R.string.repeat_every) + hours + getString(R.string.hours), Toast.LENGTH_SHORT).show();
                    break;
                case 2:
                    int days = repeatTime/60/24;
                    //display "day" or "days" for the toast
                    if(days == 1)
                        Toast.makeText(getApplicationContext(), getString(R.string.repeat_every) + days + getString(R.string.day), Toast.LENGTH_SHORT).show();
                    else Toast.makeText(getApplicationContext(), getString(R.string.repeat_every) + days + getString(R.string.days), Toast.LENGTH_SHORT).show();
                    break;
            }
        } else if (view.getId() == R.id.led_fab){
            final MaterialDialog md = new MaterialDialog.Builder(this)
                .customView(R.layout.led_chooser_dialog, false)
                .build();

            md.show();

            String colors[] = getResources().getStringArray(R.array.icon_colors_array_pro);
            for (int i = 0; i < colors.length; i++) {
                int id = getResources().getIdentifier("icon_button_bt_float" + Integer.toString(i), "id", getPackageName());
                final FloatingActionButton newFab = (FloatingActionButton) md.findViewById(id);

                newFab.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ledColor = v.getTag().toString();
                        ledFab.setBackgroundColor(getResources().getColor(getResources().getIdentifier(ledColor, "color", getPackageName())));
                        md.dismiss();
                    }
                });
            }
        }
    }

    //save the checkboxes and settings_jb_kk. create a new sharedpref for each alarm, this is using the id as an identifier for the alarmService.
    private void saveSettings() {
        if (id != -1) {
            SharedPreferences.Editor editor = sharedPref.edit();

            editor.putBoolean("vibrate" + Integer.toString(id), vibrate_cb.isChecked());
            editor.putBoolean("wake" + Integer.toString(id), wake_cb.isChecked());
            editor.putInt("repeat" + Integer.toString(id), repeatTime);
            editor.putString("led" + Integer.toString(id), ledColor);
            if (alarm_uri != null)
                editor.putString("sound" + Integer.toString(id), alarm_uri.toString());

            editor.apply();
        }
    }

    private void setUpRecurrenceSpinner(){
        ArrayAdapter<String> adapter;
        if(MainActivity.darkTheme && MainActivity.CURRENT_ANDROID_VERSION >= Build.VERSION_CODES.LOLLIPOP) {
            adapter = new ArrayAdapter<>(this, R.layout.spinner_row_dark, getResources().getStringArray(R.array.recurrence_array));
            adapter.setDropDownViewResource(R.layout.spinner_row_dropdown_dark_lollipop);
        }else if(MainActivity.darkTheme) { //pre-lollipop devices using dark theme need to color the spinner background
            adapter = new ArrayAdapter<>(this, R.layout.spinner_row_dark, getResources().getStringArray(R.array.recurrence_array));
            adapter.setDropDownViewResource(R.layout.spinner_row_dropdown_dark_prelollipop);
        }else{
            adapter = new ArrayAdapter<>(this, R.layout.spinner_row, getResources().getStringArray(R.array.recurrence_array));
            adapter.setDropDownViewResource(R.layout.spinner_row_dropdown);
        }
        recurrenceSpinner.setAdapter(adapter);

        recurrenceSpinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(Spinner spinner, View view, int i, long l) {
                spinnerSelectedValue = spinner.getSelectedItemPosition();
                //reset the indicator value for the seekbar bubble
                seekBar.setNumericTransformer(new DiscreteSeekBar.NumericTransformer() {
                    @Override
                    public int transform(int value) {
                        int newValue = 0;
                        if (spinnerSelectedValue == 1) { //hours
                            switch (value) {
                                case 0:
                                    newValue = 0;
                                    break;
                                case 1:
                                    newValue = 1;
                                    break;
                                case 2:
                                    newValue = 2;
                                    break;
                                case 3:
                                    newValue = 5;
                                    break;
                                case 4:
                                    newValue = 10;
                                    break;
                                case 5:
                                    newValue = 12;
                                    break;
                            }
                            repeatTime = newValue * 60;
                        } else if (spinnerSelectedValue == 2) { //days
                            switch (value) {
                                case 0:
                                    newValue = 0;
                                    break;
                                case 1:
                                    newValue = 1;
                                    break;
                                case 2:
                                    newValue = 7;
                                    break;
                                case 3:
                                    newValue = 14;
                                    break;
                                case 4:
                                    newValue = 21;
                                    break;
                                case 5:
                                    newValue = 28;
                                    break;
                            }
                            repeatTime = newValue * 60 * 24;
                        } else { //minutes
                            switch (value) {
                                case 0:
                                    newValue = 0;
                                    break;
                                case 1:
                                    newValue = 5;
                                    break;
                                case 2:
                                    newValue = 10;
                                    break;
                                case 3:
                                    newValue = 15;
                                    break;
                                case 4:
                                    newValue = 30;
                                    break;
                                case 5:
                                    newValue = 45;
                                    break;
                            }
                            repeatTime = newValue;
                        }
                        return newValue;
                    }
                });
            }
        });
    }

    private void setUpSeekbar(){
        seekBar = (DiscreteSeekBar) findViewById(R.id.discrete_bar);
        seekBar.setMin(0);
        seekBar.setMax(5);
        //set what the seekbar bubble displays based on the time option selected in the spinner
        seekBar.setNumericTransformer(new DiscreteSeekBar.NumericTransformer() {
            @Override
            public int transform(int value) {
                int newValue = 0;
                if (spinnerSelectedValue == 1) { //hours
                    switch (value) {
                        case 0:
                            newValue = 0;
                            break;
                        case 1:
                            newValue = 1;
                            break;
                        case 2:
                            newValue = 2;
                            break;
                        case 3:
                            newValue = 5;
                            break;
                        case 4:
                            newValue = 10;
                            break;
                        case 5:
                            newValue = 12;
                            break;
                    }
                    repeatTime = newValue * 60;
                } else if (spinnerSelectedValue == 2) { //days
                    switch (value) {
                        case 0:
                            newValue = 0;
                            break;
                        case 1:
                            newValue = 1;
                            break;
                        case 2:
                            newValue = 7;
                            break;
                        case 3:
                            newValue = 14;
                            break;
                        case 4:
                            newValue = 21;
                            break;
                        case 5:
                            newValue = 28;
                            break;
                    }
                    repeatTime = newValue * 60 * 24;
                } else { //minutes
                    switch (value) {
                        case 0:
                            newValue = 0;
                            break;
                        case 1:
                            newValue = 5;
                            break;
                        case 2:
                            newValue = 10;
                            break;
                        case 3:
                            newValue = 15;
                            break;
                        case 4:
                            newValue = 30;
                            break;
                        case 5:
                            newValue = 45;
                            break;
                    }
                    repeatTime = newValue;
                }
                return newValue;
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

    }

    @Override
    public void onDateSet(DatePickerDialog datePickerDialog, int y, int mo, int d) {
        year = y;
        month = mo;
        day = d;

        Calendar c = Calendar.getInstance();
        c.set(y, mo, d);

        DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.DEFAULT, Locale.getDefault());
        date_tv.setText(dateFormat.format(c.getTime()));
    }

    @Override
    public void onTimeSet(RadialPickerLayout radialPickerLayout, int h, int m, int s) {
        hour = h;
        minute = m;

        String AM_PM;
        if (h < 12) {
            AM_PM = "AM";
            if (h == 0) h = 12; // 12am
        } else {
            if (h != 12) h = h - 12;
            AM_PM = "PM";
        }

        String h2 = Integer.toString(h);
        if (h < 10) h2 = "0" + h;

        String min = Integer.toString(m);
        if (m < 10) min = "0" + min;

        time_tv.setText(h2 + ":" + min + " " + AM_PM);
    }

    private void themeStuffBeforeSetContentView(){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        //initialize theme preferences
        MainActivity.themeColor = sharedPreferences.getString("pref_theme_color", "md_blue_500");
        MainActivity.darkTheme = sharedPreferences.getBoolean("pref_theme_dark", false);

        //set light/dark theme
        if(MainActivity.darkTheme) {
            super.setTheme(getResources().getIdentifier("AppBaseThemeDark_"+MainActivity.themeColor + "_Accent_" + MainActivity.accentColor, "style", getPackageName()));
        }
        else {
            super.setTheme(getResources().getIdentifier("AppBaseTheme_"+MainActivity.themeColor + "_Accent_" + MainActivity.accentColor, "style", getPackageName()));
        }
    }

    private void themeStuffAfterSetContentView(){
        //set color
        RelativeLayout r = (RelativeLayout) findViewById(R.id.alarm_layout_top);
        r.setBackgroundResource(getResources().getIdentifier(MainActivity.themeColor, "color", getPackageName()));
    }
}



