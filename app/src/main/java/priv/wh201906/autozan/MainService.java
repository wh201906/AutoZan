package priv.wh201906.autozan;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.widget.RemoteViews;

public class MainService extends Service {

    NotificationManager NManager;
    MyApplication app;
    BroadcastReceiver MServiceReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        NManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        app=(MyApplication)getApplication();
        MServiceReceiver=new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ((intent.getAction()!=null) && intent.getAction().equals(MyApplication.NOTFICATION_CHANGE)) {
                    if (!app.getRunning()) {
                        app.setRunning(true);
                        Start();
                    } else {
                        app.setRunning(false);
                        Stop();
                    }
                    setNotification();
                }
            }
        };
        IntentFilter filter=new IntentFilter(MyApplication.NOTFICATION_CHANGE);
        registerReceiver(MServiceReceiver,filter);
        setNotification();
    }

    @Override
    public IBinder onBind(Intent intent) {

        return null;
    }

    void Start() {
        Intent intent=new Intent(MyApplication.ACCSERVICE_CHANGE);
        intent.putExtra("isRunning",MyApplication.ACCSERVICE_ENABLED);
        sendBroadcast(intent);
    }

    void Stop() {
        Intent intent=new Intent(MyApplication.ACCSERVICE_CHANGE);
        intent.putExtra("isRunning",MyApplication.ACCSERVICE_DISABLED);
        sendBroadcast(intent);
    }

    void setNotification() {
        RemoteViews remoteview = new RemoteViews(getPackageName(), R.layout.notification);
        Intent intent;
        PendingIntent pendingintent;
        if(app.getRunning())
            remoteview.setTextViewText(R.id.Notfication_Switch, "停止");
        else
            remoteview.setTextViewText(R.id.Notfication_Switch, "开始");
        remoteview.setTextViewText(R.id.Notfication_TittleView,"QQ自动点赞");
        intent=new Intent(MyApplication.NOTFICATION_CHANGE);
        pendingintent=PendingIntent.getBroadcast(this,0,intent,PendingIntent.FLAG_UPDATE_CURRENT);
        remoteview.setOnClickPendingIntent(R.id.Notfication_Switch,pendingintent);
        intent=new Intent(this,MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        pendingintent=PendingIntent.getActivity(this, 1,intent, PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = new Notification.Builder(this)
                .setContentIntent(pendingintent)
                .setContent(remoteview)
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(false)
                .setOngoing(true)
                .setSmallIcon(R.drawable.notification_icon)
                .build();
         startForeground(app.getNID(),notification);
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(MServiceReceiver);
        super.onDestroy();
    }
}
