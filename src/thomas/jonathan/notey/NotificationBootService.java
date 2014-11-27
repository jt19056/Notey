package thomas.jonathan.notey;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;

import java.util.List;

public class NotificationBootService extends IntentService {
    final int CURRENT_ANDROID_VERSION = Build.VERSION.SDK_INT;
    int priority;
    boolean pref_expand, pref_swipe;
    String clickNotif, intentType, pref_priority;

    public NotificationBootService() {
        super("NotificationService");
    }

    @Override
    public void onStart(Intent intent, int startId) {
        MySQLiteHelper db = new MySQLiteHelper(this);
        List<NoteyNote> allNoteys = db.getAllNoteys();

        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        intentType = intent.getExtras().getString("action");
        priority = Notification.PRIORITY_DEFAULT;

        if (intentType == null) return;
        if (intentType.compareTo("boot") == 0) {

            //Settings stuff
            initializeSettings();

            for (NoteyNote n : allNoteys) {
                PendingIntent piDismiss = createOnDismissedIntent(this, n.getId());
                Intent editIntent = new Intent(this, MainActivity.class);
                editIntent.putExtra("editNotificationID", n.getId());
                editIntent.putExtra("editNote", n.getNote());
                editIntent.putExtra("editLoc", n.getSpinnerLoc());
                editIntent.putExtra("editButton", n.getImgBtnNum());
                editIntent.putExtra("editTitle", n.getTitle());
                PendingIntent piEdit = PendingIntent.getActivity(getApplicationContext(), n.getId(), editIntent, PendingIntent.FLAG_UPDATE_CURRENT);

                if(n.getTitle()!=null){ /*do nothing*/}  //for users on < v2.0.3, they didn't have 'title' column in db
                else  {
                    n.setTitle(getString(R.string.app_name));
                    db.updateNotey(n);
                }

                Bitmap bm;
                //big white icons are un-seeable on lollipop
                if(CURRENT_ANDROID_VERSION >= 21 && (n.getIcon() == R.drawable.ic_check_white_36dp
                    || n.getIcon() == R.drawable.ic_warning_white_36dp  || n.getIcon() == R.drawable.ic_edit_white_36dp
                        || n.getIcon() == R.drawable.ic_star_white_36dp || n.getIcon() == R.drawable.ic_whatshot_white_36dp)) {
                    bm = null;
                }
                else bm = BitmapFactory.decodeResource(getResources(), n.getIcon());

                Notification notif;
                if (pref_expand && CURRENT_ANDROID_VERSION >= 16) {
                    notif = new NotificationCompat.Builder(this)
                            .setContentTitle(n.getTitle())
                            .setContentText(n.getNote())
                            .setSmallIcon(n.getIcon())
                            .setLargeIcon(bm)
                            .setStyle(new NotificationCompat.BigTextStyle().bigText(n.getNote()))
                            .setDeleteIntent(piDismiss)
                            .setContentIntent(onNotifClickPI(clickNotif, n.getNote(), n.getId(), n.getSpinnerLoc(), n.getImgBtnNum(), n.getTitle()))
                            .setOngoing(!pref_swipe)
                            .setAutoCancel(true)
                            .setPriority(priority)
                            .addAction(R.drawable.ic_edit_white_24dp,
                                    getString(R.string.edit), piEdit)
                            .addAction(R.drawable.ic_delete_white_24dp,
                                    getString(R.string.remove), piDismiss)
                            .build();
                } else if (!pref_expand && CURRENT_ANDROID_VERSION >= 16) { //not expandable, but still able to set priority
                    notif = new NotificationCompat.Builder(this)
                            .setContentTitle(n.getTitle())
                            .setContentText(n.getNote())
                            .setSmallIcon(n.getIcon())
                            .setDeleteIntent(piDismiss)
                            .setContentIntent(onNotifClickPI(clickNotif, n.getNote(), n.getId(), n.getSpinnerLoc(), n.getImgBtnNum(), n.getTitle()))
                            .setOngoing(!pref_swipe)
                            .setAutoCancel(true)
                            .setPriority(priority)
                            .build();
                }
                else { //if api < 16
                    notif = new NotificationCompat.Builder(this)
                            .setContentTitle(n.getTitle())
                            .setContentText(n.getNote())
                            .setSmallIcon(n.getIcon())
                            .setContentIntent(onNotifClickPI(clickNotif, n.getNote(), n.getId(), n.getSpinnerLoc(), n.getImgBtnNum(), n.getTitle()))
                            .setDeleteIntent(piDismiss)
                            .setOngoing(!pref_swipe)
                            .build();
                }
                    nm.notify(n.getId(), notif);
            }
        }
        stopSelf();
    }

    private void initializeSettings(){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        if (CURRENT_ANDROID_VERSION >= 16) {
            pref_expand = sharedPreferences.getBoolean("pref_expand", true);
            pref_swipe = sharedPreferences.getBoolean("pref_swipe", false);

            pref_priority = sharedPreferences.getString("pref_priority", "normal");
            if (pref_priority.equals("high")) priority = Notification.PRIORITY_MAX;
            else if (pref_priority.equals("low"))
                priority = Notification.PRIORITY_LOW;
            else if (pref_priority.equals("minimum"))
                priority = Notification.PRIORITY_MIN;

        } else {
            pref_expand = false;
            pref_swipe = sharedPreferences.getBoolean("pref_swipe", true);
        }
        clickNotif = sharedPreferences.getString("clickNotif", "edit");
    }

    private PendingIntent createOnDismissedIntent(Context context, int notificationId) {
        Intent intent = new Intent(context, NotificationDismiss.class);
        intent.putExtra("NotificationID", notificationId);

        PendingIntent pendingIntent =
                PendingIntent.getBroadcast(context.getApplicationContext(),
                        notificationId, intent, 0);
        return pendingIntent;
    }

    private PendingIntent onNotifClickPI(String clickNotif, String note, int id, int spinnerLocation, int imageButtonNumber, String title) {
        if (clickNotif.equals("edit")) {
            Intent editIntent = new Intent(this, MainActivity.class);
            editIntent.putExtra("editNotificationID", id);
            editIntent.putExtra("editNote", note);
            editIntent.putExtra("editLoc", spinnerLocation);
            editIntent.putExtra("editButton", imageButtonNumber);
            editIntent.putExtra("editTitle", title);
            return PendingIntent.getActivity(getApplicationContext(), id, editIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        } else if (clickNotif.equals("remove")) {
            return createOnDismissedIntent(this, id);
        } else return null;
    }

    @Override
    protected void onHandleIntent(Intent intent) { /*empty*/ }
}