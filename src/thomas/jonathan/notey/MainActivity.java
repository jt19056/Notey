package thomas.jonathan.notey;

import android.app.Activity;
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements OnClickListener {
    public static final int CURRENT_ANDROID_VERSION = Build.VERSION.SDK_INT;
    private static final int REQUEST_CODE = 1234;
    private Integer[] imageIconDatabase = {R.drawable.ic_check_grey600_36dp, R.drawable.ic_warning_grey600_36dp, R.drawable.ic_edit_grey600_36dp, R.drawable.ic_star_grey600_36dp, R.drawable.ic_whatshot_grey600_36dp};
    private NotificationManager nm;
    private ImageButton ib1, ib2, ib3, ib4, ib5, send_btn, menu_btn;
    private EditText et;
    private String[] spinnerPositionArray = {"0", "1", "2", "3", "4"};
    private Spinner spinner;
    private TextView mainTitle;
    private PopupMenu mPopupMenu;
    private int imageButtonNumber = 1, spinnerLocation = 0, id = (int) (Math.random() * 10000), priority;
    private boolean pref_expand, pref_swipe, impossible_to_delete = false, pref_enter, pref_voice, pref_share_action;
    private String clickNotif, pref_priority, noteTitle;
    private NoteyNote notey;
    public MySQLiteHelper db = new MySQLiteHelper(this);
    private RelativeLayout relativeLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity_dialog);

        notey = new NoteyNote(); //create a new Notey object

