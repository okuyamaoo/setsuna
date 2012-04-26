package setsuna.core.util;


import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Trigger指定のコンテナクラス<br>
 *
 * @author T.Okuyama 
 */
public class CauseContainer {

    private List executorList = new ArrayList();

    public CauseContainer() {
        
    }


    /**
     * 「対象のカラム名 チェックするタイプ それと比べる値」のフォーマットで<br>
     * 登録された値をビルドして自身が理解できる形式にフォーマットする<br>
     * 条件分呼び出す.<br>
     *
     * @param buildCauseStr 
     * @throws Exception
     */
    public void add2BuildCause(String buildCauseStr) throws Exception {
        AtomicCauseExecutor executor = new AtomicCauseExecutor();
        executor.add2BuildCause(buildCauseStr);
        this.executorList.add(executor);
    }


    // 対象のカラム名、チェックするタイプ、それと比べる値を入れる
    public void addCause(String columnName, String type, String causeValue) {
        AtomicCauseExecutor executor = new AtomicCauseExecutor();
        executor.addCause(columnName, type, causeValue);
        this.executorList.add(executor);
    }

    // addした条件を全て比べる
    // 渡される値は、Adapterからくる1レコードのイメージ
    public boolean checkAllCauseMatchValue(Map targetValue) {
        boolean ret = true;
        for (int idx = 0; idx < this.executorList.size(); idx++) {
            if (!ret) break;
            AtomicCauseExecutor executor = (AtomicCauseExecutor)this.executorList.get(idx);
            ret = executor.checkAllCauseMatchValue(targetValue);
        }
        return ret;
    }

    public void clear() {
        for (int idx = 0; idx < this.executorList.size(); idx++) {
            AtomicCauseExecutor executor = (AtomicCauseExecutor)this.executorList.get(idx);
            executor.clear();
        }
    }
}
