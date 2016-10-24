package test.install;

import android.content.pm.IPackageInstallObserver;
import android.util.Log;

/**
 * Created by skysoft on 2016/10/20.
 */
 /*静默安装回调*/
class MyPakcageInstallObserver extends IPackageInstallObserver.Stub{

    @Override
    public void packageInstalled(String packageName, int returnCode) {
        if (returnCode == 1) {
            Log.e("DEMO","安装成功");
        }else{
            Log.e("DEMO","安装失败,返回码是:"+returnCode);
        }
    }
}

