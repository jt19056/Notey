package thomas.jonathan.notey;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;

import com.crashlytics.android.Crashlytics;
import com.google.gson.Gson;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;

import io.fabric.sdk.android.services.common.Crash;

public class NotificationBuild extends BroadcastReceiver {

    PendingIntent alarmPendingIntent;

    @Override
    public void onReceive(final Context context, final Intent intent) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        NoteyNote notey = new NoteyNote();
        final MySQLiteHelper db = new MySQLiteHelper(context);

        Bundle extras = intent.getExtras();
        int id = extras.getInt("id", 1);

        //cancel the 5 second delay for undo notification
        if(NotificationDismiss.timerHashMap.get(id) != null){
            NotificationDismiss.timerHashMap.get(id).cancel();
        }
        if(NotificationDismiss.timerTaskHashMap.get(id) != null){
            NotificationDismiss.timerTaskHashMap.get(id).cancel();
        }
        
        // get all the info from the previously deleted notification
        Gson gson = new Gson();
        String json = mPrefs.getString("notification" + Integer.toString(id), "");
        List list = gson.fromJson(json, ArrayList.class);

        boolean bulletListFlag = false;
        boolean numberedListFlag = false;
        int numberedListCounter = 1;
        //set all the notification variables
        String noteTitle = (String) list.get(0);
        String note = (String) list.get(1);
        String tickerText = (String) list.get(2);
        int d = (int) ((double) list.get(3));
        int color = (int) ((double) list.get(4));
        String fullIconName = (String) list.get(5);
        int priority = (int) ((double) list.get(6));
        int imageButtonNumber = (int) ((double) list.get(7));
        String alarm_time = (String) list.get(8);
        int repeat_time = (int) ((double) list.get(9));
        try { //necessary for when updating from v2.4 to 2.4.1+
            bulletListFlag = (boolean) list.get(10);
            numberedListFlag = (boolean) list.get(11);
            numberedListCounter = (int) ((double) list.get(12));
        }catch(IndexOutOfBoundsException e) {
            e.printStackTrace();
            Crashlytics.logException(e);
        }

        String clickNotif = mPrefs.getString("clickNotif", "info"); //notification click action
        boolean pref_swipe = mPrefs.getBoolean("pref_swipe", false);
        boolean pref_expand = mPrefs.getBoolean("pref_expand", true);
        boolean pref_share_action = mPrefs.getBoolean("pref_share_action", true);
        boolean pref_use_colored_icons = mPrefs.getBoolean("pref_use_colored_icons", false);
        
