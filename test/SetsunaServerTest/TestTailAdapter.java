
import setsuna.core.adapter.*;
import java.util.*;


public class TestTailAdapter implements IAdapter {

    private int seq = 0;
    private boolean stopFlg = false;

    public String getName() {
        return "TestTailAdapter";
    }

    public long getArraivalTime() {
        return 1000*60*60;
    }

    public String[] getDataColumnNames() {
        String[] columns = {"logdate", "body", "method", "response"};
        return columns;
    }

    public Map read() throws Exception {
        if (stopFlg) return null;
        Map data = new HashMap(4);
        data.put("logdate", new Date().toString());
        data.put("body", "xxxxxxtestyyyyyy" + seq);
        data.put("method", "POST");
        data.put("response", new Integer(seq).toString());
        seq++;
        try {
            if ((seq % 10) == 0) data.put("body", "test2yyyyyy" + seq);
        } catch(Exception e) {}

        return data;
    }

    public boolean stop() {
        this.stopFlg = true;
        return true;
    }
}