package priv.wh201906.autozan;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
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
        app = (MyApplication) getApplication();
        MServiceReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ((intent.getAction() != null) && intent.getAction().equals(MyApplication.NOTFICATION_CHANGE)) {
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
        IntentFilter filter = new IntentFilter(MyApplication.NOTFICATION_CHANGE);
        registerReceiver(MServiceReceiver, filter);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(getPackageName(), "AutoZan", NotificationManager.IMPORTANCE_DEFAULT);
            NManager.createNotificationChannel(channel);
        }
        setNotification();
    }

    @Override
    public IBinder onBind(Intent intent) {

        return null;
    }

    void Start() {
        Intent intent = new Intent(MyApplication.ACCSERVICE_CHANGE);
        intent.putExtra("isRunning", MyApplication.ACCSERVICE_ENABLED);
        sendBroadcast(intent);
    }

    void Stop() {
        Intent intent = new Intent(MyApplication.ACCSERVICE_CHANGE);
        intent.putExtra("isRunning", MyApplication.ACCSERVICE_DISABLED);
        sendBroadcast(intent);
    }

    void setNotification() {
        RemoteViews remoteview = new RemoteViews(getPackageName(), R.layout.notification);
        Intent intent;
        PendingIntent pendingintent;
        if (app.getRunning())
            remoteview.setTextViewText(R.id.Notfication_Switch, "停止");
        else
            remoteview.setTextViewText(R.id.Notfication_Switch, "开始");
        remoteview.setTextViewText(R.id.Notfication_TittleView, "QQ自动点赞");
        intent = new Intent(MyApplication.NOTFICATION_CHANGE);
        pendingintent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteview.setOnClickPendingIntent(R.id.Notfication_Switch, pendingintent);
        intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        pendingintent = PendingIntent.getActivity(this, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Builder nBuilder = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            nBuilder = new Notification.Builder(this, getPackageName());
        else
            nBuilder = new Notification.Builder(this);
        nBuilder.setContentIntent(pendingintent);
        nBuilder.setContent(remoteview);
        nBuilder.setWhen(System.currentTimeMillis());
        nBuilder.setAutoCancel(false);
        nBuilder.setOngoing(true);
        nBuilder.setSmallIcon(R.drawable.notification_icon);
        startForeground(app.getNID(), nBuilder.build());

    }

    @Override
    public void onDestroy() {
        unregisterReceiver(MServiceReceiver);
        super.onDestroy();
    }
}
