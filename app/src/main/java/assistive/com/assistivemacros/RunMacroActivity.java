package assistive.com.assistivemacros;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import static assistive.com.assistivemacros.AssistiveMacros.PlaceholderFragment.isAccessibilityEnabled;


public class RunMacroActivity extends Activity {
    private final static String LT = "Macro";
    private MacroManagment mm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_run_macro2);

        if(isAccessibilityEnabled(getApplicationContext(),MacroService.SERVICE_ID)) {
            mm = MacroManagment.sharedInstance();
            final String macro = getIntent().getStringExtra("macro");
            if (macro != null) {
                Log.d(LT, "Run:" + macro);
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    public void run() {
                        Intent i = new Intent(getApplicationContext(), OverlayService.class);
                        i.setAction("running");
                        startService(i);
                        mm.runMacro(macro);

                    }
                },500);
                finish();
            }

        }else{
            //send to accessibility settings
            Toast.makeText(getApplicationContext(), "Need to activate Assistive Macros", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
            finish();
        }


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_run_macro, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
