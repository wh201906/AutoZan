package priv.wh201906.autozan;

import android.accessibilityservice.AccessibilityService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.os.SystemClock;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class AccService extends AccessibilityService
{

    MyApplication app;
    int viewState = MyApplication.BUTTON_NULL;
    int runningState = MyApplication.RUNNING_INITIALIZED;
    boolean isWindowChanged = false;
    AccessibilityNodeInfo accNodeInfo;
    BroadcastReceiver AServiceReceiver;

    final Runnable runnable = new Runnable()
    {

        @Override
        public void run()
        {

            List<AccessibilityNodeInfo> noMoreButtonList = null;

            app.LogPrintLine("自动点赞已开始");
            app.initCounter();
            do
            {
                if (!isRunning())
                {
                    return;
                }
                accNodeInfo = WaitCorrectWindow(); //等待点赞窗口
                if (accNodeInfo == null)
                    continue;
                ArrayList<AccessibilityNodeInfo> targetButtonList = getValidButton(accNodeInfo);
                if (targetButtonList.isEmpty())
                    continue;
                performClick(targetButtonList);

                app.LogPrintLine("当前页面点赞完成，将滚动到下一页");

                if (clickShowMoreButton(accNodeInfo))
                {
                    SystemClock.sleep(app.getDelayTime() + 1000);
                    accNodeInfo = WaitCorrectWindow();
                    if (accNodeInfo == null)
                        continue;
                }

                scrollDown(accNodeInfo);
                if (isWindowCorrect(accNodeInfo))
                    noMoreButtonList = accNodeInfo.findAccessibilityNodeInfosByText("暂无更多");
                else
                    noMoreButtonList = null;
            } while (noMoreButtonList == null || noMoreButtonList.isEmpty());

            app.LogPrintLine("点赞完成，共计为" + app.getCounter() + "人点赞");
            app.initCounter();
        }
    };

    @Override
    public void onCreate()
    {
        super.onCreate();
        app = (MyApplication) getApplication();
        AServiceReceiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                if ((intent.getAction() != null) && intent.getAction().equals(MyApplication.ACCSERVICE_CHANGE))
                {
                    switch (intent.getIntExtra("isRunning", -1))
                    {
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
    protected void onServiceConnected()
    {
        super.onServiceConnected();
        app.LogPrintLine("辅助服务状态:开启");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event)
    {
        if (runningState != MyApplication.RUNNING_INITIALIZED)
        {
            List<CharSequence> textList;

            int eventType = event.getEventType();
            if (eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED)
            {
                textList = event.getText();
                if (isButtonDone(textList))
                    viewState = MyApplication.BUTTON_DONE;
            }
            else if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)
            {
                if (event.getSource() == null)
                    return;
                if (!isWindowCorrect(event.getSource()))
                {
                    textList = event.getText();
                    Log.i("event.getText()", String.valueOf(textList.size()));
                    if (isButtonDone(textList))
                    {
                        viewState = MyApplication.BUTTON_DONE;
                    }
                    else if (textList.size() == 5 && textList.get(0).toString().contains("提醒") && textList.get(1).toString().contains("今日免费"))
                    {
                        List<AccessibilityNodeInfo> cancelButtons = getRootInActiveWindow().findAccessibilityNodeInfosByText("取消");
                        while (!cancelButtons.isEmpty())
                        {
                            cancelButtons.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            SystemClock.sleep(100);
                            cancelButtons = getRootInActiveWindow().findAccessibilityNodeInfosByText("取消");
                        }
                        viewState = MyApplication.BUTTON_DONE;

                    }
                    else
                        isWindowChanged = true;
                }

            }
            else if (eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED)
            {//app.LogPrintLine("TYPE_VIEW_SCROLLED\nPackageName:" + event.getPackageName().toString() + "\nEvent:"+event.toString()+"\nSource:"+event.getSource().toString());
                if (runningState != MyApplication.RUNNING_INITIALIZED)
                {
                    if (!event.getPackageName().toString().equals("com.tencent.mobileqq"))
                    {
                        //Log.v("info","窗口已改变");
                        isWindowChanged = true;
                    }
                }
            }
        }

        if (!event.getText().isEmpty()
                && event.getText().get(0).toString().contains("每天最多给"))
        {
            Log.i("event", "usual");
        }
    }

    @Override
    public void onInterrupt()
    {

    }

    @Override
    public boolean onUnbind(Intent intent)
    {
        app.LogPrintLine("辅助服务状态:关闭\n(请打开本应用的辅助服务，步骤详见教程)");
        return super.onUnbind(intent);
    }

    private boolean isButtonDone(List<CharSequence> textList)
    {
        if (textList == null)
            return false;
        if (textList.size() == 1)
        {
            String text = textList.get(0).toString();
            return (text.contains("每天最多") || text.contains("每天只能"));
        }
        return false;
    }

    private AccessibilityNodeInfo WaitCorrectWindow()
    {
        List<AccessibilityNodeInfo> targetNode1, targetNode2;
        AccessibilityNodeInfo rootWindow;
        boolean isCorrect;

        //Log.v("step","开始等待窗口");
        do
        {
            SystemClock.sleep(1000);
            rootWindow = getRootInActiveWindow();
        } while (!isWindowCorrect(rootWindow));
        isWindowChanged = false;
        return rootWindow;
    }

    private boolean isWindowCorrect(AccessibilityNodeInfo rootWindow)
    {
        List<AccessibilityNodeInfo> targetNode1, targetNode2;
        boolean isCorrect;

        if (rootWindow == null)
            rootWindow = getRootInActiveWindow();
        targetNode1 = rootWindow.findAccessibilityNodeInfosByText("设置");
        targetNode2 = rootWindow.findAccessibilityNodeInfosByText("赞了");
        isCorrect = rootWindow.getPackageName().toString().equals("com.tencent.mobileqq")
                && targetNode1 != null
                && !targetNode1.isEmpty()
                && targetNode2 != null
                && !targetNode2.isEmpty();
        return isCorrect;
    }

    private boolean isWindowCorrect() {return isWindowCorrect(null);}

    private boolean isRunning()
    {
        if (runningState != MyApplication.RUNNING_STOPPING && !isWindowChanged)
        {
            //Log.v("info","窗口正确");
            return true;
        }
        else if (runningState == MyApplication.RUNNING_STOPPING)
        {
            accNodeInfo = null;
            viewState = MyApplication.BUTTON_NULL;
            isWindowChanged = false;
            runningState = MyApplication.RUNNING_INITIALIZED;
            //Log.v("info","窗口错误_退出");
            app.LogPrintLine("自动点赞已停止");
            return false;
        }
        else
        {
            accNodeInfo = null;
            isWindowChanged = false;
            new Thread(runnable).start();
            //Log.v("info","窗口错误_重启");
            app.LogPrintLine("自动点赞已停止,等待回到点赞窗口");
            return false;
        }
    }

    private ArrayList<AccessibilityNodeInfo> getValidButton(AccessibilityNodeInfo parentNode)
    {
        ArrayList<AccessibilityNodeInfo> result = new ArrayList<AccessibilityNodeInfo>();
        if (parentNode == null)
            return result;
        result = (ArrayList<AccessibilityNodeInfo>) accNodeInfo.findAccessibilityNodeInfosByText("赞");
        Iterator<AccessibilityNodeInfo> it = result.iterator();
        while (it.hasNext())
        {
            AccessibilityNodeInfo nodeInfo = it.next();
            if (!(nodeInfo.getClassName().toString().equals("android.widget.ImageView")) || !(nodeInfo.isClickable()))
                it.remove();
        }
        return result;
    }

    private void performClick(ArrayList<AccessibilityNodeInfo> buttonList)
    {
        for (AccessibilityNodeInfo button : buttonList)
        {
            //app.LogPrintLine("正在点击当前页面第"+String.valueOf(i+1)+"个点赞按钮");
            viewState = MyApplication.BUTTON_HALF; //设置当前“赞”状态为未完成
            for (int j = 1; j <= 25; j++)
            { //循环点击单个按钮
                if (!isRunning())
                    return;
                button.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                SystemClock.sleep(app.getDelayTime());
                if (viewState == MyApplication.BUTTON_DONE)
                    break;
            }
            app.changeCounter();
            SystemClock.sleep(app.getDelayTime() + 200);
        }
    }

    private void scrollDown(AccessibilityNodeInfo parentNode)
    {
        if (!isWindowCorrect(parentNode))
            return;
        for (int i = 0; i < parentNode.getChildCount(); i++)
        {
            AccessibilityNodeInfo info = parentNode.getChild(i);
            if (info.getClassName().toString().equals("android.widget.AbsListView"))
            {
                if (!isRunning())
                    return;
                info.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
                break;
            }
        }
    }

    private boolean clickShowMoreButton(AccessibilityNodeInfo parentNode)
    {
        if (!isWindowCorrect(parentNode))
            return false;
        List<AccessibilityNodeInfo> showMoreButtonList = accNodeInfo.findAccessibilityNodeInfosByText("显示更多");
        if (showMoreButtonList != null && !showMoreButtonList.isEmpty())
        {
            showMoreButtonList.get(0).getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
            return true;
        }
        return false;
    }

    @Override
    public void onDestroy()
    {
        unregisterReceiver(AServiceReceiver);
        super.onDestroy();
    }
}
