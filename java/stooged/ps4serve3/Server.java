package stooged.ps4serve3;

import android.app.Activity;
import android.content.Context;
import android.os.Environment;
import android.widget.Toast;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import fi.iki.elonen.NanoHTTPD;

public class Server extends NanoHTTPD {

    private Context wContext;
    private String remoteIP;
    private Timer timer;
    private TimerTask timerTask;
    private boolean isWaiting = false;

    public Server(Context context, int SvrPort)
    {
        super(SvrPort);
        this.wContext = context;
    }

    @Override
    public Response serve(IHTTPSession session)
    {
        Map<String, String> headers = session.getHeaders();
        Method method = session.getMethod();
        String uri = session.getUri();
        Map<String, String> files = new HashMap<>();

        if (Method.POST.equals(method) || Method.PUT.equals(method)) {
            try {
                session.parseBody(files);
            } catch (IOException e) {
                return getResponse("Error: " + e.getMessage());
            } catch (ResponseException e) {
                return NanoHTTPD.newFixedLengthResponse(e.getStatus(), MIME_PLAINTEXT, e.getMessage());
            }
        }

        if (uri.contains("/document/")) // replace user guide path just incase
        {
            String[] urispl = uri.split("/");
            uri = "/" + urispl[urispl.length-1];
        }

        if (uri.equals("/") || uri.isEmpty())
        {
            uri =  "/index.html";
        }

        uri = uri.trim().replace(File.separatorChar, '/');
        if (uri.indexOf('?') >= 0) {
            uri = uri.substring(0, uri.indexOf('?'));
        }
        File f = new File(Utils.getDataDir(wContext) + uri);
        if (uri.toLowerCase().contains(f.getName().toLowerCase()))
        {
            if (f.getName().toLowerCase().equals("index.html")) {
                if (Utils.GetSetting(wContext,"PAYLOAD", Environment.getExternalStorageDirectory().toString() + "/PS4_50X_Payloads/ps4-hen-vtx.bin").toLowerCase().endsWith(".html"))
                {
                    File fh= new File(Utils.GetSetting(wContext,"PAYLOAD", Environment.getExternalStorageDirectory().toString() + "/PS4_50X_Payloads/ps4-hen-vtx.bin"));
                    showToast("Payload sent", Utils.Success);
                    return serveFile(f.getName(), headers, fh);
                }
                isWaiting = false;
                stoptimertask();
                remoteIP = session.getRemoteIpAddress();
                isWaiting = true;
                startTimer();
                Utils.SaveSetting(wContext,"REMHOST",remoteIP);
                showToast("Sending payload\nIP: " + remoteIP, Utils.Info);
                sendPayload(Utils.GetSetting(wContext,"PAYLOAD", Environment.getExternalStorageDirectory().toString() + "/PS4_50X_Payloads/ps4-hen-vtx.bin"), remoteIP, 9020);
            }
            return serveFile(f.getName(), headers, f);
        }
        else
        {
            return NanoHTTPD.newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Error 404: File not found");
        }
    }

