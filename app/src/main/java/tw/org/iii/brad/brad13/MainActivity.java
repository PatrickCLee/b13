package tw.org.iii.brad.brad13;
//連線狀態,BroadcastReceiver, IntentFilter
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

public class MainActivity extends AppCompatActivity {
    private ConnectivityManager cmgr;       //*1
    private MyReceiver myReceiver;          //*2
    private TextView mesg;                  //*6
    private ImageView img;                  //*7
    private boolean isAllowSDCard;          //*9
    private File downloadDir;               //*10
    private ProgressDialog progressDialog;  //*11

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(ContextCompat.checkSelfPermission(this,          //*9
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
        != PackageManager.PERMISSION_GRANTED){      //若沒權限
            ActivityCompat.requestPermissions(this,     //要權限
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    123);

        }else{
            isAllowSDCard = true;
            init();
        }

    }

    private void init(){
        if(isAllowSDCard){                                                           //*10
            downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        }

        progressDialog = new ProgressDialog(this);                          //*11
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setMessage("Downloading...");    //設定好了但還沒有叫它show

        mesg = findViewById(R.id.mesg);                                             //*6
        img = findViewById(R.id.img);                                               //*7

        cmgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);    //*1service是本來就有的所以get
        myReceiver = new MyReceiver();                                                  //*2
        IntentFilter filter = new IntentFilter(/*ConnectivityManager.CONNECTIVITY_ACTION*/);  //*2廣播接收器什麼都會收到,故需要filter過濾要的訊息 //肚子裡的東西為Action,若只有一個action可放建構的參數裡面就好
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);  //此方式可一直add,加上多個action
        filter.addAction("brad");   //*6.5要過濾哪個action,上方是定義好的網路連線,此行則是下方我們自己定義的action名為brad
        registerReceiver(myReceiver, filter);   //將接收器和過濾器註冊(掛載)                //*2
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) { //*9
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            isAllowSDCard = true;
        }else{
            isAllowSDCard = false;
        }
        init();
    }

    @Override
    public void finish() {
        unregisterReceiver(myReceiver); //解除receiver    *3
        super.finish();
    }

    private boolean isConnectNetwork() {    //*1
        NetworkInfo networkInfo = cmgr.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnectedOrConnecting();
    }

    private boolean isWifiConnected() {     //*1
        NetworkInfo networkInfo = cmgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return networkInfo.isConnected();
    }

    public void test1(View view) {Log.v("brad", "isNetwork = " + isConnectNetwork()); } //*1


    public void test2(View view) {Log.v("brad", "isWifi = " + isWifiConnected()); }   //*1


    private class MyReceiver extends BroadcastReceiver { //BR是抽象類別故要override            *2
        @Override
        public void onReceive(Context context, Intent intent) {
//            Log.v("brad", "onReceive"); //系統自己廣播出來的,我們只有過濾連線狀態故改變連線狀態時會收到
            if (intent.getAction().equals("brad")) {                            //*6.5
                String data = intent.getStringExtra("data");
                mesg.setText(data);
            } else if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                test1(null);                                                //*4
            }


        }
    }

    public void test3(View view) {                                              //*5
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

                    Intent intent = new Intent("brad");         //*6
                    intent.putExtra("data", sb.toString());     //*6.5帶資料
                    sendBroadcast(intent);  //*6只要是Context都可發廣播(Activity, Service, Application

                } catch (Exception e) {
                    Log.v("brad", e.toString());
                }
            }
        }.start();
    }

    public void test4(View view){                                   //*7
        new Thread(){
            @Override
            public void run() {
                fetchImage();
            }
        }.start();
    }

    public void test5(View view) {
        if (!isAllowSDCard) return;                                 //*10
        Toast.makeText(this,"oh no",Toast.LENGTH_SHORT);


        progressDialog.show();                     //*11因UI不能放執行序裡,故在下方fetchPDF要取消轉圈圈時要用uiHandler的方式

        new Thread(){
            @Override
            public void run() {
                fetchPDF();
            }
        }.start();                   //*8
    }

    public void test6(View view){
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT,"Sharing URL");
        intent.putExtra(Intent.EXTRA_TEXT,"http://www.url.com");
        startActivity(Intent.createChooser(intent,"Share URL"));
    }

    private void fetchPDF(){                                                //*8 xml宣告權限並在上方詢問
        try {
            URL url = new URL("https://pdfmyurl.com/?url=https://www.gamer.com.tw");
            HttpsURLConnection conn = (HttpsURLConnection)url.openConnection();
            conn.setHostnameVerifier(new HostnameVerifier() {                    //*12
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });
            conn.connect();



            File downloadFile = new File(downloadDir,"gamer.pdf");          //*10
            FileOutputStream fout = new FileOutputStream(downloadFile);

            byte[] buf = new byte[1024*4096]; //4MB                                 //*10
            BufferedInputStream bin = new BufferedInputStream(conn.getInputStream());

            int len = -1;                                                           //*10
            while( (len = bin.read(buf)) != -1){
                fout.write(buf,0,len);
            }

            bin.close();
            fout.flush();
            fout.close();                                                           //*10

            uiHandler.sendEmptyMessage(2);                                   //*11

            Log.v("brad","save OK");
        }catch (Exception e){
            Log.v("brad",e.toString());
        }finally {                                                               //finally一定會跑
            uiHandler.sendEmptyMessage(1);                                   //*11 交給uiHandler取消轉圈圈
        }
    }

    private Bitmap bmp;                                                 //*7.3

    private void fetchImage(){                                          //*7
        try {
            URL url = new URL("https://3.bp.blogspot.com/-NQ7KvxrdO1w/XZ2auy102sI/AAAAAAAAJ3I/B_BeovwcuF8WlCbwQZ8Wa1hNvbqqkF2jQCK4BGAYYCw/s1600/joker-2019-joaquin-phoenix-clown-5c.jpg");
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.connect();

//            1. 取得inputStream:conn.getInputStream();
//            2. 顯示在畫面上:ImageView
            bmp = BitmapFactory.decodeStream(conn.getInputStream()); //拿到Bitmap
            uiHandler.sendEmptyMessage(0);


        }catch (Exception e){

        }

    }

    private UIHandler uiHandler = new UIHandler();                  //*7.5

    private class UIHandler extends Handler{
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if(msg.what == 0) img.setImageBitmap(bmp);               //*7.5
            if(msg.what == 1) progressDialog.dismiss(); //*11 dismiss就看不到了 不用銷毀
            if(msg.what == 2) showPDF();                //*11
        }
    }

    private void showPDF(){
        File file = new File(downloadDir,"gamer.pdf");
        Uri pdfuri = FileProvider.getUriForFile(this,
                getPackageName() + ".name",file);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(pdfuri,"application/pdf");
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);
    }
}