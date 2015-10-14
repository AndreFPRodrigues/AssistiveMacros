package assistive.com.assistivemacros;

/**
 * Created by andre on 12-Oct-15.
 */
public class Step{
    private String text;
    private String alt;
    private String activity;

    public Step(String text, String alt, String activity) {
        this.text = text;
        this.alt = alt;
        this.activity = activity;
    }

    public String getText() {
        return text;
    }

    @Override
    public String toString(){
        return text+"!_!"+alt+"!_!"+ activity;
    }

    public String getAlt() {
        return alt;
    }

    public boolean isValid() {
        return text.length()>0|| alt.length()>0;
    }

    public String getDescription() {
        if(text.length()>0)
            return text;
        else
            return alt;
    }
}