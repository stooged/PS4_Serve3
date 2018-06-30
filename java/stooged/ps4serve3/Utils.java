package stooged.ps4serve3;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.view.View;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.widget.Toast;

public class Utils {

    public static final int Normal = 0;
    public static final int Success = 1;
    public static final int Info = 2;
    public static final int Error = 3;
    public static final int Warning = 4;

    public static String getDataDir(Context context) {
        try
        {
            return  context.getPackageManager().getPackageInfo(context.getPackageName(), 0).applicationInfo.dataDir;
        }
        catch (PackageManager.NameNotFoundException ignored)
        {
            return null;
        }
    }

    public static String getIp() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface iface = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = iface.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException ignored) {
        }
        return null;
    }

    public static void createResFile(final Context context,final int resourceId, final String fileName, final boolean isLocal)
    {
        new Thread(new Runnable() {
            public void run() {
                String fpath;
                if (isLocal) {
                    fpath = getDataDir(context) + "/" + fileName;
                } else {
                    fpath = Environment.getExternalStorageDirectory().toString() + "/PS4_50X_Payloads/" + fileName;
                }
                try {
                    InputStream in = context.getResources().openRawResource(resourceId);
                    FileOutputStream out = new FileOutputStream(fpath);
                    byte[] buff = new byte[10240];
                    int read = 0;
                    try {
                            while ((read = in.read(buff)) > 0) {
                                out.write(buff, 0, read);
                            }
                    } finally {
                        in.close();
                        out.close();
                    }
                    if (isLocal && fileName.equals("index.html"))
                    {
                        File idxFile= new File(fpath);
                        FileReader fr = new FileReader(idxFile);
                        String s;
                        String totalStr = "";
                        BufferedReader br = new BufferedReader(fr);
                        while ((s = br.readLine()) != null) {
                            totalStr += s;
                        }
                        totalStr = totalStr.replaceAll("<pname>", GetSetting(context, "LOADED", "ps4-hen-vtx"));
                        FileWriter fw = new FileWriter(idxFile);
                        fw.write(totalStr);
                        fw.close();
                    }
                } catch (FileNotFoundException ignored) {
                } catch (IOException ignored) {
                }
            }
        }).start();
    }

    public static void SaveSetting(Context context, String sKey, String sValue)
    {
        SharedPreferences sharedPref = context.getSharedPreferences("Settings", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(sKey, sValue);
        editor.apply();
    }

    public static void SaveSetting(Context context, String sKey, int iValue)
    {
        SharedPreferences sharedPref = context.getSharedPreferences("Settings", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(sKey, iValue);
        editor.apply();
    }

    public static void SaveSetting(Context context, String sKey, boolean bValue)
    {
        SharedPreferences sharedPref = context.getSharedPreferences("Settings", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(sKey, bValue);
        editor.apply();
    }

    public static boolean GetSetting(Context context, String sKey, boolean bDefault)
    {
        SharedPreferences sharedPref = context.getSharedPreferences("Settings", Context.MODE_PRIVATE);
        return sharedPref.getBoolean(sKey, bDefault);
    }

    public static String GetSetting(Context context, String sKey, String sDefault)
    {
        SharedPreferences sharedPref = context.getSharedPreferences("Settings", Context.MODE_PRIVATE);
        return sharedPref.getString(sKey, sDefault);
    }

    public static int GetSetting(Context context, String sKey, int iDefault)
    {
        SharedPreferences sharedPref = context.getSharedPreferences("Settings", Context.MODE_PRIVATE);
        return sharedPref.getInt(sKey, iDefault);
    }

    public static Drawable tintIcon(@NonNull Drawable drawable, @ColorInt int tintColor) {
        drawable.setColorFilter(tintColor, PorterDuff.Mode.SRC_IN);
        return drawable;
    }

    public static Drawable tint9PatchDrawableFrame(@NonNull Context context, @ColorInt int tintColor) {
        final NinePatchDrawable toastDrawable = (NinePatchDrawable) getDrawable(context, R.drawable.toast_frame);
        return tintIcon(toastDrawable, tintColor);
    }

    public static void setBackground(@NonNull View view, Drawable drawable) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
            view.setBackground(drawable);
        else
            view.setBackgroundDrawable(drawable);
    }

    public static Drawable getDrawable(@NonNull Context context, @DrawableRes int id) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            return context.getDrawable(id);
        else
            return context.getResources().getDrawable(id);
    }

    public static void showToast(final Activity activity  , final Context context, final String tMessage,final int tType){
        ((Activity)activity).runOnUiThread(new Runnable()
        {
            public void run()
            {
                switch (tType) {
                    case Success: {
                        Toasty.success(context, tMessage, Toast.LENGTH_SHORT, true).show();
                        break;
                    }
                    case Info: {
                        Toasty.info(context, tMessage, Toast.LENGTH_SHORT, true).show();
                        break;
                    }
                    case Error: {
                        Toasty.error(context, tMessage, Toast.LENGTH_SHORT, true).show();
                        break;
                    }
                    case Warning: {
                        Toasty.warning(context, tMessage, Toast.LENGTH_SHORT, true).show();
                        break;
                    }
                    default: {
                        Toasty.normal(context, tMessage, Toast.LENGTH_SHORT).show();
                        break;
                    }
                }
            }
        });
    }

}
