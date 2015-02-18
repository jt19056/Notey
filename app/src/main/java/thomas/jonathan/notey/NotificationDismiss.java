package thomas.jonathan.notey;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.CursorIndexOutOfBoundsException;
import android.database.sqlite.SQLiteException;
import android.preference.PreferenceManager;

public class NotificationDismiss extends BroadcastReceiver {
    NotificationManager nm;

    @Override
    public void onReceive(Context paramContext, Intent paramIntent) {
        MySQLiteHelper db = new MySQLiteHelper(paramContext);
        NoteyNote notey;
        int id = paramIntent.getExtras().getInt("NotificationID");

        //cancel the alarm pending intent
        PendingIntent alarmPendingIntent = (PendingIntent) paramIntent.getExtras().get("alarmPendingIntent");
        if (alarmPendingIntent != null) {
            AlarmManager alarmManager = (AlarmManager) paramContext.getSystemService(Context.ALARM_SERVICE);
            alarmManager.cancel(alarmPendingIntent);

            // remove the no longer needed sharedprefs
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(paramContext).edit();
            editor.remove("vibrate" + Integer.toString(id)).apply();
            editor.remove("wake" + Integer.toString(id)).apply();
            editor.remove("sound" + Integer.toString(id)).apply();
            editor.remove("repeat" + Integer.toString(id)).apply();

            clearNotificationLED(paramContext);
        }

        try {
            notey = db.getNotey(id);
            db.deleteNotey(notey);

            nm = (NotificationManager) paramContext.getSystemService(Context.NOTIFICATION_SERVICE);
            nm.cancel(id);
        } catch (SQLiteException | CursorIndexOutOfBoundsException e) {
            e.printStackTrace();
        }

    }

    public static void clearNotificationLED(Context paramContext) {
        NotificationManager nm = (NotificationManager) paramContext.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(AlarmService.LED_NOTIFICATION_ID);
    }
}
