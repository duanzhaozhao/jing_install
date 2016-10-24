package test.install;

import android.content.Context;
import android.content.Intent;
import android.content.pm.IPackageDeleteObserver;
import android.content.pm.IPackageInstallObserver;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.Charset;

//import android.content.pm.IPackageDeleteObserver;
//import android.content.pm.PackageInfo;
//import android.content.pm.PackageManager;

/**
 * Created by skysoft on 2016/10/20.
 */
public class InstallUtil {

    //    静默安装 apkFile是apk的名字,需在源码中编译
    public static void J_install(Context context, String apkFile, String packageName) {
        apkFile = "client-debug.apk";//for test 可删

        File installFile = new File(Environment.getExternalStorageDirectory(), File.separator + apkFile);
        Uri uri = Uri.fromFile(installFile);
        int installFlags = 0;
        PackageManager pm = context.getPackageManager();
        try {
            PackageInfo pi = pm.getPackageInfo(packageName, PackageManager.GET_UNINSTALLED_PACKAGES);
            if (pi != null) {
                installFlags |= PackageManager.INSTALL_REPLACE_EXISTING;
            }
        } catch (PackageManager.NameNotFoundException e) {
        }

        MyPakcageInstallObserver observer = new MyPakcageInstallObserver();
        pm.installPackage(uri, observer, installFlags, apkFile);
    }

    //静默卸载，需在源码中编译
    public static void J_uninstall(Context context, String packageName) {
        PackageManager pm = context.getPackageManager();
        MyPackageDeleteObserver observer = new MyPackageDeleteObserver();
        pm.deletePackage(packageName, observer, 0);
    }

    /* 普通安装apk */
    public static void install(Context context, String apkFile) {
        apkFile = "client-debug.apk";
        Intent intent = new Intent(Intent.ACTION_VIEW);

        intent.setDataAndType(Uri.fromFile(new File(Environment.getExternalStorageDirectory(), File.separator + apkFile)),

                "application/vnd.android.package-archive");

        context.startActivity(intent);

    }

    /* 普通卸载apk */
    public static void uninstall(Context context, String packageName) {

        Uri packageURI = Uri.parse("package:" + packageName);

        Intent uninstallIntent = new Intent(Intent.ACTION_DELETE,

                packageURI);

        context.startActivity(uninstallIntent);

    }

    //用pm命令时 安装成功 返回true  需申请root权限
    public static boolean J_installRoot(String apkPath) {
        File installFile = new File(Environment.getExternalStorageDirectory(), File.separator + "client-debug.apk");
        Log.i("aaaaa", installFile.getPath());
        apkPath = installFile.getPath();//for test 可删
        boolean result = false;
        DataOutputStream dataOutputStream = null;
        BufferedReader errorStream = null;
        try {
            // 申请root权限
            Process process = Runtime.getRuntime().exec("sh");
            Thread.sleep(2000);
            dataOutputStream = new DataOutputStream(process.getOutputStream());
            // 执行pm install命令
            String command = "pm install -r " + apkPath + "\n";
            dataOutputStream.write(command.getBytes(Charset.forName("utf-8")));
            dataOutputStream.flush();
            dataOutputStream.writeBytes("exit\n");
            dataOutputStream.flush();
            process.waitFor();
            errorStream = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String msg = "";
            String line;
            // 读取命令的执行结果
            while ((line = errorStream.readLine()) != null) {
                msg += line;
            }
            Log.d("TAG", "install msg is " + msg);
            // 如果执行结果中包含Failure字样就认为是安装失败，否则就认为安装成功
            if (!msg.contains("Failure")) {
                result = true;
            }
        } catch (Exception e) {
            Log.e("TAG", e.getMessage(), e);
        } finally {
            try {
                if (dataOutputStream != null) {
                    dataOutputStream.close();
                }
                if (errorStream != null) {
                    errorStream.close();
                }
            } catch (Exception e) {
                Log.e("TAG", e.getMessage(), e);
            }
        }
        return result;
    }

    //  用pm命令时 卸载成功 返回true  需申请root权限
    public static boolean J_uninstallRoot(String packageName) {
        boolean result = false;
        DataOutputStream dataOutputStream = null;
        BufferedReader errorStream = null;
        try {
            // 申请root权限
            Process process = Runtime.getRuntime().exec("sh");
            dataOutputStream = new DataOutputStream(process.getOutputStream());
            // 执行pm install命令
            String command = "pm uninstall " + packageName + "\n";
            dataOutputStream.write(command.getBytes(Charset.forName("utf-8")));
            dataOutputStream.flush();
            dataOutputStream.writeBytes("exit\n");
            dataOutputStream.flush();
            process.waitFor();
            errorStream = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String msg = "";
            String line;
            // 读取命令的执行结果
            while ((line = errorStream.readLine()) != null) {
                msg += line;
            }
            Log.d("TAG", "uninstall msg is " + msg);
            // 如果执行结果中包含Failure字样就认为是安装失败，否则就认为安装成功
            if (!msg.contains("Failure")) {
                result = true;
            }
        } catch (Exception e) {
            Log.e("TAG", e.getMessage(), e);
        } finally {
            try {
                if (dataOutputStream != null) {
                    dataOutputStream.close();
                }
                if (errorStream != null) {
                    errorStream.close();
                }
            } catch (Exception e) {
                Log.e("TAG", e.getMessage(), e);
            }
        }
        return result;
    }

    //通过反射实现
//    静默安装 apkFile是apk的名字
    public static void JF_install(Context context, String apkFile, String packageName) {
        apkFile = "client-debug.apk";//for test 可删
        File installFile = new File(Environment.getExternalStorageDirectory(), File.separator + apkFile);
        Uri uri = Uri.fromFile(installFile);
        int installFlags = 2;//覆盖安装
        PackageManager pm = context.getPackageManager();
        MyPakcageInstallObserver observer = new MyPakcageInstallObserver();
//         通过 getMethod 只能获取公有方法，如果获取私有方法则会抛出异常

        try {
           Method method = pm.getClass().getMethod("installPackage", Uri.class,IPackageInstallObserver.class,int.class,String.class);
            method.invoke(pm, uri, observer, installFlags,apkFile);
        } catch (Exception e) {
            Log.i("install fail",e.toString());
            e.printStackTrace();
        }

        }

    //静默卸载 反射方式
    public static void JF_uninstall(Context context, String packageName) {
        try {
            PackageManager pm = context.getPackageManager();
            MyPackageDeleteObserver observer = new MyPackageDeleteObserver();
            Method method = pm.getClass().getMethod("deletePackage", String.class, IPackageDeleteObserver.class, int.class);
                method.invoke(pm, packageName, observer, 4);//第三个参数为2时表示删除所有数据
        } catch (Exception e) {
            Log.i("uninstall fail",e.toString());
        }
    }
    }
