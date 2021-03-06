package thomas.jonathan.notey;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.CursorIndexOutOfBoundsException;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.balysv.materialripple.MaterialRippleLayout;
import com.crashlytics.android.Crashlytics;
import com.easyandroidanimations.library.ScaleInAnimation;
import com.google.gson.Gson;
import com.rey.material.widget.EditText;
import com.rey.material.widget.FloatingActionButton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import io.fabric.sdk.android.Fabric;

public class MainActivity extends Activity implements OnClickListener, View.OnLongClickListener {
    public static final int CURRENT_ANDROID_VERSION = android.os.Build.VERSION.SDK_INT;
    private static boolean justTurnedPro = false;
    private static final int REQUEST_CODE = 1234;
    public static final int SHORTCUT_NOTIF_ID = 314150413; //pi and bday
    private NotificationManager nm;
    private ImageButton ib1;
    private MaterialRippleLayout mrl_ib1;
    private Set<Integer> buttons;
    private FloatingActionButton send_btn;
    private ImageButton menu_btn;
    private ImageButton alarm_btn;
    private int selectedRippleButtonId;
    private EditText et, et_title;
    private TextView mainTitle;
    private PopupMenu mPopupMenu;
    private int imageButtonNumber = 1, priority;
    public static int id;
    private boolean pref_expand;
    private boolean pref_swipe;
    private boolean impossible_to_delete = false;
    private boolean pref_use_colored_icons;
    private String pref_default_note_type;
    private boolean pref_large_icons;
    private boolean pref_keyboard;
    private boolean pref_cursor;
    private boolean settings_activity_flag;
    private boolean about_activity_flag;
    private boolean editIntentFlag = false;
    private boolean rotateHasOccuredFlag = false;
    private boolean noteTextBoxHasFocus = true;
    private boolean bulletListFlag = false;
    private boolean numberedListFlag = false;
    private int numberedListCounter = 1;
    private String clickNotif;
    private String noteTitle;
    private String noteText;
    private String alarm_time = "";
    private int repeat_time = 0;
    private NoteyNote notey;
    private final MySQLiteHelper db = new MySQLiteHelper(this);
    private RelativeLayout layout_bottom;
    private PendingIntent alarmPendingIntent;
    private static SharedPreferences sharedPreferences;
    private String iconName = "ic_check_", fullIconName = "ic_check_white_24dp", iconColor = "white";
    private HorizontalScrollView hsv;
    public static String defaultIconsColor;

    //theme variables
    public static String themeColor;
    public static String accentColor;
    public static boolean darkTheme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        id = (int) (Math.random() * 10000);

        themeStuffBeforeSetContentView();
        setContentView(R.layout.main_activity_dialog);
        themeStuffAfterSetContentView();

