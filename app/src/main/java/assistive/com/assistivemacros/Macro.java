package assistive.com.assistivemacros;

import java.util.ArrayList;

/**
 * Created by andre on 09-Oct-15.
 */
public class Macro {
    private String hookActivity;
    private ArrayList<Step> steps;
    private String name;

    public Macro(String name)
    {
        this.name = name;
        steps = new ArrayList<Step>();
        hookActivity=null;
    }

    public Macro() {
        name="toBeDefined";
        steps = new ArrayList<Step>();
        hookActivity=null;

    }

    public boolean addStep(String text, String alt, String activity){
        if(hookActivity==null)
            hookActivity = activity;
        return steps.add(new Step(text, alt, activity));
    }

    public String getName() {
        return name;
    }

    public ArrayList<Step> getStepList() {

        return steps;
    }
    public void setName(String name) {
        this.name = name;
    }




    @Override
    public String toString() {
        String result = "From:" + hookActivity;
        for (Step step : steps) {
            result += "\n" +step.getText();
        }
    return result;
    }

    public String getHookActivity() {
        return hookActivity;
    }

    public String macroFileString(){
        String result=name+";";
        for(Step st : steps){
            result+=st.toString()+",";
        }
        return result;
    }
}
