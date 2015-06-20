package thomas.jonathan.notey;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;

import com.google.gson.Gson;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

/* This service is used to restore all the notifications stored in the database*/
public class NotificationBootService extends IntentService {
    final int CURRENT_ANDROID_VERSION = Build.VERSION.SDK_INT;
    int priority, repeatTime = 0;
    boolean pref_expand, pref_swipe, pref_share_action, pref_use_colored_icons;
    String clickNotif, intentType, pref_priority, noteString;
    PendingIntent alarmPendingIntent;

    public NotificationBootService() {
        super("NotificationService");
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onStart(Intent intent, int startId) {
        MySQLiteHelper db = new MySQLiteHelper(this);
        List<NoteyNote> allNoteys = db.getAllNoteys();

        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        intentType = intent.getExtras().getString("action");

        if (intentType == null) return;

        //loop through noteys and re-create the alarms
        if (intentType.equals("boot_alarm")) {
            for (NoteyNote n : allNoteys) {
                if (n.getAlarm() != null) {
                    // intent for service to launch info screen when alarm goes off
                    Intent myIntent = new Intent(this, AlarmService.class);
                    Bundle bundle = new Bundle();
                    bundle.putInt("alarmID", n.getId());
                    myIntent.putExtras(bundle);

                   alarmPendingIntent = PendingIntent.getService(this, n.getId(), myIntent, PendingIntent.FLAG_UPDATE_CURRENT);

                    //set alarm
                    AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
                    SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
                    repeatTime = sp.getInt("repeat" + n.getId(), 0);

                    //set repeating alarm or set regular alarm
                    if (repeatTime != 0) {
                        // check the sharedPrefs for the check box to wake up the device
                        if (PreferenceManager.getDefaultSharedPreferences(getBaseContext()).getBoolean("wake" + Integer.toString(n.getId()), true))
                            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, Long.valueOf(n.getAlarm()), repeatTime * 1000 * 60, alarmPendingIntent);
                        else
                            alarmManager.setRepeating(AlarmManager.RTC, Long.valueOf(n.getAlarm()), repeatTime * 1000 * 60, alarmPendingIntent);
                    } else { //set regualar alarm

                        //if the alarm hasn't past then continue
                        if(Long.valueOf(n.getAlarm()) > System.currentTimeMillis()) {
                            // check the sharedPrefs for the check box to wake up the device
                            if (PreferenceManager.getDefaultSharedPreferences(getBaseContext()).getBoolean("wake" + Integer.toString(n.getId()), true))
                                alarmManager.set(AlarmManager.RTC_WAKEUP, Long.valueOf(n.getAlarm()), alarmPendingIntent);
                            else
                                alarmManager.set(AlarmManager.RTC, Long.valueOf(n.getAlarm()), alarmPendingIntent);
                        }
                    }
                }
            }
        } else if (intentType.equals("boot")) {

            //Settings stuff
            initializeSettings();

            //show shortcut notification if settings say so
            if (PreferenceManager.getDefaultSharedPreferences(getBaseContext()).getBoolean("pref_shortcut", false)) {
                Notification n;
                if(CURRENT_ANDROID_VERSION >= 21 ) { //if > lollipop
                    n = new NotificationCompat.Builder(this)
                            .setContentTitle(getString(R.string.app_name))
                            .setContentText(getString(R.string.quick_note))
                            .setSmallIcon(R.drawable.ic_new_note_white)
                            .setColor(getResources().getColor(R.color.grey_500))
                            .setOngoing(true)
                            .setContentIntent(PendingIntent.getActivity(getApplicationContext(), MainActivity.SHORTCUT_NOTIF_ID, new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT))
                            .setAutoCancel(false)
                            .setPriority(Notification.PRIORITY_MIN)
                            .build();
                }else{
                    n = new NotificationCompat.Builder(this)
                            .setContentTitle(getString(R.string.app_name))
                            .setContentText(getString(R.string.quick_note))
                            .setSmallIcon(R.drawable.ic_launcher_dashclock)
                            .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_new_note))
                            .setOngoing(true)
                            .setContentIntent(PendingIntent.getActivity(getApplicationContext(), MainActivity.SHORTCUT_NOTIF_ID, new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT))
                            .setAutoCancel(false)
                            .setPriority(Notification.PRIORITY_MIN)
                            .build();
                }
                nm.notify(MainActivity.SHORTCUT_NOTIF_ID, n);
            }

