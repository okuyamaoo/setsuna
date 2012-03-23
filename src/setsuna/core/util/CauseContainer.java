package setsuna.core.util;


import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Trigger指定のコンテナクラス<br>
 *
 * @author T.Okuyama 
 */
public class CauseContainer {

    public static Map causeTypeMap = null;

    public static String CAUSE_TYPE_EQUAL = "=";

    public static String CAUSE_TYPE_LIKE = "LIKE";

    public static String CAUSE_TYPE_OVER = ">";

    public static String CAUSE_TYPE_SMALL = "<";

    public static Map numberCnvMap = null;

    protected static Integer colKey = new Integer(1);
    protected static Integer typeKey = new Integer(2);
    protected static Integer causeKey = new Integer(3);

    private List paramList = null;

    private String useTypeString = "";

    static {
        causeTypeMap = new ConcurrentHashMap(10, 8, 64);
        causeTypeMap.put(CAUSE_TYPE_EQUAL, 1);
        causeTypeMap.put(CAUSE_TYPE_LIKE, 2);
        causeTypeMap.put(CAUSE_TYPE_OVER, 3);
        causeTypeMap.put(CAUSE_TYPE_SMALL, 4);
        
        numberCnvMap = new ConcurrentHashMap();
        numberCnvMap.put('1',"1");
        numberCnvMap.put('2',"2");
        numberCnvMap.put('3',"3");
        numberCnvMap.put('4',"4");
        numberCnvMap.put('5',"5");
        numberCnvMap.put('6',"6");
        numberCnvMap.put('7',"7");
        numberCnvMap.put('8',"8");
        numberCnvMap.put('9',"9");
        numberCnvMap.put('0',"0");
        numberCnvMap.put('０',"0");
        numberCnvMap.put('９',"9");
        numberCnvMap.put('８',"8");
        numberCnvMap.put('７',"7");
        numberCnvMap.put('６',"6");
        numberCnvMap.put('５',"5");
        numberCnvMap.put('４',"4");
        numberCnvMap.put('３',"3");
        numberCnvMap.put('２',"2");
        numberCnvMap.put('１',"1");
    }


    public CauseContainer() {
        this.paramList = new ArrayList(10);
    }


    /**
     * 「対象のカラム名 チェックするタイプ それと比べる値」のフォーマットで<br>
     * 登録された値をビルドして自身が理解できる形式にフォーマットする<br>
     *
     * @param buildCauseStr 
     * @throws Exception
     */
    public void add2BuildCause(String buildCauseStr) throws Exception {

        String[] buildList = buildCauseStr.split(" ");

        Map causeMap = new HashMap(3);
        causeMap.put(colKey, buildList[0].toUpperCase());
        causeMap.put(typeKey, (Integer)causeTypeMap.get(buildList[1].toUpperCase()));
        this.useTypeString = buildList[1].toUpperCase();

        StringBuilder causeValueBuf = new StringBuilder();
        String sep = "";
        for (int idx = 2; idx < buildList.length; idx++) {
            causeValueBuf.append(sep);
            causeValueBuf.append(buildList[idx]);
            sep = " ";
        }
        causeMap.put(causeKey, causeValueBuf.toString());

        this.paramList.add(causeMap);
    }

    // 対象のカラム名、チェックするタイプ、それと比べる値を入れる
    public void addCause(String columnName, String type, String causeValue) {
        Map causeMap = new HashMap(3);
        causeMap.put(colKey, columnName.toUpperCase());
        causeMap.put(typeKey, (Integer)causeTypeMap.get(type.toUpperCase()));
        causeMap.put(causeKey, causeValue);
        this.useTypeString = type;

        this.paramList.add(causeMap);
    }

    // addした条件を全て比べる
    // 渡される値は、Adapterからくる1レコードのイメージ
    public boolean checkAllCauseMatchValue(Map targetValue) {
        boolean ret = true;
        for (int idx = 0; idx < this.paramList.size(); idx++) {
            Map cause = (Map)this.paramList.get(idx);
            String colName = (String)cause.get(colKey);
            Integer type = (Integer)cause.get(typeKey);
            String causeValue = (String)cause.get(causeKey);

            if (targetValue.containsKey(colName)) {
                String chkValue = (String)targetValue.get(colName);
                if (!this.executeCause(chkValue, type, causeValue)) {
                    ret = false;
                    break;
                }
            } else {
                ret = false;
                break;
            }
        }
        return ret;
    }


    protected boolean executeCause(String value, Integer type, String causeValue) {
        if (value == null || type == null) return false;

        SystemUtil.debug("-trigger Query=[" + value + " " + this.useTypeString + " " + causeValue + "]");
        switch (type.intValue()) {
            case 1 : 

                // EQUAL
                if (value.equals(causeValue)) return true;
                break;
            case 2 :

                // LIKE
                if (value.indexOf(causeValue) != -1) return true;
                break;
            case 3 :

                // >
                try {
                    double valueDouble = Double.parseDouble(value);
                    double causeValueDouble = Double.parseDouble(causeValue);
                    if (valueDouble > causeValueDouble) return true;
                } catch (Exception e) {

                    char[] charSep = value.toCharArray();
                    StringBuilder createStr = new StringBuilder();
                    boolean appendFlg = false;
                    for (int i= 0; i < charSep.length; i++) {
                        if (numberCnvMap.containsKey(charSep[i])) {
                            createStr.append(numberCnvMap.get(charSep[i]));
                            appendFlg = true;
                        } else if (charSep[i] == '+' && appendFlg == false) {
                            createStr.append("+");
                            appendFlg = true;
                        } else if (charSep[i] == '-' && appendFlg == false) {
                            createStr.append("-");
                            appendFlg = true;
                        } else if (appendFlg == true) {
                            break;
                        }
                    }
                    String tmpCnvCVal = createStr.toString();
                    if (tmpCnvCVal.length() > 0) value = tmpCnvCVal;
                    double valueDouble = Double.parseDouble(value);
                    double causeValueDouble = Double.parseDouble(causeValue);
                    if (valueDouble > causeValueDouble) return true;
                }
                break;
            case 4 :
                // <

                try {
                    double valueDouble = Double.parseDouble(value);
                    double causeValueDouble = Double.parseDouble(causeValue);
                    if (valueDouble > causeValueDouble) return true;
                } catch (Exception e) {
                    char[] charSep = value.toCharArray();
                    StringBuilder createStr = new StringBuilder();
                    boolean appendFlg = false;
                    for (int i= 0; i < charSep.length; i++) {
                        if (numberCnvMap.containsKey(charSep[i])) {
                            createStr.append(numberCnvMap.get(charSep[i]));
                            appendFlg = true;
                        } else if (charSep[i] == '+' && appendFlg == false) {
                            createStr.append("+");
                            appendFlg = true;
                        } else if (charSep[i] == '-' && appendFlg == false) {
                            createStr.append("-");
                            appendFlg = true;
                        } else if (appendFlg == true) {
                            break;
                        }
                    }
                    String tmpCnvCVal = createStr.toString();
                    if (tmpCnvCVal.length() > 0) value = tmpCnvCVal;

                    double valueDouble = Double.parseDouble(value);
                    double causeValueDouble = Double.parseDouble(causeValue);
                    if (valueDouble < causeValueDouble) return true;
                    
                }
                break;
            default :
                return false;
        }
        return false;
    }


    public void clear() {
        this.paramList = null;
    }
}