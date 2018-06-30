package stooged.ps4serve3;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.util.ArrayList;

    public class ElfActivity extends Activity {
        TextView text2;
        ListView listView;
        ListAdapter listAdapterObject;
        sFile[] sFiles;
        File dir;
        ProgressDialog progress;

        @Override
        protected void onDestroy() {
            super.onDestroy();
            finish();
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            setContentView(R.layout.activity_elf);
            text2 = (TextView) findViewById(R.id.text2);
            text2.setText(Utils.GetSetting(getBaseContext() ,"ELFPORT","9023"));
            text2.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        if (text2.getText().toString().isEmpty())
                        {
                            text2.setText("9023");
                        }
                        Utils.showToast(ElfActivity.this,getBaseContext(),"Port set to: " + text2.getText().toString(), Utils.Info);
                        Utils.SaveSetting(getBaseContext(),"ELFPORT",text2.getText().toString());
                    }
                    return false;
                }
            });


            File PayloadDir = new File(Environment.getExternalStorageDirectory().toString() + "/PS4_50X_Payloads/");
            if (!PayloadDir.exists()) {
                if (!PayloadDir.mkdirs()) {
                    Utils.showToast(ElfActivity.this,this,"Failed to create directory", Utils.Error);
                    return;
                }
            }
            File[] contents = PayloadDir.listFiles();
            if (contents == null) {
                Utils.showToast(ElfActivity.this,this,"Failed to read directory", Utils.Error);
            }
            loadPayloadList();
        }

        private void loadPayloadList() {
            progress = new ProgressDialog(this);
            progress.setMessage("Loading...");
            progress.show();
            dir = new File(Environment.getExternalStorageDirectory().toString() + "/PS4_50X_Payloads");
            listView = (ListView) findViewById(R.id.content);
            if (!dir.canRead()) {
                Utils.showToast(ElfActivity.this,this,"Error reading directory", Utils.Error);
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
                        sFiles[i].icon = getResources().getDrawable(R.drawable.elfico);
                    }
                    listAdapterObject = new ListAdapter(getBaseContext(), sFiles);
                    ElfActivity.this.runOnUiThread(new Runnable() {
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
                        try {
                            sendElf(selFile.getPath(), Utils.GetSetting(getBaseContext(),"REMHOST","127.0.0.1"), Integer.parseInt(text2.getText().toString()));
                        } catch(NumberFormatException ignored) {
                            Utils.showToast(ElfActivity.this,getBaseContext(),"Invalid port number: " + text2.getText().toString(), Utils.Error);
                        }
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
                    if (file.getName().toLowerCase().endsWith(".elf")) {
                        files.add(file);
                    }
                }
            }
            return files;
        }

        private void sendElf(final String filePath, final String host, final int port)  {
            new Thread(new Runnable() {
                public void run() {
                        try {
                            File f = new File(filePath);
                            Socket socket = new Socket(host, port);

                            if(f.exists())
                            {
                                BufferedOutputStream outStream = new BufferedOutputStream(socket.getOutputStream());
                                BufferedInputStream inStream = new BufferedInputStream(new FileInputStream(f));
                                byte[] buffer = new byte[1024];
                                for (int read = inStream.read(buffer); read >= 0; read = inStream.read(buffer)) {
                                    outStream.write(buffer, 0, read);
                                }
                                inStream.close();
                                outStream.close();
                                socket.close();
                                Utils.showToast(ElfActivity.this,getBaseContext(),"Elf sent", Utils.Success);
                            }
                            else
                            {
                                socket.close();
                                Utils.showToast(ElfActivity.this,getBaseContext(),"Error: Elf missing\nSelect a elf to send first", Utils.Error);
                            }
                        }
                        catch(ConnectException ignored)
                        {
                            Utils.showToast(ElfActivity.this,getBaseContext(),"Connection Failed\nLoad an elf loader payload first", Utils.Error);
                        }
                        catch (IOException ignored)
                        {
                            Utils.showToast(ElfActivity.this,getBaseContext(),"Connection Failed\nLoad an elf loader payload first", Utils.Error);
                        }

                }
            }).start();
        }

        public void text2_Click(View view) {
            text2.setCursorVisible(true);
            text2.setFocusableInTouchMode(true);
            text2.setInputType(InputType.TYPE_CLASS_NUMBER);
            text2.requestFocus();
        }

        public void text1_Click(View view) {
            finish();
        }

    }

