package thomas.jonathan.notey;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

// Toast.makeText(getApplicationContext(), "**", Toast.LENGTH_SHORT).show();
public class InfoScreenActivity extends Activity implements OnClickListener {
    public static final int CURRENT_ANDROID_VERSION = Build.VERSION.SDK_INT;
    private TextView noteText;
    private ImageButton menu_btn, back_btn, edit_btn, copy_btn, share_btn, delete_btn;
    private PopupMenu mPopupMenu;
    private int imageButtonNumber, spinnerLocation, id;
    private String noteTitle, alarm_time = "";
    private String[] internetStrings = new String[]{"www.",".com", "http://", "https://"};

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.info_activity_dialog);

        AlarmService.releaseWakeUpWakelock(); // release the wakelock which turns on the device

        initializeGUI();
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.info_back_button) {
            finish();
        }
        else if (view.getId() == R.id.info_edit_button) {
            Intent editIntent = new Intent(this, MainActivity.class);
            editIntent.putExtra("editNotificationID", id);
            editIntent.putExtra("editNote", noteText.getText().toString());
            editIntent.putExtra("editLoc", spinnerLocation);
            editIntent.putExtra("editButton", imageButtonNumber);
            editIntent.putExtra("editTitle", noteTitle);
            editIntent.putExtra("editAlarm", alarm_time);
            startActivity(editIntent);
            finish();
        }
        else if (view.getId() == R.id.info_copy_button) {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("label", noteText.getText());
            clipboard.setPrimaryClip(clip);

            Toast.makeText(getApplicationContext(), getString(R.string.text_copied), Toast.LENGTH_SHORT).show();

        }
        else if (view.getId() == R.id.info_share_button) {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, noteText.getText());
            sendIntent.setType("text/plain");
            startActivity(sendIntent);
        }
        else if (view.getId() == R.id.info_delete_button) {
            Intent intent = new Intent(this, NotificationDismiss.class);
            intent.putExtra("NotificationID", id);
            sendBroadcast(intent);
            finish();
        }
        else if (view.getId() == R.id.info_menuButton) {
            menu_btn.postDelayed(new Runnable() {
                @Override
                public void run() {
                    menu_btn.setBackgroundColor(Color.TRANSPARENT);
                }
            }, 100);
            mPopupMenu.show();
        }
        // if a web link, open it in the browser
        else if(view.getId() == R.id.info_text){
            noteText.postDelayed(new Runnable() {
                @Override
                public void run() {
                    menu_btn.setBackgroundColor(Color.TRANSPARENT);
                }
            }, 100);
            String splitString[] = noteText.getText().toString().split("\\s");
            String url;
            for(String s : splitString) {
                url = s.toLowerCase();
                if(s.toLowerCase().startsWith("www.")) // add http:// to the beggining if it doesnt already
                    url = "http://" + s.toLowerCase();
                if(!s.toLowerCase().startsWith("www.") && !s.toLowerCase().startsWith("http"))
                    url = "http://www." + s.toLowerCase();
                if(s.endsWith(".")) // remove ending period if there is one
                    url = url.substring(0, url.length()-1).toLowerCase();

                try {
                    Intent viewIntent = new Intent("android.intent.action.VIEW", Uri.parse(url));
                    startActivity(viewIntent);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(getApplicationContext(), getString(R.string.bad_url), Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            }
            finish();
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void initializeGUI() {
        Intent i = this.getIntent();

        noteText = (TextView) findViewById(R.id.info_text);
        noteText.setMovementMethod(new ScrollingMovementMethod());
        TextView titleText = (TextView) findViewById(R.id.info_title_text);
        TextView mainTitle = (TextView) findViewById(R.id.info_mainTitle);

        if (i.hasExtra("infoNotificationID")) {
            int infoID = i.getExtras().getInt("infoNotificationID");
            int infoLoc = i.getExtras().getInt("infoLoc");
            int infoBtn = i.getExtras().getInt("infoButton");
            String infoNote = i.getExtras().getString("infoNote");
            String infoTitle = i.getExtras().getString("infoTitle");
            String infoAlarm = i.getExtras().getString("infoAlarm");

            id = infoID;
            spinnerLocation = infoLoc;
            imageButtonNumber = infoBtn;
            noteTitle = infoTitle;
            alarm_time = infoAlarm;

            noteText.setText(infoNote);

            //only show note if it's not empty
            if(infoNote.equals("")) {
                noteText.setVisibility(View.GONE);
            }
            else{
                noteText.setVisibility(View.VISIBLE);
                noteText.setText(infoNote);
            }

            //only show title if it's not equal to "Notey"
            if(infoTitle.equals(getString(R.string.app_name)) || infoTitle.equals("")) {
                titleText.setVisibility(View.GONE);
            }
            else{
                titleText.setVisibility(View.VISIBLE);
                titleText.setText(infoTitle);
            }

            for(String s : internetStrings)
                if(noteText.getText().toString().toLowerCase().contains(s)) {
                    noteText.setClickable(true);

                    //selectable background. Only for jelly bean and above
                    if(CURRENT_ANDROID_VERSION >= 16) {
                        int[] attr = new int[]{android.R.attr.selectableItemBackground};
                        TypedArray ta = obtainStyledAttributes(attr);
                        Drawable draw = ta.getDrawable(0); //index zero
                        noteText.setBackground(draw);
                    }

                    noteText.setOnClickListener(this);
                    break;
                }
                else {
                    noteText.setClickable(false);

                    //off set the selectable background from above
                    if(CURRENT_ANDROID_VERSION >= 16)
                        noteText.setBackgroundColor(Color.TRANSPARENT);
                    noteText.setOnClickListener(null);
                }
            restoreNotifications();
        }

        menu_btn = (ImageButton) findViewById(R.id.info_menuButton);
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

        setupLongClickListeners(); // Long click listeners for the five buttons. All will display a toast the summarize their action to the user

        //menu popup
        mPopupMenu = new PopupMenu(this, menu_btn);
        MenuInflater menuInflater = mPopupMenu.getMenuInflater();
        menuInflater.inflate(R.menu.menu, mPopupMenu.getMenu());

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
        Typeface roboto_light = Typeface.createFromAsset(getAssets(), "ROBOTO-LIGHT.TTF");
        Typeface roboto_reg = Typeface.createFromAsset(getAssets(), "ROBOTO-REGULAR.ttf");
        noteText.setTypeface(roboto_light);
        titleText.setTypeface(roboto_reg);
        mainTitle.setTypeface(roboto_light);
    }

    private void setupLongClickListeners(){
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

    //clicking on a notification kills it. this will restore it
    private void restoreNotifications(){
        Intent localIntent = new Intent(this, NotificationBootService.class);
        localIntent.putExtra("action", "boot");
        startService(localIntent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        restoreNotifications();

        NotificationDismiss.clearNotificationLED(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }
}


