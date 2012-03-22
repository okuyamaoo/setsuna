package setsuna.core.event;

import java.util.Map;

/**
 * ユーザ作成のEvent用のインターフェース.<br>
 * Queryを通過したデータが発生しる毎にexecuteメソッドがよびだされる。
 * EventユーザクラスのライフサイクルはSetsunaCoreのexecuteEventEngineメソッド呼ばれるタイミングで
 * インスタンス化され移行は同じインスタンスがJVM停止まで利用され続ける.
 * exexuteメソッドのみが都度呼ばれるイメージ
 *
 * @author T.Okuyama
 */
public interface IScript {

    public void execute(Map data) throws Exception;
}