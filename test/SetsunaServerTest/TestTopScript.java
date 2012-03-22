import java.util.Map;

import setsuna.core.event.*;

public class TestTopScript implements IScript {

    private String pfx = null;
    public TestTopScript (String pfx) {
        this.pfx = pfx;
    }

    public void execute(Map data) throws Exception {
        System.out.println(pfx + "=" + data);
    }
}
