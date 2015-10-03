package thomas.jonathan.notey;


import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.CursorIndexOutOfBoundsException;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class AlarmService extends Service {
    final public static int LED_NOTIFICATION_ID = 0;
    final public static int LED_SOUND_ID = 1;
    final public static int VIBRATE_NOTIFICATION_ID = 2;
    final private String TAG = "Notey_AlarmService";
    private static PowerManager.WakeLock wakeLock;
    private NotificationManager nm;

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }


    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);

        final MySQLiteHelper db = new MySQLiteHelper(this);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        Bundle extras = intent.getExtras();
        int NoteID = extras.getInt("alarmID");
        NoteyNote n;
        try {
           n = db.getNotey(NoteID);

            // wake up device?
            if (PreferenceManager.getDefaultSharedPreferences(getBaseContext()).getBoolean("wake" + Integer.toString(NoteID), true)) {
                wakeUpDeviceWakelock(getBaseContext());
            }

            //play sound?
            String alarm_uri = PreferenceManager.getDefaultSharedPreferences(getBaseContext()).getString("sound" + Integer.toString(NoteID), "None");
            Notification notif = null;
            if (alarm_uri.contains("alarm_alert")) {
                Uri alert = Uri.parse("android.resource://thomas.jonathan.notey/" + R.raw.alarm_beep);
                notif = new Notification();
                notif.sound = alert;
                notif.flags = Notification.FLAG_INSISTENT;

                nm.notify(LED_SOUND_ID, notif);
            }
            else if (alarm_uri.contains("notification_sound")) {
                Uri alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                notif = new Notification();
                notif.sound = alert;

                nm.notify(LED_SOUND_ID, notif);
            }

            //intent for info screen popup
            Intent infoIntent = new Intent(this, InfoScreenActivity.class);
            infoIntent.putExtra("infoNotificationID", n.getId());
            infoIntent.putExtra("infoNote", n.getNote());
            infoIntent.putExtra("infoLoc", n.getSpinnerLoc());
            infoIntent.putExtra("infoButton", n.getImgBtnNum());
            infoIntent.putExtra("infoTitle", n.getTitle());
            infoIntent.putExtra("infoAlarm", n.getAlarm());
            infoIntent.putExtra("infoRepeat", PreferenceManager.getDefaultSharedPreferences(getBaseContext()).getInt("repeat" + Integer.toString(NoteID), 0));
            infoIntent.putExtra("infoAlarmPendingIntent", createAlarmPI(n.getId()));
            infoIntent.putExtra("infoNotif", notif);

            //vibrate for two 250ms bursts if the checkbox was checked
            if (sharedPref.getBoolean("vibrate" + Integer.toString(NoteID), true)) {
                Notification vib_only_notif = new NotificationCompat.Builder(this)
                        .setVibrate(new long[]{0, 250, 250, 250, 250})
                        .build();
                nm.notify(VIBRATE_NOTIFICATION_ID, vib_only_notif);
            }

            //start info screen
            infoIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); //activity is being called outside of an activity (this is a service). so this flag must let android know we are positive we want this activity to start.
            startActivity(infoIntent);

            flashNotificationLED(NoteID);

            stopSelf();
        }catch (CursorIndexOutOfBoundsException e){ //*edit: fixed but kept just in case* after a user deletes a repeating alarm, it will try to call AlarmService (not sure why at the moment), but there's nothing in the database. this catches that and prevents the user from seeing any crash.
            Log.d(TAG, "CursorIndexOutOfBoundsException.  Repeating alarm deleted but AlarmService still trying to be called.");
            e.printStackTrace();
            stopSelf();
        }
    }

    //re-create alarmPendingIntent to pass to info screen
    private PendingIntent createAlarmPI(int id){
        Intent myIntent = new Intent(this, AlarmService.class);
        Bundle bundle = new Bundle();
        bundle.putInt("alarmID", id);
        myIntent.putExtras(bundle);
        return PendingIntent.getService(this, id, myIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public static void wakeUpDeviceWakelock(Context ctx) {
        if (wakeLock != null) wakeLock.release();

        PowerManager pm = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK |
                PowerManager.ACQUIRE_CAUSES_WAKEUP |
                PowerManager.ON_AFTER_RELEASE, "WAKELOCK_TAG");
        wakeLock.acquire();
    }

    public static void releaseWakeUpWakelock() {
        if (wakeLock != null) wakeLock.release();
        wakeLock = null;
    }

    private void flashNotificationLED(int NoteID) {
        String ledColor = PreferenceManager.getDefaultSharedPreferences(getBaseContext()).getString("led" + Integer.toString(NoteID), "md_blue_500");
        Notification notif = new Notification();
        notif.ledARGB = getResources().getColor(getResources().getIdentifier(ledColor, "color", getPackageName()));
        notif.flags = Notification.FLAG_SHOW_LIGHTS;
        notif.ledOnMS = 1000;
        notif.ledOffMS = 500;
        nm.notify(LED_NOTIFICATION_ID, notif);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

}
