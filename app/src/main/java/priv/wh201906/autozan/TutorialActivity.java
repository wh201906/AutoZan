package priv.wh201906.autozan;

import android.app.Activity;
import android.os.Bundle;

public class TutorialActivity extends Activity
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutorial);
    }

    @Override
    protected void onPause()
    {
        finish();
        super.onPause();
    }

}
