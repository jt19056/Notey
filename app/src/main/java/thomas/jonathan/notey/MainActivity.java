package thomas.jonathan.notey;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.RecognizerIntent;
import android.support.v4.app.NotificationCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.easyandroidanimations.library.ScaleInAnimation;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends Activity implements OnClickListener {
    public static final int CURRENT_ANDROID_VERSION = Build.VERSION.SDK_INT;
    public static final int SET_ALARM_REQUEST = 1;
    private static final int REQUEST_CODE = 1234;
    private Integer[] imageIconDatabase = {R.drawable.ic_check_grey600_36dp, R.drawable.ic_warning_grey600_36dp, R.drawable.ic_edit_grey600_36dp, R.drawable.ic_star_grey600_36dp, R.drawable.ic_whatshot_grey600_36dp};
    private NotificationManager nm;
    private ImageButton ib1, ib2, ib3, ib4, ib5, send_btn, menu_btn, alarm_btn;
    private EditText et, et_title;
    private String[] spinnerPositionArray = {"0", "1", "2", "3", "4"};
    private Spinner spinner;
    private PopupMenu mPopupMenu;
    private int imageButtonNumber = 1, spinnerLocation = 0, id = (int) (Math.random() * 10000), priority;
    private boolean pref_expand, pref_swipe, impossible_to_delete = false, pref_enter, pref_voice, pref_share_action, pref_hide_middle_layout, layout_middle_is_shown, settings_activity_flag;
    private String clickNotif;
    private String noteTitle;
    private String alarm_time = "";
    private NoteyNote notey;
    public MySQLiteHelper db = new MySQLiteHelper(this);
    private RelativeLayout layout_bottom, layout_top, layout_middle;
    private PendingIntent alarmPendingIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity_dialog);

        getWindow().setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);

        notey = new NoteyNote(); //create a new Notey object

        initializeSettings();
        initializeGUI();
        setLayout();
        checkLongPressVoiceInput();

        //menu popup
        mPopupMenu = new PopupMenu(this, menu_btn);
        MenuInflater menuInflater = mPopupMenu.getMenuInflater();
        menuInflater.inflate(R.menu.menu, mPopupMenu.getMenu());

        mPopupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.settings:
                        settings_activity_flag = true; //flag so mainActivity UI does get reset after settings is called
                        Intent intent = new Intent(MainActivity.this, Settings.class);
                        startActivity(intent);
                        break;
                    case R.id.about:
                        Intent i = new Intent(MainActivity.this, About.class);
                        startActivity(i);
                        break;
                    default:
                        return true;
                }
                return true;
            }
        });

        checkForAppUpdate(); // restore notifications after app update

        checkForAnyIntents(); //checking for intents of edit button clicks or received shares

        //button click listener. for Enter key and Menu key
        et.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // If the event is a key-down event on the "enter" button, then check the prefs for what to do (either new line or send).
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                    pref_enter = sp.getBoolean("pref_enter", true);
                    // enter to send, or new line
                    if (pref_enter)
                        send_btn.performClick();
                    else et.append("\n");
                    return true;
                }
                //if hardware menu button, activate the menu button at the top of the app.
                else if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_MENU)) {
                    menu_btn.performClick();
                }
                return false;
            }
        });

        //spinner listener. changes the row of five icons based on what spinner item is selected.
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long arg3) {
                if (position == 0) {
                    spinnerLocation = 0;
                    ib1.setImageResource(R.drawable.ic_check_white_36dp);
                    ib2.setImageResource(R.drawable.ic_check_yellow_36dp);
                    ib3.setImageResource(R.drawable.ic_check_blue_36dp);
                    ib4.setImageResource(R.drawable.ic_check_green_36dp);
                    ib5.setImageResource(R.drawable.ic_check_red_36dp);

                }

                if (position == 1) {
                    spinnerLocation = 1;
                    ib1.setImageResource(R.drawable.ic_warning_white_36dp);
                    ib2.setImageResource(R.drawable.ic_warning_yellow_36dp);
                    ib3.setImageResource(R.drawable.ic_warning_blue_36dp);
                    ib4.setImageResource(R.drawable.ic_warning_green_36dp);
                    ib5.setImageResource(R.drawable.ic_warning_red_36dp);
                }

                if (position == 2) {
                    spinnerLocation = 2;
                    ib1.setImageResource(R.drawable.ic_edit_white_36dp);
                    ib2.setImageResource(R.drawable.ic_edit_yellow_36dp);
                    ib3.setImageResource(R.drawable.ic_edit_blue_36dp);
                    ib4.setImageResource(R.drawable.ic_edit_green_36dp);
                    ib5.setImageResource(R.drawable.ic_edit_red_36dp);
                }

                if (position == 3) {
                    spinnerLocation = 3;
                    ib1.setImageResource(R.drawable.ic_star_white_36dp);
                    ib2.setImageResource(R.drawable.ic_star_yellow_36dp);
                    ib3.setImageResource(R.drawable.ic_star_blue_36dp);
                    ib4.setImageResource(R.drawable.ic_star_green_36dp);
                    ib5.setImageResource(R.drawable.ic_star_red_36dp);
                }
                if (position == 4) {
                    spinnerLocation = 4;
                    ib1.setImageResource(R.drawable.ic_whatshot_white_36dp);
                    ib2.setImageResource(R.drawable.ic_whatshot_yellow_36dp);
                    ib3.setImageResource(R.drawable.ic_whatshot_blue_36dp);
                    ib4.setImageResource(R.drawable.ic_whatshot_green_36dp);
                    ib5.setImageResource(R.drawable.ic_whatshot_red_36dp);
                }

                new ScaleInAnimation(ib1).setDuration(250).animate();
                new ScaleInAnimation(ib2).setDuration(250).animate();
                new ScaleInAnimation(ib3).setDuration(250).animate();
                new ScaleInAnimation(ib4).setDuration(250).animate();
                new ScaleInAnimation(ib5).setDuration(250).animate();
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                // do nothing
            }
        });
    }

    //adapter for spinner. allows custom icons to be placed.
    public class MyAdapter extends ArrayAdapter<String> {
        public MyAdapter(Context context, int textViewResourceId, String[] objects) {
            super(context, textViewResourceId, objects);
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            return getCustomView(position, parent);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return getCustomView(position, parent);
        }

        public View getCustomView(int position, ViewGroup parent) {
            LayoutInflater inflater = getLayoutInflater();
            View row = inflater.inflate(R.layout.spinner, parent, false);

            ImageView icon = (ImageView) row.findViewById(R.id.imageView1);
            icon.setImageResource(imageIconDatabase[position]);
            new ScaleInAnimation(icon).setDuration(250).animate();

            return row;
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public void onClick(View v) {
        //if one of the five icons are clicked, highlight them and un-highlight the previous selection.
        if (v.getId() == R.id.imageButton1) {
            imageButtonNumber = 1;
            ib1.setBackgroundColor(Color.argb(150, 51, 181, 229));
            ib2.setBackgroundColor(Color.TRANSPARENT);
            ib3.setBackgroundColor(Color.TRANSPARENT);
            ib4.setBackgroundColor(Color.TRANSPARENT);
            ib5.setBackgroundColor(Color.TRANSPARENT);

        } else if (v.getId() == R.id.imageButton2) {
            imageButtonNumber = 2;
            ib2.setBackgroundColor(Color.argb(150, 51, 181, 229));
            ib1.setBackgroundColor(Color.TRANSPARENT);
            ib3.setBackgroundColor(Color.TRANSPARENT);
            ib4.setBackgroundColor(Color.TRANSPARENT);
            ib5.setBackgroundColor(Color.TRANSPARENT);
        } else if (v.getId() == R.id.imageButton3) {
            imageButtonNumber = 3;
            ib3.setBackgroundColor(Color.argb(150, 51, 181, 229));
            ib1.setBackgroundColor(Color.TRANSPARENT);
            ib2.setBackgroundColor(Color.TRANSPARENT);
            ib4.setBackgroundColor(Color.TRANSPARENT);
            ib5.setBackgroundColor(Color.TRANSPARENT);
        } else if (v.getId() == R.id.imageButton4) {
            imageButtonNumber = 4;
            ib4.setBackgroundColor(Color.argb(150, 51, 181, 229));
            ib1.setBackgroundColor(Color.TRANSPARENT);
            ib2.setBackgroundColor(Color.TRANSPARENT);
            ib3.setBackgroundColor(Color.TRANSPARENT);
            ib5.setBackgroundColor(Color.TRANSPARENT);
        } else if (v.getId() == R.id.imageButton5) {
            imageButtonNumber = 5;
            ib5.setBackgroundColor(Color.argb(150, 51, 181, 229));
            ib1.setBackgroundColor(Color.TRANSPARENT);
            ib2.setBackgroundColor(Color.TRANSPARENT);
            ib3.setBackgroundColor(Color.TRANSPARENT);
            ib4.setBackgroundColor(Color.TRANSPARENT);
        } else if (v.getId() == R.id.menuButton) {
            menu_btn.postDelayed(new Runnable() {
                @Override
                public void run() {
                    menu_btn.setBackgroundColor(Color.TRANSPARENT);
                }
            }, 100);
            mPopupMenu.show();
        }
        else if (v.getId() == R.id.alarm_btn) {
            Intent i = new Intent(this, AlarmActivity.class);
            i.putExtra("alarm_id", id);

            // if alarm is set, pass the alarm time to the next activity
            if(alarm_time != null && !alarm_time.equals("")) {
                i.putExtra("alarm_set_time", alarm_time);
                i.putExtra("alarm_pending_intent", alarmPendingIntent);
            }

            startActivityForResult(i, SET_ALARM_REQUEST);
            settings_activity_flag = false;
        }
        // hide or show the title/alarm layout
        else if (v.getId() == R.id.layout_top) {
            if(layout_middle_is_shown){
                hideMiddleLayout();

            }
            else{
                showMiddleLayout();

            }
        }
        // else if the send button is pressed
        else if (v.getId() == R.id.btn) {
            //check if user has made it not possible to remove notifications.
            // (this is a fail-safe in case they got out of the settings menu by pressing the 'home' key or some other way)
            if ((!clickNotif.equals("remove") && !clickNotif.equals("info")) && !pref_swipe && !pref_expand) {
                Toast.makeText(getApplicationContext(), getString(R.string.impossibleToDeleteAtSend), Toast.LENGTH_SHORT).show();
                impossible_to_delete = true;
            } else impossible_to_delete = false;

            //if empty and long press for voice input is false, go ahead with voice input
            if(et.getText().toString().equals("") && et_title.getText().toString().equals("") && !pref_voice){
                PackageManager pm = getPackageManager();
                List<ResolveInfo> activities = pm.queryIntentActivities(
                        new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0);
                if (activities.size() == 0) {
                    Toast.makeText(getApplicationContext(), getString(R.string.no_voice_input), Toast.LENGTH_SHORT).show();
                }
                else{
                    startVoiceRecognitionActivity();
                }
            }
            else if (et.getText().toString().equals("") && et_title.getText().toString().equals("") && pref_voice){ //text is empty but longpress is enabled
                Toast.makeText(getApplicationContext(), getString(R.string.hold_to_record), Toast.LENGTH_SHORT).show();
            }

            // if alarm time is empty, set the boolean to true, otherwise check if alarm time is greater than current time. if it is, dont set notificatin.
            boolean alarmTimeIsGreaterThanCurrentTime;
            if(alarm_time == null || alarm_time.equals(""))
                alarmTimeIsGreaterThanCurrentTime = true;
            else alarmTimeIsGreaterThanCurrentTime = Long.valueOf(alarm_time) > System.currentTimeMillis();

            //if either textbox has text AND it is possible to delete notifs AND if current time is less than alarm time, then set the alarm, continue and create notification
            if ((!et.getText().toString().equals("") || !et_title.getText().toString().equals("")) && !impossible_to_delete && alarmTimeIsGreaterThanCurrentTime) {
                int d; //the icon for the notification

                //get the icon
                if (spinnerLocation == 0) {
                    if (imageButtonNumber == 2) d = R.drawable.ic_check_yellow_36dp;
                    else if (imageButtonNumber == 3) d = R.drawable.ic_check_blue_36dp;
                    else if (imageButtonNumber == 4) d = R.drawable.ic_check_green_36dp;
                    else if (imageButtonNumber == 5) d = R.drawable.ic_check_red_36dp;
                    else d = R.drawable.ic_check_white_36dp;
                } else if (spinnerLocation == 1) {
                    if (imageButtonNumber == 2) d = R.drawable.ic_warning_yellow_36dp;
                    else if (imageButtonNumber == 3) d = R.drawable.ic_warning_blue_36dp;
                    else if (imageButtonNumber == 4) d = R.drawable.ic_warning_green_36dp;
                    else if (imageButtonNumber == 5) d = R.drawable.ic_warning_red_36dp;
                    else d = R.drawable.ic_warning_white_36dp;
                } else if (spinnerLocation == 2) {
                    if (imageButtonNumber == 2) d = R.drawable.ic_edit_yellow_36dp;
                    else if (imageButtonNumber == 3) d = R.drawable.ic_edit_blue_36dp;
                    else if (imageButtonNumber == 4) d = R.drawable.ic_edit_green_36dp;
                    else if (imageButtonNumber == 5) d = R.drawable.ic_edit_red_36dp;
                    else d = R.drawable.ic_edit_white_36dp;
                } else if (spinnerLocation == 3) {
                    if (imageButtonNumber == 2) d = R.drawable.ic_star_yellow_36dp;
                    else if (imageButtonNumber == 3) d = R.drawable.ic_star_blue_36dp;
                    else if (imageButtonNumber == 4) d = R.drawable.ic_star_green_36dp;
                    else if (imageButtonNumber == 5) d = R.drawable.ic_star_red_36dp;
                    else d = R.drawable.ic_star_white_36dp;
                } else {
                    if (imageButtonNumber == 2) d = R.drawable.ic_whatshot_yellow_36dp;
                    else if (imageButtonNumber == 3) d = R.drawable.ic_whatshot_blue_36dp;
                    else if (imageButtonNumber == 4) d = R.drawable.ic_whatshot_green_36dp;
                    else if (imageButtonNumber == 5) d = R.drawable.ic_whatshot_red_36dp;
                    else d = R.drawable.ic_whatshot_white_36dp;
                }

                String note = et.getText().toString(); //get the text

                //set title text
                if(!et_title.getText().toString().equals(""))
                    noteTitle = et_title.getText().toString();
                else noteTitle = getString(R.string.app_name);

                //set the notey object
                notey.setId(id);
                notey.setNote(note);
                notey.setIcon(d);
                notey.setImgBtnNum(imageButtonNumber);
                notey.setSpinnerLoc(spinnerLocation);
                notey.setTitle(noteTitle);
                notey.setIconName(getResources().getResourceEntryName(d));

                //add alarm to db and set it
                String noteForNotification = note;  // use a temp string to add the alarm info to the notification
                if(alarm_time != null && !alarm_time.equals("")){  //if alarm time is valid, and if we are not in and editIntent
                    notey.setAlarm(alarm_time); // add to db

                    // add the alarm date/time to the notification
                    Date date = new Date(Long.valueOf(alarm_time));
                    noteForNotification += "\n"+ getString(R.string.alarm) + ": " +  AlarmActivity.format_date.format(date) + " " + AlarmActivity.format_time.format(date);

                    // intent for alarm service to launch
                    Intent myIntent = new Intent(this, AlarmService.class);
                    Bundle bundle = new Bundle();
                    bundle.putInt("alarmID", id);
                    myIntent.putExtras(bundle);

                    //set alarm
                    AlarmManager alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
                    alarmPendingIntent = PendingIntent.getService(this, 0, myIntent, PendingIntent.FLAG_UPDATE_CURRENT);

                    alarmManager.cancel(alarmPendingIntent); // cancel any alarm that might already exist

                    // check the sharedPrefs for the check box to wake up the device
                    if(PreferenceManager.getDefaultSharedPreferences(getBaseContext()).getBoolean("wake" + Integer.toString(id), true))
                        alarmManager.set(AlarmManager.RTC_WAKEUP, Long.valueOf(alarm_time), alarmPendingIntent);
                    else alarmManager.set(AlarmManager.RTC, Long.valueOf(alarm_time), alarmPendingIntent);
                }


                //does notey exist in database? if yes-update. if no-add new notey.
                if (db.checkIfExist(id)) db.updateNotey(notey);
                else db.addNotey(notey);

                //intents for expandable notifications
                PendingIntent piDismiss = createOnDismissedIntent(this, id);
                PendingIntent piEdit = createEditIntent(note, noteTitle);
                PendingIntent piShare = createShareIntent(note);

                // set expandable notification buttons
                Bitmap bm;
                //big white icons are un-seeable on lollipop, have a null LargeIcon if that's the case
                if(CURRENT_ANDROID_VERSION >= 21 && notey.getIconName().contains("white_36dp")) {
                    bm = null;
                } else bm = BitmapFactory.decodeResource(getResources(), d);

                //build the notification!
                Notification n;
                if (pref_expand && pref_share_action && CURRENT_ANDROID_VERSION >= 16) { //jelly bean and above with expandable notifs settings allowed && share action button is enabled
                    n = new NotificationCompat.Builder(this)
                            .setContentTitle(noteTitle)
                            .setContentText(note)
                            .setSmallIcon(d)
                            .setLargeIcon(bm)
                            .setStyle(new NotificationCompat.BigTextStyle().bigText(noteForNotification))
                            .setDeleteIntent(piDismiss)
                            .setOngoing(!pref_swipe)
                            .setContentIntent(onNotifClickPI(clickNotif, note, noteTitle))
                            .setAutoCancel(true)
                            .setPriority(priority)
                            .addAction(R.drawable.ic_edit_white_24dp,
                                    getString(R.string.edit), piEdit) //edit button on notification
                            .addAction(R.drawable.ic_share_white_24dp,
                                    getString(R.string.share), piShare) // share button
                            .addAction(R.drawable.ic_delete_white_24dp,
                                    getString(R.string.remove), piDismiss) //remove button
                            .build();
                }
                // same as above, but without share action button
                else if (pref_expand && !pref_share_action && CURRENT_ANDROID_VERSION >= 16) {
                    n = new NotificationCompat.Builder(this)
                            .setContentTitle(noteTitle)
                            .setContentText(note)
                            .setSmallIcon(d)
                            .setLargeIcon(bm)
                            .setStyle(new NotificationCompat.BigTextStyle().bigText(noteForNotification))
                            .setDeleteIntent(piDismiss)
                            .setOngoing(!pref_swipe)
                            .setContentIntent(onNotifClickPI(clickNotif, note, noteTitle))
                            .setAutoCancel(true)
                            .setPriority(priority)
                            .addAction(R.drawable.ic_edit_white_24dp,
                                    getString(R.string.edit), piEdit) //edit button on notification
                            .addAction(R.drawable.ic_delete_white_24dp,
                                    getString(R.string.remove), piDismiss) //remove button
                            .build();
                }
                else if (!pref_expand && CURRENT_ANDROID_VERSION >= 16) { //not expandable, but still able to set priority
                    n = new NotificationCompat.Builder(this)
                            .setContentTitle(noteTitle)
                            .setContentText(note)
                            .setSmallIcon(d)
                            .setDeleteIntent(piDismiss)
                            .setOngoing(!pref_swipe)
                            .setContentIntent(onNotifClickPI(clickNotif, note, noteTitle))
                            .setAutoCancel(true)
                            .setPriority(priority)
                            .build();
                } else { //if api < 16. they cannot have expandable notifs or any type of priority
                    n = new NotificationCompat.Builder(this)
                            .setContentTitle(noteTitle)
                            .setContentText(note)
                            .setSmallIcon(d)
                            .setContentIntent(onNotifClickPI(clickNotif, note, noteTitle))
                            .setDeleteIntent(piDismiss)
                            .setOngoing(!pref_swipe)
                            .build();
                }
                nm.notify(id, n);
                finish();
            }
            // alarm time has past, show toast
            else if(!alarmTimeIsGreaterThanCurrentTime){
                Toast.makeText(getApplicationContext(), getString(R.string.alarm_not_set), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setLayout() {
        //show keyboard at start
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        et.setText("");

        //keep layout where it belongs on screen
        layout_bottom = (RelativeLayout) findViewById(R.id.layout_bottom); //row containing the text box
        RelativeLayout.LayoutParams parms = (RelativeLayout.LayoutParams) layout_bottom.getLayoutParams();
        parms.addRule(RelativeLayout.BELOW, R.id.tableRow1);
        final int spinnerHeight = spinner.getLayoutParams().height;
        layout_bottom.getLayoutParams().height = spinnerHeight + (int) convertDpToPixel(10, this); //get spinner height + 10dp
        et.getLayoutParams().height = spinnerHeight; //set the row's height to that of the spinner's + 10dp

        setupMiddleRow();

        /* resizing the window when more/less text is added */
        final RelativeLayout relativeLayoutCopy = layout_bottom;
        final EditText editTextCopy = et;

        //when text is added or deleted, count the num lines of text there are and adjust the size of the textbox accordingly.
        et.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                if(et.getText().toString().equals("") && et_title.getText().toString().equals("")) { //if both note and title are blank, set to mic
                    new ScaleInAnimation(send_btn).setDuration(250).animate();
                    send_btn.setImageDrawable(getResources().getDrawable(R.drawable.ic_mic_grey600_36dp));

                }

                if (et.getLineCount() > 1) {
                    et.getLayoutParams().height = RelativeLayout.LayoutParams.WRAP_CONTENT;
                    layout_bottom.getLayoutParams().height = et.getLayoutParams().height;
                }
                else {
                    et.setLayoutParams(editTextCopy.getLayoutParams());
                    layout_bottom.setLayoutParams(relativeLayoutCopy.getLayoutParams());
                }
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) { /*do nothing*/  }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //only animate the mic switching to the send button when the text starts at 0 (no text)
                if(before == 0 && start == 0) {
                    if(!send_btn.getDrawable().getConstantState().equals(getResources().getDrawable(R.drawable.ic_send_grey600_36dp).getConstantState())) { //switch it to the send icon if not already
                        new ScaleInAnimation(send_btn).setDuration(250).animate();
                        send_btn.setImageDrawable(getResources().getDrawable(R.drawable.ic_send_grey600_36dp));
                    }
                }
            }
        });

        // title text change listener to switch icon
        et_title.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                if(et_title.getText().toString().equals("") && et.getText().toString().equals("")) {
                    new ScaleInAnimation(send_btn).setDuration(250).animate();
                    send_btn.setImageDrawable(getResources().getDrawable(R.drawable.ic_mic_grey600_36dp));

                }
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) { /*do nothing*/  }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //only animate the mic switching to the send button when the text starts at 0 (no text)
                if(before == 0 && start == 0) {
                    if(!send_btn.getDrawable().getConstantState().equals(getResources().getDrawable(R.drawable.ic_send_grey600_36dp).getConstantState())){
                        new ScaleInAnimation(send_btn).setDuration(250).animate();
                        send_btn.setImageDrawable(getResources().getDrawable(R.drawable.ic_send_grey600_36dp));
                    }
                }
            }
        });
    }

    private void checkForAnyIntents() {
        Intent i = this.getIntent();
        String action = i.getAction();
        String type = i.getType();

        noteTitle = getString(R.string.app_name); // setting the note title before the intents, that way if there are no intents it is already set

        //edit notification intent. set the gui to the notey wanting to be edited
        if (i.hasExtra("editNotificationID")) {
            int editID = i.getExtras().getInt("editNotificationID");
            int editLoc = i.getExtras().getInt("editLoc");
            int editBtn = i.getExtras().getInt("editButton");
            String editNote = i.getExtras().getString("editNote");
            String editTitle = i.getExtras().getString("editTitle");
            String editAlarm = i.getExtras().getString("editAlarm");
            PendingIntent editAlarmPI = (PendingIntent) i.getExtras().get("editAlarmPendingIntent");

            id = editID;
            et.setText(editNote);
            et.setSelection(et.getText().length());
            spinnerLocation = editLoc;
            imageButtonNumber = editBtn;
            noteTitle = editTitle;
            if(!noteTitle.equals(getString(R.string.app_name))) //if title is not Notey, then display it
                et_title.setText(noteTitle);
            et_title.setSelection(et_title.getText().length());
            alarm_time = editAlarm;
            alarmPendingIntent = editAlarmPI;

            //if alarm is not empty AND has past, remove alarm
            if(alarm_time != null && !alarm_time.equals("")){
                if(Long.valueOf(alarm_time) < System.currentTimeMillis())
                    alarm_time = "";
            }

            // switch alarm button icon to show an alarm is set
            if(alarm_time != null && !alarm_time.equals("")) {
               new ScaleInAnimation(alarm_btn).setDuration(250).animate();
                alarm_btn.setImageDrawable(getResources().getDrawable(R.drawable.ic_alarm_on_grey600_36dp));
            }

            spinner.setSelection(spinnerLocation);
            setSelectedButton(imageButtonNumber);
            setSpinnerPosition();

            //resize window for amount of text
            et.getLayoutParams().height = RelativeLayout.LayoutParams.WRAP_CONTENT;
            layout_bottom.getLayoutParams().height = RelativeLayout.LayoutParams.WRAP_CONTENT;

            //clicking on notifications kill them. this will restore all the notifcations
            Intent localIntent = new Intent(this, NotificationBootService.class);
            localIntent.putExtra("action", "boot");
            startService(localIntent);
        } else if ((Intent.ACTION_SEND.equals(action) || action.equals("com.google.android.gm.action.AUTO_SEND")) && type != null) {
            if ("text/plain".equals(type)) {
                handleSendText(i); // Handle text being sent
                //resize window for amount of text
                et.getLayoutParams().height = RelativeLayout.LayoutParams.WRAP_CONTENT;
                et.getLayoutParams().width = RelativeLayout.LayoutParams.WRAP_CONTENT;
                layout_bottom.getLayoutParams().height = RelativeLayout.LayoutParams.WRAP_CONTENT;
            }
        }
    }

    private void checkForAppUpdate(){
        int versionCode = -1;
        try { //get current version num
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            versionCode = pInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        //checks the current version code with the one stored in shared prefs. if they are different, then there was an update, so restore notifications
        if (versionCode != sharedPref.getInt("VERSION_CODE", -1)) {
            //if on before v2.0.4, change the on click notifcation setting to the info screen
            if(sharedPref.getInt("VERSION_CODE", -1) <= 14){

                addIconNamesToDB();

                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString("clickNotif", "info");
                editor.apply();
            }
            Intent localIntent = new Intent(this, NotificationBootService.class);
            localIntent.putExtra("action", "boot");
            startService(localIntent);
        }
        //update version code in shared prefs
        sharedPref.edit().putInt("VERSION_CODE", versionCode).apply();
    }

    public void initializeGUI() {
        //setup spinner
        spinner = (Spinner) findViewById(R.id.spinner1);
        spinner.setAdapter(new MyAdapter(this, R.layout.spinner, spinnerPositionArray));

        //setup the five icons
        ib1 = (ImageButton) findViewById(R.id.imageButton1);
        ib2 = (ImageButton) findViewById(R.id.imageButton2);
        ib3 = (ImageButton) findViewById(R.id.imageButton3);
        ib4 = (ImageButton) findViewById(R.id.imageButton4);
        ib5 = (ImageButton) findViewById(R.id.imageButton5);

        ib1.setBackgroundColor(Color.argb(150, 51, 181, 229)); //make the first one in the row look selected
        ib2.setBackgroundColor(Color.TRANSPARENT);
        ib3.setBackgroundColor(Color.TRANSPARENT);
        ib4.setBackgroundColor(Color.TRANSPARENT);
        ib5.setBackgroundColor(Color.TRANSPARENT);

        send_btn = (ImageButton) findViewById(R.id.btn); //send button
        menu_btn = (ImageButton) findViewById(R.id.menuButton);
        alarm_btn = (ImageButton) findViewById(R.id.alarm_btn);

        layout_middle = (RelativeLayout) findViewById(R.id.layout_middle);
        layout_top = (RelativeLayout) findViewById(R.id.layout_top);

        nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.cancel(id);
        id++;

        //set the click listener
        ib1.setOnClickListener(this);
        ib2.setOnClickListener(this);
        ib3.setOnClickListener(this);
        ib4.setOnClickListener(this);
        ib5.setOnClickListener(this);
        send_btn.setOnClickListener(this);
        menu_btn.setOnClickListener(this);
        alarm_btn.setOnClickListener(this);

        impossible_to_delete = false; //set setting for unable to delete notifications to false, will be checked before pressing send

        //set textviews and change font
        TextView mainTitle = (TextView) findViewById(R.id.mainTitle);
        et = (EditText) findViewById(R.id.editText1);
        et_title = (EditText) findViewById(R.id.editText_title);

        Typeface font = Typeface.createFromAsset(getAssets(), "ROBOTO-LIGHT.TTF");
        et.setTypeface(font);
        et_title.setTypeface(font);
        mainTitle.setTypeface(font);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void initializeSettings() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        if (CURRENT_ANDROID_VERSION >= 16) {
            pref_expand = sharedPreferences.getBoolean("pref_expand", true);
            pref_swipe = sharedPreferences.getBoolean("pref_swipe", false);

            String pref_priority = sharedPreferences.getString("pref_priority", "normal");
            if (pref_priority.equals("high")) priority = Notification.PRIORITY_MAX;
            else if (pref_priority.equals("low"))
                priority = Notification.PRIORITY_LOW;
            else if (pref_priority.equals("minimum"))
                priority = Notification.PRIORITY_MIN;

        } else { //ics can't have expandable notifs, so set swipe to true as default
            pref_expand = false;
            pref_swipe = sharedPreferences.getBoolean("pref_swipe", true);
        }
        clickNotif = sharedPreferences.getString("clickNotif", "info"); //notification click action
        pref_voice = sharedPreferences.getBoolean("pref_voice", false);
        pref_share_action = sharedPreferences.getBoolean("pref_share_action", true);

        //show middle layout settings
        pref_hide_middle_layout = sharedPreferences.getBoolean("pref_hide_middle_layout", false);

        layout_middle_is_shown = !pref_hide_middle_layout;
    }

    private PendingIntent createOnDismissedIntent(Context context, int notificationId) {
        //we want to signal the NotificationDismiss receiver for removing notifications
        Intent intent = new Intent(context, NotificationDismiss.class);
        intent.putExtra("NotificationID", notificationId);
        intent.putExtra("alarmPendingIntent", alarmPendingIntent);

        return PendingIntent.getBroadcast(context.getApplicationContext(), notificationId, intent, 0);
    }

    private PendingIntent createEditIntent(String note, String title){
        Intent editIntent = new Intent(this, MainActivity.class);
        editIntent.putExtra("editNotificationID", id);
        editIntent.putExtra("editNote", note);
        editIntent.putExtra("editLoc", spinnerLocation);
        editIntent.putExtra("editButton", imageButtonNumber);
        editIntent.putExtra("editTitle", title);
        editIntent.putExtra("editAlarm", alarm_time);
        editIntent.putExtra("editAlarmPendingIntent", alarmPendingIntent);

        return PendingIntent.getActivity(getApplicationContext(), id, editIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private PendingIntent createInfoScreenIntent(String note, String title){
        Intent infoIntent = new Intent(this, InfoScreenActivity.class);
        infoIntent.putExtra("infoNotificationID", id);
        infoIntent.putExtra("infoNote", note);
        infoIntent.putExtra("infoLoc", spinnerLocation);
        infoIntent.putExtra("infoButton", imageButtonNumber);
        infoIntent.putExtra("infoTitle", title);
        infoIntent.putExtra("infoAlarm", alarm_time);
        return PendingIntent.getActivity(getApplicationContext(), id, infoIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private PendingIntent createShareIntent(String note){
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, note);
        sendIntent.setType("text/plain");
        return PendingIntent.getActivity(this, id, sendIntent, 0);
    }

    private PendingIntent onNotifClickPI(String clickNotif, String note, String title) {
        if (clickNotif.equals("info")) {
            return createInfoScreenIntent(note, title);
        } else if (clickNotif.equals("edit")) {
            return createEditIntent(note, title);
        } else if (clickNotif.equals("remove")) {
            return createOnDismissedIntent(this, id);
        } else return null;
    }

    private void setSpinnerPosition() {
        if (imageButtonNumber == 1) {
            ib1.setBackgroundColor(Color.argb(150, 51, 181, 229));
            ib2.setBackgroundColor(Color.TRANSPARENT);
            ib3.setBackgroundColor(Color.TRANSPARENT);
            ib4.setBackgroundColor(Color.TRANSPARENT);
            ib5.setBackgroundColor(Color.TRANSPARENT);

        } else if (imageButtonNumber == 2) {
            ib2.setBackgroundColor(Color.argb(150, 51, 181, 229));
            ib1.setBackgroundColor(Color.TRANSPARENT);
            ib3.setBackgroundColor(Color.TRANSPARENT);
            ib4.setBackgroundColor(Color.TRANSPARENT);
            ib5.setBackgroundColor(Color.TRANSPARENT);
        } else if (imageButtonNumber == 3) {
            ib3.setBackgroundColor(Color.argb(150, 51, 181, 229));
            ib1.setBackgroundColor(Color.TRANSPARENT);
            ib2.setBackgroundColor(Color.TRANSPARENT);
            ib4.setBackgroundColor(Color.TRANSPARENT);
            ib5.setBackgroundColor(Color.TRANSPARENT);
        } else if (imageButtonNumber == 4) {
            ib4.setBackgroundColor(Color.argb(150, 51, 181, 229));
            ib1.setBackgroundColor(Color.TRANSPARENT);
            ib2.setBackgroundColor(Color.TRANSPARENT);
            ib3.setBackgroundColor(Color.TRANSPARENT);
            ib5.setBackgroundColor(Color.TRANSPARENT);
        } else if (imageButtonNumber == 5) {
            ib5.setBackgroundColor(Color.argb(150, 51, 181, 229));
            ib1.setBackgroundColor(Color.TRANSPARENT);
            ib2.setBackgroundColor(Color.TRANSPARENT);
            ib3.setBackgroundColor(Color.TRANSPARENT);
            ib4.setBackgroundColor(Color.TRANSPARENT);
        }
    }

    private void setSelectedButton(int button) {
        if (button == 0) {
            spinnerLocation = 0;
            ib1.setImageResource(R.drawable.ic_check_white_36dp);
            ib2.setImageResource(R.drawable.ic_check_yellow_36dp);
            ib3.setImageResource(R.drawable.ic_check_blue_36dp);
            ib4.setImageResource(R.drawable.ic_check_green_36dp);
            ib5.setImageResource(R.drawable.ic_check_red_36dp);

        }

        if (button == 1) {
            spinnerLocation = 1;
            ib1.setImageResource(R.drawable.ic_warning_white_36dp);
            ib2.setImageResource(R.drawable.ic_warning_yellow_36dp);
            ib3.setImageResource(R.drawable.ic_warning_blue_36dp);
            ib4.setImageResource(R.drawable.ic_warning_green_36dp);
            ib5.setImageResource(R.drawable.ic_warning_red_36dp);
        }

        if (button == 2) {
            spinnerLocation = 2;
            ib1.setImageResource(R.drawable.ic_edit_white_36dp);
            ib2.setImageResource(R.drawable.ic_edit_yellow_36dp);
            ib3.setImageResource(R.drawable.ic_edit_blue_36dp);
            ib4.setImageResource(R.drawable.ic_edit_green_36dp);
            ib5.setImageResource(R.drawable.ic_edit_red_36dp);
        }

        if (button == 3) {
            spinnerLocation = 3;
            ib1.setImageResource(R.drawable.ic_star_white_36dp);
            ib2.setImageResource(R.drawable.ic_star_yellow_36dp);
            ib3.setImageResource(R.drawable.ic_star_blue_36dp);
            ib4.setImageResource(R.drawable.ic_star_green_36dp);
            ib5.setImageResource(R.drawable.ic_star_red_36dp);
        }
        if (button == 4) {
            spinnerLocation = 4;
            ib1.setImageResource(R.drawable.ic_whatshot_white_36dp);
            ib2.setImageResource(R.drawable.ic_whatshot_yellow_36dp);
            ib3.setImageResource(R.drawable.ic_whatshot_blue_36dp);
            ib4.setImageResource(R.drawable.ic_whatshot_green_36dp);
            ib5.setImageResource(R.drawable.ic_whatshot_red_36dp);
        }
    }

    void handleSendText(Intent intent) { //for intents from other apps who want to share text to notey
        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (sharedText != null) {
            // Update ui to reflect text being shared
            et.setText(sharedText);
            et.setSelection(et.getText().length());

            noteTitle = intent.getStringExtra(Intent.EXTRA_SUBJECT);
        }
    }

    public static float convertDpToPixel(float dp, Context context) {
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        return dp * (metrics.densityDpi / 160f);
    }

    private void startVoiceRecognitionActivity(){
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.speak_now));
        startActivityForResult(intent, REQUEST_CODE);
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);

        // Handles the results from the voice recognition activity.
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK)
        {
            // get the voice input and put it into the text field
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (!results.isEmpty()) {
                String input = results.get(0);
                et.setText(input);
                et.setSelection(et.getText().length());
            }
        }

        // gets the set alarm time (in milliseconds)
        if (requestCode == SET_ALARM_REQUEST && resultCode == RESULT_OK) {
            alarm_time = data.getExtras().getString("alarm_time", "");

            // switch alarm button icon to show an alarm is set. also show toast
            if(alarm_time != null && !alarm_time.equals("")) {
                new ScaleInAnimation(alarm_btn).setDuration(250).animate();
                alarm_btn.setImageDrawable(getResources().getDrawable(R.drawable.ic_alarm_on_grey600_36dp));

                Date date = new Date(Long.valueOf(alarm_time));
                Toast.makeText(getApplicationContext(), getString(R.string.alarm_set_for) + " " +
                        AlarmActivity.format_date.format(date) + " " + AlarmActivity.format_time.format(date), Toast.LENGTH_LONG).show();
            }
            else {
                new ScaleInAnimation(alarm_btn).setDuration(250).animate();
                alarm_btn.setImageDrawable(getResources().getDrawable(R.drawable.ic_alarm_add_grey600_36dp));
            }
        }

    }

    public void checkLongPressVoiceInput(){
        if(pref_voice){ //if long press for voice input is true
            send_btn.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if(et.getText().toString().equals("")) { //if text is empty, go ahead with voice input
                        PackageManager pm = getPackageManager();
                        List<ResolveInfo> activities = pm.queryIntentActivities(
                                new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0);
                        if (activities.size() == 0) {
                            Toast.makeText(getApplicationContext(), getString(R.string.no_voice_input), Toast.LENGTH_SHORT).show();
                        } else {
                            startVoiceRecognitionActivity();
                        }
                    }
                    return true;
                }
            });
        }
        else send_btn.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return false;
            }
        });
    }

    private void showMiddleLayout(){

        //show middle layout
        layout_middle.setVisibility(View.VISIBLE);
        layout_middle_is_shown = true;

        //reset middle layout
        RelativeLayout.LayoutParams p = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        p.addRule(RelativeLayout.BELOW, R.id.tableRow1);
        p.setMargins(0, 5, 0, 5);
        layout_middle.setLayoutParams(p);

        //reset bottom layout
        RelativeLayout.LayoutParams p2 = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        p2.addRule(RelativeLayout.BELOW, R.id.layout_middle);
        p2.setMargins(0,5,0,5);
        layout_bottom.setLayoutParams(p2);

        //resize entire window
        getWindow().setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);

    }

    private void hideMiddleLayout(){
        layout_middle.setVisibility(View.GONE);
        layout_middle_is_shown = false;

        //move bottom layout up
        RelativeLayout.LayoutParams p = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, 0, 0, 0);
        p.addRule(RelativeLayout.BELOW, R.id.tableRow1);
        layout_bottom.setLayoutParams(p);

        getWindow().setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);
    }

    private void setupMiddleRow(){
        // to show the middle row or to not? that is the question
        if(!pref_hide_middle_layout){
            showMiddleLayout();
            layout_top.setOnClickListener(null);
            layout_top.setOnTouchListener(null);
        }
        else{
            hideMiddleLayout();
            layout_top.setOnClickListener(this);

            layout_top.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        // change color
                        layout_top.setBackgroundColor(getResources().getColor(R.color.blue300));
                    }
                    else if (event.getAction() == MotionEvent.ACTION_UP) {
                        // set to normal color
                        layout_top.setBackgroundColor(getResources().getColor(R.color.material_blue));
                    }
                    return false;
                }
            });
        }

    }

    /* changes were made from v2.0.3 to v2.0.4. this fudged up the icon numbers. a new column in the
    *   database was created called 'iconName'.  This converts to old icon's ints to the new ints and writes
    *   their proper name to the database.  Notey now uses iconName to set the icon to prevent such a thing
    *   from happening again.
    */
    private void addIconNamesToDB(){
        List<NoteyNote> allNoteys = db.getAllNoteys();
        int newIconInt;
        String newIconName;
        for(NoteyNote n : allNoteys){
            if(n.getIcon() >= 2130837510 && n.getIcon() <= 2130837512){
                newIconInt = n.getIcon() - 6;
                newIconName = getResources().getResourceEntryName(newIconInt);
                n.setIconName(newIconName);
            }
            else if(n.getIcon() >= 2130837513 && n.getIcon() <= 2130837519){
                newIconInt = n.getIcon() - 5;
                newIconName = getResources().getResourceEntryName(newIconInt);
                n.setIconName(newIconName);
            }
            else if(n.getIcon() >= 2130837520 && n.getIcon() <= 2130837522){
                newIconInt = n.getIcon() - 3;
                newIconName = getResources().getResourceEntryName(newIconInt);
                n.setIconName(newIconName);
            }
            else if(n.getIcon() >= 2130837523 && n.getIcon() <= 2130837528){
                newIconInt = n.getIcon() - 2;
                newIconName = getResources().getResourceEntryName(newIconInt);
                n.setIconName(newIconName);
            }
            else if(n.getIcon() >= 2130837530 && n.getIcon() <= 2130837547){
                newIconInt = n.getIcon() + 2;
                newIconName = getResources().getResourceEntryName(newIconInt);
                n.setIconName(newIconName);
            }
            else n.setIconName(getResources().getResourceEntryName(R.drawable.ic_check_white_36dp)); //else default to white check

            db.updateNotey(n);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // if alarmactivity is called don't reset the ui, otherwise do
        if (settings_activity_flag) {
            initializeSettings();
            checkLongPressVoiceInput();
            setupMiddleRow();
        }
    }
}


