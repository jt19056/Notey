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
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.widget.RemoteViews;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class NotificationDismiss extends BroadcastReceiver{
    NotificationManager nm;
    public static Map<Integer, Timer> timerHashMap = new HashMap<>();
    public static Map<Integer, TimerTask> timerTaskHashMap = new HashMap<>();


    @Override
    public void onReceive(final Context paramContext, final Intent paramIntent) {

        final MySQLiteHelper db = new MySQLiteHelper(paramContext);
        final int id = paramIntent.getExtras().getInt("NotificationID");
        nm = (NotificationManager) paramContext.getSystemService(Context.NOTIFICATION_SERVICE);
        final SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(paramContext).edit();

        //give the user 5 seconds before removing the notification
        Timer timer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                nm.cancel(id);
                // remove the no longer needed sharedprefs
                editor.remove("notification" + Integer.toString(id)).apply();
                editor.remove("vibrate" + Integer.toString(id)).apply();
                editor.remove("wake" + Integer.toString(id)).apply();
                editor.remove("sound" + Integer.toString(id)).apply();
                editor.remove("repeat" + Integer.toString(id)).apply();
            }
        };
        timer.schedule(timerTask, 5000, 5000);

        //put the timer and timertask into their hashmaps. this will be used to cancel the undo if necessary
        timerHashMap.put(id,timer);
        timerTaskHashMap.put(id, timerTask);

        //undo notification layout
        RemoteViews remoteViews;
        if(MainActivity.CURRENT_ANDROID_VERSION >= Build.VERSION_CODES.LOLLIPOP)
            remoteViews= new RemoteViews(paramContext.getPackageName(), R.layout.undo_delete_layout_lollipop);
        else remoteViews= new RemoteViews(paramContext.getPackageName(), R.layout.undo_delete_layout_ics_jb_kk);

        //intent to rebuild the notification
        Intent intent = new Intent(paramContext, NotificationBuild.class);
        Bundle bundle = new Bundle();
        bundle.putInt("id", id);
        intent.putExtras(bundle);

        //get the notification icon
        NoteyNote notey = null;
        int smallIcon = R.drawable.md_transparent;
        try {
            notey = db.getNotey(id);
            if(notey != null) smallIcon = notey.getIcon();
        } catch (SQLiteException | CursorIndexOutOfBoundsException e) {
            e.printStackTrace();
        }

        // build undo notification
        NotificationCompat.Builder n = new NotificationCompat.Builder(paramContext)
                .setSmallIcon(smallIcon)
                .setContent(remoteViews)
                .setContentIntent(PendingIntent.getBroadcast(paramContext, id, intent, 0));

        nm.notify(id, n.build());

        //cancel the alarm pending intent
        PendingIntent alarmPendingIntent = (PendingIntent) paramIntent.getExtras().get("alarmPendingIntent");
        if (alarmPendingIntent != null) {
            AlarmManager alarmManager = (AlarmManager) paramContext.getSystemService(Context.ALARM_SERVICE);
            alarmManager.cancel(alarmPendingIntent);

            clearNotificationLED(paramContext);
        }

        try {
            if(notey != null)  db.deleteNotey(notey);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void clearNotificationLED(Context paramContext) {
        NotificationManager nm = (NotificationManager) paramContext.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(AlarmService.LED_NOTIFICATION_ID);
    }
}
