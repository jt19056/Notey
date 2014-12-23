package thomas.jonathan.notey;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/* Receives intent to start NotificationBootService after boot.  This is to restore alarms for the noteys.*/
public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent localIntent = new Intent(context, NotificationBootService.class);
        localIntent.putExtra("action", "boot_alarm");
        context.startService(localIntent);
    }
}
