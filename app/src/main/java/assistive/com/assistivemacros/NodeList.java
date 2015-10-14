package assistive.com.assistivemacros;

import android.graphics.Rect;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;

/**
 * Created by andre on 25-May-15.
 */
public class NodeList {
    private  ArrayList<AccessibilityNodeInfo> scrollNodes ;
    private  ArrayList<AccessibilityNodeInfo> nodeList;
    private static AccessibilityNodeInfo lastSource =null;
    private static Step lastStep=null;
    private static NodeList nl=null;

    public NodeList(AccessibilityNodeInfo source){
        nodeList = new ArrayList<AccessibilityNodeInfo>();
        scrollNodes = new ArrayList<AccessibilityNodeInfo>();
        listUpdate(source);
    }

    public static NodeList sharedInstance(AccessibilityNodeInfo source, Step step){
        if(source!=null){
            nl= new NodeList(source);
            lastStep=step;
        }
        return nl;
    }


    /**
     * Rebuild list with the current content of the view
     *
     * @param source
     */
    private synchronized void listUpdate(AccessibilityNodeInfo source) {
        AccessibilityNodeInfo child;
        Rect outBounds = new Rect();

        AccessibilityNodeInfo n;
        boolean addScroll = false;
        if(source!=null) {
            for (int i = 0; i < source.getChildCount(); i++) {
                child = source.getChild(i);

                if (child != null) {
                    if (child.isScrollable() && !addScroll) {
                        source.getBoundsInScreen(outBounds);
                        addScroll = true;
                        scrollNodes.add(child);
                    }
                    if (child.getChildCount() == 0) {

                        outBounds = new Rect();
                        if (source.getClassName().toString().contains("Linear")) {
                            source.getBoundsInScreen(outBounds);

                        } else
                            child.getBoundsInScreen(outBounds);
                        if ((outBounds.centerX() > 0 && outBounds.centerY() > 0)) {

                            nodeList.add(child);
                        }
                    } else {
                        child.getBoundsInScreen(outBounds);

                        nodeList.add(child);

                        listUpdate(child);
                    }
                }
            }
            lastSource=source;
        }

    }

    public ArrayList <AccessibilityNodeInfo> getAccessibleNodesList() {
        return nodeList;
    }

    public ArrayList <AccessibilityNodeInfo>  getScrollNodesList() {
        return scrollNodes;
    }
}
