
import java.util.*;
import java.io.*;
import java.sql.*;

import setsuna.core.*;
import setsuna.core.util.*;
import setsuna.core.adapter.*;
import setsuna.core.event.*;
import setsuna.core.query.*;


public class SetsunaServerTopTest extends Thread{

    public static SetsunaCore setsuna = new SetsunaCore();

    public SetsunaServerTopTest() {
    }


    public static void main(String[] args) {
        try {



            setsuna.executeAdapterEngine("TestTopAdapter");



            CauseContainer causeContainer = new CauseContainer();
            causeContainer.addCause("loadaverage1", CauseContainer.CAUSE_TYPE_OVER, "1.00"); 
            ConditionContainer conditionContainer = new ConditionContainer("select * from (SELECT avg(to_number(t1.cpuwa)) as waavg FROM (select cpuwa from TestTopAdapter order by logdate desc LIMIT 20) t1) t2 where t2.waavg > 5");
            setsuna.executeQueryEngine("TopQuery", "TestTopAdapter", causeContainer, conditionContainer);


            EventContainer eventContainer = new EventContainer(EventContainer.SCRIPT_TYPE_JAVA, new TestTopScript("PrefixTop"), "PrefixTop_Event");
            setsuna.executeEventEngine(eventContainer, "TopQuery");
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

}