        //add alarm to db and set it
        String noteForNotification = note;  // use a temp string to add the alarm info to the notification
        if (alarm_time != null && !alarm_time.equals("")) {  //if alarm time is valid, and if we are not in and editIntent
            notey.setAlarm(alarm_time); // add to db

            // intent for alarm service to launch
            Intent myIntent = null;
            try {
                myIntent = new Intent(context, AlarmService.class);
                Bundle bundle = new Bundle();
                bundle.putInt("alarmID", id);
                myIntent.putExtras(bundle);
            }catch (NullPointerException npe){
                Crashlytics.logException(npe);
            }

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(MainActivity.ALARM_SERVICE);

            // if alarm time is empty, set the boolean to true, otherwise check if alarm time is greater than current time. if it is, don't set notification.
            boolean alarmTimeIsGreaterThanCurrentTime;
            alarmTimeIsGreaterThanCurrentTime = alarm_time.equals("") || Long.valueOf(alarm_time) > System.currentTimeMillis();

            //if alarm_time is old, don't set pending intent. in this case, the alarm hasn't occured, so set the alarm
            if (alarmTimeIsGreaterThanCurrentTime && myIntent != null) {
                //set alarm
                alarmPendingIntent = PendingIntent.getService(context, id, myIntent, PendingIntent.FLAG_UPDATE_CURRENT);

                alarmManager.cancel(alarmPendingIntent); // cancel any alarm that might already exist
            }

            //set repeating alarm or set regular alarm
            if (repeat_time != 0) {
                
                //if alarm_time is old, set alarm_time to currTime+repeat_time to avoid alarm going off directly after user creates note
                if (!alarmTimeIsGreaterThanCurrentTime) {
                    alarm_time = Long.toString((System.currentTimeMillis() + (long) (repeat_time * 1000 * 60)));
                }

                // check the sharedPrefs for the check box to wake up the device
                if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("wake" + Integer.toString(id), true))
                    alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, Long.valueOf(alarm_time), repeat_time * 1000 * 60, alarmPendingIntent);
                else
                    alarmManager.setRepeating(AlarmManager.RTC, Long.valueOf(alarm_time), repeat_time * 1000 * 60, alarmPendingIntent);
            } else { //set regualar alarm
                // check the sharedPrefs for the check box to wake up the device
                if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("wake" + Integer.toString(id), true))
                    alarmManager.set(AlarmManager.RTC_WAKEUP, Long.valueOf(alarm_time), alarmPendingIntent);
                else
                    alarmManager.set(AlarmManager.RTC, Long.valueOf(alarm_time), alarmPendingIntent);
            }

            // add the alarm date/time to the notification
            DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.DEFAULT, Locale.getDefault());
            DateFormat timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault());
            Date date = new Date(Long.valueOf(alarm_time));
            noteForNotification += "\n" + context.getString(R.string.alarm) + ": " + dateFormat.format(date) + ", " + timeFormat.format(date);
        }

        PendingIntent piDismiss = createOnDismissedIntent(context, id);
        PendingIntent piEdit = createEditIntent(id, imageButtonNumber, alarm_time, repeat_time, bulletListFlag, numberedListFlag, numberedListCounter, context);
        PendingIntent piShare = createShareIntent(note, id, context);


        Bitmap bm = BitmapFactory.decodeResource(context.getResources(), d);

        //set the notey object
        notey.setId(id);
        notey.setNote(note);
        notey.setIcon(d);
        notey.setImgBtnNum(imageButtonNumber);
        notey.setTitle(noteTitle);
        notey.setIconName(fullIconName);

        //does notey exist in database? if yes-update. if no-add new notey.
        if (db.checkIfExist(id)) db.updateNotey(notey);
        else db.addNotey(notey);

        db.close();

        //build the notification!!
        Notification n;
        if (pref_expand && pref_share_action && MainActivity.CURRENT_ANDROID_VERSION >= 21 && !pref_use_colored_icons) { //lollipop and above with expandable notifs settings_jb_kk allowed && share action button is enabled && they want the lollipop icon color
            n = new NotificationCompat.Builder(context)
                    .setContentTitle(noteTitle)
                    .setContentText(note)
                    .setTicker(tickerText)
                    .setSmallIcon(d)
                    .setColor(color)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(noteForNotification))
                    .setDeleteIntent(piDismiss)
                    .setOngoing(!pref_swipe)
                    .setContentIntent(onNotifClickPI(clickNotif, note, noteTitle, id, imageButtonNumber, alarm_time, repeat_time, bulletListFlag, numberedListFlag, numberedListCounter, context))
                    .setAutoCancel(false)
                    .setPriority(priority)
                    .addAction(R.drawable.ic_edit_white_24dp,
                            context.getString(R.string.edit), piEdit) //edit button on notification
                    .addAction(R.drawable.ic_share_white_24dp,
                            context.getString(R.string.share), piShare) // share button
                    .addAction(R.drawable.ic_delete_white_24dp,
                            context.getString(R.string.remove), piDismiss) //remove button
                    .build();
        }
        //same as above, but use LargeIcon instead of SetColor
        else if (pref_expand && pref_share_action && MainActivity.CURRENT_ANDROID_VERSION >= 21 && pref_use_colored_icons) { //lollipop and above with expandable notifs settings_jb_kk allowed && share action button is enabled && they want the lollipop icon color
            n = new NotificationCompat.Builder(context)
                    .setContentTitle(noteTitle)
                    .setContentText(note)
                    .setTicker(tickerText)
                    .setSmallIcon(d)
                    .setLargeIcon(bm)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(noteForNotification))
                    .setDeleteIntent(piDismiss)
                    .setOngoing(!pref_swipe)
                    .setContentIntent(onNotifClickPI(clickNotif, note, noteTitle, id, imageButtonNumber, alarm_time, repeat_time, bulletListFlag, numberedListFlag, numberedListCounter, context))
                    .setAutoCancel(false)
                    .setPriority(priority)
                    .addAction(R.drawable.ic_edit_white_24dp,
                            context.getString(R.string.edit), piEdit) //edit button on notification
                    .addAction(R.drawable.ic_share_white_24dp,
                            context.getString(R.string.share), piShare) // share button
                    .addAction(R.drawable.ic_delete_white_24dp,
                            context.getString(R.string.remove), piDismiss) //remove button
                    .build();
        }
        else if (pref_expand && pref_share_action && MainActivity.CURRENT_ANDROID_VERSION >= 16 && MainActivity.CURRENT_ANDROID_VERSION < 21) { //jelly bean and kitkat with expandable notifs settings_jb_kk allowed && share action button is enabled
            n = new NotificationCompat.Builder(context)
                    .setContentTitle(noteTitle)
                    .setContentText(note)
                    .setTicker(tickerText)
                    .setSmallIcon(d)
                    .setLargeIcon(bm)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(noteForNotification))
                    .setDeleteIntent(piDismiss)
                    .setOngoing(!pref_swipe)
                    .setContentIntent(onNotifClickPI(clickNotif, note, noteTitle, id, imageButtonNumber, alarm_time, repeat_time, bulletListFlag, numberedListFlag, numberedListCounter, context))
                    .setAutoCancel(false)
                    .setPriority(priority)
                    .addAction(R.drawable.ic_edit_white_24dp,
                            context.getString(R.string.edit), piEdit) //edit button on notification
                    .addAction(R.drawable.ic_share_white_24dp,
                            context.getString(R.string.share), piShare) // share button
                    .addAction(R.drawable.ic_delete_white_24dp,
                            context.getString(R.string.remove), piDismiss) //remove button
                    .build();
        }
        // same as above, but without share action button. lollipop only
        else if (pref_expand && !pref_share_action && MainActivity.CURRENT_ANDROID_VERSION >= 21 && !pref_use_colored_icons) {
            n = new NotificationCompat.Builder(context)
                    .setContentTitle(noteTitle)
                    .setContentText(note)
                    .setTicker(tickerText)
                    .setSmallIcon(d)
                    .setColor(color)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(noteForNotification))
                    .setDeleteIntent(piDismiss)
                    .setOngoing(!pref_swipe)
                    .setContentIntent(onNotifClickPI(clickNotif, note, noteTitle, id, imageButtonNumber, alarm_time, repeat_time, bulletListFlag, numberedListFlag, numberedListCounter, context))
                    .setAutoCancel(false)
                    .setPriority(priority)
                    .addAction(R.drawable.ic_edit_white_24dp,
                            context.getString(R.string.edit), piEdit) //edit button on notification
                    .addAction(R.drawable.ic_delete_white_24dp,
                            context.getString(R.string.remove), piDismiss) //remove button
                    .build();
        }
        //same as above, but use LargeIcon instead of SetColor
        else if (pref_expand && !pref_share_action && MainActivity.CURRENT_ANDROID_VERSION >= 21 && pref_use_colored_icons) {
            n = new NotificationCompat.Builder(context)
                    .setContentTitle(noteTitle)
                    .setContentText(note)
                    .setTicker(tickerText)
                    .setSmallIcon(d)
                    .setLargeIcon(bm)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(noteForNotification))
                    .setDeleteIntent(piDismiss)
                    .setOngoing(!pref_swipe)
                    .setContentIntent(onNotifClickPI(clickNotif, note, noteTitle, id, imageButtonNumber, alarm_time, repeat_time, bulletListFlag, numberedListFlag, numberedListCounter, context))
                    .setAutoCancel(false)
                    .setPriority(priority)
                    .addAction(R.drawable.ic_edit_white_24dp,
                            context.getString(R.string.edit), piEdit) //edit button on notification
                    .addAction(R.drawable.ic_delete_white_24dp,
                            context.getString(R.string.remove), piDismiss) //remove button
                    .build();
        }
        // same as above, but without share action button. jelly bean and kitkat.
        else if (pref_expand && !pref_share_action && MainActivity.CURRENT_ANDROID_VERSION >= 16 && MainActivity.CURRENT_ANDROID_VERSION < 21) {
            n = new NotificationCompat.Builder(context)
                    .setContentTitle(noteTitle)
                    .setContentText(note)
                    .setTicker(tickerText)
                    .setSmallIcon(d)
                    .setLargeIcon(bm)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(noteForNotification))
                    .setDeleteIntent(piDismiss)
                    .setOngoing(!pref_swipe)
                    .setContentIntent(onNotifClickPI(clickNotif, note, noteTitle, id, imageButtonNumber, alarm_time, repeat_time, bulletListFlag, numberedListFlag, numberedListCounter, context))
                    .setAutoCancel(false)
                    .setPriority(priority)
                    .addAction(R.drawable.ic_edit_white_24dp,
                            context.getString(R.string.edit), piEdit) //edit button on notification
                    .addAction(R.drawable.ic_delete_white_24dp,
                            context.getString(R.string.remove), piDismiss) //remove button
                    .build();
        } else if (!pref_expand && MainActivity.CURRENT_ANDROID_VERSION >= 21 && !pref_use_colored_icons) { //not expandable, but still able to set priority. lollipop only.
            n = new NotificationCompat.Builder(context)
                    .setContentTitle(noteTitle)
                    .setContentText(note)
                    .setTicker(tickerText)
                    .setSmallIcon(d)
                    .setColor(color)
                    .setDeleteIntent(piDismiss)
                    .setOngoing(!pref_swipe)
                    .setContentIntent(onNotifClickPI(clickNotif, note, noteTitle, id, imageButtonNumber, alarm_time, repeat_time, bulletListFlag, numberedListFlag, numberedListCounter, context))
                    .setAutoCancel(false)
                    .setPriority(priority)
                    .build();
        }
        //same as above, but use LargeIcon instead of SetColor
        else if (!pref_expand && MainActivity.CURRENT_ANDROID_VERSION >= 21 && pref_use_colored_icons) { //not expandable, but still able to set priority. lollipop only.
            n = new NotificationCompat.Builder(context)
                    .setContentTitle(noteTitle)
                    .setContentText(note)
                    .setTicker(tickerText)
                    .setSmallIcon(d)
                    .setLargeIcon(bm)
                    .setDeleteIntent(piDismiss)
                    .setOngoing(!pref_swipe)
                    .setContentIntent(onNotifClickPI(clickNotif, note, noteTitle, id, imageButtonNumber, alarm_time, repeat_time, bulletListFlag, numberedListFlag, numberedListCounter, context))
                    .setAutoCancel(false)
                    .setPriority(priority)
                    .build();
        } 
        else if (!pref_expand && MainActivity.CURRENT_ANDROID_VERSION >= 16 && MainActivity.CURRENT_ANDROID_VERSION < 21) { //not expandable, but still able to set priority. jb and kitkat.
            n = new NotificationCompat.Builder(context)
                    .setContentTitle(noteTitle)
                    .setContentText(note)
                    .setTicker(tickerText)
                    .setSmallIcon(d)
                    .setLargeIcon(bm)
                    .setDeleteIntent(piDismiss)
                    .setOngoing(!pref_swipe)
                    .setContentIntent(onNotifClickPI(clickNotif, note, noteTitle, id, imageButtonNumber, alarm_time, repeat_time, bulletListFlag, numberedListFlag, numberedListCounter, context))
                    .setAutoCancel(false)
                    .setPriority(priority)
                    .build();
        } 
        else { //if api < 16. they cannot have expandable notifs or any type of priority
            n = new NotificationCompat.Builder(context)
                    .setContentTitle(noteTitle)
                    .setContentText(note)
                    .setTicker(tickerText)
                    .setSmallIcon(d)
                    .setLargeIcon(bm)
                    .setAutoCancel(false)
                    .setContentIntent(onNotifClickPI(clickNotif, note, noteTitle, id, imageButtonNumber, alarm_time, repeat_time, bulletListFlag, numberedListFlag, numberedListCounter, context))
                    .setDeleteIntent(piDismiss)
                    .setOngoing(!pref_swipe)
                    .build();
        }

        nm.notify(id, n);

        // save all the info of the notification. this is for undo notification re-building
        list = Arrays.asList(
            noteTitle, // 0 string
            note, // 1 string
            tickerText, // 2 string
            d, // 3 int
            color, // 4 int
            fullIconName, // 5 string
            priority, // 6 int
            imageButtonNumber, // 7 int
            alarm_time, // 8 string
            repeat_time, // 9 int
            bulletListFlag, //10 boolean
            numberedListFlag, //11 boolean
            numberedListCounter //12 int
        );

        //save to sharedprefs
        SharedPreferences.Editor prefsEditor = mPrefs.edit();
        gson = new Gson();
        json = gson.toJson(list);
        prefsEditor.putString("notification" + Integer.toString(id), json);
        prefsEditor.apply();
    }

    private PendingIntent createOnDismissedIntent(Context context, int notificationId) {
        //we want to signal the NotificationDismiss receiver for removing notifications
        Intent intent = new Intent(context, NotificationDismiss.class);
        intent.putExtra("NotificationID", notificationId);
        intent.putExtra("alarmPendingIntent", alarmPendingIntent);

        return PendingIntent.getBroadcast(context.getApplicationContext(), notificationId, intent, 0);
    }

    private PendingIntent createEditIntent(int id, int imageButtonNumber, String alarm_time, int repeat_time, boolean bulletListFlag, boolean numberedListFlag, int numberedListCounter, Context context) {
        Intent editIntent = new Intent(context, MainActivity.class);
        editIntent.putExtra("editNotificationID", id);
        editIntent.putExtra("editButton", imageButtonNumber);
        editIntent.putExtra("editAlarm", alarm_time);
        editIntent.putExtra("editRepeat", repeat_time);
        editIntent.putExtra("editAlarmPendingIntent", alarmPendingIntent);
        editIntent.putExtra("bulletListFlag", bulletListFlag);
        editIntent.putExtra("numberedListFlag", numberedListFlag);
        editIntent.putExtra("numberedListCounter", numberedListCounter);

        return PendingIntent.getActivity(context, id, editIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private PendingIntent createShareIntent(String note, int id, Context context) {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, note);
        sendIntent.setType("text/plain");
        return PendingIntent.getActivity(context, id, sendIntent, 0);
    }

    private PendingIntent onNotifClickPI(String clickNotif, String note, String title, int id, int imageButtonNumber, String alarm_time, int repeat_time, boolean bulletListFlag, boolean numberedListFlag, int numberedListCounter, Context context) {
        switch (clickNotif) {
            case "info":
                return createInfoScreenIntent(note, title, id, imageButtonNumber, alarm_time, repeat_time, context);
            case "edit":
                return createEditIntent(id, imageButtonNumber, alarm_time, repeat_time, bulletListFlag, numberedListFlag, numberedListCounter, context);
            case "remove":
                return createOnDismissedIntent(context, id);
            default:
                return createInfoScreenIntent(note, title, id, imageButtonNumber, alarm_time, repeat_time, context);
        }
    }

    private PendingIntent createInfoScreenIntent(String note, String title, int id, int imageButtonNumber, String alarm_time, int repeat_time, Context context) {
        Intent infoIntent = new Intent(context, InfoScreenActivity.class);
        infoIntent.putExtra("infoNotificationID", id);
        infoIntent.putExtra("infoNote", note);
        infoIntent.putExtra("infoButton", imageButtonNumber);
        infoIntent.putExtra("infoTitle", title);
        infoIntent.putExtra("infoAlarm", alarm_time);
        infoIntent.putExtra("infoRepeat", repeat_time);
        infoIntent.putExtra("infoAlarmPendingIntent", alarmPendingIntent);
        return PendingIntent.getActivity(context, id, infoIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
