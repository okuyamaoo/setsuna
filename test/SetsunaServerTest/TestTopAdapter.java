
import setsuna.core.adapter.*;
import java.util.*;
import java.io.*;

public class TestTopAdapter implements IAdapter {

    private Runtime runtime = null;
    private Process process = null;
    private InputStream is = null;
    private BufferedReader in = null;

    private int seq = 0;
    private boolean stopFlg = false;

    public TestTopAdapter() throws Exception {
        String commandStr = "top -b -d 1 -U okuyama";
        this.runtime = Runtime.getRuntime();
        this.process = this.runtime.exec(commandStr.split(" "));
        this.is = this.process.getInputStream();
        this.in = new BufferedReader(new InputStreamReader(is));
    }

    public String getName() {
        return "TestTopAdapter";
    }

    public long getArraivalTime() {
        return 1000*60*60;
    }

    public String[] getDataColumnNames() {
        String[] columns = {"logdate", "loadaverage1", "loadaverage5", "loadaverage10", "cpuuser", "cpusys", "cpuwa", "memtotal", "memused", "memfree", "swaptotal", "swapused", "cached"};
        return columns;
    }

    public Map read() throws Exception {

        String line = null;
        Map data = new HashMap(12);

        while (true) {
            line = this.in.readLine();
            if (line == null || stopFlg == true) {
                this.process.destroy();
                return null;
            }

            while(true) {
                line = line.replaceAll("  ", " ");
                if (line.indexOf("  ") == -1) break;
            }

            if (line.indexOf("top") == 0) {
                String[] lineSplit = line.split(" ");
                data.put("logdate", lineSplit[2]);
                data.put("loadaverage1", ((String[])lineSplit[11].split(","))[0]);
                data.put("loadaverage5", ((String[])lineSplit[12].split(","))[0]);
                data.put("loadaverage10", lineSplit[13]);
            }

            if (line.indexOf("Cpu") == 0) {
                String[] lineSplit = line.split(" ");
                data.put("cpuuser", lineSplit[1]);
                data.put("cpusys", lineSplit[2]);
                data.put("cpuwa", ((String[])lineSplit[5].split("\\."))[0]);
            }

            if (line.indexOf("Mem") == 0) {

                String[] lineSplit = line.split(" ");
                data.put("memtotal", lineSplit[1]);
                data.put("memused", lineSplit[3]);
                data.put("memfree", lineSplit[5]);
            } 

            if (line.indexOf("Swap") == 0) {
                String[] lineSplit = line.split(" ");
                data.put("swaptotal", lineSplit[1]);
                data.put("swapused", lineSplit[3]);
                data.put("cached", lineSplit[8]);
                break;
            }
        }
/*
0   1    2     3   4  5      6     7  8        9   10       11    12    13
top - 21:34:56 up 21 days,  3:24,  2 users,  load average: 0.02, 0.05, 0.32
Tasks: 136 total,   1 running, 135 sleeping,   0 stopped,   0 zombie
0         1         2        3       4        5         6       7         8
Cpu(s):  0.2%us,  0.2%sy,  0.0%ni, 99.5%id,  0.0%wa,  0.0%hi,  0.0%si,  0.0%st

0         1      2        3       4        5      6          7     8
Mem:   3711520k total,  3026680k used,   684840k free,     9084k buffers
0         1       2         3     4       5       6        7       8
Swap:  8385920k total,   155980k used,  8229940k free,  2838416k cached
*/
        return data;
    }

    public boolean stop() {
        this.stopFlg = true;
        return true;
    }
}
