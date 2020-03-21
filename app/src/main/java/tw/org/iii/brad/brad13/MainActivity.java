package tw.org.iii.brad.brad13;

import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {
    private ConnectivityManager cmgr;
    private MyReceiver myReceiver;
    private TextView mesg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mesg = findViewById(R.id.mesg);

        cmgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);    //service是本來就有的所以get
        myReceiver = new MyReceiver();
        IntentFilter filter = new IntentFilter(/*ConnectivityManager.CONNECTIVITY_ACTION*/);   //肚子裡的東西為Action,若只有一個action可放建構的參數裡面就好
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);  //此方式可一直add,加上多個action
        filter.addAction("brad");   //要過濾哪個action,上方是定義好的網路連線,此行則是下方我們自己定義的action名為brad
        registerReceiver(myReceiver, filter);
    }

    @Override
    public void finish() {
        unregisterReceiver(myReceiver); //解除receiver
        super.finish();
    }

    private boolean isConnectNetwork() {
        NetworkInfo networkInfo = cmgr.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnectedOrConnecting();
    }

    private boolean isWifiConnected() {
        NetworkInfo networkInfo = cmgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return networkInfo.isConnected();

    }

    public void test1(View view) {
        Log.v("brad", "isNetwork = " + isConnectNetwork());
    }

    public void test2(View view) {
        Log.v("brad", "isWifi = " + isWifiConnected());
    }


    private class MyReceiver extends BroadcastReceiver { //BR是抽象類別故要override
        @Override
        public void onReceive(Context context, Intent intent) {
//            Log.v("brad", "onReceive"); //系統自己廣播出來的,我們只有過濾連線狀態故改變連線狀態時會收到
            if (intent.getAction().equals("brad")) {
                String data = intent.getStringExtra("data");
                mesg.setText(data);
            } else if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                test1(null);
            }


        }
    }

    public void test3(View view) {
        new Thread() {      //網際網路行為必須要在另外的執行序執行不能在MainThread,若要呈現在畫面上則還要Handler
            @Override
            public void run() {
                try {
//                    URL url = new URL("https://bradchao.com/");  //安卓8以上要看http則在manifest的application中的屬性usesCleartextTraffic = true
                    URL url = new URL("https://bradchao.com/wp"); //執行上行版本後會出現此行url
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.connect();

                    BufferedReader reader =
                            new BufferedReader(
                                    new InputStreamReader(conn.getInputStream()));
                    String line;
                    StringBuffer sb = new StringBuffer();
                    while ((line = reader.readLine()) != null) {
                        sb.append(line + "\n");
                    }
                    reader.close();

                    Intent intent = new Intent("brad");
                    intent.putExtra("data", sb.toString()); //帶資料
                    sendBroadcast(intent);  //只要是Context都可發廣播(Activity, Service, Application

                } catch (Exception e) {
                    Log.v("brad", e.toString());
                }
            }
        }.start();
    }
}