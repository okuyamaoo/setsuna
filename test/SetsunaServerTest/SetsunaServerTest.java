
import java.util.*;
import java.io.*;
import java.sql.*;

import setsuna.core.*;
import setsuna.core.util.*;
import setsuna.core.adapter.*;
import setsuna.core.event.*;
import setsuna.core.query.*;


public class SetsunaServerTest extends Thread{

    public static SetsunaCore setsuna = new SetsunaCore();

    public SetsunaServerTest() {
    }


    public static void main(String[] args) {
        try {


            setsuna.executeAdapterEngine("TestTailAdapter");



            CauseContainer causeContainer = new CauseContainer();
            causeContainer.addCause("response", CauseContainer.CAUSE_TYPE_LIKE, "290"); 
            ConditionContainer conditionContainer = new ConditionContainer("SELECT BODY FROM TestTailAdapter WHERE BODY LIKE 'test2%' LIMIT 1");
            setsuna.executeQueryEngine("GrepQuery", "TestTailAdapter", causeContainer, conditionContainer);


            EventContainer eventContainer = new EventContainer(EventContainer.SCRIPT_TYPE_JAVA, new TestUserScript("Prefix1"), "Prefix1_Event");
            setsuna.executeEventEngine(eventContainer, "GrepQuery");
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}