//        Field[] ID_Fields = R.drawable.class.getFields();
//        int[] resArray = new int[ID_Fields.length];
//        for(int i = 0; i < ID_Fields.length; i++)
//            try {
//                resArray[i] = ID_Fields[i].getInt(null);
//            } catch (IllegalArgumentException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            } catch (IllegalAccessException e) {
//                e.printStackTrace();
//            }
//
//        for(int i=0; i<resArray.length; i++){
//            String name = getResources().getResourceEntryName(resArray[i]);
//            writeToLog(Integer.toString(resArray[i]) + " - " + name);
//        }

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

            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                // TODO Auto-generated method stub
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
        // else if the send button is pressed
        else if (v.getId() == R.id.btn) {
            //check if user has made it not possible to remove notifications.
            // (this is a fail-safe in case they got out of the settings menu by pressing the 'home' key or some other way)
            if ((!clickNotif.equals("remove") && !clickNotif.equals("info")) && !pref_swipe && !pref_expand) {
                Toast.makeText(getApplicationContext(), getString(R.string.impossibleToDeleteAtSend), Toast.LENGTH_SHORT).show();
                impossible_to_delete = true;
            } else impossible_to_delete = false;

            //if empty and long press for voice input is false, go ahead with voice input
            if(et.getText().toString().equals("") && !pref_voice){
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
            else if (et.getText().toString().equals("") && pref_voice){ //text is empty but longpress is enabled
                Toast.makeText(getApplicationContext(), getString(R.string.hold_to_record), Toast.LENGTH_SHORT).show();
            }

            //if textbox isn't empty and it is possible to delete notifs, continue and create notification
            if (!et.getText().toString().equals("") && !impossible_to_delete) {
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

                //set the notey object
                notey.setId(id);
                notey.setNote(et.getText().toString());
                notey.setIcon(d);
                notey.setImgBtnNum(imageButtonNumber);
                notey.setSpinnerLoc(spinnerLocation);
                notey.setTitle(noteTitle);
                notey.setIconName(getResources().getResourceEntryName(d));

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
                            .setStyle(new NotificationCompat.BigTextStyle().bigText(note))
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
                            .setStyle(new NotificationCompat.BigTextStyle().bigText(note))
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
        }
    }

    private void setLayout() {
        //show keyboard at start
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        et.setText("");

        //keep layout where it belongs on screen
        relativeLayout = (RelativeLayout) findViewById(R.id.layout_bottom); //row containing the text box
        RelativeLayout.LayoutParams parms = (RelativeLayout.LayoutParams) relativeLayout.getLayoutParams();
        parms.addRule(RelativeLayout.BELOW, R.id.tableRow1);
        final int spinnerHeight = spinner.getLayoutParams().height;
        relativeLayout.getLayoutParams().height = spinnerHeight + (int) convertDpToPixel(10, this); //get spinner height + 10dp
        et.getLayoutParams().height = spinnerHeight; //set the row's height to that of the spinner's + 10dp

        //resizing the window when more/less text is added
        final int origHeight = relativeLayout.getLayoutParams().height;

        final RelativeLayout relativeLayoutCopy = relativeLayout;
        final EditText editTextCopy = et;

        //when text is added or deleted, count the num lines of text there are and adjust the size of the textbox accordingly.
        et.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                if(et.getText().toString().equals(""))
                    send_btn.setImageDrawable(getResources().getDrawable(R.drawable.ic_mic_grey600_36dp));
                else send_btn.setImageDrawable(getResources().getDrawable(R.drawable.ic_send_grey600_36dp));

                if (et.getLineCount() > 1) {
                    et.getLayoutParams().height = RelativeLayout.LayoutParams.WRAP_CONTENT;
                    relativeLayout.getLayoutParams().height = et.getLayoutParams().height;
                }
                else {
                    et.setLayoutParams(editTextCopy.getLayoutParams());
                    relativeLayout.setLayoutParams(relativeLayoutCopy.getLayoutParams());
                    int a = et.getLayoutParams().height;
                    int b = relativeLayout.getLayoutParams().height;
                    int c = 1;
                }
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) { /*do nothing*/  }

            public void onTextChanged(CharSequence s, int start, int before, int count) { /*do nothing*/ }
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

            id = editID;
            et.setText(editNote);
            et.setSelection(et.getText().length());
            spinnerLocation = editLoc;
            imageButtonNumber = editBtn;
            noteTitle = editTitle;

            spinner.setSelection(spinnerLocation);
            setSelectedButton(imageButtonNumber);
            setSpinnerPosition();

            //resize window for amount of text
            et.getLayoutParams().height = RelativeLayout.LayoutParams.WRAP_CONTENT;
            relativeLayout.getLayoutParams().height = RelativeLayout.LayoutParams.WRAP_CONTENT;

            //clicking on notifications kill them. this will restore all the notifcations
            Intent localIntent = new Intent(this, NotificationBootService.class);
            localIntent.putExtra("action", "boot");
            startService(localIntent);
        } else if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                handleSendText(i); // Handle text being sent
                //resize window for amount of text
                et.getLayoutParams().height = RelativeLayout.LayoutParams.WRAP_CONTENT;
                et.getLayoutParams().width = RelativeLayout.LayoutParams.WRAP_CONTENT;
                relativeLayout.getLayoutParams().height = RelativeLayout.LayoutParams.WRAP_CONTENT;
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
                editor.commit();
            }
            Intent localIntent = new Intent(this, NotificationBootService.class);
            localIntent.putExtra("action", "boot");
            startService(localIntent);
        }
        //update version code in shared prefs
        sharedPref.edit().putInt("VERSION_CODE", versionCode).apply();
    }

    private void initializeGUI() {
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

        impossible_to_delete = false; //set setting for unable to delete notifications to false, will be checked before pressing send

        //set textviews and change font
        mainTitle = (TextView) findViewById(R.id.mainTitle);
        et = (EditText) findViewById(R.id.editText1);
        Typeface font = Typeface.createFromAsset(getAssets(), "fonts/ROBOTO-LIGHT.TTF");
        et.setTypeface(font);
        mainTitle.setTypeface(font);
    }

    private void initializeSettings() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        if (CURRENT_ANDROID_VERSION >= 16) {
            pref_expand = sharedPreferences.getBoolean("pref_expand", true);
            pref_swipe = sharedPreferences.getBoolean("pref_swipe", false);

            pref_priority = sharedPreferences.getString("pref_priority", "normal");
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
    }

    private PendingIntent createOnDismissedIntent(Context context, int notificationId) {
        //we want to signal the NotificationDismiss receiver for removing notifications
        Intent intent = new Intent(context, NotificationDismiss.class);
        intent.putExtra("NotificationID", notificationId);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context.getApplicationContext(), notificationId, intent, 0);
        return pendingIntent;
    }

    private PendingIntent createEditIntent(String note, String title){
        Intent editIntent = new Intent(this, MainActivity.class);
        editIntent.putExtra("editNotificationID", id);
        editIntent.putExtra("editNote", note);
        editIntent.putExtra("editLoc", spinnerLocation);
        editIntent.putExtra("editButton", imageButtonNumber);
        editIntent.putExtra("editTitle", title);

        return PendingIntent.getActivity(getApplicationContext(), id, editIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private PendingIntent createInfoScreenIntent(String note, String title){
        Intent infoIntent = new Intent(this, InfoScreenActivity.class);
        infoIntent.putExtra("infoNotificationID", id);
        infoIntent.putExtra("infoNote", note);
        infoIntent.putExtra("infoLoc", spinnerLocation);
        infoIntent.putExtra("infoButton", imageButtonNumber);
        infoIntent.putExtra("infoTitle", title);
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
        float px = dp * (metrics.densityDpi / 160f);
        return px;
    }

    private void startVoiceRecognitionActivity(){
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.speak_now));
        startActivityForResult(intent, REQUEST_CODE);
    }


    // Handles the results from the voice recognition activity.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK)
        {
            // get the voice input and put it into the text field
            ArrayList<String> textMatchList = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (!textMatchList.isEmpty()) {
                String input = textMatchList.get(0);
                et.setText(input);
                et.setSelection(et.getText().length());
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
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

    public void writeToLog(String data) {
        FileOutputStream fos;
        try {
            final File dir = new File("/sdcard/Android/data/thomas.jonathan.notey/");
            if (!dir.exists()) {
                dir.mkdirs();
            }

            final File myFile = new File(dir, "logs.txt");

            if (!myFile.exists()) {
                myFile.createNewFile();
            }

            String seperator = "...\n";
            fos = new FileOutputStream(myFile, true);
            fos.write(data.getBytes());
            fos.write(seperator.getBytes());
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
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
        initializeSettings();
        checkLongPressVoiceInput();
        super.onResume();
    }

}


