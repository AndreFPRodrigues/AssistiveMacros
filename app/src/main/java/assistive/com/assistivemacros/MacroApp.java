package assistive.com.assistivemacros;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

public class MacroApp extends Activity {
    private final static String LT = "Macro";
    LinearLayout macroLayout;
    private MacroManagment mm;
    private String macroName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_run_macro);
        mm = MacroManagment.sharedInstance();
        final String macro = getIntent().getStringExtra("macro");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.run_macro, menu);

        return true;
    }

    public void newMacro(View v) {


               //add button overlay
                Intent i = new Intent(getApplicationContext(), OverlayService.class);
                startService(i);

        //go to homescreen
        Intent startMain = new Intent(Intent.ACTION_MAIN);
        startMain.addCategory(Intent.CATEGORY_HOME);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(startMain);


    }


    @Override
    public void onPause() {
        super.onPause();  // Always call the superclass method first
        finish();
    }


}
