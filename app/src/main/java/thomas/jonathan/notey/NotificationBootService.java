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

import java.util.Date;
import java.util.List;

/* This service is used to restore all the notifications stored in the database*/
public class NotificationBootService extends IntentService {
    final int CURRENT_ANDROID_VERSION = Build.VERSION.SDK_INT;
    int priority, repeatTime = 0;
    boolean pref_expand, pref_swipe, pref_share_action;
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

            //show shortcut notification if settings_jb_kk say so
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
                PendingIntent piDismiss = createOnDismissedIntent(n.getId());
                PendingIntent piEdit = createEditIntent(n);
                PendingIntent piShare = createShareIntent(n);

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
                int color = getResources().getColor(R.color.grey_500); // grey, for grey background with white icons
                if(CURRENT_ANDROID_VERSION >= 21){
                    if (n.getIconName().contains("yellow")) {
                        color = getResources().getColor(R.color.yellow_500);
                    }
                    else if (n.getIconName().contains("blue")) {
                        color = getResources().getColor(R.color.cyan_a400);
                    }
                    else if (n.getIconName().contains("green")) {
                        color = getResources().getColor(R.color.green_a700);
                    }
                    else if (n.getIconName().contains("red")) {
                        color = getResources().getColor(R.color.red_a400);
                    }
                }

                Bitmap bm = BitmapFactory.decodeResource(getResources(), ico);

                Notification notif;
                if (pref_expand && pref_share_action && CURRENT_ANDROID_VERSION >= 21) { //expandable, with share button, and on lollipop and above
                    notif = new NotificationCompat.Builder(this)
                            .setContentTitle(n.getTitle())
                            .setContentText(n.getNote())
                            .setTicker(tickerText)
                            .setSmallIcon(ico)
                            .setColor(color)
                            .setStyle(new NotificationCompat.BigTextStyle().bigText(noteString))
                            .setDeleteIntent(piDismiss)
                            .setContentIntent(onNotifClickPI(clickNotif, n))
                            .setOngoing(!pref_swipe)
                            .setAutoCancel(false)
                            .setPriority(priority)
                            .addAction(R.drawable.ic_edit_white_24dp,
                                    getString(R.string.edit), piEdit)
                            .addAction(R.drawable.ic_share_white_24dp,
                                    getString(R.string.share), piShare)
                            .addAction(R.drawable.ic_delete_white_24dp,
                                    getString(R.string.remove), piDismiss)
                            .build();
                }
                else if (pref_expand && pref_share_action && CURRENT_ANDROID_VERSION >= 16 && CURRENT_ANDROID_VERSION < 21) { //expandable, with share button, and on jelly bean or greater. also, jb or kk.
                    notif = new NotificationCompat.Builder(this)
                            .setContentTitle(n.getTitle())
                            .setContentText(n.getNote())
                            .setTicker(tickerText)
                            .setSmallIcon(ico)
                            .setLargeIcon(bm)
                            .setStyle(new NotificationCompat.BigTextStyle().bigText(noteString))
                            .setDeleteIntent(piDismiss)
                            .setContentIntent(onNotifClickPI(clickNotif, n))
                            .setOngoing(!pref_swipe)
                            .setAutoCancel(false)
                            .setPriority(priority)
                            .addAction(R.drawable.ic_edit_white_24dp,
                                    getString(R.string.edit), piEdit)
                            .addAction(R.drawable.ic_share_white_24dp,
                                    getString(R.string.share), piShare)
                            .addAction(R.drawable.ic_delete_white_24dp,
                                    getString(R.string.remove), piDismiss)
                            .build();
                }
                // same as above, except no share action button. lollipop.
                else if (pref_expand && !pref_share_action && CURRENT_ANDROID_VERSION >= 21) {
                    notif = new NotificationCompat.Builder(this)
                            .setContentTitle(n.getTitle())
                            .setContentText(n.getNote())
                            .setTicker(tickerText)
                            .setSmallIcon(ico)
                            .setColor(color)
                            .setStyle(new NotificationCompat.BigTextStyle().bigText(noteString))
                            .setDeleteIntent(piDismiss)
                            .setContentIntent(onNotifClickPI(clickNotif, n))
                            .setOngoing(!pref_swipe)
                            .setAutoCancel(false)
                            .setPriority(priority)
                            .addAction(R.drawable.ic_edit_white_24dp,
                                    getString(R.string.edit), piEdit)
                            .addAction(R.drawable.ic_delete_white_24dp,
                                    getString(R.string.remove), piDismiss)
                            .build();
                }
                // same as above, except no share action button. jb & kk.
                else if (pref_expand && !pref_share_action && CURRENT_ANDROID_VERSION >= 16 && CURRENT_ANDROID_VERSION < 21) {
                    notif = new NotificationCompat.Builder(this)
                            .setContentTitle(n.getTitle())
                            .setContentText(n.getNote())
                            .setTicker(tickerText)
                            .setSmallIcon(ico)
                            .setLargeIcon(bm)
                            .setStyle(new NotificationCompat.BigTextStyle().bigText(noteString))
                            .setDeleteIntent(piDismiss)
                            .setContentIntent(onNotifClickPI(clickNotif, n))
                            .setOngoing(!pref_swipe)
                            .setAutoCancel(false)
                            .setPriority(priority)
                            .addAction(R.drawable.ic_edit_white_24dp,
                                    getString(R.string.edit), piEdit)
                            .addAction(R.drawable.ic_delete_white_24dp,
                                    getString(R.string.remove), piDismiss)
                            .build();
                }
                //not expandable, but still able to set priority. lollipop.
                else if (!pref_expand && CURRENT_ANDROID_VERSION >= 21) {
                    notif = new NotificationCompat.Builder(this)
                            .setContentTitle(n.getTitle())
                            .setContentText(n.getNote())
                            .setTicker(tickerText)
                            .setSmallIcon(ico)
                            .setColor(color)
                            .setDeleteIntent(piDismiss)
                            .setContentIntent(onNotifClickPI(clickNotif, n))
                            .setOngoing(!pref_swipe)
                            .setAutoCancel(false)
                            .setPriority(priority)
                            .build();
                }
                //not expandable, but still able to set priority. jb & kk.
                else if (!pref_expand && CURRENT_ANDROID_VERSION >= 16  && CURRENT_ANDROID_VERSION < 21) {
                    notif = new NotificationCompat.Builder(this)
                            .setContentTitle(n.getTitle())
                            .setContentText(n.getNote())
                            .setTicker(tickerText)
                            .setSmallIcon(ico)
                            .setLargeIcon(bm)
                            .setDeleteIntent(piDismiss)
                            .setContentIntent(onNotifClickPI(clickNotif, n))
                            .setOngoing(!pref_swipe)
                            .setAutoCancel(false)
                            .setPriority(priority)
                            .build();
                }
                //if api < 16
                else {
                    notif = new NotificationCompat.Builder(this)
                            .setContentTitle(n.getTitle())
                            .setContentText(n.getNote())
                            .setTicker(tickerText)
                            .setSmallIcon(ico)
                            .setLargeIcon(bm)
                            .setContentIntent(onNotifClickPI(clickNotif, n))
                            .setDeleteIntent(piDismiss)
                            .setOngoing(!pref_swipe)
                            .setAutoCancel(false)
                            .build();
                }
                nm.notify(n.getId(), notif);
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