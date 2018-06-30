package stooged.ps4serve3;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import fi.iki.elonen.NanoHTTPD;

public class MainActivity extends Activity {
    NanoHTTPD webServer;
    TextView text1,text2,text3;
    ListView listView;
    ListAdapter listAdapterObject;
    sFile[] sFiles;
    File dir;
    ProgressDialog progress;
    String LocIp;


    @Override
    protected void onDestroy() {
        super.onDestroy();
        StopServer();
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        text1 = (TextView) findViewById(R.id.text1);
        text2 = (TextView) findViewById(R.id.text2);
        text3 = (TextView) findViewById(R.id.text3);
        text1.setTextColor(0xFF33b5e5);
        text3.setTextColor(0xFFFFFFFF);
        LocIp = Utils.getIp();
        text2.setText(Utils.GetSetting(this,"LOADED","ps4-hen-vtx"));
        if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
            SetPermissions();
        }
        else {
            initApp();
        }
    }

    private void StopServer() {
        if (webServer != null) {
            if (webServer.isAlive()) {
                webServer.closeAllConnections();
                webServer.stop();
            }
            webServer = null;
        }
    }

    private void StartServer()
    {
        webServer = new Server(this,8080);
        try {
            webServer.start();
            text1.setText("http://" +   LocIp + ":8080/");
        } catch (IOException e) {
            e.printStackTrace();
            Utils.showToast(MainActivity.this,getBaseContext(),"Failed to start server on port 8080\nTrying to use port 9090", Utils.Warning);
            try {
                webServer = null;
                webServer = new Server(this,9090);
                webServer.start();
                text1.setText("http://" +   LocIp + ":9090/");
            }
            catch (IOException ignored)
            {
                Utils.showToast(MainActivity.this,getBaseContext(),"Failed to start server\nPort 8080 and 9090 might be in use by another app", Utils.Error);
                text1.setTextColor(0xFFD50000);
                text3.setTextColor(0xFFD50000);
                text3.setText("Error: ");
                text1.setText("Port 8080 and 9090 are blocked");
            }
        }
    }

    private void initApp()
    {
        new Thread(new Runnable() {
            public void run() {
        File PayloadDir = new File(Environment.getExternalStorageDirectory().toString() + "/PS4_50X_Payloads/");
        if (!PayloadDir.exists()) {
            if (!PayloadDir.mkdirs()) {
                Utils.showToast(MainActivity.this,getBaseContext(),"Failed to create directory", Utils.Error);
                return;
            }
        }
        File[] contents = PayloadDir.listFiles();
        if (contents == null) {
            Utils.showToast(MainActivity.this,getBaseContext(),"Failed to read directory", Utils.Error);
        }
        else if (contents.length == 0) {
            Utils.createResFile(getBaseContext(),R.raw.hen_pl,"ps4-hen-vtx.bin",false);
            Utils.createResFile(getBaseContext(),R.raw.dump_pl,"ps4-dumper-vtx.bin",false);
            Utils.createResFile(getBaseContext(),R.raw.ftp_pl,"ps4-ftp-vtx.bin",false);
            Utils.createResFile(getBaseContext(),R.raw.backup_pl,"DB_SG_Backup.bin",false);
            Utils.createResFile(getBaseContext(),R.raw.atou_pl,"AppToUsb.bin",false);
            Utils.createResFile(getBaseContext(),R.raw.mira_pl,"MiraFW_Orbis.bin",false);
            Utils.createResFile(getBaseContext(),R.raw.cache_pl,"Cache_Install.bin",false);
            Utils.SaveSetting(getBaseContext(),"PAYLOAD",Environment.getExternalStorageDirectory().toString() + "/PS4_50X_Payloads/ps4-hen-vtx.bin");
            Utils.SaveSetting(getBaseContext(),"LOADED","ps4-hen-vtx");
            Utils.SaveSetting(getBaseContext(),"VERSION",BuildConfig.VERSION_CODE);
            MainActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    text2.setText("ps4-hen-vtx");
                }
            });
        }
        else
        {
            if (Utils.GetSetting(getBaseContext(),"VERSION",10) < BuildConfig.VERSION_CODE)
            {
                Utils.SaveSetting(getBaseContext(),"VERSION",BuildConfig.VERSION_CODE);
                Utils.createResFile(getBaseContext(),R.raw.hen_pl,"ps4-hen-vtx.bin",false);
                Utils.createResFile(getBaseContext(),R.raw.dump_pl,"ps4-dumper-vtx.bin",false);
                Utils.createResFile(getBaseContext(),R.raw.ftp_pl,"ps4-ftp-vtx.bin",false);
                Utils.createResFile(getBaseContext(),R.raw.backup_pl,"DB_SG_Backup.bin",false);
                Utils.createResFile(getBaseContext(),R.raw.atou_pl,"AppToUsb.bin",false);
                Utils.createResFile(getBaseContext(),R.raw.mira_pl,"MiraFW_Orbis.bin",false);
                Utils.createResFile(getBaseContext(),R.raw.cache_pl,"Cache_Install.bin",false);
                Utils.showToast(MainActivity.this,getBaseContext(),"Updated payloads", Utils.Info);
            }
        }
                Utils.createResFile(MainActivity.this,R.raw.index_html,"index.html",true);
            }
        }).start();
        loadPayloadList();
        StartServer();
    }

    @TargetApi(23)
    private void SetPermissions() {
            String[] perms = {"android.permission.INTERNET", "android.permission.ACCESS_WIFI_STATE", "android.permission.ACCESS_NETWORK_STATE", "android.permission.READ_EXTERNAL_STORAGE", "android.permission.WRITE_EXTERNAL_STORAGE"};
            int permsRequestCode = 200;
            requestPermissions(perms, permsRequestCode);
    }

    public void onRequestPermissionsResult(int permsRequestCode, String[] permissions, int[] grantResults){
        switch(permsRequestCode){
            case 200:
                initApp();
                break;
        }
    }

    private void loadPayloadList() {

        progress = new ProgressDialog(this);
        progress.setMessage("Loading...");
        progress.show();
        dir = new File(Environment.getExternalStorageDirectory().toString() + "/PS4_50X_Payloads");
        listView = (ListView) findViewById(R.id.content);
        if (!dir.canRead()) {
            Utils.showToast(MainActivity.this,this,"Error reading directory", Utils.Error);
            return;
        }

        new Thread(new Runnable() {
                public void run() {
                    final ArrayList<File> files = filter(dir.listFiles());
                    sFiles = new sFile[files.size()];
                    for (int i = 0; i < files.size(); i++) {
                        sFiles[i] = new sFile();
                        if (files.get(i).getName().endsWith("/") || files.get(i).getName().endsWith("\\")) {
                            sFiles[i].label = files.get(i).getName().substring(0, files.get(i).getName().length() - 1);
                        } else {
                            sFiles[i].label = files.get(i).getName().substring(0,files.get(i).getName().length()- 4);
                        }
                        sFiles[i].name = files.get(i).getPath();
                        sFiles[i].lFile = files.get(i);
                        sFiles[i].icon = getResources().getDrawable(R.drawable.binico);
                    }
                    listAdapterObject = new ListAdapter(getBaseContext(), sFiles);
                    MainActivity.this.runOnUiThread(new Runnable() {
                        public void run() {
                            listView.setAdapter(listAdapterObject);
                        }
                    });
                    new Sorter().sort(sFiles);
                    handler.sendEmptyMessage(0);
                }
            }).start();

            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    TextView FileUrl = (TextView) view.findViewById(R.id.label);
                    File selFile = new File(FileUrl.getText().toString());
                    if (!selFile.isDirectory()) {
                        Utils.SaveSetting(getBaseContext(),"PAYLOAD",selFile.getPath());
                        Utils.showToast(MainActivity.this,MainActivity.this,"Selected: " + selFile.getName().substring(0,selFile.getName().length()- 4), Utils.Info);
                        text2.setText(selFile.getName().substring(0,selFile.getName().length()- 4));
                        Utils.SaveSetting(getBaseContext(),"LOADED",selFile.getName().substring(0,selFile.getName().length()- 4));
                        Utils.createResFile(MainActivity.this,R.raw.index_html,"index.html",true);
                    }
                }
            });
    }

     Handler handler = new Handler(new Handler.Callback() {
         @Override
         public boolean handleMessage(Message msg) {
             if (progress!=null) {
                 if (progress.isShowing()) {
                     progress.dismiss();
                 }
             }
             return false;
         }
     });

    public ArrayList<File> filter(File[] file_list) {
        ArrayList<File> files = new ArrayList<>();
        for(File file: file_list) {
            if (file.isDirectory()) {
            }else{
                if (file.getName().toLowerCase().endsWith(".bin")) {
                    files.add(file);
                }
            }
        }
        return files;
    }

    public void btn1_Click(View view) {
        Intent intent = new Intent(this, ElfActivity.class);
        startActivity(intent);
    }

}

