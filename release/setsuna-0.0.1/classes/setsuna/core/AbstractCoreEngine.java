package setsuna.core;


/**
 * SetsunaCoreEngineの親クラス.<br>
 *
 * @author T.Okuyama
 */
abstract public class AbstractCoreEngine extends Thread {

    protected String myName = null;

    protected Object preEventObj = null;


    protected boolean endStatus = false;

    protected boolean errEnd = false;


    abstract public Object preEvent(Object param) throws Exception;

    abstract public Object doEvent(Object preEventResult) throws Exception;

    abstract public void endEvent(Object doEventResult) throws Exception;

    abstract public void stopEvent();

    public void setPreEventParam(Object obj) {
        this.preEventObj = obj;
    }

    public void setEngineName(String name) {
        this.myName = name;
    }

    public String getEngineName() {
        return this.myName;
    }

    public boolean getEndStatus() {
        return this.endStatus;
    }

    public boolean getErrEnd() {
        return this.errEnd;
    }


    public void run() {
        try {

            Object preEventResult = this.preEvent(this.preEventObj);
            Object doEventResult = this.doEvent(preEventResult);
            this.endEvent(doEventResult);
            this.endStatus = true;
        } catch(Throwable te) {
            te.printStackTrace();
            this.endStatus = true;
            this.errEnd = true;
        }
    }
}