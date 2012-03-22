
import org.msgpack.rpc.Client;
import org.msgpack.rpc.loop.EventLoop;
 
/**
 * Setsunaを"-server true"にて立ち上げた場合にデータを投入する<br>
 * Clientのサンプル.<br>
 * SetsunaのServerモードはMessagePack-RPCで出来ているため、<br>
 * ClientもMessagePack-RPCで作成する必要がある。<br>
 * Setsuna側には以下のメソッドが定義されているので<br>
 * それをに実装する.<br>
 * -- 定義メソッド ----------<br>
 * int next (String[] data)<br>
 * --------------------------<br>
 * 引数は1レコード当たりのデータを表す配列<br>
 * 戻り値は呼び出しの成否(0=正常終了, -9=カラム定義と合わない , -1=内部エラー)<br>
 *
 * @author T.Okuyama
 */
public class SetsunaServerModeClientSample {

    /**
     * SetsunaをRPC越しに呼び出し
     * 渡す値はカラム数分のString値の配列.一貫して同じ長さ分を送ること
     * 戻り値:正常終了 0, カラム定義と合わない -9, 内部エラー -1
     */
    public static interface RPCInterface {
        /**
         *
         * @param data カラムデータの配列。必ずカラム数分必要
         * @return int 結果:正常終了=0, カラム定義と合わない=-9, 内部エラー=-1
         */
        int next(String[] data);
    }
 
    public static void main(String[] args) throws Exception {
        EventLoop loop = EventLoop.defaultEventLoop();
 
        Client cli = new Client("localhost", 10028, loop);
        RPCInterface iface = cli.proxy(RPCInterface.class);
 
        String[] list = new String[3];
        list[0] = "aa";
        list[1] = "bb";
        list[2] = "cc";
        for (int i = 0; i < 1000; i++) {
            // データを登録する
            System.out.println(iface.next(list));
        }
        cli.close();
        loop.shutdown();
    }
}