        getWindow().setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);

        notey = new NoteyNote(); //create a new Notey object
        nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        initializeSettings();
        initializeGUI();
        setProImageButtons();
        setLayout();

        //menu popup
        mPopupMenu = new PopupMenu(this, menu_btn);
        setUpMenu();

        checkForAppUpdate(); // restore notifications after app update

        rotateHasOccuredFlag = checkForAnyIntents(); //checking for intents of edit button clicks or received shares. will return false if there were no intents that happened.

        if(!rotateHasOccuredFlag && savedInstanceState != null){
            et.setText(savedInstanceState.getString("noteText"));
            et_title.setText(savedInstanceState.getString("noteTitle"));
            iconName = savedInstanceState.getString("iconName");
            iconColor = savedInstanceState.getString("iconColor");
            setImageButtonsBasedOffIconSelection();
        }

        //button click listener. for Enter key and Menu key
        et.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // If the event is a key-down event on the "enter" button
                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    et.append("\n"); // enter for new line
                    return true;
                }
                //if hardware menu button, activate the menu button at the top of the app.
                else if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_MENU)) {
                    menu_btn.performClick();
                    return true;
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
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public void onClick(View v) {
        //if one horizontal scroll are clicked, highlight them and un-highlight the previous selection.
        if (buttons.contains(v.getId())) {
            if (v.getId() != selectedRippleButtonId + 1) {
                MaterialRippleLayout rip = (MaterialRippleLayout) findViewById(selectedRippleButtonId);
                rip.setRadius(0);
            }

            //set new icon name and new selected ib
            iconName = v.getTag().toString();

            //get the name of the current button clicked to determine which ripple button we clicked
            String imageButtonName = getResources().getResourceEntryName(v.getId());
            selectedRippleButtonId = getResources().getIdentifier("ripple_" + imageButtonName, "id", getPackageName());
        } else if (v.getId() == R.id.pallete_btn) {
            final MaterialDialog md = new MaterialDialog.Builder(MainActivity.this)
                    .customView(R.layout.icon_color_chooser_dialog_pro, false)
                    .build();

            md.show();

            setIconColorOnClickListeners(md);
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
        else {
            if (v.getId() == R.id.btn) {
                //check if user has made it not possible to remove notifications.
                // (this is a fail-safe in case they got out of the settings_jb_kk menu by pressing the 'home' key or some other way)
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

                //if either textbox has text AND it is possible to delete notifs AND (if current time is less than alarm time OR there is a repeating alarm), then set the alarm, continue and create notification
                if ((!et.getText().toString().equals("") || !et_title.getText().toString().equals("")) && !impossible_to_delete && (alarmTimeIsGreaterThanCurrentTime || repeat_time != 0)) {
                    //use the 36dp or 24dp icons
                    if (pref_large_icons) fullIconName = iconName + iconColor + "_36dp";
                    else fullIconName = iconName + iconColor + "_24dp";

                    int d = getResources().getIdentifier(fullIconName, "drawable", getPackageName());
                    int color = getResources().getColor(R.color.md_grey_500); // grey, for grey background with white icons
                    //if not greater than lollipop set the colors. otherwise, use white and set the background icon color

                    //get string color for lollipop notification background color
                    // converts ex. red -> md_red_A400  or  blue -> md_blue_500
                    if (CURRENT_ANDROID_VERSION >= 21 && !pref_use_colored_icons) {
                        String colorArray[] = getResources().getStringArray(R.array.icon_colors_array_pro);
                        int c = Arrays.asList(colorArray).indexOf(iconColor);
                        String colorNames[] = getResources().getStringArray(R.array.icon_color_names_pro);
                        String colorString = Arrays.asList(colorNames).get(c);
                        color = getResources().getColor(getResources().getIdentifier(colorString, "color", getPackageName()));
                    } else if (CURRENT_ANDROID_VERSION >= 21 && iconColor.equals("white")) { //for lollipop white icons, use white icon with grey background so they can see it
                        pref_use_colored_icons = false; //make them use the lollipop circle background
                        color = getResources().getColor(R.color.md_grey_500);
                    }

                    noteText = et.getText().toString(); //get the text

                    //set title text
                    String tickerText;  //if title is there, set ticker to title. otherwise set it to the note
                    if (!et_title.getText().toString().equals("")) {
                        noteTitle = et_title.getText().toString();
                        tickerText = noteTitle;
                    } else {
                        noteTitle = getString(R.string.app_name);
                        tickerText = noteText;
                    }

                    //set the notey object
                    notey.setId(id);
                    notey.setNote(noteText);
                    notey.setIcon(d);
                    notey.setImgBtnNum(imageButtonNumber);
                    notey.setTitle(noteTitle);
                    notey.setIconName(fullIconName);

                    //add alarm to db and set it
                    if (alarm_time != null && !alarm_time.equals("")) {  //if alarm time is valid, and if we are not in and editIntent
                        notey.setAlarm(alarm_time); // add to db

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

                            //if alarm_time is old, set alarm_time to currTime+repeat_time to avoid alarm going off directly after user creates note
                            if (!alarmTimeIsGreaterThanCurrentTime) {
                                alarm_time = Long.toString((System.currentTimeMillis() + (long) (repeat_time * 1000 * 60)));
                            }

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

                    //intent to build the notification
                    Intent intent = new Intent(this, NotificationBuild.class);
                    Bundle bundle = new Bundle();
                    bundle.putInt("id", id);
                    intent.putExtras(bundle);
                    sendBroadcast(intent);

                    finish();

                    // save all the info of the notification. this is for undo notification re-building
                    List list = Arrays.asList(
                            noteTitle, // 0 string
                            noteText, // 1 string
                            tickerText, // 2 string
                            d, // 3 int
                            color, // 4 int
                            fullIconName, // 5 string
                            priority, // 6 int
                            imageButtonNumber, // 7 int
                            alarm_time, // 8 string
                            repeat_time, // 9 int
                            bulletListFlag, // 10 boolean
                            numberedListFlag, // 11 boolean
                            numberedListCounter //12 int
                    );

                    //save to sharedprefs
                    SharedPreferences.Editor prefsEditor = sharedPreferences.edit();
                    Gson gson = new Gson();
                    String json = gson.toJson(list);
                    prefsEditor.putString("notification" + Integer.toString(id), json);
                    prefsEditor.apply();
                }
                // alarm time has past, show toast
                else if (!alarmTimeIsGreaterThanCurrentTime) {
                    Toast.makeText(getApplicationContext(), getString(R.string.alarm_not_set), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    public boolean onLongClick(View v) {
        //any button in the horiztonal scroll
        if (buttons.contains(v.getId())) {
            iconName = v.getTag().toString();
            createIconPickerDialog();

            //un-highlight old. if user clicks on same icon twice in a row,  don't un-highlight it
            if (v.getId() != selectedRippleButtonId) {
                MaterialRippleLayout rip = (MaterialRippleLayout) findViewById(selectedRippleButtonId);
                rip.setRadius(0);
            }
            //get the name of the current button clicked to determine which ripple button we clicked
            String imageButtonName = getResources().getResourceEntryName(v.getId());
            selectedRippleButtonId = getResources().getIdentifier("ripple_" + imageButtonName, "id", getPackageName());

            //highlight the correct image button
            MaterialRippleLayout rip = (MaterialRippleLayout) findViewById(selectedRippleButtonId);
            rip.setRadius(1000);
        }

        return false;
    }


    private void setIconColorOnClickListeners(final MaterialDialog md) {
        String colors[] = getResources().getStringArray(R.array.icon_colors_array_pro);

        for (int i = 0; i < colors.length; i++) {
            int id = getResources().getIdentifier("icon_button_bt_float" + Integer.toString(i), "id", getPackageName());
            final FloatingActionButton fab = (FloatingActionButton) md.findViewById(id);
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    iconColor = v.getTag().toString();
                    setImageButtonsBasedOffIconSelection();
                    md.dismiss();
                }
            });
        }
    }

    private void createIconPickerDialog() {
        final MaterialDialog md = new MaterialDialog.Builder(MainActivity.this)
                .customView(R.layout.icon_chooser_dialog_pro, false)
                .backgroundColor(darkTheme ? getResources().getColor(R.color.md_grey_800) : getResources().getColor(R.color.md_grey_700))
                .build();
        md.show();

        //based on the icon clicked, show all the color options for that icon
        String[] colorArray = getResources().getStringArray(R.array.icon_colors_array_pro);

        for (int i = 0; i < colorArray.length; i++) {
            int id = getResources().getIdentifier("icon_chooser_dialog_imageview_" + Integer.toString(i + 1), "id", getPackageName());
            ImageView iv = (ImageView) md.findViewById(id);
            iv.setImageResource(getResources().getIdentifier(iconName + colorArray[i] + "_36dp", "drawable", getPackageName()));
            iv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    iconColor = v.getTag().toString();
                    setImageButtonsBasedOffIconSelection();
                    md.dismiss();
                }
            });
        }
    }

    private void setImageButtonsBasedOffIconSelection() {
        String[] icons = getResources().getStringArray(R.array.icons_array_pro);
        //loop through the imagebuttons in the horizontal scroll, set each icon to the correct color
        for (int i = 0; i < icons.length; i++) {
            ImageButton ib = (ImageButton) findViewById(getResources().getIdentifier("imageButton" + Integer.toString(i + 1), "id", getPackageName()));
            ib.setImageResource(getResources().getIdentifier(icons[i] + iconColor + "_36dp", "drawable", getPackageName()));
        }
    }

    private void setLayout() {
        //show keyboard at start
        if(pref_keyboard) getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        //keep layout where it belongs on screen
        layout_bottom = (RelativeLayout) findViewById(R.id.layout_bottom); //row containing the note text box
        RelativeLayout.LayoutParams parms = (RelativeLayout.LayoutParams) layout_bottom.getLayoutParams();
        parms.addRule(RelativeLayout.BELOW, R.id.divider);

        /* resizing the window when more/less text is added */
        final RelativeLayout relativeLayoutCopy = layout_bottom;
        final EditText editTextCopy = et;

        //for non-lollipop devices using dark theme, both text boxes lose their 10dp padding, must programattically set them
        if(darkTheme && CURRENT_ANDROID_VERSION < android.os.Build.VERSION_CODES.LOLLIPOP) {
            et.setPadding((int) (10 * getResources().getDisplayMetrics().density), (int) (10 * getResources().getDisplayMetrics().density),
                    (int) (10 * getResources().getDisplayMetrics().density), (int) (10 * getResources().getDisplayMetrics().density));

            et_title.setPadding((int) (10 * getResources().getDisplayMetrics().density), (int) (10 * getResources().getDisplayMetrics().density),
                    (int) (10 * getResources().getDisplayMetrics().density), (int) (10 * getResources().getDisplayMetrics().density));
        }

        //when text is added or deleted, count the num lines of text there are and adjust the size of the textbox accordingly.
        et.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                if (et.getText().toString().equals("") && et_title.getText().toString().equals("")) { //if both note and title are blank, set to mic
                    new ScaleInAnimation(send_btn).setDuration(250).animate();
                    send_btn.setIcon(getResources().getDrawable(R.drawable.ic_mic_white_36dp), false);
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
                //check whether to add a bullet point or number
                if(s.length() > 0 && before == 0 && s.toString().charAt(s.toString().length()-1) == '\n') {
                    int lastIndex = 0;
                    int numTimes = 0;
                    char findStr = '\n';
                    while(lastIndex != -1){
                        lastIndex = s.toString().indexOf(findStr, lastIndex);
                        if(lastIndex != -1){
                            numTimes ++;
                            lastIndex += 1;
                        }
                    }

                    //basically, if text is not empty, continue counting. else start with 1)
                    if(s.toString().length() >= 3 && s.toString().substring(0, Math.min(s.toString().length(), 2)).equals("\n1")) //if the first 3 chars are \n
                        numberedListCounter = numTimes;
                    else if(s.toString().contains("1"))
                        numberedListCounter = numTimes +1;
                    else numberedListCounter = 1;

                    if (bulletListFlag)
                        et.append("• ");
                    else if (numberedListFlag)
                        et.append(numberedListCounter++ + ") ");
                }

                //only animate the mic switching to the send button when the text starts at 0 (no text)
                if (before == 0 && start == 0) {
                    if (!send_btn.getIcon().getConstantState().equals(getResources().getDrawable(R.drawable.ic_send_white_36dp).getConstantState())) { //switch it to the send icon if not already
                        new ScaleInAnimation(send_btn).setDuration(250).animate();
                        send_btn.setIcon(getResources().getDrawable(R.drawable.ic_send_white_36dp), false);
                    }
                }
            }
        });

        // title text change listener to switch icon
        et_title.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                if (et_title.getText().toString().equals("") && et.getText().toString().equals("")) {
                    new ScaleInAnimation(send_btn).setDuration(250).animate();
                    send_btn.setIcon(getResources().getDrawable(R.drawable.ic_mic_white_36dp), false);
                }
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) { /*do nothing*/ }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //only animate the mic switching to the send button when the text starts at 0 (no text)
                if (before == 0 && start == 0) {
                    if (!send_btn.getIcon().getConstantState().equals(getResources().getDrawable(R.drawable.ic_send_white_36dp).getConstantState())) {
                        new ScaleInAnimation(send_btn).setDuration(250).animate();
                        send_btn.setIcon(getResources().getDrawable(R.drawable.ic_send_white_36dp), false);
                    }
                }
            }
        });

        //where to place the cursor on app start. true = Title. false = Note, so do nothing
        if (pref_cursor)
        {
            noteTextBoxHasFocus = false;
            et_title.requestFocus();
        }
    }

    private boolean checkForAnyIntents() {
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

                String editAlarm = i.getExtras().getString("editAlarm");
                boolean doAlarmActivity = i.getExtras().getBoolean("doEditAlarmActivity", false);
                bulletListFlag = i.getExtras().getBoolean("bulletListFlag", false);
                numberedListFlag= i.getExtras().getBoolean("numberedListFlag", false);
                numberedListCounter= i.getExtras().getInt("numberedListCounter", 1);
                PendingIntent editAlarmPI = (PendingIntent) i.getExtras().get("editAlarmPendingIntent");

                id = editID;
                et.setText(nTemp.getNote());
                et.setSelection(et.getText().length());

                fullIconName = nTemp.getIconName();

                iconName = fullIconName.split("_")[0] + "_" + fullIconName.split("_")[1] + "_";  //ic_check_red_24dp ---> ic_check_
                if (iconName.contains("shopping")) {
                    iconName += "cart_"; //make it so iconname is ic_shopping_cart_
                    iconColor = fullIconName.split("_")[3];
                } else if (iconName.contains("note")) {
                    iconName += "add_";
                    iconColor = fullIconName.split("_")[3];
                } else if (iconName.contains("attach")) {
                    iconName += "money_";
                    iconColor = fullIconName.split("_")[3];
                } else if (iconName.contains("brightness")) {
                    iconName += "low_";
                    iconColor = fullIconName.split("_")[3];
                } else if (iconName.contains("directions")) {
                    iconName += "car_";
                    iconColor = fullIconName.split("_")[3];
                } else if (iconName.contains("flash")) {
                    iconName += "on_";
                    iconColor = fullIconName.split("_")[3];
                } else if (iconName.contains("local")) {
                    iconName += "florist_";
                    iconColor = fullIconName.split("_")[3];
                } else if (iconName.contains("music")) {
                    iconName += "note_";
                    iconColor = fullIconName.split("_")[3];
                } else if (iconName.contains("cake")) {
                    iconName += "variant_";
                    iconColor = fullIconName.split("_")[3];
                } else if (iconName.contains("format")) {
                    iconName += "align_left_";
                    iconColor = fullIconName.split("_")[4];
                } else iconColor = fullIconName.split("_")[2];

                if (iconColor.contains("light")) iconColor += "_green";
                else if (iconColor.contains("deep")) iconColor += "_orange";

                //un-highlight the check mark
                ib1.setBackgroundColor(Color.TRANSPARENT);
                mrl_ib1.setRadius(0);

                imageButtonNumber = nTemp.getImgBtnNum();
                noteTitle = nTemp.getTitle();
                if (!noteTitle.equals(getString(R.string.app_name))) //if title is not Notey, then display it
                    et_title.setText(noteTitle);
                et_title.setSelection(et_title.getText().length());
                alarm_time = editAlarm;
                repeat_time = PreferenceManager.getDefaultSharedPreferences(getBaseContext()).getInt("repeat" + Integer.toString(id), 0);
                alarmPendingIntent = editAlarmPI;

                // switch alarm button icon to show an alarm is set
                if ((alarm_time != null && !alarm_time.equals("")) || repeat_time != 0) {
                    new ScaleInAnimation(alarm_btn).setDuration(250).animate();
                    alarm_btn.setImageDrawable(getResources().getDrawable(R.drawable.ic_alarm_on_white_24dp));
                }

                setImageButtonsBasedOffIconSelection();

                //resize window for amount of text
                et.getLayoutParams().height = RelativeLayout.LayoutParams.WRAP_CONTENT;
                layout_bottom.getLayoutParams().height = RelativeLayout.LayoutParams.WRAP_CONTENT;

                //if the alarm text was pressed on the info screen, doAlarmActivity is set to true. if it is true, then go straight into the alarm activity to edit the alarm
                if (doAlarmActivity) {
                    alarm_btn.performClick();
                }
                return true;
            } else if ((Intent.ACTION_SEND.equals(action) || action.equals("com.google.android.gm.action.AUTO_SEND")) && type != null) {
                if ("text/plain".equals(type)) {
                    handleSendText(i); // Handle text being sent
                    //resize window for amount of text
                    et.getLayoutParams().height = RelativeLayout.LayoutParams.WRAP_CONTENT;
                    layout_bottom.getLayoutParams().height = RelativeLayout.LayoutParams.WRAP_CONTENT;
                }
                return true;
            }
        } catch (CursorIndexOutOfBoundsException e) { //catches if the user deletes a note from the notfication tray while the info screen is active. then presses something on the info screen such as the alarm.
            e.printStackTrace();
            Crashlytics.logException(e);
            return false;
        }
        return false;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        //scroll to the correct icon in the horizontal scroll view if we are opening an edit intent OR if the user has rotated the screen
        if (editIntentFlag || !rotateHasOccuredFlag) {
            String icons[] = getResources().getStringArray(R.array.icons_array_pro);
            int selected = Arrays.asList(icons).indexOf(iconName);
            MaterialRippleLayout rip = (MaterialRippleLayout) findViewById(getResources().getIdentifier("ripple_imageButton" + Integer.toString(selected + 1), "id", getPackageName()));
            rip.setRadius(1000);
            selectedRippleButtonId = rip.getId();
            int x = rip.getLeft();
            int y = rip.getTop();
            hsv.scrollTo(x, y);
            editIntentFlag = false;
        }
    }

    private void checkForAppUpdate() {
        int versionCode = -1;
        try { //get current version num
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            versionCode = pInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            Crashlytics.logException(e);
        }
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        int userVersion = sharedPref.getInt("VERSION_CODE", -1);

        //checks the current version code with the one stored in shared prefs to determine if app has been updated
        if (versionCode != userVersion) {
            //if on before v2.0.4 and not first install, change the on click notifcation setting to the info screen
            if (userVersion <= 14 && userVersion != -1) {
                addIconNamesToDB();
                sharedPref.edit().putString("clickNotif", "info").apply();
            }

            //always restore notifications
            Intent localIntent = new Intent(this, NotificationBootService.class);
            localIntent.putExtra("action", "boot");
            startService(localIntent);
        }
        //update version code in shared prefs
        sharedPref.edit().putInt("VERSION_CODE", versionCode).apply();
    }

    @TargetApi(android.os.Build.VERSION_CODES.JELLY_BEAN)
    private void initializeGUI() {
        //setup the five icons
        ib1 = (ImageButton) findViewById(R.id.imageButton1);
        ImageButton ib2 = (ImageButton) findViewById(R.id.imageButton2);
        ImageButton ib3 = (ImageButton) findViewById(R.id.imageButton3);
        ImageButton ib4 = (ImageButton) findViewById(R.id.imageButton4);
        ImageButton ib5 = (ImageButton) findViewById(R.id.imageButton5);
        ImageButton ib6 = (ImageButton) findViewById(R.id.imageButton6);
        ImageButton ib7 = (ImageButton) findViewById(R.id.imageButton7);
        mrl_ib1 = (MaterialRippleLayout) findViewById(R.id.ripple_imageButton1);
        MaterialRippleLayout mrl_ib2 = (MaterialRippleLayout) findViewById(R.id.ripple_imageButton2);
        MaterialRippleLayout mrl_ib3 = (MaterialRippleLayout) findViewById(R.id.ripple_imageButton3);
        MaterialRippleLayout mrl_ib4 = (MaterialRippleLayout) findViewById(R.id.ripple_imageButton4);
        MaterialRippleLayout mrl_ib5 = (MaterialRippleLayout) findViewById(R.id.ripple_imageButton5);
        MaterialRippleLayout mrl_ib6 = (MaterialRippleLayout) findViewById(R.id.ripple_imageButton6);
        MaterialRippleLayout mrl_ib7 = (MaterialRippleLayout) findViewById(R.id.ripple_imageButton7);
        selectedRippleButtonId = mrl_ib1.getId();

        send_btn = (FloatingActionButton) findViewById(R.id.btn); //send button
        send_btn.setBackgroundColor(getResources().getColor(getResources().getIdentifier(accentColor, "color", getPackageName())));

        menu_btn = (ImageButton) findViewById(R.id.menuButton);
        alarm_btn = (ImageButton) findViewById(R.id.alarm_btn);
        ImageButton pallete_btn = (ImageButton) findViewById(R.id.pallete_btn);

        nm.cancel(id);
        id++;

        //set the click listener
        ib1.setOnClickListener(this);
        ib1.setOnLongClickListener(this);
        ib2.setOnClickListener(this);
        ib2.setOnLongClickListener(this);
        ib3.setOnClickListener(this);
        ib3.setOnLongClickListener(this);
        ib4.setOnClickListener(this);
        ib4.setOnLongClickListener(this);
        ib5.setOnClickListener(this);
        ib5.setOnLongClickListener(this);
        mrl_ib1.setOnClickListener(this);
        mrl_ib1.setOnLongClickListener(this);
        mrl_ib2.setOnClickListener(this);
        mrl_ib2.setOnLongClickListener(this);
        mrl_ib3.setOnClickListener(this);
        mrl_ib3.setOnLongClickListener(this);
        mrl_ib4.setOnClickListener(this);
        mrl_ib4.setOnLongClickListener(this);
        mrl_ib5.setOnClickListener(this);
        mrl_ib5.setOnLongClickListener(this);
        mrl_ib6.setOnClickListener(this);
        mrl_ib6.setOnLongClickListener(this);
        mrl_ib7.setOnClickListener(this);
        mrl_ib7.setOnLongClickListener(this);

        send_btn.setOnClickListener(this);
        menu_btn.setOnClickListener(this);
        alarm_btn.setOnClickListener(this);
        pallete_btn.setOnClickListener(this);

        ib1.setBackgroundColor(Color.TRANSPARENT);
        mrl_ib1.setRadius(1000);
        ib2.setBackgroundColor(Color.TRANSPARENT);
        ib3.setBackgroundColor(Color.TRANSPARENT);
        ib4.setBackgroundColor(Color.TRANSPARENT);
        ib5.setBackgroundColor(Color.TRANSPARENT);
        ib6.setBackgroundColor(Color.TRANSPARENT);
        ib7.setBackgroundColor(Color.TRANSPARENT);

        buttons = new HashSet<>(Arrays.asList(R.id.imageButton1, R.id.imageButton2, R.id.imageButton3, R.id.imageButton4, R.id.imageButton5, R.id.imageButton6, R.id.imageButton7));

        impossible_to_delete = false; //set setting for unable to delete notifications to false, will be checked before pressing send

        //set textviews and change font
        mainTitle = (TextView) findViewById(R.id.mainTitle);
        et = (EditText) findViewById(R.id.editText1);
        et_title = (EditText) findViewById(R.id.editText_title);

        if(bulletListFlag) {
            et.setText("• ");
            et.setSelection(et.getText().length());
            new ScaleInAnimation(send_btn).setDuration(250).animate();
            send_btn.setIcon(getResources().getDrawable(R.drawable.ic_send_white_36dp), false);
        }
        else if(numberedListFlag) {
            et.setText("1) ");
            et.setSelection(et.getText().length());
            new ScaleInAnimation(send_btn).setDuration(250).animate();
            send_btn.setIcon(getResources().getDrawable(R.drawable.ic_send_white_36dp), false);
        }
        else {
            et.setText("");
        }

        //if dark theme, set backgrounds for the two text boxes
        if(darkTheme){
            et.setHintTextColor(getResources().getColor(R.color.hint_foreground_material_dark));
            et_title.setHintTextColor(getResources().getColor(R.color.hint_foreground_material_dark));
            //lollipop+ get elevation

            if(CURRENT_ANDROID_VERSION >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                et.setBackgroundResource(R.drawable.rectangle_dark);
                et_title.setBackgroundResource(R.drawable.rectangle_dark);
            }
            else{
                et.setBackgroundResource(R.drawable.rectangle_pre_lollipop_dark);
                et_title.setBackgroundResource(R.drawable.rectangle_pre_lollipop_dark);
            }
        }
        else{
            et.setHintTextColor(getResources().getColor(R.color.hint_foreground_material_light));
            et_title.setHintTextColor(getResources().getColor(R.color.hint_foreground_material_light));
        }

        setEditTextTypeFaces();

        setImageButtonsBasedOffIconSelection();
    }

    private void setEditTextTypeFaces(){
        String fontString = sharedPreferences.getString("pref_font", "Roboto");
        Typeface font = Typeface.createFromAsset(getAssets(), "fonts/" + fontString + ".ttf");
        et.setTypeface(font);
        et_title.setTypeface(font);
        mainTitle.setTypeface(Typeface.createFromAsset(getAssets(), "ROBOTO-REGULAR.ttf"));
    }

    @TargetApi(android.os.Build.VERSION_CODES.JELLY_BEAN)
    private void initializeSettings() {
        if (CURRENT_ANDROID_VERSION >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
            pref_expand = sharedPreferences.getBoolean("pref_expand", true);
            pref_swipe = sharedPreferences.getBoolean("pref_swipe", false);
            pref_use_colored_icons = sharedPreferences.getBoolean("pref_use_colored_icons", false);

            pref_default_note_type = sharedPreferences.getString("defaultNoteType", "plain");
            switch(pref_default_note_type){
                case "bullet":
                    bulletListFlag = true;
                    numberedListFlag = false;
                    break;
                case "number":
                    numberedListFlag = true;
                    bulletListFlag = false;
                    break;
                default: //plain
                    bulletListFlag = false;
                    numberedListFlag = false;
                    break;
            }

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
        pref_large_icons = sharedPreferences.getBoolean("pref_large_icons", false); //icon size for notifications
        pref_keyboard = sharedPreferences.getBoolean("pref_keyboard", true); //launch keyboard on start
        pref_cursor = sharedPreferences.getBoolean("pref_cursor", false); //where to place the cursor on app start. true = Title. false = Note.

        iconColor = defaultIconsColor;
    }

    private void setProImageButtons() {
        buttons = new HashSet<>(Arrays.asList(R.id.imageButton1, R.id.imageButton2, R.id.imageButton3, R.id.imageButton4, R.id.imageButton5,
                R.id.imageButton6, R.id.imageButton7, R.id.imageButton8, R.id.imageButton9, R.id.imageButton10, R.id.imageButton11, R.id.imageButton12, R.id.imageButton13, R.id.imageButton14, R.id.imageButton15,
                R.id.imageButton16, R.id.imageButton17, R.id.imageButton18, R.id.imageButton19, R.id.imageButton20, R.id.imageButton21, R.id.imageButton22, R.id.imageButton23, R.id.imageButton24, R.id.imageButton25,
                R.id.imageButton26, R.id.imageButton27, R.id.imageButton28, R.id.imageButton29, R.id.imageButton30));

        ImageButton ib8 = (ImageButton) findViewById(R.id.imageButton8);
        ImageButton ib9 = (ImageButton) findViewById(R.id.imageButton9);
        ImageButton ib10 = (ImageButton) findViewById(R.id.imageButton10);
        ImageButton ib11 = (ImageButton) findViewById(R.id.imageButton11);
        ImageButton ib12 = (ImageButton) findViewById(R.id.imageButton12);
        ImageButton ib13 = (ImageButton) findViewById(R.id.imageButton13);
        ImageButton ib14 = (ImageButton) findViewById(R.id.imageButton14);
        ImageButton ib15 = (ImageButton) findViewById(R.id.imageButton15);
        ImageButton ib16 = (ImageButton) findViewById(R.id.imageButton16);
        ImageButton ib17 = (ImageButton) findViewById(R.id.imageButton17);
        ImageButton ib18 = (ImageButton) findViewById(R.id.imageButton18);
        ImageButton ib19 = (ImageButton) findViewById(R.id.imageButton19);
        ImageButton ib20 = (ImageButton) findViewById(R.id.imageButton20);
        ImageButton ib21 = (ImageButton) findViewById(R.id.imageButton21);
        ImageButton ib22 = (ImageButton) findViewById(R.id.imageButton22);
        ImageButton ib23 = (ImageButton) findViewById(R.id.imageButton23);
        ImageButton ib24 = (ImageButton) findViewById(R.id.imageButton24);
        ImageButton ib25 = (ImageButton) findViewById(R.id.imageButton25);
        ImageButton ib26 = (ImageButton) findViewById(R.id.imageButton26);
        ImageButton ib27 = (ImageButton) findViewById(R.id.imageButton27);
        ImageButton ib28 = (ImageButton) findViewById(R.id.imageButton28);
        ImageButton ib29 = (ImageButton) findViewById(R.id.imageButton29);
        ImageButton ib30 = (ImageButton) findViewById(R.id.imageButton30);

        MaterialRippleLayout mrl_ib8 = (MaterialRippleLayout) findViewById(R.id.ripple_imageButton8);
        MaterialRippleLayout mrl_ib9 = (MaterialRippleLayout) findViewById(R.id.ripple_imageButton9);
        MaterialRippleLayout mrl_ib10 = (MaterialRippleLayout) findViewById(R.id.ripple_imageButton10);
        MaterialRippleLayout mrl_ib11 = (MaterialRippleLayout) findViewById(R.id.ripple_imageButton11);
        MaterialRippleLayout mrl_ib12 = (MaterialRippleLayout) findViewById(R.id.ripple_imageButton12);
        MaterialRippleLayout mrl_ib13 = (MaterialRippleLayout) findViewById(R.id.ripple_imageButton13);
        MaterialRippleLayout mrl_ib14 = (MaterialRippleLayout) findViewById(R.id.ripple_imageButton14);
        MaterialRippleLayout mrl_ib15 = (MaterialRippleLayout) findViewById(R.id.ripple_imageButton15);
        MaterialRippleLayout mrl_ib16 = (MaterialRippleLayout) findViewById(R.id.ripple_imageButton16);
        MaterialRippleLayout mrl_ib17 = (MaterialRippleLayout) findViewById(R.id.ripple_imageButton17);
        MaterialRippleLayout mrl_ib18 = (MaterialRippleLayout) findViewById(R.id.ripple_imageButton18);
        MaterialRippleLayout mrl_ib19 = (MaterialRippleLayout) findViewById(R.id.ripple_imageButton19);
        MaterialRippleLayout mrl_ib20 = (MaterialRippleLayout) findViewById(R.id.ripple_imageButton20);
        MaterialRippleLayout mrl_ib21 = (MaterialRippleLayout) findViewById(R.id.ripple_imageButton21);
        MaterialRippleLayout mrl_ib22 = (MaterialRippleLayout) findViewById(R.id.ripple_imageButton22);
        MaterialRippleLayout mrl_ib23 = (MaterialRippleLayout) findViewById(R.id.ripple_imageButton23);
        MaterialRippleLayout mrl_ib24 = (MaterialRippleLayout) findViewById(R.id.ripple_imageButton24);
        MaterialRippleLayout mrl_ib25 = (MaterialRippleLayout) findViewById(R.id.ripple_imageButton25);
        MaterialRippleLayout mrl_ib26 = (MaterialRippleLayout) findViewById(R.id.ripple_imageButton26);
        MaterialRippleLayout mrl_ib27 = (MaterialRippleLayout) findViewById(R.id.ripple_imageButton27);
        MaterialRippleLayout mrl_ib28 = (MaterialRippleLayout) findViewById(R.id.ripple_imageButton28);
        MaterialRippleLayout mrl_ib29 = (MaterialRippleLayout) findViewById(R.id.ripple_imageButton29);
        MaterialRippleLayout mrl_ib30 = (MaterialRippleLayout) findViewById(R.id.ripple_imageButton30);

        ib8.setOnClickListener(this);
        ib8.setOnLongClickListener(this);
        ib9.setOnClickListener(this);
        ib9.setOnLongClickListener(this);
        ib10.setOnClickListener(this);
        ib10.setOnLongClickListener(this);
        ib11.setOnClickListener(this);
        ib11.setOnLongClickListener(this);
        ib12.setOnClickListener(this);
        ib12.setOnLongClickListener(this);
        ib13.setOnClickListener(this);
        ib13.setOnLongClickListener(this);
        ib14.setOnClickListener(this);
        ib14.setOnLongClickListener(this);
        ib15.setOnClickListener(this);
        ib15.setOnLongClickListener(this);
        ib16.setOnClickListener(this);
        ib16.setOnLongClickListener(this);
        ib17.setOnClickListener(this);
        ib17.setOnLongClickListener(this);
        ib18.setOnClickListener(this);
        ib18.setOnLongClickListener(this);
        ib19.setOnClickListener(this);
        ib19.setOnLongClickListener(this);
        ib20.setOnClickListener(this);
        ib20.setOnLongClickListener(this);
        ib21.setOnClickListener(this);
        ib21.setOnLongClickListener(this);
        ib22.setOnClickListener(this);
        ib22.setOnLongClickListener(this);
        ib23.setOnClickListener(this);
        ib23.setOnLongClickListener(this);
        ib24.setOnClickListener(this);
        ib24.setOnLongClickListener(this);
        ib25.setOnClickListener(this);
        ib25.setOnLongClickListener(this);
        ib26.setOnClickListener(this);
        ib26.setOnLongClickListener(this);
        ib27.setOnClickListener(this);
        ib27.setOnLongClickListener(this);
        ib28.setOnClickListener(this);
        ib28.setOnLongClickListener(this);
        ib29.setOnClickListener(this);
        ib29.setOnLongClickListener(this);
        ib30.setOnClickListener(this);
        ib30.setOnLongClickListener(this);

        mrl_ib8.setOnClickListener(this);
        mrl_ib8.setOnLongClickListener(this);
        mrl_ib9.setOnClickListener(this);
        mrl_ib9.setOnLongClickListener(this);
        mrl_ib10.setOnClickListener(this);
        mrl_ib10.setOnLongClickListener(this);
        mrl_ib11.setOnClickListener(this);
        mrl_ib11.setOnLongClickListener(this);
        mrl_ib12.setOnClickListener(this);
        mrl_ib12.setOnLongClickListener(this);
        mrl_ib13.setOnClickListener(this);
        mrl_ib13.setOnLongClickListener(this);
        mrl_ib14.setOnClickListener(this);
        mrl_ib14.setOnLongClickListener(this);
        mrl_ib15.setOnClickListener(this);
        mrl_ib15.setOnLongClickListener(this);
        mrl_ib16.setOnClickListener(this);
        mrl_ib16.setOnLongClickListener(this);
        mrl_ib17.setOnClickListener(this);
        mrl_ib17.setOnLongClickListener(this);
        mrl_ib18.setOnClickListener(this);
        mrl_ib18.setOnLongClickListener(this);
        mrl_ib19.setOnClickListener(this);
        mrl_ib19.setOnLongClickListener(this);
        mrl_ib20.setOnClickListener(this);
        mrl_ib20.setOnLongClickListener(this);
        mrl_ib21.setOnClickListener(this);
        mrl_ib21.setOnLongClickListener(this);
        mrl_ib22.setOnClickListener(this);
        mrl_ib22.setOnLongClickListener(this);
        mrl_ib23.setOnClickListener(this);
        mrl_ib23.setOnLongClickListener(this);
        mrl_ib24.setOnClickListener(this);
        mrl_ib24.setOnLongClickListener(this);
        mrl_ib25.setOnClickListener(this);
        mrl_ib25.setOnLongClickListener(this);
        mrl_ib26.setOnClickListener(this);
        mrl_ib26.setOnLongClickListener(this);
        mrl_ib27.setOnClickListener(this);
        mrl_ib27.setOnLongClickListener(this);
        mrl_ib28.setOnClickListener(this);
        mrl_ib28.setOnLongClickListener(this);
        mrl_ib29.setOnClickListener(this);
        mrl_ib29.setOnLongClickListener(this);
        mrl_ib30.setOnClickListener(this);
        mrl_ib30.setOnLongClickListener(this);

        ib8.setBackgroundColor(Color.TRANSPARENT);
        ib9.setBackgroundColor(Color.TRANSPARENT);
        ib10.setBackgroundColor(Color.TRANSPARENT);
        ib11.setBackgroundColor(Color.TRANSPARENT);
        ib12.setBackgroundColor(Color.TRANSPARENT);
        ib13.setBackgroundColor(Color.TRANSPARENT);
        ib14.setBackgroundColor(Color.TRANSPARENT);
        ib15.setBackgroundColor(Color.TRANSPARENT);
        ib16.setBackgroundColor(Color.TRANSPARENT);
        ib17.setBackgroundColor(Color.TRANSPARENT);
        ib18.setBackgroundColor(Color.TRANSPARENT);
        ib19.setBackgroundColor(Color.TRANSPARENT);
        ib20.setBackgroundColor(Color.TRANSPARENT);
        ib21.setBackgroundColor(Color.TRANSPARENT);
        ib22.setBackgroundColor(Color.TRANSPARENT);
        ib23.setBackgroundColor(Color.TRANSPARENT);
        ib24.setBackgroundColor(Color.TRANSPARENT);
        ib25.setBackgroundColor(Color.TRANSPARENT);
        ib26.setBackgroundColor(Color.TRANSPARENT);
        ib27.setBackgroundColor(Color.TRANSPARENT);
        ib28.setBackgroundColor(Color.TRANSPARENT);
        ib29.setBackgroundColor(Color.TRANSPARENT);
        ib30.setBackgroundColor(Color.TRANSPARENT);
    }

    private void handleSendText(Intent intent) { //for intents from other apps who want to share text to notey
        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (sharedText != null) {
            // Update ui to reflect text being shared
            et.setText(sharedText);
            et.setSelection(et.getText().length());

            noteTitle = intent.getStringExtra(Intent.EXTRA_SUBJECT);
            et_title.setText((noteTitle));
            et_title.setSelection(et_title.getText().length());
        }
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
        String TAG = "Notey_MainActivity";
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
                alarm_btn.setImageDrawable(getResources().getDrawable(R.drawable.ic_alarm_on_white_24dp));
                showAlarmToast();

            } else {
                new ScaleInAnimation(alarm_btn).setDuration(250).animate();
                alarm_btn.setImageDrawable(getResources().getDrawable(R.drawable.ic_alarm_add_white_24dp));
            }
        }
    }

    private void setUpMenu() {
        MenuInflater menuInflater = mPopupMenu.getMenuInflater();
        mPopupMenu.getMenu().clear(); //clear the menu so list items aren't duplicated
        menuInflater.inflate(R.menu.menu_pro, mPopupMenu.getMenu());

        mPopupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.plain_note:
                        bulletListFlag = false;
                        numberedListFlag = false;
                        et.setText("");
                        break;
                    case R.id.bullet_list:
                        bulletListFlag = true;
                        numberedListFlag = false;
                        et.setText("• ");
                        et.setSelection(et.getText().length()); //set cursor to end
                        break;
                    case R.id.numbered_list:
                        numberedListFlag = true;
                        bulletListFlag = false;
                        numberedListCounter = 1;
                        et.setText(numberedListCounter++ + ") ");
                        et.setSelection(et.getText().length()); //set cursor to end
                        break;
                    case R.id.settings:
                        settings_activity_flag = true; //flag so mainActivity UI does get reset after settings_jb_kk is called
                        Intent intent = new Intent(MainActivity.this, Settings.class);
                        startActivity(intent);
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

    private void showAlarmToast() {
        long diffInMillisec = Long.valueOf(alarm_time) - System.currentTimeMillis();
        long diffInSec = TimeUnit.MILLISECONDS.toSeconds(diffInMillisec);
        diffInSec /= 60;
        long minutes = diffInSec % 60;
        diffInSec /= 60;
        long hours = diffInSec % 24;
        diffInSec /= 24;
        long days = diffInSec;

        String dateString = "";
        if (days != 0) {
            if (hours != 0 && minutes != 0) { // 3 day, 2 hr, and 5 min
                if (days > 1)
                    dateString = Long.toString(days) + "" + getString(R.string.days) + ", ";
                else dateString = Long.toString(days) + "" + getString(R.string.day) + ", ";

                if (hours > 1)
                    dateString += Long.toString(hours) + "" + getString(R.string.hours) + ", and ";
                else dateString += Long.toString(hours) + "" + getString(R.string.hour) + ", and ";

                if (minutes > 1)
                    dateString += Long.toString(minutes) + "" + getString(R.string.minutes);
                else dateString += Long.toString(minutes) + "" + getString(R.string.minute);
            } else if (hours != 0 && minutes == 0) { // 3 day and 2 hr
                if (days > 1)
                    dateString = Long.toString(days) + "" + getString(R.string.days) + " and ";
                else dateString = Long.toString(days) + "" + getString(R.string.day) + " and ";

                if (hours > 1) dateString += Long.toString(hours) + "" + getString(R.string.hours);
                else dateString += Long.toString(hours) + "" + getString(R.string.hour);
            } else if (hours == 0 && minutes != 0) { // 3 day and 5 min
                if (days > 1)
                    dateString = Long.toString(days) + "" + getString(R.string.days) + " and ";
                else dateString = Long.toString(days) + "" + getString(R.string.day) + " and ";

                if (minutes > 1)
                    dateString += Long.toString(minutes) + "" + getString(R.string.minutes);
                else dateString += Long.toString(minutes) + "" + getString(R.string.minute);
            } else // 3 day
                if (days > 1) dateString = Long.toString(days) + "" + getString(R.string.days);
                else dateString = Long.toString(days) + "" + getString(R.string.day);
        } else {
            if (hours != 0 && minutes != 0) { // 2 hr and 5 min
                if (hours > 1)
                    dateString += Long.toString(hours) + "" + getString(R.string.hours) + " and ";
                else dateString += Long.toString(hours) + "" + getString(R.string.hour) + " and ";

                if (minutes > 1)
                    dateString += Long.toString(minutes) + "" + getString(R.string.minutes);
                else dateString += Long.toString(minutes) + "" + getString(R.string.minute);
            } else if (hours == 0 && minutes != 0) { // 5 min
                if (minutes > 1)
                    dateString = Long.toString(minutes) + "" + getString(R.string.minutes);
                else dateString = Long.toString(minutes) + "" + getString(R.string.minute);
            } else if (hours != 0 && minutes == 0) { // 2 hr
                if (hours > 1) dateString += Long.toString(hours) + "" + getString(R.string.hours);
                else dateString += Long.toString(hours) + "" + getString(R.string.hour);
            } else dateString = getString(R.string.less_than_a_minute); // less than 1 min
        }


        Toast.makeText(getApplicationContext(), getString(R.string.alarm_set_for) + " " +
                dateString + " " + getString(R.string.from_now), Toast.LENGTH_LONG).show();
    }

    /* changes were made from v2.0.3 to v2.0.4. this fudged up the icon numbers. a new column in the
    *   database was created called 'iconName' to prevent this issue from happening again.
    *   This converts to old icon's ints to the new ints and writes their proper name to the database.
    */
    private void addIconNamesToDB() {
        List<NoteyNote> allNoteys = null;
        try {
            allNoteys = db.getAllNoteys();
        } catch (RuntimeException e) {
            e.printStackTrace();
            Crashlytics.logException(e);
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

    private void themeStuffBeforeSetContentView() {
        //initialize theme preferences
        themeColor = sharedPreferences.getString("pref_theme_color", "md_blue_500");
        accentColor = sharedPreferences.getString("pref_accent_color", "md_pink_500");
        darkTheme = sharedPreferences.getBoolean("pref_theme_dark", false);
        defaultIconsColor = sharedPreferences.getString("pref_default_icon_color", "white");

        //set light/dark theme
        if (darkTheme) {
            super.setTheme(getResources().getIdentifier("AppBaseThemeDark_" + themeColor + "_Accent_" + accentColor, "style", getPackageName()));
        } else {
            super.setTheme(getResources().getIdentifier("AppBaseTheme_" + themeColor + "_Accent_" + accentColor, "style", getPackageName()));
        }
    }

    private void themeStuffAfterSetContentView() {
        //set color
        RelativeLayout r = (RelativeLayout) findViewById(R.id.layout_top);
        r.setBackgroundResource(getResources().getIdentifier(themeColor, "color", getPackageName()));

        TableRow tableRow = (TableRow) findViewById(R.id.tableRow1);
        hsv = (HorizontalScrollView) findViewById(R.id.horizontal_scroll_view);
        View divider = findViewById(R.id.divider);
        if (darkTheme) {
            tableRow.setBackgroundResource(R.color.md_grey_800);
            hsv.setBackgroundResource(R.color.md_grey_800);
            divider.setBackgroundColor(getResources().getColor(R.color.md_divider_white));
        } else {
            tableRow.setBackgroundResource(R.color.md_grey_700);
            hsv.setBackgroundResource(R.color.md_grey_700);
            divider.setBackgroundColor(getResources().getColor(R.color.md_divider_black));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // if settings_jb_kk activity was called, reset the ui
        if (settings_activity_flag) {
            initializeSettings();
            setEditTextTypeFaces();
            setImageButtonsBasedOffIconSelection();
            settings_activity_flag = false;
        }
        if (about_activity_flag) { // if about activity was called, check to see if the user had typed in the secret code and are now Pro. if yes, set up everything like a normal pro purchase.
            initializeSettings();
            if (justTurnedPro) {
                setUpMenu();
                justTurnedPro = false;
            }
            about_activity_flag = false;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("noteText", et.getText().toString());
        outState.putString("noteTitle", et_title.getText().toString());
        outState.putString("iconName", iconName);
        outState.putString("iconColor", iconColor);
    }
}


