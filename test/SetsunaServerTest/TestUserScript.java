import java.util.Map;

import setsuna.core.event.*;

public class TestUserScript implements IScript {

    private String pfx = null;
    public TestUserScript (String pfx) {
        this.pfx = pfx;
    }

    public void execute(Map data) throws Exception {
        System.out.println(pfx + "=" + data.get("body"));
    }
}
