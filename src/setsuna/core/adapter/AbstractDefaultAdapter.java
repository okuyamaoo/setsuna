package setsuna.core.adapter;

import setsuna.core.util.*;


/**
 * SetsunaMainを利用して立ち上げる場合の入力用のAdapterに共通処理を提供するクラス.<br>
 *
 * @author T.Okuyama
 */
public abstract class AbstractDefaultAdapter {

    public volatile boolean outputFlg = SetsunaStaticConfig.OUTPUT_ADAPTER_DATA;

    protected void debug(String data) {
        SystemUtil.debug(data);
    }

    protected void outputAdapterData(String data) {
        if(outputFlg)
            SystemUtil.outputAdapterData(data);
    }

    protected void outputAdapterData(String[] data) {
        if(outputFlg)
            SystemUtil.outputAdapterData(data);
    }
}
