package setsuna.core.util;

import java.io.*;
import java.net.*;
import java.util.*;


/**
 * .<br>
 *
 * @author T.Okuyama
 */
public class CustomLineReader {

    private InputStream is = null;

    private BufferedInputStream bis = null;

    public CustomLineReader(InputStream is) {

        this.is = is;
        this.bis = new BufferedInputStream(this.is);
    }


    public String readLine() throws Exception {

        byte[] b = new byte[1];
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        int i = 0;
        while (bis.read(b, 0, 1) != -1) {

            if (b[0] != 13 && b[0] != 10) {
                bos.write(b, 0, 1);
            } else if (b[0] == 10) {
                break;
            }
        }
        return bos.toString();
    }


    public boolean ready() throws Exception {
        if(this.bis.available() > 0) return true;
        return false;
    }


    public int read(byte[] buf) throws Exception {

        for (int i= 0; i < buf.length; i++) {
            bis.read(buf, i, 1);
        }

        return buf.length;

    }


    public void mark(int point) throws Exception {
        this.bis.mark(point);
    }


    public int read() throws Exception {
        return this.bis.read();
    }


    public void close() {

        try {
            if (this.bis != null) {
                this.bis.close();
                this.bis = null;
            }

            if (this.is != null) {
                this.is.close();
                this.is = null;
            }
        } catch (Exception e) {
            // 無視
        }
    }
}