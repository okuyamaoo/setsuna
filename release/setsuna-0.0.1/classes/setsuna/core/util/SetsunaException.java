package setsuna.core.util;



/**
 * Exceptionの共通クラス.<br>
 *
 * @author T.Okuyama
 */
public class SetsunaException extends Exception {

    public SetsunaException() {
        super();
    }

    /**
     * コンストラクタ
     *
     * @param message 例外文字列
     */
    public SetsunaException(String message) {
        super(message);
    }

    /**
     * コンストラクタ
     *
     * @param message 例外文字列
     * @param th 例外
     */
    public SetsunaException(String message, Throwable th) {
        super(message, th);
    }

    /**
     * コンストラクタ
     *
     * @param th 例外
     */
    public SetsunaException(Throwable th) {
        super(th);
    }
}