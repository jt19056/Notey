package thomas.jonathan.notey;


import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;


public class AlarmService extends Service {

    private NotificationManager mManager;

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

//        if(action.equals("start_alarm")) {
        Bundle extras = intent.getExtras();
      //  String message = extras.getString("EXTRA");
            int NoteID = extras.getInt("alarmID");
            NoteyNote n = db.getNotey(NoteID);

            Intent infoIntent = new Intent(this, InfoScreenActivity.class);
            infoIntent.putExtra("infoNotificationID", n.getId());
            infoIntent.putExtra("infoNote", n.getNote());
            infoIntent.putExtra("infoLoc", n.getSpinnerLoc());
            infoIntent.putExtra("infoButton", n.getImgBtnNum());
            infoIntent.putExtra("infoTitle", n.getTitle());
            infoIntent.putExtra("infoAlarm", n.getAlarm());

            infoIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); //activity is being called outside of an activity (this is a service). so this flag must let android know we are positive we want this activity to start.
            startActivity(infoIntent);
//        }


//        mManager = (NotificationManager) this.getApplicationContext().getSystemService(this.getApplicationContext().NOTIFICATION_SERVICE);
//        Intent intent1 = new Intent(this.getApplicationContext(),MainActivity.class);
//
//        Notification notification = new Notification(R.drawable.ic_launcher,"This is a test message!", System.currentTimeMillis());
//        intent1.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP| Intent.FLAG_ACTIVITY_CLEAR_TOP);
//
//        PendingIntent pendingNotificationIntent = PendingIntent.getActivity( this.getApplicationContext(),0, intent1,PendingIntent.FLAG_UPDATE_CURRENT);
//        notification.flags |= Notification.FLAG_AUTO_CANCEL;
//        notification.setLatestEventInfo(this.getApplicationContext(), "AlarmManagerDemo", "This is a test message!", pendingNotificationIntent);
//
//        mManager.notify(0, notification);
    }

    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
    }

}
