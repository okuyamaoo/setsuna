package setsuna.core.util;

import java.util.*;


/**
 * Setsuna SystemAPI.<br>
 *
 * @author T.Okuyama
 */
public class SystemUtil {

    public static void debug(Object obj) {
        if (SetsunaStaticConfig.DEBUG_SETSUNA) 
            System.out.println(obj);
    }

}
