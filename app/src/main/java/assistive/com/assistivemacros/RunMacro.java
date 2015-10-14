package assistive.com.assistivemacros;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Handler;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;
import java.util.Stack;

/**
 * Created by andre on 25-May-15.
 */
public class RunMacro {
    private final static String TAG = "MACROS";
    /**
     * Macro step
     */
    private static Step laststep;
    private static int countScrolls;
    private static final int THRESHOLD_SCROLL = 4;
    private static int failedTries;


    private static RunMacro mSharedInstance = null;
    // navigation variables
    final int FOWARD = 0;
    final int BACKWARD = 1;
    private final int THRESHOLD_GUARD = 50;
    int navDirection = FOWARD;
    private Stack<Step> command;
    private boolean runningMacro = false;
    private NodeList nodeList;
    private MacroService ms;
    private boolean toScroll;


    public RunMacro(Stack<Step> st, MacroService ms) {
        runningMacro = true;
        command = st;
        this.ms = ms;
        failedTries=0;
        checkStep();
        mSharedInstance = this;
    }

    public static RunMacro sharedInstance() {
        if (mSharedInstance != null && mSharedInstance.runningMacro)
            return mSharedInstance;
        else
            return null;
    }

    public synchronized void checkStep() {

        if (command.size() == 0 || failedTries>5|| !runningMacro) {
            runningMacro = false;
            playFinishMacro();
            return;
        }

        Step step = command.peek();
        nodeList = ms.refreshNodeList(step);

       // String split[] = step.split("!_!");
        String stepText = step.getText();
        if (!step.isValid() ) {
            command.pop();
            playFinishMacro();
            runningMacro=false;
            return;
        }

        if (nodeList!=null &&!searchAndClick(nodeList.getAccessibleNodesList(), step) ) {
            if(!toScroll){
                toScroll = true;
                Handler handler = new Handler();
                final String finalStep = stepText;
                handler.postDelayed(new Runnable() {
                    public void run() {
                        if (toScroll) {
                            toScroll = false;
                            //if it can scroll to find intended target then back
                           if(!scroll(nodeList.getScrollNodesList(), finalStep) ){
                               /*Log.d(MacroService.TAG, "BACK");
                               //fix back
                               ms.back();
                               //todo used to avoid double back before update, will cause problems in lists
                               toScroll=true;*/
                           }

                        }
                    }
                }, 500);
            }
        } else {
            countScrolls = 0;
            toScroll = false;
            return;
        }

        AccessibilityNodeInfo n;

        if (countScrolls > THRESHOLD_SCROLL) {
            if(laststep!=null&&!laststep.equals(step)) {
                command.push(laststep);
                countScrolls = 0;
                failedTries+=1;
                Log.d(TAG, "Pushed :" + laststep);
            }else{
                countScrolls = 0;
                Log.d(TAG, "FAILED :" + failedTries);
                failedTries+=1;

            }
            checkStep();
        }


    }

    private boolean scroll(ArrayList<AccessibilityNodeInfo> scrollNodesList, String step) {
        if (scrollNodesList.size() > 0) {
            for (AccessibilityNodeInfo n : scrollNodesList) {
                if (navDirection == FOWARD) {
                    if (!n.performAction(
                            AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD)) {
                        navDirection = BACKWARD;
                        countScrolls++;

                        n
                                .performAction(
                                        AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD);
                    }
                } else {
                    if (!n.performAction(
                            AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD)) {
                        countScrolls++;
                        navDirection = FOWARD;
                        n
                                .performAction(
                                        AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD);
                    }
                }

            }
            Handler handler = new Handler();

            handler.postDelayed(new Runnable() {
                public void run() {
                    checkStep();
                }
            }, 200);
            return true;
        }
        return false;

    }

    public synchronized boolean searchAndClick(ArrayList<AccessibilityNodeInfo> list, Step step) {
        int i =0;
        //Log.d(MacroService.TAG, "Searching:" + step.getText());
        for (AccessibilityNodeInfo n : list) {
            i++;
            String text = ms.getText(n);
           // Log.d(MacroService.TAG, "Searching:" + step.getText() + " alt:"+step.getAlt() + " text:"+text +" size:"+ list.size() + " i:"+i);

            if (text != null && (text.equals(step.getDescription()))) {
                int failSafe = 0;
                String result;
                do {
                    failSafe++;
                    result = clickNode(n);

                } while ((result == null || result.length() == 0)
                        && failSafe < THRESHOLD_GUARD);

                laststep = step;
                countScrolls = 0;
                if (failSafe < THRESHOLD_GUARD) {
                    command.pop();
                    if (command.size() == 0) {
                        runningMacro = false;
                        playFinishMacro();
                    }
                    checkStep();
                }
                Log.d(TAG, "FOUND:" +text + " in:" + step.getDescription());
                return true;
            }
        }
        return false;
    }

    public String clickNode(AccessibilityNodeInfo n) {
        boolean result = false;
        if (n != null) {

            if (n.isClickable()) {

                result = n.performAction(
                        AccessibilityNodeInfo.ACTION_CLICK);

            } else {
                if (n.getParent() != null) {

                    result = n

                            .getParent()
                            .performAction(
                                    AccessibilityNodeInfo.ACTION_CLICK);
                }


            }

            if (!result || ms.getText(n).length() < 1) {
                return null;
            }
            return ms.getText(n);
        }
        return "";
    }

    private void playFinishMacro() {
        final MediaPlayer mp = MediaPlayer.create(ms, R.raw.click);
        mp.start();
       /* Intent intent = new Intent();
        intent.setAction("running");
        ms.sendBroadcast(intent);*/
        Log.d(TAG, "stopService ahaha");
        ms.stopService(new Intent(ms, OverlayService.class));

    }

    public void stopExecution() {
        runningMacro=false;
    }
}
