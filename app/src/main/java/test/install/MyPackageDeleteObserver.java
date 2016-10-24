package test.install;


import android.content.pm.IPackageDeleteObserver;
import android.util.Log;

/**
 * Created by skysoft on 2016/10/20.
 */
 /* 静默卸载回调 */
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
