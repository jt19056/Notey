package thomas.jonathan.notey;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.database.CursorIndexOutOfBoundsException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.speech.RecognizerIntent;
import android.support.v4.app.NotificationCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebView;
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

import com.afollestad.materialdialogs.MaterialDialog;
import com.android.vending.billing.IInAppBillingService;
import com.easyandroidanimations.library.ScaleInAnimation;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import thomas.jonathan.notey.util.IabHelper;
import thomas.jonathan.notey.util.IabResult;
import thomas.jonathan.notey.util.Inventory;
import thomas.jonathan.notey.util.Purchase;

public class MainActivity extends Activity implements OnClickListener {
    public static final int CURRENT_ANDROID_VERSION = Build.VERSION.SDK_INT;
    public static boolean justTurnedPro = false;
    public static boolean spinnerChanged = false;
    private static final int REQUEST_CODE = 1234;
    public static final int SHORTCUT_NOTIF_ID = 314150413; //pi and bday
    private final String TAG = "Notey_MainActivity";
    private List<Integer> imageIconList;
    private NotificationManager nm;
    private ImageButton ib1, ib2, ib3, ib4, ib5, send_btn, menu_btn, alarm_btn;
    private EditText et, et_title;
    private Spinner spinner;
    private PopupMenu mPopupMenu;
    private int imageButtonNumber = 1, spinnerLocation = 0, id = (int) (Math.random() * 10000), priority;
    private boolean pref_expand;
    private boolean pref_swipe;
    private boolean impossible_to_delete = false;
    private boolean pref_enter;
    private boolean pref_share_action;
    private boolean settings_activity_flag;
    private boolean about_activity_flag;
    private boolean editIntentFlag = false;
    private boolean noteTextBoxHasFocus = false;
    private String clickNotif;
    private String noteTitle;
    private String alarm_time = "";
    private int repeat_time = 0;
    private NoteyNote notey;
    public MySQLiteHelper db = new MySQLiteHelper(this);
    private RelativeLayout layout_bottom;
    private PendingIntent alarmPendingIntent;
    private static List<String> pref_icons;
    private List<String> spinnerPositionList;
    private static Context appContext;
    public static final SimpleDateFormat format_short_date = new SimpleDateFormat("MMM dd"), format_short_time = new SimpleDateFormat("hh:mm a");

