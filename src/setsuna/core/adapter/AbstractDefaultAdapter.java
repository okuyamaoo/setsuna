package setsuna.core.adapter;

import setsuna.core.util.*;


/**
 * SetsunaMainを利用して立ち上げる場合の入力用のAdapterに共通処理を提供するクラス.<br>
 *
 * @author T.Okuyama
 */
public abstract class AbstractDefaultAdapter {


    protected void debug(String data) {
        SystemUtil.debug(data);
    }

    protected void outputAdapterData(String data) {
        SystemUtil.outputAdapterData(data);
    }

    protected void outputAdapterData(String[] data) {
        SystemUtil.outputAdapterData(data);
    }
}
