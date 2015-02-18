package thomas.jonathan.notey;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/* This reciever is used for listening for the boot_completed action.
*  After boot, run the NotificationBootService to restore all the notifications.*/
public class DeviceBootListener extends BroadcastReceiver {
    @Override
    public void onReceive(Context paramContext, Intent paramIntent) {
        Intent localIntent = new Intent(paramContext, NotificationBootService.class);
        localIntent.putExtra("action", "boot");
        paramContext.startService(localIntent);
    }
}