    /* in app billing variables */
    public static IabHelper mHelper;
    public static final String SKU_PRO_VERSION = "thomas.jonathan.notey.pro";
    public static final String SKU_TIP_VERSION = "thomas.jonathan.notey.tip";
    public static boolean proVersionEnabled = false;
    public static String payload = Integer.toString((int) (Math.random() * 1000000));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity_dialog);

        getWindow().setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);

        notey = new NoteyNote(); //create a new Notey object
        nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        appContext = getApplicationContext();

        initializeSettings();
        loadData();
        initializeGUI();

        setLayout();

        //menu popup
        mPopupMenu = new PopupMenu(this, menu_btn);
        setUpMenu();

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

        //focus change listeners for the note and title text boxes. these are for determining where the text will go when using the mic button
        et.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    noteTextBoxHasFocus = true;
                }
            }
        });

        et_title.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    noteTextBoxHasFocus = false;
                }
            }
        });

        //spinner listener. changes the row of five icons based on what spinner item is selected.
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long arg3) {
                if (editIntentFlag) {
                    spinner.setSelection(spinnerLocation);
                    position = spinnerLocation;
                    editIntentFlag = false;
                } else spinnerLocation = position;
                String s = pref_icons.get(position);

                //the text the user sees is different than the icon names
                if (s.equals("smile")) s = "mood";
                if (s.equals("heart")) s = "favorite";
                if (s.equals("note")) s = "note_add";

                ib1.setImageResource(getResources().getIdentifier("ic_" + s + "_white_36dp", "drawable", getPackageName()));
                ib2.setImageResource(getResources().getIdentifier("ic_" + s + "_yellow_36dp", "drawable", getPackageName()));
                ib3.setImageResource(getResources().getIdentifier("ic_" + s + "_blue_36dp", "drawable", getPackageName()));
                ib4.setImageResource(getResources().getIdentifier("ic_" + s + "_green_36dp", "drawable", getPackageName()));
                ib5.setImageResource(getResources().getIdentifier("ic_" + s + "_red_36dp", "drawable", getPackageName()));

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

            icon.setImageResource(imageIconList.get(position));
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
            ib1.setBackgroundColor(getResources().getColor(R.color.grey_600));

            ib2.setBackgroundColor(Color.TRANSPARENT);
            ib3.setBackgroundColor(Color.TRANSPARENT);
            ib4.setBackgroundColor(Color.TRANSPARENT);
            ib5.setBackgroundColor(Color.TRANSPARENT);
        } else if (v.getId() == R.id.imageButton2) {
            imageButtonNumber = 2;
            ib2.setBackgroundColor(getResources().getColor(R.color.grey_600));

            ib1.setBackgroundColor(Color.TRANSPARENT);
            ib3.setBackgroundColor(Color.TRANSPARENT);
            ib4.setBackgroundColor(Color.TRANSPARENT);
            ib5.setBackgroundColor(Color.TRANSPARENT);
        } else if (v.getId() == R.id.imageButton3) {
            imageButtonNumber = 3;
            ib3.setBackgroundColor(getResources().getColor(R.color.grey_600));

            ib1.setBackgroundColor(Color.TRANSPARENT);
            ib2.setBackgroundColor(Color.TRANSPARENT);
            ib4.setBackgroundColor(Color.TRANSPARENT);
            ib5.setBackgroundColor(Color.TRANSPARENT);
        } else if (v.getId() == R.id.imageButton4) {
            imageButtonNumber = 4;
            ib4.setBackgroundColor(getResources().getColor(R.color.grey_600));

            ib1.setBackgroundColor(Color.TRANSPARENT);
            ib2.setBackgroundColor(Color.TRANSPARENT);
            ib3.setBackgroundColor(Color.TRANSPARENT);
            ib5.setBackgroundColor(Color.TRANSPARENT);
        } else if (v.getId() == R.id.imageButton5) {
            imageButtonNumber = 5;
            ib5.setBackgroundColor(getResources().getColor(R.color.grey_600));

            ib1.setBackgroundColor(Color.TRANSPARENT);
            ib2.setBackgroundColor(Color.TRANSPARENT);
            ib3.setBackgroundColor(Color.TRANSPARENT);
            ib4.setBackgroundColor(Color.TRANSPARENT);
        } else if (v.getId() == R.id.menuButton) {
            mPopupMenu.show();
        } else if (v.getId() == R.id.alarm_btn) {
            Intent i = new Intent(this, AlarmActivity.class);
            i.putExtra("alarm_id", id);

            // if alarm is set, pass the alarm time to the next activity. if no repeat, check for regular alarm
            if (repeat_time != 0) {
                i.putExtra("repeat_set_time", repeat_time);
                i.putExtra("alarmPendingIntent", alarmPendingIntent);
                if (alarm_time != null && !alarm_time.equals(""))
                    i.putExtra("alarm_set_time", alarm_time);
            } else if (alarm_time != null && !alarm_time.equals("")) {
                i.putExtra("alarm_set_time", alarm_time);
                i.putExtra("alarmPendingIntent", alarmPendingIntent);
            }

            startActivityForResult(i, id);
            settings_activity_flag = false;
            about_activity_flag = false;
        }
        // else if the send button is pressed
        else if (v.getId() == R.id.btn) {
            //check if user has made it not possible to remove notifications.
            // (this is a fail-safe in case they got out of the settings menu by pressing the 'home' key or some other way)
            if ((!clickNotif.equals("remove") && !clickNotif.equals("info")) && !pref_swipe && !pref_expand) {
                Toast.makeText(getApplicationContext(), getString(R.string.impossibleToDeleteAtSend), Toast.LENGTH_SHORT).show();
                impossible_to_delete = true;
            } else impossible_to_delete = false;

            //if empty, go ahead with voice input
            if (et.getText().toString().equals("") && et_title.getText().toString().equals("")) {
                PackageManager pm = getPackageManager();
                List<ResolveInfo> activities = pm.queryIntentActivities(
                        new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0);
                if (activities.size() == 0) {
                    Toast.makeText(getApplicationContext(), getString(R.string.no_voice_input), Toast.LENGTH_SHORT).show();
                } else {
                    startVoiceRecognitionActivity();
                }
            }

            // if alarm time is empty, set the boolean to true, otherwise check if alarm time is greater than current time. if it is, dont set notificatin.
            boolean alarmTimeIsGreaterThanCurrentTime;
            alarmTimeIsGreaterThanCurrentTime = alarm_time == null || alarm_time.equals("") || Long.valueOf(alarm_time) > System.currentTimeMillis();

            //if either textbox has text AND it is possible to delete notifs AND if current time is less than alarm time, then set the alarm, continue and create notification
            if ((!et.getText().toString().equals("") || !et_title.getText().toString().equals("")) && !impossible_to_delete && alarmTimeIsGreaterThanCurrentTime) {

                String s = pref_icons.get(spinnerLocation);

                if (s.equals("smile")) s = "mood";
                if (s.equals("heart")) s = "favorite";
                if (s.equals("note")) s = "note_add";

                int d;
                if (imageButtonNumber == 2)
                    d = getResources().getIdentifier("ic_" + s + "_yellow_36dp", "drawable", getPackageName());
                else if (imageButtonNumber == 3)
                    d = getResources().getIdentifier("ic_" + s + "_blue_36dp", "drawable", getPackageName());
                else if (imageButtonNumber == 4)
                    d = getResources().getIdentifier("ic_" + s + "_green_36dp", "drawable", getPackageName());
                else if (imageButtonNumber == 5)
                    d = getResources().getIdentifier("ic_" + s + "_red_36dp", "drawable", getPackageName());
                else
                    d = getResources().getIdentifier("ic_" + s + "_white_36dp", "drawable", getPackageName());

                String note = et.getText().toString(); //get the text

                //set title text
                String tickerText;  //if title is there, set ticker to title. otherwise set it to the note
                if (!et_title.getText().toString().equals("")) {
                    noteTitle = et_title.getText().toString();
                    tickerText = noteTitle;
                } else {
                    noteTitle = getString(R.string.app_name);
                    tickerText = note;
                }

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
                if (alarm_time != null && !alarm_time.equals("")) {  //if alarm time is valid, and if we are not in and editIntent
                    notey.setAlarm(alarm_time); // add to db

                    // add the alarm date/time to the notification
                    Date date = new Date(Long.valueOf(alarm_time));
                    noteForNotification += "\n" + getString(R.string.alarm) + ": " + format_short_date.format(date) + ", " + format_short_time.format(date);

                    // intent for alarm service to launch
                    Intent myIntent = new Intent(this, AlarmService.class);
                    Bundle bundle = new Bundle();
                    bundle.putInt("alarmID", id);
                    myIntent.putExtras(bundle);

                    //set alarm
                    AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
                    alarmPendingIntent = PendingIntent.getService(this, id, myIntent, PendingIntent.FLAG_UPDATE_CURRENT);

                    alarmManager.cancel(alarmPendingIntent); // cancel any alarm that might already exist

                    //set repeating alarm or set regular alarm
                    if (repeat_time != 0) {
                        // check the sharedPrefs for the check box to wake up the device
                        if (PreferenceManager.getDefaultSharedPreferences(getBaseContext()).getBoolean("wake" + Integer.toString(id), true))
                            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, Long.valueOf(alarm_time), repeat_time * 1000 * 60, alarmPendingIntent);
                        else
                            alarmManager.setRepeating(AlarmManager.RTC, Long.valueOf(alarm_time), repeat_time * 1000 * 60, alarmPendingIntent);
                    } else { //set regualar alarm
                        // check the sharedPrefs for the check box to wake up the device
                        if (PreferenceManager.getDefaultSharedPreferences(getBaseContext()).getBoolean("wake" + Integer.toString(id), true))
                            alarmManager.set(AlarmManager.RTC_WAKEUP, Long.valueOf(alarm_time), alarmPendingIntent);
                        else
                            alarmManager.set(AlarmManager.RTC, Long.valueOf(alarm_time), alarmPendingIntent);
                    }
                }

                //does notey exist in database? if yes-update. if no-add new notey.
                if (db.checkIfExist(id)) db.updateNotey(notey);
                else db.addNotey(notey);

                //intents for expandable notifications
                PendingIntent piDismiss = createOnDismissedIntent(this, id);
                PendingIntent piEdit = createEditIntent();
                PendingIntent piShare = createShareIntent(note);

                // set expandable notification buttons
                Bitmap bm;
                //big white icons are un-seeable on lollipop, have a null LargeIcon if that's the case
                if (CURRENT_ANDROID_VERSION >= 21 && notey.getIconName().contains("white_36dp")) {
                    bm = null;
                } else bm = BitmapFactory.decodeResource(getResources(), d);

                //build the notification!
                Notification n;
                if (pref_expand && pref_share_action && CURRENT_ANDROID_VERSION >= 16) { //jelly bean and above with expandable notifs settings allowed && share action button is enabled
                    n = new NotificationCompat.Builder(this)
                            .setContentTitle(noteTitle)
                            .setContentText(note)
                            .setTicker(tickerText)
                            .setSmallIcon(d)
                            .setLargeIcon(bm)
                            .setStyle(new NotificationCompat.BigTextStyle().bigText(noteForNotification))
                            .setDeleteIntent(piDismiss)
                            .setOngoing(!pref_swipe)
                            .setContentIntent(onNotifClickPI(clickNotif, note, noteTitle))
                            .setAutoCancel(false)
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
                            .setTicker(tickerText)
                            .setSmallIcon(d)
                            .setLargeIcon(bm)
                            .setStyle(new NotificationCompat.BigTextStyle().bigText(noteForNotification))
                            .setDeleteIntent(piDismiss)
                            .setOngoing(!pref_swipe)
                            .setContentIntent(onNotifClickPI(clickNotif, note, noteTitle))
                            .setAutoCancel(false)
                            .setPriority(priority)
                            .addAction(R.drawable.ic_edit_white_24dp,
                                    getString(R.string.edit), piEdit) //edit button on notification
                            .addAction(R.drawable.ic_delete_white_24dp,
                                    getString(R.string.remove), piDismiss) //remove button
                            .build();
                } else if (!pref_expand && CURRENT_ANDROID_VERSION >= 16) { //not expandable, but still able to set priority
                    n = new NotificationCompat.Builder(this)
                            .setContentTitle(noteTitle)
                            .setContentText(note)
                            .setTicker(tickerText)
                            .setSmallIcon(d)
                            .setDeleteIntent(piDismiss)
                            .setOngoing(!pref_swipe)
                            .setContentIntent(onNotifClickPI(clickNotif, note, noteTitle))
                            .setAutoCancel(false)
                            .setPriority(priority)
                            .build();
                } else { //if api < 16. they cannot have expandable notifs or any type of priority
                    n = new NotificationCompat.Builder(this)
                            .setContentTitle(noteTitle)
                            .setContentText(note)
                            .setTicker(tickerText)
                            .setSmallIcon(d)
                            .setAutoCancel(false)
                            .setContentIntent(onNotifClickPI(clickNotif, note, noteTitle))
                            .setDeleteIntent(piDismiss)
                            .setOngoing(!pref_swipe)
                            .build();
                }
                nm.notify(id, n);
                finish();
            }
            // alarm time has past, show toast
            else if (!alarmTimeIsGreaterThanCurrentTime) {
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

        /* resizing the window when more/less text is added */
        final RelativeLayout relativeLayoutCopy = layout_bottom;
        final EditText editTextCopy = et;

        //when text is added or deleted, count the num lines of text there are and adjust the size of the textbox accordingly.
        et.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                if (et.getText().toString().equals("") && et_title.getText().toString().equals("")) { //if both note and title are blank, set to mic
                    new ScaleInAnimation(send_btn).setDuration(250).animate();
                    send_btn.setImageDrawable(getResources().getDrawable(R.drawable.ic_mic_grey600_36dp));
                }

                if (et.getLineCount() > 1) {
                    et.getLayoutParams().height = RelativeLayout.LayoutParams.WRAP_CONTENT;
                    layout_bottom.getLayoutParams().height = et.getLayoutParams().height;
                } else {
                    et.setLayoutParams(editTextCopy.getLayoutParams());
                    layout_bottom.setLayoutParams(relativeLayoutCopy.getLayoutParams());
                }
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) { /*do nothing*/ }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //only animate the mic switching to the send button when the text starts at 0 (no text)
                if (before == 0 && start == 0) {
                    if (!send_btn.getDrawable().getConstantState().equals(getResources().getDrawable(R.drawable.ic_send_grey600_36dp).getConstantState())) { //switch it to the send icon if not already
                        new ScaleInAnimation(send_btn).setDuration(250).animate();
                        send_btn.setImageDrawable(getResources().getDrawable(R.drawable.ic_send_grey600_36dp));
                    }
                }
            }
        });

        // title text change listener to switch icon
        et_title.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                if (et_title.getText().toString().equals("") && et.getText().toString().equals("")) {
                    new ScaleInAnimation(send_btn).setDuration(250).animate();
                    send_btn.setImageDrawable(getResources().getDrawable(R.drawable.ic_mic_grey600_36dp));

                }
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) { /*do nothing*/ }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //only animate the mic switching to the send button when the text starts at 0 (no text)
                if (before == 0 && start == 0) {
                    if (!send_btn.getDrawable().getConstantState().equals(getResources().getDrawable(R.drawable.ic_send_grey600_36dp).getConstantState())) {
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

        if (action == null) action = "";

        //edit notification intent. set the gui to the notey wanting to be edited
        try {
            if (i.hasExtra("editNotificationID")) {
                MySQLiteHelper db = new MySQLiteHelper(this);
                editIntentFlag = true;
                int editID = i.getExtras().getInt("editNotificationID");
                NoteyNote nTemp = db.getNotey(editID);

                int editBtn = i.getExtras().getInt("editButton");
                String editAlarm = i.getExtras().getString("editAlarm");
                int editRepeat = i.getExtras().getInt("editRepeat", 0);
                boolean doAlarmActivity = i.getExtras().getBoolean("doEditAlarmActivity", false);
                PendingIntent editAlarmPI = (PendingIntent) i.getExtras().get("editAlarmPendingIntent");

                id = editID;
                et.setText(nTemp.getNote());
                et.setSelection(et.getText().length());
                spinnerLocation = nTemp.getSpinnerLoc();
                imageButtonNumber = nTemp.getImgBtnNum();
                noteTitle = nTemp.getTitle();
                if (!noteTitle.equals(getString(R.string.app_name))) //if title is not Notey, then display it
                    et_title.setText(noteTitle);
                et_title.setSelection(et_title.getText().length());
                alarm_time = editAlarm;
                repeat_time = editRepeat;
                alarmPendingIntent = editAlarmPI;

                // switch alarm button icon to show an alarm is set
                if ((alarm_time != null && !alarm_time.equals("")) || repeat_time != 0) {
                    new ScaleInAnimation(alarm_btn).setDuration(250).animate();
                    alarm_btn.setImageDrawable(getResources().getDrawable(R.drawable.ic_alarm_on_grey600_36dp));
                }

                spinner.setSelection(spinnerLocation);
                setImageButtonsBasedOffSpinnerSelection();
                setWhichImageButtonIsSelected(editBtn);
                //resize window for amount of text
                et.getLayoutParams().height = RelativeLayout.LayoutParams.WRAP_CONTENT;
                layout_bottom.getLayoutParams().height = RelativeLayout.LayoutParams.WRAP_CONTENT;

                //if the alarm text was pressed on the info screen, doAlarmActivity is set to true. if it is true, then go straight into the alarm activity to edit the alarm
                if (doAlarmActivity) {
                    alarm_btn.performClick();
                }
            } else if ((Intent.ACTION_SEND.equals(action) || action.equals("com.google.android.gm.action.AUTO_SEND")) && type != null) {
                if ("text/plain".equals(type)) {
                    handleSendText(i); // Handle text being sent
                    //resize window for amount of text
                    et.getLayoutParams().height = RelativeLayout.LayoutParams.WRAP_CONTENT;
                    et.getLayoutParams().width = RelativeLayout.LayoutParams.WRAP_CONTENT;
                    layout_bottom.getLayoutParams().height = RelativeLayout.LayoutParams.WRAP_CONTENT;
                }
            }
        } catch (CursorIndexOutOfBoundsException e) { //catches if the user deletes a note from the notfication tray while the info screen is active. then presses something on the info screen such as the alarm.
            e.printStackTrace();
        }
    }

    private void setWhichImageButtonIsSelected(int loc) {
        if (loc == 1) {
            imageButtonNumber = 1;
            ib1.setBackgroundColor(getResources().getColor(R.color.grey_600));

            ib2.setBackgroundColor(Color.TRANSPARENT);
            ib3.setBackgroundColor(Color.TRANSPARENT);
            ib4.setBackgroundColor(Color.TRANSPARENT);
            ib5.setBackgroundColor(Color.TRANSPARENT);
        } else if (loc == 2) {
            imageButtonNumber = 2;
            ib2.setBackgroundColor(getResources().getColor(R.color.grey_600));

            ib1.setBackgroundColor(Color.TRANSPARENT);
            ib3.setBackgroundColor(Color.TRANSPARENT);
            ib4.setBackgroundColor(Color.TRANSPARENT);
            ib5.setBackgroundColor(Color.TRANSPARENT);
        } else if (loc == 3) {
            imageButtonNumber = 3;
            ib3.setBackgroundColor(getResources().getColor(R.color.grey_600));

            ib1.setBackgroundColor(Color.TRANSPARENT);
            ib2.setBackgroundColor(Color.TRANSPARENT);
            ib4.setBackgroundColor(Color.TRANSPARENT);
            ib5.setBackgroundColor(Color.TRANSPARENT);
        } else if (loc == 4) {
            imageButtonNumber = 4;
            ib4.setBackgroundColor(getResources().getColor(R.color.grey_600));

            ib1.setBackgroundColor(Color.TRANSPARENT);
            ib2.setBackgroundColor(Color.TRANSPARENT);
            ib3.setBackgroundColor(Color.TRANSPARENT);
            ib5.setBackgroundColor(Color.TRANSPARENT);
        } else if (loc == 5) {
            imageButtonNumber = 5;
            ib5.setBackgroundColor(getResources().getColor(R.color.grey_600));

            ib1.setBackgroundColor(Color.TRANSPARENT);
            ib2.setBackgroundColor(Color.TRANSPARENT);
            ib3.setBackgroundColor(Color.TRANSPARENT);
            ib4.setBackgroundColor(Color.TRANSPARENT);
        }
    }

    private void checkForAppUpdate() {
        int versionCode = -1;
        try { //get current version num
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            versionCode = pInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        //checks the current version code with the one stored in shared prefs.
        if (versionCode != sharedPref.getInt("VERSION_CODE", -1)) {
            //if on before v2.0.4 and not first install, change the on click notifcation setting to the info screen
            if (sharedPref.getInt("VERSION_CODE", -1) <= 14 && sharedPref.getInt("VERSION_CODE", -1) != -1) {

                addIconNamesToDB();

                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString("clickNotif", "info");
                editor.apply();
            }

            //always restore notifications
            Intent localIntent = new Intent(this, NotificationBootService.class);
            localIntent.putExtra("action", "boot");
            startService(localIntent);
        }
        //update version code in shared prefs
        sharedPref.edit().putInt("VERSION_CODE", versionCode).apply();
    }

    public void initializeGUI() {
        setUpSpinner();

        //setup the five icons
        ib1 = (ImageButton) findViewById(R.id.imageButton1);
        ib2 = (ImageButton) findViewById(R.id.imageButton2);
        ib3 = (ImageButton) findViewById(R.id.imageButton3);
        ib4 = (ImageButton) findViewById(R.id.imageButton4);
        ib5 = (ImageButton) findViewById(R.id.imageButton5);

        send_btn = (ImageButton) findViewById(R.id.btn); //send button
        menu_btn = (ImageButton) findViewById(R.id.menuButton);
        alarm_btn = (ImageButton) findViewById(R.id.alarm_btn);

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

        ib1.setBackgroundColor(getResources().getColor(R.color.grey_600));
        ib2.setBackgroundColor(Color.TRANSPARENT);
        ib3.setBackgroundColor(Color.TRANSPARENT);
        ib4.setBackgroundColor(Color.TRANSPARENT);
        ib5.setBackgroundColor(Color.TRANSPARENT);

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
            switch (pref_priority) {
                case "high":
                    priority = Notification.PRIORITY_MAX;
                    break;
                case "low":
                    priority = Notification.PRIORITY_LOW;
                    break;
                case "minimum":
                    priority = Notification.PRIORITY_MIN;
                    break;
            }

        } else { //ics can't have expandable notifs, so set swipe to true as default
            pref_expand = false;
            pref_swipe = sharedPreferences.getBoolean("pref_swipe", true);
        }
        clickNotif = sharedPreferences.getString("clickNotif", "info"); //notification click action
        pref_share_action = sharedPreferences.getBoolean("pref_share_action", true);

        proVersionEnabled = sharedPreferences.getBoolean("proVersionEnabled", false);

        // Create new note shortcut in the notification tray
        boolean pref_shortcut = sharedPreferences.getBoolean("pref_shortcut", false);
        if (pref_shortcut) {
            buildShortcutNotification();
        } else {
            nm.cancel(SHORTCUT_NOTIF_ID);
        }

        Set<String> selections = sharedPreferences.getStringSet("pref_icon_picker", null);

        //if no icons are selected, automatically select the default five. otherwise, get their selection
        if (selections == null || selections.size() == 0) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putStringSet("pref_icon_picker", new HashSet<>(Arrays.asList(getResources().getStringArray(R.array.default_icons))));
            editor.apply();
            pref_icons = Arrays.asList(getResources().getStringArray(R.array.default_icons));
        } else pref_icons = Arrays.asList(selections.toArray(new String[selections.size()]));
        Collections.sort(pref_icons);

        //just make check first in the list
        if (pref_icons.size() > 1 && pref_icons.get(1).equals("check")) {
            Collections.swap(pref_icons, 1, 0);
        }
    }

    private void setUpSpinner() {
        setUpIcons();
        spinner = (Spinner) findViewById(R.id.spinner1);
        spinner.setAdapter(new MyAdapter(this, R.layout.spinner, spinnerPositionList.toArray(new String[spinnerPositionList.size()])));
    }


    private void setUpIcons() {
        imageIconList = new ArrayList<>();
        spinnerPositionList = new ArrayList<>();
        for (int i = 0; i < pref_icons.size(); i++) {
            imageIconList.add(convertStringToIcon(pref_icons.get(i)));
            spinnerPositionList.add(Integer.toString(i));
        }
    }

    //turns name like edit into ic_edit_grey600_36dp
    private int convertStringToIcon(String s) {
        int ic;
        if (s.equals("smile")) s = "mood";
        if (s.equals("heart")) s = "favorite";
        if (s.equals("note")) s = "note_add";

        s = "ic_" + s + "_grey600_36dp";
        ic = getResources().getIdentifier(s, "drawable", getPackageName());

        return ic;
    }

    private PendingIntent createOnDismissedIntent(Context context, int notificationId) {
        //we want to signal the NotificationDismiss receiver for removing notifications
        Intent intent = new Intent(context, NotificationDismiss.class);
        intent.putExtra("NotificationID", notificationId);
        intent.putExtra("alarmPendingIntent", alarmPendingIntent);

        return PendingIntent.getBroadcast(context.getApplicationContext(), notificationId, intent, 0);
    }

    private PendingIntent createEditIntent() {
        Intent editIntent = new Intent(this, MainActivity.class);
        editIntent.putExtra("editNotificationID", id);
        editIntent.putExtra("editButton", imageButtonNumber);
        editIntent.putExtra("editAlarm", alarm_time);
        editIntent.putExtra("editRepeat", repeat_time);
        editIntent.putExtra("editAlarmPendingIntent", alarmPendingIntent);

        return PendingIntent.getActivity(getApplicationContext(), id, editIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private PendingIntent createInfoScreenIntent(String note, String title) {
        Intent infoIntent = new Intent(this, InfoScreenActivity.class);
        infoIntent.putExtra("infoNotificationID", id);
        infoIntent.putExtra("infoNote", note);
        infoIntent.putExtra("infoLoc", spinnerLocation);
        infoIntent.putExtra("infoButton", imageButtonNumber);
        infoIntent.putExtra("infoTitle", title);
        infoIntent.putExtra("infoAlarm", alarm_time);
        infoIntent.putExtra("infoRepeat", repeat_time);
        infoIntent.putExtra("infoAlarmPendingIntent", alarmPendingIntent);
        return PendingIntent.getActivity(getApplicationContext(), id, infoIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private PendingIntent createShareIntent(String note) {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, note);
        sendIntent.setType("text/plain");
        return PendingIntent.getActivity(this, id, sendIntent, 0);
    }

    private PendingIntent onNotifClickPI(String clickNotif, String note, String title) {
        switch (clickNotif) {
            case "info":
                return createInfoScreenIntent(note, title);
            case "edit":
                return createEditIntent();
            case "remove":
                return createOnDismissedIntent(this, id);
            case "shortcut":
                return PendingIntent.getActivity(getApplicationContext(), SHORTCUT_NOTIF_ID, new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
            default:
                return null;
        }
    }

    private void setImageButtonsBasedOffSpinnerSelection() {
        String s = pref_icons.get(spinnerLocation);

        if (s.equals("smile")) s = "mood";
        if (s.equals("heart")) s = "favorite";
        if (s.equals("note")) s = "note_add";

        ib1.setImageResource(getResources().getIdentifier("ic_" + s + "_white_36dp", "drawable", getPackageName()));
        ib2.setImageResource(getResources().getIdentifier("ic_" + s + "_yellow_36dp", "drawable", getPackageName()));
        ib3.setImageResource(getResources().getIdentifier("ic_" + s + "_blue_36dp", "drawable", getPackageName()));
        ib4.setImageResource(getResources().getIdentifier("ic_" + s + "_green_36dp", "drawable", getPackageName()));
        ib5.setImageResource(getResources().getIdentifier("ic_" + s + "_red_36dp", "drawable", getPackageName()));
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

    private void startVoiceRecognitionActivity() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.speak_now));
        startActivityForResult(intent, REQUEST_CODE);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(TAG, "onActivityResult(" + requestCode + ", " + resultCode + ", " + data + ")");

        super.onActivityResult(requestCode, resultCode, data);

        // Handles the results from the voice recognition activity.
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            // get the voice input and put it into the text field
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (!results.isEmpty()) {
                String input = results.get(0);
                // put the spoken words into the note textbox or the title textbox
                if (noteTextBoxHasFocus) {
                    et.setText(input);
                    et.setSelection(et.getText().length());
                } else {
                    et_title.setText(input);
                    et_title.setSelection(et.getText().length());
                }
            }
        }

        // gets the set alarm time (in milliseconds)
        if (requestCode == id && resultCode == RESULT_OK) {
            alarm_time = data.getExtras().getString("alarm_time", "");
            repeat_time = data.getExtras().getInt("repeat_time", 0);

            // switch alarm button icon to show an alarm is set. also show toast
            if ((alarm_time != null && !alarm_time.equals("")) || repeat_time != 0) {
                new ScaleInAnimation(alarm_btn).setDuration(250).animate();
                alarm_btn.setImageDrawable(getResources().getDrawable(R.drawable.ic_alarm_on_grey600_36dp));

                Date date = new Date(Long.valueOf(alarm_time));
                Toast.makeText(getApplicationContext(), getString(R.string.alarm_set_for) + " " +
                        format_short_date.format(date) + ", " + format_short_time.format(date), Toast.LENGTH_LONG).show();
            } else {
                new ScaleInAnimation(alarm_btn).setDuration(250).animate();
                alarm_btn.setImageDrawable(getResources().getDrawable(R.drawable.ic_alarm_add_grey600_36dp));
            }
        }

        //IAB stuff
        if (mHelper == null) return;

        // Pass on the activity result to the helper for handling
        if (!mHelper.handleActivityResult(requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data);
            setUpMenu();
        } else {
            Log.d(TAG, "onActivityResult handled by IABUtil.");
        }

    }

    private void setUpMenu() {
        MenuInflater menuInflater = mPopupMenu.getMenuInflater();
        mPopupMenu.getMenu().clear(); //clear the menu so list items aren't duplicated
        //if not a pro user, show the updgrade menu option
        if (!proVersionEnabled) {
            menuInflater.inflate(R.menu.menu, mPopupMenu.getMenu());
        } else menuInflater.inflate(R.menu.menu_pro, mPopupMenu.getMenu());

        mPopupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.settings:
                        settings_activity_flag = true; //flag so mainActivity UI does get reset after settings is called
                        Intent intent = new Intent(MainActivity.this, Settings.class);
                        startActivity(intent);
                        break;
                    case R.id.go_pro:
                        try {
                            doInAppBillingStuff();
                        }catch (Exception e){
                            e.printStackTrace();
                            Toast.makeText(getApplicationContext(), getString(R.string.play_store_fail), Toast.LENGTH_SHORT).show();
                        }
                        MaterialDialog md = new MaterialDialog.Builder(MainActivity.this)
                                .title(getResources().getString(R.string.go_pro))
                                .customView(R.layout.webview_dialog_layout, false)
                                .positiveText(getResources().getString(R.string.tip))
                                .negativeText(getResources().getString(R.string.pro))
                                .neutralText(getResources().getString(R.string.no_thanks))
                                .callback(new MaterialDialog.ButtonCallback() {
                                    @Override
                                    public void onPositive(MaterialDialog dialog) {
                                        super.onPositive(dialog);
                                        Log.i(TAG, "Upgrade button clicked; launching purchase flow for PRO + TIP upgrade.");
                                        try {
                                            mHelper.launchPurchaseFlow(MainActivity.this, SKU_TIP_VERSION, 10001, mPurchaseFinishedListener, payload);
                                        }catch (Exception e){
                                            e.printStackTrace();
                                            Toast.makeText(getApplicationContext(), getString(R.string.play_store_fail), Toast.LENGTH_SHORT).show();
                                        }
                                    }

                                    @Override
                                    public void onNegative(MaterialDialog dialog) {
                                        super.onNegative(dialog);
                                        Log.i(TAG, "Upgrade button clicked; launching purchase flow for PRO upgrade.");
                                        try {
                                            mHelper.launchPurchaseFlow(MainActivity.this, SKU_PRO_VERSION, 10001, mPurchaseFinishedListener, payload);
                                        }catch (Exception e){
                                            e.printStackTrace();
                                            Toast.makeText(getApplicationContext(), getString(R.string.play_store_fail), Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                })
                                .build();
                        WebView webView = (WebView) md.getCustomView().findViewById(R.id.pro_features_webview);
                        webView.loadUrl("file:///android_asset/ProFeatures.html");
                        md.show();
                        break;
                    case R.id.about:
                        about_activity_flag = true;
                        Intent i = new Intent(MainActivity.this, About.class);
                        startActivity(i);
                        break;
                    default:
                        return true;
                }
                return true;
            }
        });
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void buildShortcutNotification() {
        Notification n = new NotificationCompat.Builder(this)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.quick_note))
                .setSmallIcon(R.drawable.ic_launcher_dashclock)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_new_note))
                .setOngoing(true)
                .setContentIntent(onNotifClickPI("shortcut", "", ""))
                .setAutoCancel(false)
                .setPriority(Notification.PRIORITY_MIN)
                .build();
        nm.notify(SHORTCUT_NOTIF_ID, n);
    }

    /* changes were made from v2.0.3 to v2.0.4. this fudged up the icon numbers. a new column in the
    *   database was created called 'iconName' to prevent this issue from happening again.
    *   This converts to old icon's ints to the new ints and writes their proper name to the database.
    */
    private void addIconNamesToDB() {
        List<NoteyNote> allNoteys = null;
        try {
            allNoteys = db.getAllNoteys();
        }catch(RuntimeException e){
            e.printStackTrace();
        }
        int newIconInt;
        String newIconName;
        if (allNoteys != null) {
            for (NoteyNote n : allNoteys) {
                if (n.getIcon() >= 2130837510 && n.getIcon() <= 2130837512) {
                    newIconInt = n.getIcon() - 6;
                    newIconName = getResources().getResourceEntryName(newIconInt);
                    n.setIconName(newIconName);
                } else if (n.getIcon() >= 2130837513 && n.getIcon() <= 2130837519) {
                    newIconInt = n.getIcon() - 5;
                    newIconName = getResources().getResourceEntryName(newIconInt);
                    n.setIconName(newIconName);
                } else if (n.getIcon() >= 2130837520 && n.getIcon() <= 2130837522) {
                    newIconInt = n.getIcon() - 3;
                    newIconName = getResources().getResourceEntryName(newIconInt);
                    n.setIconName(newIconName);
                } else if (n.getIcon() >= 2130837523 && n.getIcon() <= 2130837528) {
                    newIconInt = n.getIcon() - 2;
                    newIconName = getResources().getResourceEntryName(newIconInt);
                    n.setIconName(newIconName);
                } else if (n.getIcon() >= 2130837530 && n.getIcon() <= 2130837547) {
                    newIconInt = n.getIcon() + 2;
                    newIconName = getResources().getResourceEntryName(newIconInt);
                    n.setIconName(newIconName);
                } else
                    n.setIconName(getResources().getResourceEntryName(R.drawable.ic_check_white_36dp)); //else default to white check

                db.updateNotey(n);
            }
        }
    }

    private void doInAppBillingStuff() {
        loadData();

        // get public key which was generated from my Dev Play Account. This is split up for security reasons.
        String row3 = "kjbLj5687y153Ra3s64Mq+2ZZnWqEjY8PhDwEN2WNKhntBfBaRcGVD2FNpkcMeZdkv524e9O0VqJJCCLhyg9kil3Nx+h+pnbvtID5lwRIAbyPYbOn2xLXt";
        String row1 = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEApuEWdJzuPhdnu28kyd+8/GwNCjTXfMXfBST5+LgFYaljmb0ah7Op8gULgOeLGo1OeK4oLfKAWw";
        String row4 = "LLB*AqB/w*8uNGl$F6$jI5*fYWl8eO2aF3*zKQI**DAQAB";
        String row2 = "sfeDLPklID/xCkAsWVPAGfdiTsyIU83zxRMJc2jYUxcIbzUotWslQxaKVSaH35pcN+PqrG3eMOShTCHN9VxqMkn3Zr+g8HkiXOKMVduCgDFrWEtv8Xiqk1";
        row4 = row4.replace("*", "");
        String base64EncodedPublicKey = row1 + row2 + row3 + row4.replace("$", "");

        Log.d(TAG, "Creating IAB helper.");
        mHelper = new IabHelper(this, base64EncodedPublicKey);

        mHelper.enableDebugLogging(true);

        Log.d(TAG, "Starting setup.");
        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                Log.d(TAG, "Setup finished.");

                if (!result.isSuccess()) {
                    // Oh noes, there was a problem.
                    return;
                }

                if (mHelper == null) {
                    return;
                }
                Log.d(TAG, "Setup successful. Querying inventory.");

                // Hooray, IAB is fully set up!
                mHelper.queryInventoryAsync(mQueryFinishedListener);
            }

        });
    }

    // Listener that's called when we finish querying the items and subscriptions we own
    IabHelper.QueryInventoryFinishedListener mQueryFinishedListener = new IabHelper.QueryInventoryFinishedListener() {
        @Override
        public void onQueryInventoryFinished(IabResult result, Inventory inv) {
            Log.d(TAG, "Query inventory finished.");

            if (mHelper == null) {
                return;
            }

            if (result.isFailure()) {
                return;
            }

            Log.d(TAG, "Query inventory was successful.");
            Purchase proVersion = inv.getPurchase(SKU_PRO_VERSION);

            if (proVersion != null && verifyDeveloperPayload(proVersion)) {
                proVersionEnabled = true;
                saveData();
            }

            if (proVersionEnabled) {
                saveData();
                setUpMenu();
            }

            Log.d(TAG, "User is " + (proVersionEnabled ? "PREMIUM" : "NOT PREMIUM"));
            Log.d(TAG, "Initial inventory query finished; enabling main UI.");
        }
    };

    // Callback for when a purchase is finished
    IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        @Override
        public void onIabPurchaseFinished(IabResult result, Purchase info) {
            Log.d(TAG, "Purchase finished: " + result + ", purchase: " + info);

            if (mHelper == null) return;

            if (result.isFailure()) {
                Log.d(TAG, "Error purchasing: " + result);

                return;
            } else mHelper.queryInventoryAsync(mQueryFinishedListener);

            if (!verifyDeveloperPayload(info)) {
                Log.d(TAG, "Error purchasing. Authenticity verification failed.");
                return;
            }

            Log.d(TAG, "Purchase successful.");

            if (info.getSku().equals(SKU_PRO_VERSION)) {
                Log.d(TAG, "Purchasing premium upgrade. Congratulating user.");

                Toast.makeText(getApplicationContext(), getString(R.string.thank_you_for_pro), Toast.LENGTH_SHORT).show();
                proVersionEnabled = true;
                saveData();
                setUpProGUI();
                setUpSpinner();
            } else if (info.getSku().equals(SKU_TIP_VERSION)) {
                Log.d(TAG, "Purchasing premium upgrade. Congratulating user.");

                Toast.makeText(getApplicationContext(), getString(R.string.thank_you_for_contribution), Toast.LENGTH_SHORT).show();
                proVersionEnabled = true;
                saveData();
                setUpProGUI();
                setUpSpinner();
            }
        }
    };

    public static void setUpProGUI() {
        //make all the icons enabled initially
        pref_icons = Arrays.asList(appContext.getResources().getStringArray(R.array.icon_picker_values));
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(appContext).edit();
        editor.putStringSet("pref_icon_picker", new HashSet<>(Arrays.asList(appContext.getResources().getStringArray(R.array.icon_picker_values))));
        editor.apply();

        //when upgrading to pro, adding more icons to the spinner alters the spinner location's number. this is to fix that
        MySQLiteHelper db = new MySQLiteHelper(appContext);
        List<NoteyNote> allNoteys = db.getAllNoteys();
        for (NoteyNote n : allNoteys) {
            if (n.getSpinnerLoc() == 1) { //change edit icon. no changing check, that stays first
                n.setSpinnerLoc(2);
                db.updateNotey(n);
            } else if (n.getSpinnerLoc() == 2) { //change warning sign
                n.setSpinnerLoc(7);
                db.updateNotey(n);
            } else if (n.getSpinnerLoc() == 3) { //change star
                n.setSpinnerLoc(8);
                db.updateNotey(n);
            } else if (n.getSpinnerLoc() == 4) { //change flame
                n.setSpinnerLoc(9);
                db.updateNotey(n);
            }
        }
    }

    boolean verifyDeveloperPayload(Purchase p) {
        payload = p.getDeveloperPayload();
        return true;
    }

    //save the flag which states the user is now Pro
    void saveData() {
        SharedPreferences.Editor spe = PreferenceManager.getDefaultSharedPreferences(this).edit();
        spe.putBoolean("proVersionEnabled", proVersionEnabled);
        spe.apply();
        Log.d(TAG, "Saved data: proVersionEnabled = " + String.valueOf(proVersionEnabled));

    }

    // gets whether user is Pro
    void loadData() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        proVersionEnabled = sp.getBoolean("proVersionEnabled", false);
        Log.d(TAG, "Loaded data: proVersionEnabled = " + String.valueOf(proVersionEnabled));

    }

    @Override
    protected void onResume() {
        super.onResume();
        // if settings activity was called, reset the ui
        if (settings_activity_flag) {
            initializeSettings();
            if (spinnerChanged) { //if the spinner was altered then refresh it
                setUpSpinner();
                spinnerChanged = false;
            }
            settings_activity_flag = false;
        }
        if (about_activity_flag) { // if about activity was called, check to see if the user had typed in the secret code and are now Pro. if yes, set up everything like a normal pro purchase.
            initializeSettings();
            if (justTurnedPro) {
                setUpMenu();
                setUpSpinner();
                justTurnedPro = false;
            }
            about_activity_flag = false;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //unbind from the In-app Billing service when done with the activity
        Log.d(TAG, "Destroying helper.");
        if (mHelper != null) mHelper.dispose();
        mHelper = null;
    }
}


