package thomas.jonathan.notey;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
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
import android.net.Uri;
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

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

// Toast.makeText(getApplicationContext(), "**", Toast.LENGTH_SHORT).show();
public class InfoScreenActivity extends Activity implements OnClickListener {

    private TextView noteText;
    private ImageButton menu_btn, back_btn, edit_btn, copy_btn, share_btn, delete_btn;
    private PopupMenu mPopupMenu;
    private int imageButtonNumber, spinnerLocation, id;
    private String noteTitle;
    private String[] internetStrings = new String[]{"www.",".com", "http://", "https://"};

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.info_activity_dialog);

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
            editIntent.putExtra("editNote", noteText.getText());
            editIntent.putExtra("editLoc", spinnerLocation);
            editIntent.putExtra("editButton", imageButtonNumber);
            editIntent.putExtra("editTitle", noteTitle);
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

    private void initializeGUI() {
        Intent i = this.getIntent();

        noteText = (TextView) findViewById(R.id.info_text);
        TextView mainTitle = (TextView) findViewById(R.id.info_mainTitle);

        if (i.hasExtra("infoNotificationID")) {
            int infoID = i.getExtras().getInt("infoNotificationID");
            int infoLoc = i.getExtras().getInt("infoLoc");
            int infoBtn = i.getExtras().getInt("infoButton");
            String infoNote = i.getExtras().getString("infoNote");
            String infoTitle = i.getExtras().getString("infoTitle");

            id = infoID;
            spinnerLocation = infoLoc;
            imageButtonNumber = infoBtn;
            noteTitle = infoTitle;
            noteText.setText(infoNote);

            for(String s : internetStrings)
                if(noteText.getText().toString().toLowerCase().contains(s)) {
                    noteText.setClickable(true);
                    noteText.setOnClickListener(this);
                    break;
                }
                else {
                    noteText.setClickable(false);
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
        Typeface font = Typeface.createFromAsset(getAssets(), "fonts/ROBOTO-LIGHT.TTF");
        noteText.setTypeface(font);
        mainTitle.setTypeface(font);
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
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    //hardware button click listener. for Menu key
    public boolean onKeyUp(View v, int keyCode, KeyEvent event) {
        //if hardware menu button, activate the menu button at the top of the app.
        if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                (keyCode == KeyEvent.KEYCODE_MENU)) {
            menu_btn.performClick();
        }
        return false;
    }
}


