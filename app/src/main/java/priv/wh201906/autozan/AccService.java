package priv.wh201906.autozan;

import android.accessibilityservice.AccessibilityService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.annotation.Nullable;

import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class AccService extends AccessibilityService {

    MyApplication app;
    int viewState = MyApplication.BUTTON_NULL;
    int runningState = MyApplication.RUNNING_INITIALIZED;
    boolean isWindowChanged = false;
    AccessibilityNodeInfo accNodeInfo;
    BroadcastReceiver AServiceReceiver;

    final Runnable runnable = new Runnable() {

        @Override
        public void run() {

            List<AccessibilityNodeInfo> tempNodeInfo1 = null;
            List<AccessibilityNodeInfo> tempNodeInfo2 = null;

            if (!isCorrectWindow()) {
                return;
            }
            app.LogPrintLine("自动点赞已开始");
            app.initCounter();
            try {
                do {
                    do {
                        if (!isCorrectWindow()) {
                            return;
                        }
                        accNodeInfo = WaitCorrectWindow(); //等待点赞窗口
                        ArrayList<AccessibilityNodeInfo> targetButtonList = getValidButton(accNodeInfo);
                        if (targetButtonList.isEmpty())
                            continue;
                        performClick(targetButtonList);

                        app.LogPrintLine("当前页面点赞完成，将滚动到下一页");

                        scrollDown(accNodeInfo);

                        tempNodeInfo1 = accNodeInfo.findAccessibilityNodeInfosByText("显示更多");
                        tempNodeInfo2 = accNodeInfo.findAccessibilityNodeInfosByText("暂无更多");
                    } while ((tempNodeInfo1 == null
                            || tempNodeInfo1.isEmpty())
                            && (tempNodeInfo2 == null
                            || tempNodeInfo2.isEmpty()));

                    //tempNodeInfo1=null;
                    //tempNodeInfo2=null;

                    Log.v("step", "循环点击若干页(到“显示更多”)中的每个“赞”**********End");
                    //循环点击若干页(到“显示更多”)中的每个“赞”**********End
                    if (!isCorrectWindow()) {
                        return;
                    }

                    if (accNodeInfo.findAccessibilityNodeInfosByText("显示更多").size() == 1) {
                        //点击“显示更多”并向下滚动一页**********Start
                        Log.v("step", "点击“显示更多”并向下滚动一页**********Start");
                        accNodeInfo.findAccessibilityNodeInfosByText("显示更多").get(0).getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        Thread.sleep(app.getDelayTime() + 1000);
                        //点击完成，下滚
                        accNodeInfo = WaitCorrectWindow();
                        Log.v("step", "已获得窗口");
                        for (int i = 0; i < accNodeInfo.getChildCount(); i++) {//滚动
                            AccessibilityNodeInfo info = accNodeInfo.getChild(i);
                            if (info.getClassName().toString().equals("android.widget.AbsListView")) {
                                info.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
                                break;
                            }
                        }
                        //Log.v("step","点击“显示更多”并向下滚动一页**********End");
                        //点击“显示更多”并向下滚动一页**********Rnd
                    }

                    Log.i("msg", String.valueOf(accNodeInfo.findAccessibilityNodeInfosByText("暂无更多").size()));
                    tempNodeInfo1 = accNodeInfo.findAccessibilityNodeInfosByText("暂无更多");
                } while (tempNodeInfo1.isEmpty() || tempNodeInfo1.size() == 0);
                //tempNodeInfo1=null;
                Log.v("step", "循环点击所有“赞”**********End");
                //循环点击所有“赞”**********End
                app.LogPrintLine("点赞完成，共计为" + String.valueOf(app.getCounter()) + "人点赞");
                app.initCounter();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        app = (MyApplication) getApplication();
        AServiceReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ((intent.getAction() != null) && intent.getAction().equals(MyApplication.ACCSERVICE_CHANGE)) {
                    switch (intent.getIntExtra("isRunning", -1)) {
                        case MyApplication.ACCSERVICE_ERROR:

                            break;
                        case MyApplication.ACCSERVICE_ENABLED:
                            runningState = MyApplication.RUNNING_RUNNING;
                            new Thread(runnable).start();
                            break;
                        case MyApplication.ACCSERVICE_DISABLED:
                            runningState = MyApplication.RUNNING_STOPPING;
                            break;

                    }
                }
            }
        };

        IntentFilter filter = new IntentFilter(MyApplication.ACCSERVICE_CHANGE);
        registerReceiver(AServiceReceiver, filter);
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        app.LogPrintLine("辅助服务状态:开启");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (runningState != MyApplication.RUNNING_INITIALIZED) {
            List<AccessibilityNodeInfo> tempNodeInfo1;
            List<AccessibilityNodeInfo> tempNodeInfo2;
            List<CharSequence> list;

            switch (event.getEventType()) {
                case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:
                    //Log.v("info","TYPE_NOTIFICATION_STATE_CHANGED\nPackageName:" + event.getPackageName().toString() + "\nEvent:"+event.toString()+"\nSource:"+(event.getSource()==null?"null":event.getSource().toString()));

                    list = event.getText();
                    if (list.size() == 1) {
                        //Log.v("test",list.get(0).toString());
                        if (list.get(0).toString().contains("每天最多"))
                            viewState = MyApplication.BUTTON_DONE;
                    }
                    break;
                case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:

                    if (runningState != MyApplication.RUNNING_INITIALIZED) {
                        tempNodeInfo1 = event.getSource().findAccessibilityNodeInfosByText("设置");
                        tempNodeInfo2 = event.getSource().findAccessibilityNodeInfosByText("赞了");
                        if (!event.getPackageName().toString().equals("com.tencent.mobileqq")
                                || tempNodeInfo1.isEmpty()
                                || tempNodeInfo1.size() == 0
                                || tempNodeInfo2.isEmpty()
                                || tempNodeInfo2.size() == 0) {
                            //tempNodeInfo1=null;
                            //tempNodeInfo2=null;
                            list = event.getText();
                            if (list.size() == 1) {
                                if (list.get(0).toString().contains("每天最多")) {
                                    viewState = MyApplication.BUTTON_DONE;
                                } /*else if (event.getClassName().toString().contains("VisitorsActivity")) {

                                } */ else {
                                    //Log.v("info","窗口已改变");
                                    isWindowChanged = true;
                                }
                            } else if (list.size() == 5) {
                                if (list.get(0).toString().contains("提醒") && list.get(1).toString().contains("今日免费")) {
                                    getRootInActiveWindow()
                                            .findAccessibilityNodeInfosByText("取消").get(0).
                                            performAction(AccessibilityNodeInfo.ACTION_CLICK);
                                    viewState = MyApplication.BUTTON_DONE;
                                } else {
                                    //Log.v("info","窗口已改变");
                                    isWindowChanged = true;
                                }

                            } else {
                                //Log.v("info","窗口已改变");
                                isWindowChanged = true;
                            }
                        }
                    }
                    break;
                case AccessibilityEvent.TYPE_VIEW_SCROLLED:
                    //app.LogPrintLine("TYPE_VIEW_SCROLLED\nPackageName:" + event.getPackageName().toString() + "\nEvent:"+event.toString()+"\nSource:"+event.getSource().toString());
                    if (runningState != MyApplication.RUNNING_INITIALIZED) {
                        if (!event.getPackageName().toString().equals("com.tencent.mobileqq")) {
                            //Log.v("info","窗口已改变");
                            isWindowChanged = true;
                        }
                    }
                    break;

            }
        }

        if (!event.getText().isEmpty()
                && event.getText().get(0).toString().contains("每天最多给")) {
            Log.i("event", "usual");
        } else {

//            Log.i("event",
//                    //"PackageName:"+event.getPackageName().toString()+"\n" +
//                    "Type:" + ((event.getEventType()) == (AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)
//                            ? "NOTIFICATION_STATE_CHANGED\n"
//                            + "PackageName:" + event.getPackageName().toString() + "\n"
//                            + "\n群主" + String.valueOf(getRootInActiveWindow().findAccessibilityNodeInfosByText("群主"))
//                            + "\n返回" + String.valueOf(getRootInActiveWindow().findAccessibilityNodeInfosByText("返回"))
//                            + "\n群聊成员" + String.valueOf(getRootInActiveWindow().findAccessibilityNodeInfosByText("群聊成员"))
//                            + "\n排序" + String.valueOf(getRootInActiveWindow().findAccessibilityNodeInfosByText("排序"))
//                            + "\n更多" + String.valueOf(getRootInActiveWindow().findAccessibilityNodeInfosByText("更多")) : ""));
            AccessibilityNodeInfo temp1 = getRootInActiveWindow();
            if(temp1==null)
                return;

        }
    }

    @Override
    public void onInterrupt() {

    }

    @Override
    public boolean onUnbind(Intent intent) {
        app.LogPrintLine("辅助服务状态:关闭\n(请打开本应用的辅助服务，步骤详见教程)");
        return super.onUnbind(intent);
    }

    @Nullable
    private AccessibilityNodeInfo WaitCorrectWindow() {
        List<AccessibilityNodeInfo> tempNodeInfo1;
        List<AccessibilityNodeInfo> tempNodeInfo2;
        AccessibilityNodeInfo info;
        boolean isCorrect = false;

        //Log.v("step","开始等待窗口");
        try {
            do {
                Thread.sleep(1000);
                info = getRootInActiveWindow();
                Log.i("RootWindow",info.toString());
                tempNodeInfo1 = info.findAccessibilityNodeInfosByText("设置");
                tempNodeInfo2 = info.findAccessibilityNodeInfosByText("赞了");
                isCorrect = info.getPackageName().toString().equals("com.tencent.mobileqq")
                        && !tempNodeInfo1.isEmpty()
                        && !(tempNodeInfo1.size() == 0)
                        && !tempNodeInfo2.isEmpty()
                        && !(tempNodeInfo2.size() == 0);
            } while (!isCorrect);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        //tempNodeInfo1=null;
        //tempNodeInfo2=null;
        //Log.v("step","结束等待窗口");
        //Log.v("info","设置:"+String.valueOf(info.findAccessibilityNodeInfosByText("设置").isEmpty()));
        //Log.v("info","赞了:"+String.valueOf(info.findAccessibilityNodeInfosByText("赞了").isEmpty()));
        isWindowChanged = false;
        return info;
    }

    private void showNodeTree(AccessibilityNodeInfo accessibilityNodeInfo) {
        if (accessibilityNodeInfo == null)
            return;
        Log.v("TAG", "找到了view的文本是:" + accessibilityNodeInfo.getText());
        Log.v("TAG", "当前应用的包名是:" + accessibilityNodeInfo.getPackageName() + accessibilityNodeInfo.getClassName());
        if (accessibilityNodeInfo.getChildCount() != 0) {
            for (int i = 0; i < accessibilityNodeInfo.getChildCount(); i++) {
                showNodeTree(accessibilityNodeInfo.getChild(i));
            }
        }
    }

    private boolean isCorrectWindow() {
        if (runningState != MyApplication.RUNNING_STOPPING && !isWindowChanged) {
            //Log.v("info","窗口正确");
            return true;
        } else if (runningState == MyApplication.RUNNING_STOPPING) {
            accNodeInfo = null;
            viewState = MyApplication.BUTTON_NULL;
            isWindowChanged = false;
            runningState = MyApplication.RUNNING_INITIALIZED;
            //Log.v("info","窗口错误_退出");
            app.LogPrintLine("自动点赞已停止");
            return false;
        } else {
            accNodeInfo = null;
            isWindowChanged = false;
            new Thread(runnable).start();
            //Log.v("info","窗口错误_重启");
            app.LogPrintLine("自动点赞已停止,等待回到点赞窗口");
            return false;
        }
    }

    private ArrayList<AccessibilityNodeInfo> getValidButton(AccessibilityNodeInfo parentNode) {
        ArrayList<AccessibilityNodeInfo> result = new ArrayList<AccessibilityNodeInfo>();
        if (parentNode == null)
            return result;
        result = (ArrayList<AccessibilityNodeInfo>) accNodeInfo.findAccessibilityNodeInfosByText("赞");
        Iterator<AccessibilityNodeInfo> it = result.iterator();
        while (it.hasNext()) {
            AccessibilityNodeInfo nodeInfo = it.next();
            if (!(nodeInfo.getClassName().toString().equals("android.widget.ImageView")) || !(nodeInfo.isClickable()))
                it.remove();
        }
        return result;
    }

    private void performClick(ArrayList<AccessibilityNodeInfo> buttonList) {
        try {
            for (AccessibilityNodeInfo button : buttonList) {
                //app.LogPrintLine("正在点击当前页面第"+String.valueOf(i+1)+"个点赞按钮");
                viewState = MyApplication.BUTTON_HALF; //设置当前“赞”状态为未完成
                for (int j = 1; j <= 25; j++) { //循环点击单个按钮
                    if (!isCorrectWindow()) {
                        return;
                    }
                    button.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    Thread.sleep(app.getDelayTime());
                    if (viewState == MyApplication.BUTTON_DONE) {
                        break;
                    }
                }
                app.changeCounter();
                Thread.sleep(app.getDelayTime() + 200);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void scrollDown(AccessibilityNodeInfo parentNode){
        for (int i = 0; i < parentNode.getChildCount(); i++) {
            AccessibilityNodeInfo info = parentNode.getChild(i);
            if (info.getClassName().toString().equals("android.widget.AbsListView")) {
                if (!isCorrectWindow()) {
                    return;
                }
                info.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
                break;
            }
        }
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(AServiceReceiver);
        super.onDestroy();
    }
}
