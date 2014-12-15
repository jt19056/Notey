package thomas.jonathan.notey;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

/* This service is used to restore all the notifications stored in the database*/
public class NotificationBootService extends IntentService {
    final int CURRENT_ANDROID_VERSION = Build.VERSION.SDK_INT;
    int priority;
    boolean pref_expand, pref_swipe, pref_share_action;
    String clickNotif, intentType, pref_priority, noteString;

    public NotificationBootService() {
        super("NotificationService");
    }

    @Override
    public void onStart(Intent intent, int startId) {
        MySQLiteHelper db = new MySQLiteHelper(this);
        List<NoteyNote> allNoteys = db.getAllNoteys();

        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        intentType = intent.getExtras().getString("action");

        if (intentType == null) return;
        if (intentType.compareTo("boot") == 0) {

            //Settings stuff
            initializeSettings();

            for (NoteyNote n : allNoteys) {
                PendingIntent piDismiss = createOnDismissedIntent(n.getId());
                PendingIntent piEdit = createEditIntent(n);
                PendingIntent piShare = createShareIntent(n);

                if (n.getTitle() != null) { /*do nothing*/}  //for users on < v2.0.3, they didn't have 'title' column in db
                else {
                    n.setTitle(getString(R.string.app_name));
                    db.updateNotey(n);
                }

                noteString = n.getNote(); //temp note text to display alarm
                if(n.getAlarm() != null){
                    Date date = new Date(Long.valueOf(n.getAlarm()));

                    noteString += "\n"+ getString(R.string.alarm) + ": " +  AlarmActivity.format_date.format(date.getTime()) + " " + AlarmActivity.format_time.format(date.getTime());

//                    Intent myIntent = new Intent(this, AlarmReceiver.class);
//                    PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, myIntent,0);
//
//                    AlarmManager alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
//                    alarmManager.set(AlarmManager.RTC, alarm_time, pendingIntent);
                }

                //converts the iconName to int. if there isn't an iconName, default to check white
                int ico;
                try {
                    ico = getResources().getIdentifier(n.getIconName(), "drawable", getPackageName());
                } catch (SQLiteException e) {
                    ico = R.drawable.ic_check_white_36dp;
                    e.printStackTrace();
                } catch (Exception e) {
                    ico = R.drawable.ic_check_white_36dp;
                    e.printStackTrace();
                }


                Bitmap bm;
                //big white icons are un-seeable on lollipop
                if (CURRENT_ANDROID_VERSION >= 21 && n.getIconName().contains("white_36dp")) {
                    bm = null;
                } else bm = BitmapFactory.decodeResource(getResources(), ico);

                Notification notif;
                if (pref_expand && pref_share_action && CURRENT_ANDROID_VERSION >= 16) { //expandable, with share button, and on jelly bean or greater
                    notif = new NotificationCompat.Builder(this)
                            .setContentTitle(n.getTitle())
                            .setContentText(n.getNote())
                            .setSmallIcon(ico)
                            .setLargeIcon(bm)
                            .setStyle(new NotificationCompat.BigTextStyle().bigText(noteString))
                            .setDeleteIntent(piDismiss)
                            .setContentIntent(onNotifClickPI(clickNotif, n))
                            .setOngoing(!pref_swipe)
                            .setAutoCancel(true)
                            .setPriority(priority)
                            .addAction(R.drawable.ic_edit_white_24dp,
                                    getString(R.string.edit), piEdit)
                            .addAction(R.drawable.ic_share_white_24dp,
                                    getString(R.string.share), piShare)
                            .addAction(R.drawable.ic_delete_white_24dp,
                                    getString(R.string.remove), piDismiss)
                            .build();
                }
                // same as above, except no share action button
                else if (pref_expand && !pref_share_action && CURRENT_ANDROID_VERSION >= 16) {
                    notif = new NotificationCompat.Builder(this)
                            .setContentTitle(n.getTitle())
                            .setContentText(n.getNote())
                            .setSmallIcon(ico)
                            .setLargeIcon(bm)
                            .setStyle(new NotificationCompat.BigTextStyle().bigText(noteString))
                            .setDeleteIntent(piDismiss)
                            .setContentIntent(onNotifClickPI(clickNotif, n))
                            .setOngoing(!pref_swipe)
                            .setAutoCancel(true)
                            .setPriority(priority)
                            .addAction(R.drawable.ic_edit_white_24dp,
                                    getString(R.string.edit), piEdit)
                            .addAction(R.drawable.ic_delete_white_24dp,
                                    getString(R.string.remove), piDismiss)
                            .build();
                }
                //not expandable, but still able to set priority
                else if (!pref_expand && CURRENT_ANDROID_VERSION >= 16) {
                    notif = new NotificationCompat.Builder(this)
                            .setContentTitle(n.getTitle())
                            .setContentText(n.getNote())
                            .setSmallIcon(ico)
                            .setDeleteIntent(piDismiss)
                            .setContentIntent(onNotifClickPI(clickNotif, n))
                            .setOngoing(!pref_swipe)
                            .setAutoCancel(true)
                            .setPriority(priority)
                            .build();
                }
                //if api < 16
                else {
                    notif = new NotificationCompat.Builder(this)
                            .setContentTitle(n.getTitle())
                            .setContentText(n.getNote())
                            .setSmallIcon(ico)
                            .setContentIntent(onNotifClickPI(clickNotif, n))
                            .setDeleteIntent(piDismiss)
                            .setOngoing(!pref_swipe)
                            .build();
                }
                nm.notify(n.getId(), notif);
            }
        }
        stopSelf();
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
            else priority = Notification.PRIORITY_DEFAULT;

        } else {
            pref_expand = false;
            pref_swipe = sharedPreferences.getBoolean("pref_swipe", true);
        }
        clickNotif = sharedPreferences.getString("clickNotif", "edit");
        pref_share_action = sharedPreferences.getBoolean("pref_share_action", true);
    }

    private PendingIntent createOnDismissedIntent(int notificationId) {
        Intent intent = new Intent(this, NotificationDismiss.class);
        intent.putExtra("NotificationID", notificationId);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), notificationId, intent, 0);
        return pendingIntent;
    }

    private PendingIntent createEditIntent(NoteyNote n) {
        Intent editIntent = new Intent(this, MainActivity.class);
        editIntent.putExtra("editNotificationID", n.getId());
        editIntent.putExtra("editNote", n.getNote());
        editIntent.putExtra("editLoc", n.getSpinnerLoc());
        editIntent.putExtra("editButton", n.getImgBtnNum());
        editIntent.putExtra("editTitle", n.getTitle());
        editIntent.putExtra("editAlarm", n.getAlarm());
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
        return PendingIntent.getActivity(getApplicationContext(), n.getId(), infoIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private PendingIntent onNotifClickPI(String clickNotif, NoteyNote n) {
        if (clickNotif.equals("info")) {
            return createInfoScreenIntent(n);
        } else if (clickNotif.equals("edit")) {
            return createEditIntent(n);
        } else if (clickNotif.equals("remove")) {
            return createOnDismissedIntent(n.getId());
        } else return null;
    }

    @Override
    protected void onHandleIntent(Intent intent) { /*empty*/ }
}