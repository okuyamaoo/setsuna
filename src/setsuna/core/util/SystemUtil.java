package setsuna.core.util;

import java.util.*;


/**
 * Setsuna SystemAPI.<br>
 *
 * @author T.Okuyama
 */
public class SystemUtil {


    // TODO:ここでOutput実装をするとAdapterのデータを出力可能
    public static void outputAdapterData(String adapterData) {
        //System.out.println(adapterData);
    }

    // TODO:ここでOutput実装をするとAdapterのデータを出力可能
    public static void outputAdapterData(String[] adapterData) {
        //System.out.println(adapterData);
    }

    public static void debug(Object obj) {
        if (SetsunaStaticConfig.DEBUG_SETSUNA > 0) 
            System.out.println("Debug : " + new Date() + " - - " + obj);
    }

    public static void printout(Object obj) {
        if (SetsunaStaticConfig.DEBUG_SETSUNA != 2) 
            System.out.println(obj);
    }

    public static void printout(int obj) {
        if (SetsunaStaticConfig.DEBUG_SETSUNA != 2) 
            System.out.println(obj);
    }

    public static void printout(long obj) {
        if (SetsunaStaticConfig.DEBUG_SETSUNA != 2) 
            System.out.println(obj);
    }

    public static void printout(boolean obj) {
        if (SetsunaStaticConfig.DEBUG_SETSUNA != 2) 
            System.out.println(obj);
    }

    public static void printout(double obj) {
        if (SetsunaStaticConfig.DEBUG_SETSUNA != 2) 
            System.out.println(obj);
    }

    public static void printout(char obj) {
        if (SetsunaStaticConfig.DEBUG_SETSUNA != 2) 
            System.out.println(obj);
    }
}