    private Response serveFile(String uri, Map<String, String> header, File file) {
        Response res;
        String mime = getMimeTypeForFile(uri);
        try {
            String etag = Integer.toHexString((file.getPath() + file.lastModified() + "" + file.length()).hashCode());
            long startFrom = 0;
            long endAt = -1;
            String range = header.get("range");
            if (range != null) {
                if (range.startsWith("bytes=")) {
                    range = range.substring("bytes=".length());
                    int minus = range.indexOf('-');
                    try {
                        if (minus > 0) {
                            startFrom = Long.parseLong(range.substring(0, minus));
                            endAt = Long.parseLong(range.substring(minus + 1));
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            long fileLen = file.length();
            if (range != null && startFrom >= 0) {
                if (startFrom >= fileLen) {
                    res = createResponse(Response.Status.RANGE_NOT_SATISFIABLE, MIME_PLAINTEXT, "");
                    res.addHeader("Content-Range", "bytes 0-0/" + fileLen);
                    res.addHeader("ETag", etag);
                } else {
                    if (endAt < 0) {
                        endAt = fileLen - 1;
                    }
                    long newLen = endAt - startFrom + 1;
                    if (newLen < 0) {
                        newLen = 0;
                    }

                    final long dataLen = newLen;
                    FileInputStream fis = new FileInputStream(file) {
                        @Override
                        public int available() throws IOException {
                            return (int) dataLen;
                        }
                    };
                    fis.skip(startFrom);
                    res = createResponse(Response.Status.PARTIAL_CONTENT, mime, fis);
                    res.addHeader("Content-Length", "" + dataLen);
                    res.addHeader("Content-Range", "bytes " + startFrom + "-" + endAt + "/" + fileLen);
                    res.addHeader("ETag", etag);
                }
            } else {
                if (etag.equals(header.get("if-none-match")))
                    res = createResponse(Response.Status.NOT_MODIFIED, mime, "");
                else {
                    res = createResponse(Response.Status.OK, mime, new FileInputStream(file));
                    res.addHeader("Content-Length", "" + fileLen);
                    res.addHeader("ETag", etag);
                }
            }
        } catch (IOException ioe) {
            res = getResponse("Forbidden: Reading file failed");
        }
        return (res == null) ? getResponse("Error 404: File not found") : res;
    }

    private void sendPayload(final String filePath, final String host, final int port)  {
        new Thread(new Runnable() {
            public void run() {
                while(isWaiting)
                {
                    try {
                        File f = new File(filePath);
                        Socket socket = new Socket(host, port);
                        isWaiting = false;
                        stoptimertask();
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
                            showToast("Payload sent", Utils.Success);
                        }
                        else
                        {
                            socket.close();
                            showToast("Error: Payload missing\nSelect a payload to send first", Utils.Error);
                        }
                    }
                    catch(ConnectException e)
                    {
                        try
                        {
                            Thread.sleep(1000);
                        }
                        catch(InterruptedException ignored){}
                    }
                    catch (IOException ignored){}
                }
            }
        }).start();
    }

    private Response createResponse(Response.Status status, String mimeType, InputStream message) {
        Response res = NanoHTTPD.newChunkedResponse(status, mimeType, message);
        res.addHeader("Accept-Ranges", "bytes");
        return res;
    }

    private Response createResponse(Response.Status status, String mimeType, String message) {
        Response res = NanoHTTPD.newFixedLengthResponse(status, mimeType, message);
        res.addHeader("Accept-Ranges", "bytes");
        return res;
    }

    private Response getResponse(String message) {
        return createResponse(Response.Status.OK, "text/plain", message);
    }

    public void startTimer() {
        timer = new Timer();
        initializeTimerTask();
        timer.schedule(timerTask, 15000, 30000);
    }

    public void initializeTimerTask() {
        timerTask = new TimerTask() {
            public void run() {
                stoptimertask();
                isWaiting = false;
                showToast("Failed to send payload", Utils.Error);
            }
        };
    }

    public void stoptimertask() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    private void showToast(final String tMessage, final int tType){
        ((Activity)wContext).runOnUiThread(new Runnable()
        {
            public void run()
            {
                switch (tType) {
                    case Utils.Success: {
                        Toasty.success(wContext, tMessage, Toast.LENGTH_SHORT, true).show();
                        break;
                    }
                    case Utils.Info: {
                        Toasty.info(wContext, tMessage, Toast.LENGTH_SHORT, true).show();
                        break;
                    }
                    case Utils.Error: {
                        Toasty.error(wContext, tMessage, Toast.LENGTH_SHORT, true).show();
                        break;
                    }
                    case Utils.Warning: {
                        Toasty.warning(wContext, tMessage, Toast.LENGTH_SHORT, true).show();
                        break;
                    }
                    default: {
                        Toasty.normal(wContext, tMessage, Toast.LENGTH_SHORT).show();
                        break;
                    }
                }
            }
        });
    }


}
