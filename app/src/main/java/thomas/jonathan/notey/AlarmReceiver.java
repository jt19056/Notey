package thomas.jonathan.notey;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/* Receives intent to start AlarmService.  This is to show the info screen when the alarm goes off.*/
public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent i = new Intent(context, AlarmService.class);

        //get alarm id and pass to the AlarmService
        int NoteID = intent.getExtras().getInt("alarmID");
        i.putExtra("alarmID", NoteID);

        context.startService(i);
    }
}
