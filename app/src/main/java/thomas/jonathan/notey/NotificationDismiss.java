package thomas.jonathan.notey;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.CursorIndexOutOfBoundsException;
import android.database.sqlite.SQLiteException;

public class NotificationDismiss extends BroadcastReceiver {
    NotificationManager nm;

    @Override
    public void onReceive(Context paramContext, Intent paramIntent) {
        MySQLiteHelper db = new MySQLiteHelper(paramContext);
        NoteyNote notey;
        int id = paramIntent.getExtras().getInt("NotificationID");

        try {
            notey = db.getNotey(id);
            db.deleteNotey(notey);

            nm = (NotificationManager) paramContext.getSystemService(paramContext.NOTIFICATION_SERVICE);
            nm.cancel(id);
        } catch (SQLiteException e) {
            e.printStackTrace();
        } catch (CursorIndexOutOfBoundsException e){
            e.printStackTrace();
        } catch (Exception e){
            e.printStackTrace();
        }

    }
}
