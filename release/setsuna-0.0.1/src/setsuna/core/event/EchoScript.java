package setsuna.core.event;

import java.util.*;


/**
 * 渡された値を標準出力に出力するechoイベント用スクリプトクラス.<br>
 *
 * @author T.Okuyama
 */
public class EchoScript implements IScript {


    public EchoScript() {
    }

    public void execute(Map data) throws Exception {
        try {
            StringBuilder argStr = new StringBuilder(100);
            argStr.append("{");
            String sep = "";

            Set entrySet = data.entrySet();
            Iterator entryIte = entrySet.iterator(); 

            while(entryIte.hasNext()) {

                Map.Entry obj = (Map.Entry)entryIte.next();

                argStr.append(sep);
                argStr.append("\"");
                argStr.append(obj.getKey());
                argStr.append("\"");
                argStr.append(":");
                argStr.append("\"");
                argStr.append(obj.getValue());
                argStr.append("\"");
                sep = ",";
            }
            argStr.append("}");

            System.out.println(argStr.toString());
        } catch (Exception e) {
            throw e;
        }
    }
}