            for (NoteyNote n : allNoteys) {

                //for users on < v2.0.3, they didn't have 'title' column in db
                if (n.getTitle() == null) {
                    n.setTitle(getString(R.string.app_name));
                    db.updateNotey(n);
                }

                noteString = n.getNote(); //temp note text to display alarm
                if (n.getAlarm() != null) {
                    Date date = new Date(Long.valueOf(n.getAlarm()));
                    // add the alarm info to the notification
                    noteString += "\n" + getString(R.string.alarm) + ": " + MainActivity.format_short_date.format(date) + ", " + MainActivity.format_short_time.format(date);
                }

                String tickerText;  //if title is there, set ticker to title. otherwise set it to the note
                if (n.getTitle().equals("") || n.getTitle().equals(getString(R.string.app_name)))
                    tickerText = n.getNote();
                else tickerText = n.getTitle();


                //converts the iconName to int. if there isn't an iconName, default to check white
                int ico;
                try {
                    ico = getResources().getIdentifier(n.getIconName(), "drawable", getPackageName());
                } catch (Exception e) {
                    ico = R.drawable.ic_check_white_36dp;
                    e.printStackTrace();
                }

                //set background color if on lollipop or above
                int color = getResources().getColor(R.color.md_grey_500); // grey, for grey background with white icons
                //get icon color
                String iconColor;
                if(n.getIconName().contains("shopping") || n.getIconName().contains("note") || n.getIconName().contains("attach") || n.getIconName().contains("brightness") ||
                        n.getIconName().contains("directions") || n.getIconName().contains("flash") || n.getIconName().contains("local") || n.getIconName().contains("music")) {
                    iconColor = n.getIconName().split("_")[3];
                }
                else{
                    iconColor = n.getIconName().split("_")[2];
                }
                //if not greater than lollipop set the colors. otherwise, use white and set the background icon color\
                //get string color for lollipop notification background color
                // converts ex. red -> md_red_A400  or  blue -> md_blue_500
                if(CURRENT_ANDROID_VERSION >= 21 && !pref_use_colored_icons) {
                            //light_green or deep_orange special cases
                    if(iconColor.contains("light")) iconColor += "_green";
                    else if(iconColor.contains("deep")) iconColor += "_orange";

                    String colorArray[] = getResources().getStringArray(R.array.icon_colors_array_pro);

                    int c = Arrays.asList(colorArray).indexOf(iconColor);

                    String colorNames[] = getResources().getStringArray(R.array.icon_color_names_pro);

                    String colorString = Arrays.asList(colorNames).get(c);

                    color = getResources().getColor(getResources().getIdentifier(colorString, "color", getPackageName()));
                }
                else if(CURRENT_ANDROID_VERSION >= 21 && iconColor.equals("white")){ //for lollipop white icons, use white icon with grey background so they can see it
                    pref_use_colored_icons = false; //make them use the lollipop circle background
                    color = getResources().getColor(R.color.md_grey_500);
                }

                repeatTime = PreferenceManager.getDefaultSharedPreferences(this).getInt("repeat" + n.getId(), 0);

                //intent to build the notification
                Intent undoIntent = new Intent(this, NotificationBuild.class);
                Bundle bundle = new Bundle();
                bundle.putInt("id", n.getId());
                undoIntent.putExtras(bundle);
                sendBroadcast(undoIntent);

                // save all the info of the notification. this is for undo notification re-building
                List list = Arrays.asList(
                        n.getTitle(), // 0 string
                        n.getNote(), // 1 string
                        tickerText, // 2 string
                        ico, // 3 int
                        color, // 4 int
                        n.getIconName(), // 5 string
                        priority, // 6 int
                        n.getImgBtnNum(), // 7 int
                        n.getAlarm(), // 8 string
                        repeatTime // 9 int
                );

                //save to sharedprefs
                SharedPreferences.Editor prefsEditor = PreferenceManager.getDefaultSharedPreferences(getBaseContext()).edit();
                Gson gson = new Gson();
                String json = gson.toJson(list);
                prefsEditor.putString("notification" + Integer.toString(n.getId()), json);
                prefsEditor.apply();
            }
        }
        stopSelf();
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void initializeSettings() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        if (CURRENT_ANDROID_VERSION >= 16) {
            pref_expand = sharedPreferences.getBoolean("pref_expand", true);
            pref_swipe = sharedPreferences.getBoolean("pref_swipe", false);
            pref_use_colored_icons = sharedPreferences.getBoolean("pref_use_colored_icons", false);

            pref_priority = sharedPreferences.getString("pref_priority", "normal");
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
                default:
                    priority = Notification.PRIORITY_DEFAULT;
                    break;
            }

        } else {
            pref_expand = false;
            pref_swipe = sharedPreferences.getBoolean("pref_swipe", true);
        }
        clickNotif = sharedPreferences.getString("clickNotif", "info");
        pref_share_action = sharedPreferences.getBoolean("pref_share_action", true);
    }

    private PendingIntent createOnDismissedIntent(int notificationId) {
        Intent intent = new Intent(this, NotificationDismiss.class);
        intent.putExtra("NotificationID", notificationId);
        return PendingIntent.getBroadcast(getApplicationContext(), notificationId, intent, 0);
    }

    private PendingIntent createEditIntent(NoteyNote n) {
        Intent editIntent = new Intent(this, MainActivity.class);
        editIntent.putExtra("editNotificationID", n.getId());
        editIntent.putExtra("editNote", n.getNote());
        editIntent.putExtra("editLoc", n.getSpinnerLoc());
        editIntent.putExtra("editButton", n.getImgBtnNum());
        editIntent.putExtra("editTitle", n.getTitle());
        editIntent.putExtra("editAlarm", n.getAlarm());
        editIntent.putExtra("editRepeat", repeatTime);
        editIntent.putExtra("editAlarmPendingIntent", alarmPendingIntent);
        return PendingIntent.getActivity(getApplicationContext(), n.getId(), editIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private PendingIntent createShareIntent(NoteyNote n) {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, n.getNote());
        sendIntent.setType("text/plain");
        return PendingIntent.getActivity(this, n.getId(), sendIntent, 0);
    }

    private PendingIntent createInfoScreenIntent(NoteyNote n) {
        Intent infoIntent = new Intent(this, InfoScreenActivity.class);
        infoIntent.putExtra("infoNotificationID", n.getId());
        infoIntent.putExtra("infoNote", n.getNote());
        infoIntent.putExtra("infoLoc", n.getSpinnerLoc());
        infoIntent.putExtra("infoButton", n.getImgBtnNum());
        infoIntent.putExtra("infoTitle", n.getTitle());
        infoIntent.putExtra("infoAlarm", n.getAlarm());
        infoIntent.putExtra("infoRepeat", repeatTime);
        infoIntent.putExtra("infoAlarmPendingIntent", alarmPendingIntent);
        return PendingIntent.getActivity(getApplicationContext(), n.getId(), infoIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private PendingIntent onNotifClickPI(String clickNotif, NoteyNote n) {
        switch (clickNotif) {
            case "info":
                return createInfoScreenIntent(n);
            case "edit":
                return createEditIntent(n);
            case "remove":
                return createOnDismissedIntent(n.getId());
            default:
                return null;
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) { /*empty*/ }
}