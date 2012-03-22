package setsuna.core.util;

import java.util.*;

import setsuna.core.event.*;



/**
 * Event用のコンテナクラス<br>
 *
 * @author T.Okuyama
 */
public class EventContainer {

    public static String SCRIPT_TYPE_JAVA = "java";

    public static String SCRIPT_TYPE_JAVASCRIPT = "javascript";

    public static String SCRIPT_TYPE_SCALA = "scala";

    public static String SCRIPT_TYPE_GROOVY = "groovy";

    public static String SCRIPT_TYPE_PHP = "php";


    private String scriptType = null;

    private Object script = null;

    private String eventName = null;


    public String getEventName() {
        return this.eventName;
    }

    public EventContainer(String scriptType, Object script, String eventName) {
        this.scriptType = scriptType;
        this.script = script;
        this.eventName = eventName;
    }


    public void executeEvent(Map data) throws Exception {
        if(scriptType.equals(SCRIPT_TYPE_JAVA)) {
            ((IScript)script).execute(data);
        } else {
            throw new SetsunaException("It cannot perform in this script.");
        }
    }

}