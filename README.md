# jing_install
静默安装与卸载


Android实现静默卸载与安装
静默安装简介：一般情况下，Android系统安装apk会出现一个安装界面，用户可以点击确定或者取消来进行apk的安装。
但在实际的项目需求中，有一种需求，就是希望apk在后台安装(不出现安装界面的提示)，这种安装方式称为静默安装。
以下用两张方式实现静默安装与卸载：
一、PM命令实现静默安装与卸载
注：这种方法需要root权限
1. 直接adb shell 命令卸载应用
adb shell pm uninstall -k (应用包名)
adb shell pm install  -r  (apk路径)
2. 在Android程序代码中实现pm
Runtime.getRuntime().exec("sh");// 申请root权限
Runtime.getRuntime().exec("pm uninstall -k uninstallPackageName")//卸载
Runtime.getRuntime().exec("pm install -r unstallapkPath")//安装
安装可选的参数：
参数	说明
-l	锁定应用程序
-r	重新安装应用，且保留应用数据
-t	允许测试apk被安装
-i <INSTALLER_PACKAGE_NAME>	指定安装包的包名
-s	安装到sd卡
-f	安装到系统 内置存储中（默认安装位置）
-d	允许降级安装（同一应用低级换高级）
-g	授予应用程序清单中列出的所有权限（只有6.0系统可用）
卸载可选的参数：
参数	说明
-k	卸载应用且保留数据与缓存（如果不加-k则全部删除）
声明权限：
在Manifest文件中加入如下语句：
<uses-permission android:name="android.permission.INSTALL_PACKAGES" />
<uses-permission android:name="android.permission.DELETE_PACKAGES" />


二、调用PackageInstaller接口实现静默安装与卸载
注：需要系统签名
实现原理
android自带了一个安装程序—/system/app/PackageInstaller.apk.大多数情况下，我们手机上安装应用都是通过这个apk来安装的。.我们在应用程序中控制安装应用APP，其实就是发送一个如下的intent。去调用packageinstaller进行安装，具体的操作代码如下：
/* 安装apk */
Intent intent = new Intent();
intent.setAction(Intent.ACTION_VIEW);
intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
intent.setDataAndType(Uri.parse("file://"+ fileName)，
   "application/vnd.android.package-archive");
context.startActivity(intent);
对比应用正常安装的流程，静默安装的本质就是去掉如下图所示的用户授权同意安装的过程，直接进行应用安装。
 
源码分析
阅读过源码后我们知道，系统的安装过程其实是调用了系统中的PackageInstaller来完成的。希望做到静默安装，就是找到一个方法，绕过PackageInstaller中的权限授予提示，继续完成安装的步骤。 
所以，思路很简单，我们可以从两方面去操作：
•	找到PackageInstaller源码，跳过权限授予提醒，直接调用后面的安装API即可完成安装。（这样能够良好的兼容正常安装，不易出错）
•	使用pm install 命令进行安装。
调用PackageInstaller中隐藏的API
查看PackageInstaller源码我们能够发现，其实PackageInstaller也是通过使用PackageManager进行安装的。调用的是其installPackage方法，但是此方法是一个abstract，且是对外不可见的（hide），
定义如下所示：
public abstract class PackageManager {
………
/**
 * 安装应用APK文件
 * @param packageURI 待安装的APK文件位置，可以是'file:'或'content:' URI.
 * @param observer 一个APK文件安装状态的观察器
 * @param flags 安装形式 INSTALL_FORWARD_LOCK， INSTALL_REPLACE_EXISTING， INSTALL_ALLOW_TEST.
 * @paraminstallerPackageName APK安装包的PackageName
 */
// @SystemApi
public abstract void installPackage(UripackageURI， PackageInstallObserverobserver，int flags， StringinstallerPackageName);
}
因为，installPackage是系统的API，为了使用PackageManager.installPackage()，考虑通过反射机制可以调用installPackage()。
静默安装和卸载具体实现如下
1、	通过反射实现
注：需要系统签名，并且需要push到/system/app/

安装：

