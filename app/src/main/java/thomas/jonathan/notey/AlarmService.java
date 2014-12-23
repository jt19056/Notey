package thomas.jonathan.notey;


import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.Vibrator;
import android.preference.PreferenceManager;


public class AlarmService extends Service {
    final public static int LED_NOTIFICATION_ID = 0;
    private static PowerManager.WakeLock wakeLock;

    @Override
    public IBinder onBind(Intent arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void onCreate() {
        // TODO Auto-generated method stub
        super.onCreate();
    }


    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);

        MySQLiteHelper db = new MySQLiteHelper(this);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        Bundle extras = intent.getExtras();
        int NoteID = extras.getInt("alarmID");
        NoteyNote n = db.getNotey(NoteID);

        // wake up device?
        if (PreferenceManager.getDefaultSharedPreferences(getBaseContext()).getBoolean("wake" + Integer.toString(NoteID), true)) {
            wakeUpDeviceWakelock(getBaseContext());
        }

        //intent for info screen popup
        Intent infoIntent = new Intent(this, InfoScreenActivity.class);
        infoIntent.putExtra("infoNotificationID", n.getId());
        infoIntent.putExtra("infoNote", n.getNote());
        infoIntent.putExtra("infoLoc", n.getSpinnerLoc());
        infoIntent.putExtra("infoButton", n.getImgBtnNum());
        infoIntent.putExtra("infoTitle", n.getTitle());
        infoIntent.putExtra("infoAlarm", n.getAlarm());

        //vibrate for two 250ms bursts if the checkbox was checked
        if (sharedPref.getBoolean("vibrate" + Integer.toString(NoteID), true)) {
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            long pattern[] = new long[]{0, 250, 250, 250, 250};
            v.vibrate(pattern, -1); //-1 so it only occurs once. 0 if repeat forever
//            v.vibrate(500);
        }

        //start info screen
        infoIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); //activity is being called outside of an activity (this is a service). so this flag must let android know we are positive we want this activity to start.
        startActivity(infoIntent);

        flashNotificationLED();

        stopSelf();

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

    private void flashNotificationLED() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification notif = new Notification();
        notif.ledARGB = Color.CYAN;
        notif.flags = Notification.FLAG_SHOW_LIGHTS;
        notif.ledOnMS = 1000;
        notif.ledOffMS = 500;
        nm.notify(LED_NOTIFICATION_ID, notif);
    }

    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
    }

}
