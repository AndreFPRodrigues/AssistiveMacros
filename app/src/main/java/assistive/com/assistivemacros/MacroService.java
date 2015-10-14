package assistive.com.assistivemacros;

import android.accessibilityservice.AccessibilityService;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.os.Handler;

import java.util.ArrayList;


/**
 * Created by andre on 25-May-15.
 */
public class MacroService extends AccessibilityService {
    public static final String SERVICE_ID = "assistive.com.assistivemacros/.MacroService";
    static final String TAG = "MACROS";
    private MacroManagment mm = null;
    private AccessibilityNodeInfo lastSource;
    private boolean delay;
    private String currentActivity="";
    private long lastEventTimestamp=0;

    /**
     * Gets the node text either getText() or contentDescription
     *
     * @param src
     * @return node text/description null if it doesnt have
     */
    public static String getText(AccessibilityNodeInfo src) {
        String text = null;

        if (src.getText() != null || src.getContentDescription() != null) {
            if (src.getText() != null){
                text = src.getText().toString();
            }
            else {
                text = src.getContentDescription().toString();
            }
            //src.recycle();
        }

        return text;
    }


    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        AccessibilityNodeInfo source = event.getSource();


        final RunMacro rm = RunMacro.sharedInstance();
        if (mm == null) {
            mm = MacroManagment.sharedInstance();
        }

        updateTree(event);
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            //check if activity change, if so check if there are available macros and launch service
            if(!currentActivity.equals(getActivityName(event))) {
                currentActivity = getActivityName(event);
                if(!mm.recordMacro() && rm==null)
                    checkMacroHooks(currentActivity);
            }
        }else
        if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_CLICKED) {

            //TODO record hierarchical position of the the macro
            //TODO record in json
            //TODO save to personal app space
            if (mm.recordMacro()) {
                String text = "";
                String alt="";
                if (event.getText().size() > 0) {
                    Log.d(TAG, event.getText().get(0).toString());
                    text = event.getText().get(0).toString();
                }

                source = event.getSource();
                if (source == null) {
                    mm.addStep(text, alt, currentActivity);
                    return;
                }
                //Log.d(TAG, "sourcE:" + source.toString());

                String sourceText = getDescription(source);

                 alt= sourceText;
                //Log.d(TAG, "label:" + sourceText);
                mm.addStep(text,alt,currentActivity);
            }
        }

        // Macro step
        if (rm != null && !delay) {
            delay = true;
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                public void run() {
                    rm.checkStep();
                    delay = false;
                }
            }, 300);
        }

    }

    private void checkMacroHooks(String currentActivity) {
        ArrayList <String> availableMacros = mm.macrosFromActivity(currentActivity);
        if(availableMacros!=null){
            Log.d(MacroService.TAG, " Adding MacroLIST:" );

            //add button overlay
            Intent i = new Intent(getApplicationContext(), OverlayService.class);
            i.setAction("macroList");
            startService(i);
        }else{
            //TODO remove dependency, checking if the overlay is from the macrolist if it is then it is removing it
            if(OverlayService.mode==OverlayService.MACROLIST) {
                Log.d(MacroService.TAG, " Removing MacroLIST:" );

                Intent i = new Intent(getApplicationContext(), OverlayService.class);
                i.setAction("macroList");
                stopService(i);
            }

        }

    }

    private synchronized void updateTree(AccessibilityEvent  event){
        AccessibilityNodeInfo source = event.getSource();
        if (source != null &&event.getEventTime()>lastEventTimestamp) {
            lastSource = getRootParent(source);
            lastEventTimestamp=event.getEventTime();
        }
    }

    private String getActivityName(AccessibilityEvent event) {
        ComponentName componentName = new ComponentName(
                event.getPackageName().toString(),
                event.getClassName().toString()
        );

        ActivityInfo activityInfo = tryGetActivity(componentName);
        boolean isActivity = activityInfo != null;
        if (isActivity)
            return componentName.flattenToShortString();
        return "";
    }

    /**
     * Get root parent from node source
     *
     * @param source
     * @return
     */
    private AccessibilityNodeInfo getRootParent(AccessibilityNodeInfo source) {
        AccessibilityNodeInfo current = source;
        while (current.getParent() != null) {
            AccessibilityNodeInfo oldCurrent = current;
            current = current.getParent();
            //oldCurrent.recycle();
        }
        return current;
    }

    /**
     * If creating macro is active it sends the text of the clicked node
     */
    public static String getDescription(AccessibilityNodeInfo src) {

        try {
            if (src != null) {
                String text;
                if ((text = getText(src)) != null)
                    return cleanText(text);
                else {
                    int numchild = src.getChildCount();
                    for (int i = 0; i < numchild; i++) {
                        if ((text = getText(src.getChild(i))) != null) {
                            return cleanText(text);
                        } else {
                            src.getChild(i).recycle();
                        }
                    }
                    src = src.getParent();
                    numchild = src.getChildCount();
                    for (int i = 0; i < numchild; i++) {
                        if ((text = getText(src.getChild(i))) != null) {
                            return cleanText(text);
                        } else {
                            src.getChild(i).recycle();
                        }
                    }
                }
            }
        } catch (Exception e) {
            return "";
        }
        return "";
    }

    private static String cleanText(String text) {
        String result = text.replaceAll("\"", " ");
        result = text.replaceAll("\'", " ");
        result = result.replaceAll("[\r\n]", " ");
        return result;
    }

    private ActivityInfo tryGetActivity(ComponentName componentName) {
        try {
            return getPackageManager().getActivityInfo(componentName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    @Override
    public void onInterrupt() {

    }

    @Override
    public void onServiceConnected() {
        mm = MacroManagment.sharedInstance();
        mm.setAccessibleService(this);
        Log.d(TAG, "CONNECTED");

    }

    public boolean home() {
        return performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME);

    }


    public NodeList refreshNodeList(Step step) {
        return NodeList.sharedInstance(lastSource, step);
    }


    public boolean back() {
        return performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);

    }
}