接下来在程序中通过反射拿到PackageManager的installPackage方法，代码如下：
最后在调用安装方法之前，先来看看这个方法的几个参数：
注：需要拷贝IPackageInstallObserver.aidl和IPackageDeleteObserver.aidl到我们的app/src/main/aidl/目录中并且包名一定要为android.content.pm。拷贝的源码路径：\frameworks\base\core\java\android\content\pm
•	packageURI： 安装包的地址。
•	observer：安装完成后的回调函数。observer可以直接给null，如果想要设置回调的话，需要新建一个类实现IPackageInstallOberver类（从源码中复制过来）。
flags：安装方式，主要有普通安装和覆盖安装。1表示普通，2表示覆盖。
有如下值：
INSTALL_FORWARD_LOCK,
INSTALL_REPLACE_EXISTING,
INSTALL_ALLOW_TEST,
INSTALL_EXTERNAL,
INSTALL_INTERNAL,
INSTALL_FROM_ADB,
INSTALL_ALL_USERS,
INSTALL_ALLOW_DOWNGRADE,
INSTALL_GRANT_RUNTIME_PERMISSIONS,
INSTALL_FORCE_VOLUME_UUID,
INSTALL_FORCE_PERMISSION_PROMPT,
INSTALL_EPHEMERAL,
INSTALL_DONT_KILL_APP
•	installerPackageName：执行安装的应用程序名，apk的名字。
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
   Method method = pm.getClass().getMethod("installPackage", Uri.class,IPackageInstallObserver.class,int.class,String.class);//得到installPackage（）方法

            method.invoke(pm, uri, observer, installFlags,apkFile);//调用installPackage（）方法
        } catch (Exception e) {
            Log.i("install fail",e.toString());
            e.printStackTrace();
        }

        }

/*静默安装回调*/
class MyPakcageInstallObserver extends IPackageInstallObserver.Stub{

    @Override
    public void packageInstalled(String packageName, int returnCode) {

//通过returnCode  0表示失败 1表示成功
        if (returnCode == 1) {
            Log.e("DEMO","安装成功");
        }else{
            Log.e("DEMO","安装失败,返回码是:"+returnCode);
        }
    }
}
静默卸载：
//静默卸载 反射方式
public static void JF_uninstall(Context context, String packageName) {
    try {
        PackageManager pm = context.getPackageManager();
        MyPackageDeleteObserver observer = new MyPackageDeleteObserver();
//得到deletePackage（）方法
        Method method = pm.getClass().getMethod("deletePackage", String.class, IPackageDeleteObserver.class, int.class);

//调用deletePackage（）
            method.invoke(pm, packageName, observer, 2);//第三个参数为2时表示删除所有数据，第三个参数有以下值可选：DELETE_KEEP_DATA,
DELETE_ALL_USERS,
DELETE_SYSTEM_APP（值为4，可删除系统应用或用系统签名的应用）,
DELETE_DONT_KILL_APP

    } catch (Exception e) {
        Log.i("uninstall fail",e.toString());
    }
}

//静默卸载回调  
class MyPackageDeleteObserver extends IPackageDeleteObserver.Stub {

    @Override
    public void packageDeleted(String packageName, int returnCode) {
        if (returnCode == 1) {
            Log.e("DEMO","卸载成功...");
        }else{
            Log.e("DEMO","卸载失败...返回码:"+returnCode);
        }
    }
}

声明权限：
在Manifest文件中加入如下语句：
<uses-permission android:name="android.permission.INSTALL_PACKAGES" />
<uses-permission android:name="android.permission.DELETE_PACKAGES" />
2、	在源码中编译实现
注：也需要系统签名
若不通过反射的方式，则另外一种方法是可以通过在源码中编译实现静默安装卸载
安装：
代码如下
//    静默安装 apkFile是apk的名字
public static void J_install(Context context, String apkFile, String packageName) {
    apkFile = "client-debug.apk";//for test 可删
    File installFile = new File(Environment.getExternalStorageDirectory(), File.separator + apkFile);
    Uri uri = Uri.fromFile(installFile);
    int installFlags = 2;//覆盖安装
    PackageManager pm = context.getPackageManager();
//安装函数的回调，此类在反射实现中有定义
       MyPakcageInstallObserver observer = new MyPakcageInstallObserver();
    pm.installPackage(uri, observer, installFlags, apkFile);//参数同反射实现中的参数意义一样
}
卸载：
代码如下
//静默卸载
public static void J_uninstall(Context context, String packageName) {
    PackageManager pm = context.getPackageManager();
    MyPackageDeleteObserver observer = new MyPackageDeleteObserver();
    pm.deletePackage(packageName, observer, 0);
}

声明权限：
在Manifest文件中加入如下语句：
<uses-permission android:name="android.permission.INSTALL_PACKAGES" />
<uses-permission android:name="android.permission.DELETE_PACKAGES" />
由于然后在manifest文件中，在<manifest/>节点中加入sharedUserId属性：
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.example.slienceinstall"
    android:sharedUserId="android.uid.system"          //声明为系统应用
    android:versionCode="1"
    android:versionName="1.0" >
  ... ...
<manifest/>
3、自动更新
自动更新功能的实现原理，就是我们事先和后台协商好一个接口，我们在应用的主Activity里，去访问这个接口，如果需要更新，后台会返回一些数据(比如，提示语；最新版本的url等)。然后开始下载，下载完成后开始调用静默安装方法覆盖原来的安装程序，这样用户的应用就保持最新的拉。
 

 
 
