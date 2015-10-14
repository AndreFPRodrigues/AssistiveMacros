package assistive.com.assistivemacros;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Stack;
import java.util.regex.Pattern;

import android.content.Intent;
import android.os.Environment;
import android.util.Log;
import android.widget.ArrayAdapter;

import javax.crypto.Mac;

public class MacroManagment {
    public static final int NAV_MACRO = 0;
    public static final int TOUCH_MACRO = 1;
    private final static String LT = "MACROS";
    private static MacroManagment mSharedInstance = null;
    private HashMap<String, ArrayList<String>> macroHooks;
    private ArrayList<String> lastHookedMacros;
    private ArrayList<Macro> mMacros;
    private long lastWrite = 0;
    private String filepath;
    private Macro currentMacro;
    private int macroMode = NAV_MACRO;
    private boolean recording = false;

    private MacroService ms;

    private RunMacro rm;

    public MacroManagment() {
        macroHooks = new HashMap<String, ArrayList<String>>();
        mMacros = new ArrayList<Macro>();
        filepath = Environment.getExternalStorageDirectory().toString()
                + "/macros.text";
        File f = new File(filepath);
        readMacrosFromFile(f);

    }

    public static MacroManagment sharedInstance() {
        if (mSharedInstance == null) {
            mSharedInstance = new MacroManagment();
        }
        return mSharedInstance;
    }

    private void readMacrosFromFile(File f) {
        try {
            Scanner scan = new Scanner(f);
            while (scan.hasNextLine()) {
                String line = scan.nextLine();
                String split[] = line.split(";");
                if (split.length > 1) {
                    Macro m = new Macro(split[0]);
                    String[] splitSteps = split[1].split(",");
                    for (String steps : splitSteps) {
                        String[] stepApp = steps.split("!_!");
                        String alt = "";
                        if (stepApp.length > 2)
                            alt = stepApp[1];
                        m.addStep(stepApp[0], alt, stepApp[stepApp.length - 1]);
                    }
                    addMacroHook(m);
                    mMacros.add(m);
                }

            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }


    }

    private void addMacroHook(Macro m) {
        ArrayList<String> macroNameList;
        if (macroHooks.containsKey(m.getHookActivity())) {
            macroNameList = macroHooks.get(m.getHookActivity());
        } else {
            macroNameList = new ArrayList<String>();
        }
        macroNameList.add(m.getName());
        macroHooks.put(m.getHookActivity(), macroNameList);

    }

    public void setAccessibleService(MacroService ms) {
        this.ms = ms;
    }

    public void createMacro() {
        Log.d(LT, "new macro");
        currentMacro = new Macro();
        // currentName = name;
        recording = true;
    }

    public boolean addStep(String text, String alt, String activity) {
        Log.d(LT, "text:" + text + " alt:" + alt + " act:" + activity);
        return currentMacro.addStep(text, alt, activity);
    }

    public boolean finishMacro() {
        long ls = System.currentTimeMillis();
        if ((ls - lastWrite) > 500) {
            lastWrite = ls;
            //macros.put(currentName, currentMacro);
            mMacros.add(currentMacro);
            addMacroHook(currentMacro);
            // code should be in the method bellow (bug issues with
            // screenReader)
            File file = new File(filepath);
            FileWriter fw;

            try {
                fw = new FileWriter(file, true);
                fw.write(currentMacro.getName() + ";");
                for (Step macro : currentMacro.getStepList()) {
                    fw.write(macro.toString() + ",");
                }
                fw.write("\n");
                fw.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
            macroMode = NAV_MACRO;
            return true;
        }
        return false;
    }

    public void runMacro(String macro) {
        ArrayList<Step> cm = getMacroStepsByName(macro);
        //macros.get(macro);
        Stack<Step> st = new Stack<Step>();
        for (Step s : cm) {
            st.add(0, s);
        }
        rm = new RunMacro(st, ms);
    }

    private ArrayList<Step> getMacroStepsByName(String macro) {
        for (Macro m : mMacros) {
            if (m.getName().equals(macro))
                return m.getStepList();
        }
        return null;

    }

    public boolean recordMacro() {
        return recording;
    }


    public void stopExecution() {
        if (rm != null) {
            rm.stopExecution();
        }
    }

    public void setMacroName(String macroName) {
        currentMacro.setName(macroName);
    }

    public void setRecord(boolean recording) {
        this.recording = recording;
    }

    public void discardMacro() {
        macroMode = NAV_MACRO;
        currentMacro = null;

    }

    public HashMap<String, ArrayList<String>> getMacroList() {
        Log.d(MacroService.TAG, "populate");

        HashMap<String, ArrayList<String>> cleanList = new HashMap<String, ArrayList<String>>();
        for (Macro m : mMacros) {
            String name = m.getName();
            ArrayList<Step> steps = m.getStepList();
            ArrayList<String> stepList = new ArrayList<String>();
            stepList.add(m.getHookActivity());
            for (Step step : steps) {
                stepList.add(step.getDescription());
            }
            cleanList.put(name, stepList);
        }

        return cleanList;
    }


    public void delete(String macroName) {
        //macros.remove(macroName);
        for (Macro m : mMacros) {
            if (m.getName().equals(macroName)) {
                mMacros.remove(m);
                break;
            }
        }
        rewriteMacroFiles();
        removeShortcutIcon(macroName);
    }

    private void rewriteMacroFiles() {
        File file = new File(filepath);
        FileWriter fw;

        try {
            fw = new FileWriter(file, false);
            for (Macro mc : mMacros) {
                fw.write(mc.macroFileString() + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void removeShortcutIcon(String app_name) {
        Intent shortcutInt = new Intent(ms.getApplicationContext(), RunMacroActivity.class);
        shortcutInt.setAction(Intent.ACTION_MAIN);

        Intent addInt = new Intent();
        addInt.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutInt);
        addInt.putExtra(Intent.EXTRA_SHORTCUT_NAME, app_name);
        addInt
                .setAction("com.android.launcher.action.UNINSTALL_SHORTCUT");// Broadcast the created intent
        ms.getApplicationContext().sendBroadcast(addInt);
        Log.d(MacroService.TAG, "DELETED MACRO SHORT:" + app_name);
    }


    public String getCurrentHookActivity() {
        return currentMacro.getHookActivity();
    }

    //get list of macros register in the activity save last list
    public ArrayList<String> macrosFromActivity(String activity) {
        lastHookedMacros = macroHooks.get(activity);
        return lastHookedMacros;
    }

    //get last list of macros from activity
    public ArrayList<String> lastHookList(){
        return lastHookedMacros;
    }
}
