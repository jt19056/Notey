package thomas.jonathan.notey;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/*Reciever to dismiss notifications.  Get the ID, delete from database, and cancel (remove) the notification from the tray.*/
public class NotificationDismiss extends BroadcastReceiver
{
    NotificationManager nm;

    @Override
    public void onReceive(Context paramContext, Intent paramIntent){
        MySQLiteHelper db = new MySQLiteHelper(paramContext);
        NoteyNote notey;
        int id = paramIntent.getExtras().getInt("NotificationID");
        notey = db.getNotey(id);
        db.deleteNotey(notey);

        nm = (NotificationManager) paramContext.getSystemService(paramContext.NOTIFICATION_SERVICE);
        nm.cancel(id);
    }
}
