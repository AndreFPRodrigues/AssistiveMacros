package assistive.com.assistivemacros;

import android.app.AlertDialog;
import android.app.Service;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * Created by andre on 25-May-15.
 */
public class OverlayService extends Service {

    public static final String OVERLAY_SERVICE = "assistive.com.assistivemacros.OverlayService";
    public static final int IDLE = -1;
    public static  final int RECORDING = 0;
    public static  final int RUNNING = 1;
    public static  final int STANDBY = 2;
    public static  final int MACROLIST = 3;
    public static  final int STOP= 4;

    private WindowManager windowManager;
    private ImageView overlayButton;
    private WindowManager.LayoutParams params;
    public static  int mode = IDLE;
    private boolean expanded = false;
    private ListView listview;

    @Override
    public IBinder onBind(Intent intent) {
        // Not used
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(MacroService.TAG, " REMOVINaG:" + mode);
        if (overlayButton != null) windowManager.removeView(overlayButton);
        if (listview != null) windowManager.removeView(listview);
        mode=IDLE;


    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(windowManager==null)
            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        if(overlayButton==null)
            overlayButton = new ImageView(this);
        else{
            windowManager.removeView(overlayButton);
        }
        String state = intent.getAction();
        if (state == null)
            state = "";
        Log.d(MacroService.TAG, "init_state:" + state);
        switch (state) {
            case "running":
                overlayButton.setImageResource(R.drawable.stop);
                mode = RUNNING;
                Log.d(MacroService.TAG, "RUNNING");
                break;
            case "macroList":
                if (mode == IDLE || mode == MACROLIST||mode==STOP) {
                    mode = MACROLIST;
                    overlayButton.setImageResource(R.drawable.more);

                }
                break;
            default:
                Log.d(MacroService.TAG, "default:" + state);

                if(mode==STOP){
                    mode = IDLE;
                    break;
                }
                mode = STANDBY;
                overlayButton.setImageResource(R.drawable.record);
                Toast.makeText(getApplicationContext(), (String) getResources().getString(R.string.press_record),
                        Toast.LENGTH_LONG).show();
                break;
        }
        Log.d(MacroService.TAG, "end_state:" + mode);


        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = 400;
        params.y = 100;

        overlayButton.setOnTouchListener(new android.view.View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (event.getEventTime() - event.getDownTime() < 300) {
                            switch (mode) {
                                case RECORDING:
                                    setMacroName(MacroManagment.sharedInstance(), windowManager);


                                    break;
                                case RUNNING:
                                    MacroManagment.sharedInstance().stopExecution();
                                    stopSelf();
                                    break;
                                case STANDBY:
                                    MacroManagment mm = MacroManagment.sharedInstance();
                                    mm.createMacro();
                                    Toast.makeText(getApplicationContext(), (String) getResources().getString(R.string.recording),
                                            Toast.LENGTH_LONG).show();
                                    overlayButton.setImageResource(R.drawable.stop);
                                    mode = RECORDING;
                                    break;
                                case MACROLIST:
                                    if (expanded) {
                                        if (listview != null) {
                                            overlayButton.setImageResource(R.drawable.more);
                                            windowManager.removeView(listview);
                                            listview = null;
                                            Log.d(MacroService.TAG, "Removing");
                                        }
                                    } else {
                                        overlayButton.setImageResource(R.drawable.less);
                                        overlayList();
                                    }
                                    expanded = !expanded;
                                    break;
                            }

                        }
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(overlayButton, params);
                        return true;
                }
                return false;
            }

        });

        windowManager.addView(overlayButton, params);

        return START_STICKY;
    }

    private void overlayList() {
        listview = new ListView(this);
        listview.setBackgroundColor(Color.rgb(10, 151, 126));
        listview.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        MacroManagment mm = MacroManagment.sharedInstance();
        ArrayList<String> macros = mm.lastHookList();
        final ArrayAdapter<String> adapter = new ArrayAdapter(this, R.layout.macro_list_overlay, macros);
        listview.setAdapter(adapter);

        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, final View view,
                                    int position, long id) {
                final String macroname = (String) parent.getItemAtPosition(position);
                mode = RUNNING;
                overlayButton.setImageResource(R.drawable.stop);
                windowManager.removeView(listview);
                listview = null;
                MacroManagment.sharedInstance().runMacro(macroname);
            }

        });
        WindowManager.LayoutParams params1 = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.OPAQUE);
        params1.gravity = Gravity.CENTER_HORIZONTAL;

        windowManager.addView(listview, params1);
        windowManager.removeView(overlayButton);
        windowManager.addView(overlayButton, params);

    }

    //Alert window to name and save the macro or discard it
    private void setMacroName(final MacroManagment mm, final WindowManager windowManager) {

        // Set an EditText view to get user input
        final EditText input = new EditText(this);

        //Stop recording steps
        MacroManagment.sharedInstance().setRecord(false);

        AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setTitle("Set Macro Name")
                .setNegativeButton("Discard", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        mode=STOP;
                        MacroManagment.sharedInstance().discardMacro();
                        stopSelf();

                    }
                }).setView(input)
                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String value = input.getText().toString();
                        mm.setMacroName(value);
                        String hookActivity = mm.getCurrentHookActivity();
                        createShortcut(value, hookActivity);
                        mode=STOP;
                        MacroManagment.sharedInstance().finishMacro();
                        Intent i = new Intent(getApplicationContext(), OverlayService.class);
                        i.setAction("macroList");
                        stopService(i);
                        stopSelf();
                    }
                }).create();
        WindowManager.LayoutParams wmlp = alertDialog.getWindow().getAttributes();
        wmlp.gravity = Gravity.TOP | Gravity.LEFT;
        wmlp.x = 100;   //x position
        wmlp.y = 100;   //y position
        alertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        alertDialog.show();

    }

    private void createShortcut(String name, String hookActivity) {

        if (!isHomeScreen(hookActivity)) {
            return;
        }

        Intent shortcutIntent = new Intent(getApplicationContext(),
                RunMacroActivity.class);
        shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        shortcutIntent.setAction(Intent.ACTION_MAIN);


        shortcutIntent.putExtra("macro", name);

        Intent addIntent = new Intent();
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, name);
        addIntent.putExtra(
                Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                Intent.ShortcutIconResource.fromContext(
                        getApplicationContext(), R.mipmap.ic_launcher));
        addIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
        getApplicationContext().sendBroadcast(addIntent);


        Log.d(MacroService.TAG, "saving:" + name);

    }

    private boolean isHomeScreen(String hookActivity) {

        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        ResolveInfo defaultLauncher = getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        String nameOfLauncherPkg = defaultLauncher.activityInfo.packageName;
        return nameOfLauncherPkg.equals(hookActivity.split("/")[0]);
    }


}
