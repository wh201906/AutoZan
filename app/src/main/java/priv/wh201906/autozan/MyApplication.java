package priv.wh201906.autozan;

import android.app.Application;
import android.content.Intent;

import java.util.Calendar;

public class MyApplication extends Application
{

    static final String NOTFICATION_CHANGE = "priv.wh201906.autozan.SWITCHBUTTONCLICKED";
    static final String ACCSERVICE_CHANGE = "priv.wh201906.autozan.ACCSERVICECHANGE";
    static final String LOGOUT_RELOAD = "priv.wh201906.autozan.LOGOUTRELOAD";
    static final String LOGOUT_PRINTLINE = "priv.wh201906.autozan.LOGOUTPRINTLINE";


    static final int ACCSERVICE_ERROR = -1;
    static final int ACCSERVICE_DISABLED = 0;
    static final int ACCSERVICE_ENABLED = 1;

    static final int BUTTON_NULL = -1;
    static final int BUTTON_HALF = 0;
    static final int BUTTON_DONE = 1;

    static final int RUNNING_INITIALIZED = 0;
    static final int RUNNING_RUNNING = 1;
    static final int RUNNING_STOPPING = -1;

    private boolean Running;
    private long Exiting = 0L;
    private int NID;
    private int DelayTime = 200;
    private boolean isLogOut = true;
    private String Logtext = "";
    private int counter = 0;
    private int subcounter = 0;

    @Override
    public void onCreate()
    {
        super.onCreate();
        Running = false;
        Exiting = 0L;
        NID = Math.abs((int) System.currentTimeMillis());
    }

    public void setRunning(boolean running)
    {
        this.Running = running;
    }

    public boolean getRunning()
    {
        return this.Running;
    }

    public void setExiting(long exiting)
    {
        this.Exiting = exiting;
    }

    public long getExiting()
    {
        return this.Exiting;
    }

    public int getNID()
    {
        return NID;
    }

    public void setDelayTime(int delayTime)
    {
        DelayTime = delayTime;
    }

    public int getDelayTime()
    {
        return DelayTime;
    }

    public void setLogOut(boolean logOut)
    {
        isLogOut = logOut;
    }

    public boolean isLogOut()
    {
        return isLogOut;
    }

    public void initCounter()
    {
        counter = 0;
        subcounter = 0;
    }

    public void changeCounter()
    {
        counter++;
        subcounter++;
        if (subcounter >= 10)
        {
            LogPrintLine("已给" + counter + "人点赞");
            subcounter = 0;
        }
    }

    public int getCounter()
    {
        return counter;
    }


    public void LogPrintLine(String logtext)
    {
        if (this.isLogOut())
        {
            Calendar calendar = Calendar.getInstance();
            String text = "\n" + calendar.get(Calendar.HOUR_OF_DAY) + ":" + calendar.get(Calendar.MINUTE) + ":" + calendar.get(Calendar.SECOND) + " " + logtext;
            Logtext = Logtext + text;
            sendBroadcast(new Intent(LOGOUT_PRINTLINE).putExtra(LOGOUT_PRINTLINE, text));
        }
    }

    public void LogReload()
    {
        sendBroadcast(new Intent(LOGOUT_RELOAD).putExtra(LOGOUT_RELOAD, Logtext));
    }


}
