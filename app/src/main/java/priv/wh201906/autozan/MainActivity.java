package priv.wh201906.autozan;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.method.ScrollingMovementMethod;
import android.view.KeyEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends Activity {

    MyApplication app;
    TextView logView;
    TextView delayView;

    SharedPreferences settings;
    BroadcastReceiver ActivityReceiver;

    private void regReceiver() {
        IntentFilter filter=new IntentFilter();
        filter.addAction(MyApplication.LOGOUT_PRINTLINE);
        filter.addAction(MyApplication.LOGOUT_RELOAD);
        registerReceiver(ActivityReceiver,filter);
        filter=null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent=new Intent(this,MainService.class);
        startService(intent);

        app=(MyApplication)getApplication();
        logView=(TextView)findViewById(R.id.Main_LogView);
        delayView=(TextView)findViewById(R.id.Main_ZanTimeView);
        logView.setMovementMethod(ScrollingMovementMethod.getInstance());
        settings=getSharedPreferences("settings",Context.MODE_PRIVATE);

        ActivityReceiver=new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ((intent.getAction()!=null) && intent.getAction().equals(MyApplication.LOGOUT_PRINTLINE)) {
                    logView.append(intent.getStringExtra(MyApplication.LOGOUT_PRINTLINE));
                    int offset=logView.getLineCount()*logView.getLineHeight();
                    if (offset>logView.getHeight()) {
                        logView.scrollTo(0,offset-logView.getHeight());
                    }
                } else if (intent.getAction().equals(MyApplication.LOGOUT_RELOAD)) {
                    logView.setText(intent.getStringExtra(MyApplication.LOGOUT_RELOAD));
                    int offset=logView.getLineCount()*logView.getLineHeight();
                    if (offset>logView.getHeight()) {
                        logView.scrollTo(0,offset-logView.getHeight());
                    }
                }
            }
        };

        findViewById(R.id.Main_TutorialButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this,TutorialActivity.class));
            }
        });
        findViewById(R.id.Main_AccButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            }
        });
        findViewById(R.id.Main_LockButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    SharedPreferences.Editor editor=settings.edit();
                    if (settings.contains("LockTime")) {
                        Settings.System.putInt(getContentResolver(),Settings.System.SCREEN_OFF_TIMEOUT,settings.getInt("LockTime",15000));
                        app.LogPrintLine(settings.getInt("LockTime",15000)==Integer.MAX_VALUE?"无需恢复":"已恢复锁屏时间为"+String.valueOf(settings.getInt("LockTime",15000)/1000)+"秒");
                        editor.remove("LockTime");
                        editor.commit();
                    } else {
                        editor.putInt("LockTime",Settings.System.getInt(getContentResolver(),Settings.System.SCREEN_OFF_TIMEOUT));
                        editor.commit();
                        Settings.System.putInt(getContentResolver(),Settings.System.SCREEN_OFF_TIMEOUT,Integer.MAX_VALUE);
                        app.LogPrintLine(settings.getInt("LockTime",15000)==Integer.MAX_VALUE?"无需设置":"已保存锁屏时间为"+String.valueOf(settings.getInt("LockTime",15000)/1000)+"秒并设置为永不锁屏");
                    }
                } catch (Settings.SettingNotFoundException e) {
                    e.printStackTrace();
                }
            }
        });
        findViewById(R.id.Main_QQButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(getPackageManager().getLaunchIntentForPackage("com.tencent.mobileqq"));
            }
        });
        findViewById(R.id.Main_ContactButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String[] items=new String[] {"GitHub:wh201906","邮箱:wh201906@yandex.com"};
                new AlertDialog.Builder(MainActivity.this)
                        .setItems(items, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch (which) {
                                    case 0:
                                        startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://github.com/wh201906")));
                                        break;
                                    case 1:
                                        ClipboardManager cm=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
                                        if(cm!=null) {
                                            cm.setPrimaryClip(ClipData.newPlainText("Email","wh201906@yandex.com"));
                                            Toast.makeText(MainActivity.this,"已复制到剪贴板",Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(MainActivity.this,"复制到剪贴板失败",Toast.LENGTH_SHORT).show();
                                        }
                                        break;
                                }
                            }
                        })
                        .setTitle("联系方式")
                        .create()
                        .show();
            }
        });

        SeekBar seekbar=(SeekBar)findViewById(R.id.Main_ZanSeekBar);
        seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                delayView.setText(getString(R.string.Main_interval)+String.valueOf(progress2time(progress)/1000f)+"秒");
                app.setDelayTime(progress2time(progress));
                settings.edit().putInt("DelayTime",progress2time(progress)).commit();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        seekbar.setProgress(time2progress(settings.getInt("DelayTime",200)));
        delayView.setText(getString(R.string.Main_interval)+String.valueOf(progress2time(seekbar.getProgress())/1000f)+"秒");
        app.setDelayTime(progress2time(seekbar.getProgress()));

        CheckBox checkbox=(CheckBox)findViewById(R.id.Main_LogCheckBox);
        checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                settings.edit().putBoolean("isLogOut",isChecked).commit();
                app.setLogOut(isChecked);
                if(isChecked) {
                    logView.setVisibility(View.VISIBLE);
                    regReceiver();
                } else {
                    logView.setVisibility(View.INVISIBLE);
                    unregisterReceiver(ActivityReceiver);
                }
            }
        });

        checkbox.setChecked(settings.getBoolean("isLogOut",true));
        app.setLogOut(checkbox.isChecked());
        logView.setVisibility(app.isLogOut()?View.VISIBLE:View.INVISIBLE);
        if(app.isLogOut()) {
            logView.setVisibility(View.VISIBLE);
            regReceiver();
        } else {
            logView.setVisibility(View.INVISIBLE);
        }

        logView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                ClipboardManager cm=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
                if(cm!=null) {
                    cm.setPrimaryClip(ClipData.newPlainText("LogOut",logView.getText().toString().trim()));
                    Toast.makeText(MainActivity.this,"已复制到剪贴板",Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this,"复制到剪贴板失败",Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        });

        try {
            app.LogPrintLine("当前应用版本:"+this.getPackageManager().getPackageInfo(this.getPackageName(),0).versionName);
        } catch (Exception e) {
            app.LogPrintLine("错误:无法获取当前应用版本号");
        }
        app.LogPrintLine("当前系统版本:"+ Build.VERSION.RELEASE);
        app.LogPrintLine("当前SDK等级:"+ Build.VERSION.SDK_INT);
        app.LogPrintLine("(请打开本应用的辅助服务，步骤详见教程)");
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (logView.getText().toString().equals("")) {
            app.LogReload();
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if(keyCode==KeyEvent.KEYCODE_BACK) {
            if((System.currentTimeMillis()-app.getExiting())>=2000L) {
                app.setExiting(System.currentTimeMillis());
                Toast.makeText(MainActivity.this,"再按一次退出应用",Toast.LENGTH_SHORT).show();
                return true;
            } else {
                //app.getNManager().cancel(app.getNID());
                this.finish();
                System.exit(0);
                return true;
            }
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(ActivityReceiver);
        super.onDestroy();
    }

    private int progress2time(int progress)
    {
        if(progress<10)
            return progress*10+10;
        else
            return (progress-10)*50+100;
    }
    private int time2progress(int time)
    {
        if(time<=100)
            return time/10-1;
        else
            return (time-100)/50+10;
    }
}