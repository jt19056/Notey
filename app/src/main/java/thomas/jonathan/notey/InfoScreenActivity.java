package thomas.jonathan.notey;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.rey.material.widget.TextView;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

// Toast.makeText(getApplicationContext(), "**", Toast.LENGTH_SHORT).show();
public class InfoScreenActivity extends Activity implements OnClickListener {
    public static final int CURRENT_ANDROID_VERSION = Build.VERSION.SDK_INT;
    private TextView noteText;
    private ImageButton back_btn;
    private ImageButton edit_btn;
    private ImageButton copy_btn;
    private ImageButton share_btn;
    private ImageButton delete_btn;
    private PopupMenu mPopupMenu;
    private PendingIntent alarmPendingIntent;
    private int imageButtonNumber, spinnerLocation, id, repeat_time;
    private String noteTitle, alarm_time = "";
    private String[] internetStrings = new String[]{"www.", ".com", "http://", "https://"};
    private Notification notification;
    private SharedPreferences sharedPreferences;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        themeStuffBeforeSetContentView();
        setContentView(R.layout.info_activity_dialog);
        themeStuffAfterSetContentView();

        AlarmService.releaseWakeUpWakelock(); // release the wakelock which turns on the device

        initializeGUI();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // wait a second then cancel the notification created to vibrate the device
        try {
            Thread.sleep(1000);
        } catch(InterruptedException e) {
            Thread.currentThread().interrupt();
            Crashlytics.logException(e);
        }
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.cancel(AlarmService.VIBRATE_NOTIFICATION_ID);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.info_back_button) {
            finish();
        } else if (view.getId() == R.id.info_edit_button) {
            Intent editIntent = new Intent(this, MainActivity.class);
            editIntent.putExtra("editNotificationID", id);
            editIntent.putExtra("editNote", noteText.getText().toString());
            editIntent.putExtra("editLoc", spinnerLocation);
            editIntent.putExtra("editButton", imageButtonNumber);
            editIntent.putExtra("editTitle", noteTitle);
            editIntent.putExtra("editAlarm", alarm_time);
            editIntent.putExtra("editRepeat", repeat_time);
            editIntent.putExtra("editAlarmPendingIntent", alarmPendingIntent);
            startActivity(editIntent);
            finish();
        } else if (view.getId() == R.id.info_copy_button) {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip;
            //if no note text, copy the title
            if(noteText.getText() == null || noteText.getText().toString().isEmpty())
                clip = ClipData.newPlainText("label", noteTitle);
            else clip = ClipData.newPlainText("label", noteText.getText());
            clipboard.setPrimaryClip(clip);

            Toast.makeText(getApplicationContext(), getString(R.string.text_copied), Toast.LENGTH_SHORT).show();

        } else if (view.getId() == R.id.info_share_button) {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, noteText.getText().toString());
            sendIntent.setType("text/plain");
            startActivity(sendIntent);
        } else if (view.getId() == R.id.info_delete_button) {
            Intent intent = new Intent(this, NotificationDismiss.class);
            intent.putExtra("NotificationID", id);
            sendBroadcast(intent);
            finish();
        } else if (view.getId() == R.id.info_menuButton) {
            mPopupMenu.show();
        }
        // if a web link, open it in the browser
        else if (view.getId() == R.id.info_note_text) {
            String splitString[] = noteText.getText().toString().split("\\s");
            String url;
            for (String s : splitString) {
                url = s.toLowerCase();
                if (s.toLowerCase().startsWith("www.")) // add http:// to the beggining if it doesnt already
                    url = "http://" + s.toLowerCase();
                if (!s.toLowerCase().startsWith("www.") && !s.toLowerCase().startsWith("http"))
                    url = "http://www." + s.toLowerCase();
                if (s.endsWith(".")) // remove ending period if there is one
                    url = url.substring(0, url.length() - 1).toLowerCase();

                try {
                    Intent viewIntent = new Intent("android.intent.action.VIEW", Uri.parse(url));
                    startActivity(viewIntent);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(getApplicationContext(), getString(R.string.bad_url), Toast.LENGTH_SHORT).show();
                    Crashlytics.logException(e);
                    e.printStackTrace();
                }
            }
            finish();
        } else if (view.getId() == R.id.info_alarm_text) {
            Intent editIntent = new Intent(this, MainActivity.class);
            editIntent.putExtra("editNotificationID", id);
            editIntent.putExtra("editNote", noteText.getText().toString());
            editIntent.putExtra("editLoc", spinnerLocation);
            editIntent.putExtra("editButton", imageButtonNumber);
            editIntent.putExtra("editTitle", noteTitle);
            editIntent.putExtra("editAlarm", alarm_time);
            editIntent.putExtra("editRepeat", repeat_time);
            editIntent.putExtra("editAlarmPendingIntent", alarmPendingIntent);
            editIntent.putExtra("doEditAlarmActivity", true);
            startActivity(editIntent);
            finish();
        } else if (view.getId() == R.id.info_repeat_text) {
            Intent editIntent = new Intent(this, MainActivity.class);
            editIntent.putExtra("editNotificationID", id);
            editIntent.putExtra("editNote", noteText.getText().toString());
            editIntent.putExtra("editLoc", spinnerLocation);
            editIntent.putExtra("editButton", imageButtonNumber);
            editIntent.putExtra("editTitle", noteTitle);
            editIntent.putExtra("editAlarm", alarm_time);
            editIntent.putExtra("editRepeat", repeat_time);
            editIntent.putExtra("editAlarmPendingIntent", alarmPendingIntent);
            editIntent.putExtra("doEditAlarmActivity", true);
            startActivity(editIntent);
            finish();
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void initializeGUI() {
        Intent i = this.getIntent();

        noteText = (TextView) findViewById(R.id.info_note_text);
        TextView alarmText = (TextView) findViewById(R.id.info_alarm_text);
        TextView repeatText = (TextView) findViewById(R.id.info_repeat_text);
        noteText.setMovementMethod(new ScrollingMovementMethod());
        TextView titleText = (TextView) findViewById(R.id.info_title_text);
        titleText.setMovementMethod(new ScrollingMovementMethod());
        TextView mainTitle = (TextView) findViewById(R.id.info_mainTitle);

        ImageView imageView = (ImageView) findViewById(R.id.info_imageView);

        if (i.hasExtra("infoNotificationID")) {
            int infoID = i.getExtras().getInt("infoNotificationID");
            int infoLoc = i.getExtras().getInt("infoLoc");
            int infoBtn = i.getExtras().getInt("infoButton");
            String infoNote = i.getExtras().getString("infoNote");
            String infoTitle = i.getExtras().getString("infoTitle");
            String infoAlarm = i.getExtras().getString("infoAlarm");
            int infoRepeat = i.getExtras().getInt("infoRepeat");
            PendingIntent infoAlarmPI = (PendingIntent) i.getExtras().get("infoAlarmPendingIntent");

            Object notifObj = i.getExtras().get("infoNotif");
            if (notifObj != null)
            {
                String notifString = notifObj.toString();

                if (notifString != null && !notifString.equals("")) {
                    notification = (Notification) i.getExtras().get("infoNotif");
                }
            }

            id = infoID;
            spinnerLocation = infoLoc;
            imageButtonNumber = infoBtn;
            noteTitle = infoTitle;
            alarm_time = infoAlarm;
            repeat_time = infoRepeat;
            alarmPendingIntent = infoAlarmPI;

            noteText.setText(infoNote);

            //only show note if it's not empty
            if (infoNote.equals("")) {
                noteText.setVisibility(View.GONE);
            } else {
                noteText.setVisibility(View.VISIBLE);
                noteText.setText(infoNote);
            }

            //only show alarm if it's not empty
            if (infoAlarm == null || infoAlarm.equals("")) {
                alarmText.setVisibility(View.GONE);
                alarmText.setClickable(false);
                alarmText.setOnClickListener(null);
                repeatText.setVisibility(View.GONE);
                repeatText.setClickable(false);
                repeatText.setOnClickListener(null);
            } else {
                DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.DEFAULT, Locale.getDefault());
                DateFormat timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault());
                alarmText.setVisibility(View.VISIBLE);
                Date date = new Date(Long.valueOf(infoAlarm));
                alarmText.setText(getString(R.string.alarm) + ": " + dateFormat.format(date) + ", " + timeFormat.format(date));
                alarmText.setClickable(true);
                alarmText.setOnClickListener(this);
                //whether or not to display the repeat time text
                if(repeat_time != 0){
                    repeatText.setVisibility(View.VISIBLE);
                    repeatText.setClickable(true);
                    repeatText.setOnClickListener(this);
                    //minutes
                    if(repeat_time <= 45) {
                        repeatText.setText(getString(R.string.repeat_every) + repeat_time + getString(R.string.minutes));
                    }
                    //hours
                    else if (repeat_time >= 60 && repeat_time <= 720){
                        int hours = repeat_time/60;
                        //display "hour" or "hours" for the toast
                        if(hours == 1)
                            repeatText.setText(getString(R.string.repeat_every) + hours + getString(R.string.hour));
                        else repeatText.setText(getString(R.string.repeat_every) + hours + getString(R.string.hours));

                    }
                    //days
                    else if (repeat_time >= 1440){
                        int days = repeat_time/60/24;
                        //display "day" or "days" for the toast
                        if(days == 1)
                            repeatText.setText(getString(R.string.repeat_every) + days + getString(R.string.day));
                        else repeatText.setText(getString(R.string.repeat_every) + days + getString(R.string.days));

                    }
                    //else idk what happened, just hide the text box
                    else {
                        repeatText.setVisibility(View.GONE);
                        repeatText.setClickable(false);
                        repeatText.setOnClickListener(null);
                    }
                } else {
                    repeatText.setVisibility(View.GONE);
                    repeatText.setClickable(false);
                    repeatText.setOnClickListener(null);
                }
            }

            //if dark theme, set backgrounds for the two text boxes
            if(MainActivity.darkTheme){
                //lollipop+ get elevation
                if(CURRENT_ANDROID_VERSION >= Build.VERSION_CODES.LOLLIPOP) {
                    titleText.setBackgroundResource(R.drawable.rectangle_dark);
                    noteText.setBackgroundResource(R.drawable.rectangle_dark);
                    alarmText.setBackgroundResource(R.drawable.rectangle_dark);
                    repeatText.setBackgroundResource(R.drawable.rectangle_dark);
                } else {
                    titleText.setBackgroundResource(R.drawable.rectangle_pre_lollipop_dark);
                    titleText.setPadding(8, 8, 8, 8);
                    noteText.setBackgroundResource(R.drawable.rectangle_pre_lollipop_dark);
                    noteText.setPadding(8, 8, 8, 8);
                    alarmText.setBackgroundResource(R.drawable.rectangle_pre_lollipop_dark);
                    alarmText.setPadding(8, 8, 8, 8);
                    repeatText.setBackgroundResource(R.drawable.rectangle_pre_lollipop_dark);
                    repeatText.setPadding(8, 8, 8, 8);
                }
            }

            //show icon
            MySQLiteHelper db = new MySQLiteHelper(this);
            boolean pref_use_colored_icons = sharedPreferences.getBoolean("pref_use_colored_icons", false);
            if (db.checkIfExist(infoID)) {
                if(CURRENT_ANDROID_VERSION < 21 || pref_use_colored_icons) { //for below lollipop, show the colored icon on a grey circle.  OR if user wants to use the colored icons
                    NoteyNote n = db.getNotey(infoID);
                    imageView.setImageResource(getResources().getIdentifier(n.getIconName().replace("24", "36"), "drawable", getPackageName())); //use 36dp icon instead of 24
                    imageView.setBackgroundResource(R.drawable.circle_grey);
                }else{ //for lollipop, show the white icon on a colored circle
                    NoteyNote n = db.getNotey(infoID);
                    String icon = n.getIconName().replace("24", "36"); //use 36dp icon instead of 24

                    String iconColor, replaceMe;
                    if(n.getIconName().contains("shopping") || n.getIconName().contains("note") || n.getIconName().contains("attach") || n.getIconName().contains("brightness") ||
                            n.getIconName().contains("directions") || n.getIconName().contains("flash") || n.getIconName().contains("local") || n.getIconName().contains("music") ||
                            n.getIconName().contains("cake")) {
                        iconColor = n.getIconName().split("_")[3];
                    }
                    else if (n.getIconName().contains("format")){
                        iconColor = n.getIconName().split("_")[4];
                    }
                    else{
                        iconColor = n.getIconName().split("_")[2];
                    }

                    //light_green or deep_orange special cases
                    if(iconColor.contains("light")) iconColor += "_green";
                    else if(iconColor.contains("deep")) iconColor += "_orange";

                    //show white icon with colored background
                    if(icon.contains("white")) { // white icon with gray background for white icons
                        imageView.setImageResource(getResources().getIdentifier(icon, "drawable", getPackageName()));
                        imageView.setBackgroundResource(R.drawable.circle_grey);
                    }else  {
                        imageView.setImageResource(getResources().getIdentifier(icon.replace(iconColor, "white"), "drawable", getPackageName()));
                        imageView.setBackgroundResource(getResources().getIdentifier("circle_" + iconColor, "drawable", getPackageName()));
                    }
                }
            }

            //only show title if it's not equal to "Notey"
            if (infoTitle.equals(getString(R.string.app_name)) || infoTitle.equals("")) {
                titleText.setVisibility(View.GONE);
            } else {
                titleText.setVisibility(View.VISIBLE);
                titleText.setText(infoTitle);
            }

            for (String s : internetStrings)
                if (noteText.getText().toString().toLowerCase().contains(s)) {
                    noteText.setClickable(true);

//                    //selectable background. Only for jelly bean and above
//                    if (CURRENT_ANDROID_VERSION >= 16) {
//                        int[] attr = new int[]{android.R.attr.selectableItemBackground};
//                        TypedArray ta = obtainStyledAttributes(attr);
//                        Drawable draw = ta.getDrawable(0); //index zero
//                        noteText.setBackground(draw);
//                    }

                    noteText.setOnClickListener(this);
                    break;
                }
                else {
                    noteText.setClickable(false);

                    //off set the selectable background from above
//                    if (CURRENT_ANDROID_VERSION >= 16)
//                        noteText.setBackgroundColor(Color.TRANSPARENT);
                    noteText.setOnClickListener(null);
                }
        }

        ImageButton menu_btn = (ImageButton) findViewById(R.id.info_menuButton);
        back_btn = (ImageButton) findViewById(R.id.info_back_button);
        edit_btn = (ImageButton) findViewById(R.id.info_edit_button);
        copy_btn = (ImageButton) findViewById(R.id.info_copy_button);
        share_btn = (ImageButton) findViewById(R.id.info_share_button);
        delete_btn = (ImageButton) findViewById(R.id.info_delete_button);

        menu_btn.setOnClickListener(this);
        back_btn.setOnClickListener(this);
        edit_btn.setOnClickListener(this);
        copy_btn.setOnClickListener(this);
        share_btn.setOnClickListener(this);
        delete_btn.setOnClickListener(this);

        //set button colors to grey500 instead of grey600 for dark theme
        if(MainActivity.darkTheme){
            back_btn.setColorFilter(Color.argb(255, 158, 158, 158));
            edit_btn.setColorFilter(Color.argb(255, 158, 158, 158));
            copy_btn.setColorFilter(Color.argb(255, 158, 158, 158));
            share_btn.setColorFilter(Color.argb(255, 158, 158, 158));
            delete_btn.setColorFilter(Color.argb(255, 158, 158, 158));
        }

        setupLongClickListeners(); // Long click listeners for the five buttons. All will display a toast to summarize their action to the user

        //menu popup
        mPopupMenu = new PopupMenu(this, menu_btn);
        MenuInflater menuInflater = mPopupMenu.getMenuInflater();
        mPopupMenu.getMenu().clear(); //clear the menu so list items aren't duplicated
        menuInflater.inflate(R.menu.menu_pro, mPopupMenu.getMenu()); //default pro menu because I don't want to deal with the IAP in this class

        mPopupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.settings:
                        Intent intent = new Intent(InfoScreenActivity.this, Settings.class);
                        startActivity(intent);
                        break;
                    case R.id.about:
                        Intent i = new Intent(InfoScreenActivity.this, About.class);
                        startActivity(i);
                        break;
                    default:
                        return true;
                }
                return true;
            }
        });

        //set font
        String fontString = sharedPreferences.getString("pref_font", "Roboto");
        Typeface font = Typeface.createFromAsset(getAssets(), "fonts/" + fontString + ".ttf");
        Typeface fontBold = Typeface.createFromAsset(getAssets(), "fonts/" + fontString + "-Bold.ttf");
        noteText.setTypeface(font);
        alarmText.setTypeface(font);
        repeatText.setTypeface(font);
        titleText.setTypeface(fontBold);
        mainTitle.setTypeface(Typeface.createFromAsset(getAssets(), "ROBOTO-REGULAR.ttf"));
    }

    private void setupLongClickListeners() {
        back_btn.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(getApplicationContext(), getString(R.string.go_back), Toast.LENGTH_SHORT).show();
                return true;
            }
        });
        edit_btn.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(getApplicationContext(), getString(R.string.edit), Toast.LENGTH_SHORT).show();
                return true;
            }
        });
        copy_btn.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(getApplicationContext(), getString(R.string.copy), Toast.LENGTH_SHORT).show();
                return true;
            }
        });
        share_btn.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(getApplicationContext(), getString(R.string.share), Toast.LENGTH_SHORT).show();
                return true;
            }
        });
        delete_btn.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(getApplicationContext(), getString(R.string.remove), Toast.LENGTH_SHORT).show();
                return true;
            }
        });
    }

    private void themeStuffBeforeSetContentView(){
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        //initialize theme preferences
        MainActivity.themeColor = sharedPreferences.getString("pref_theme_color", "md_blue_500");
        MainActivity.accentColor = sharedPreferences.getString("pref_accent_color", "md_pink_500");
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
        RelativeLayout r = (RelativeLayout) findViewById(R.id.info_layout_top);
        r.setBackgroundResource(getResources().getIdentifier(MainActivity.themeColor, "color", getPackageName()));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //stop the led flashing
        NotificationDismiss.clearNotificationLED(this);

        //stop the ringtone if there is one going off
        if(notification != null){
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            notificationManager.cancel(AlarmService.LED_SOUND_ID);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }
}


