package test.install;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    Button install,delete;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(test.packmanager.R.layout.activity_main);
    }

    @Override
    public void onClick(final View v) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                switch (v.getId()){
                    case test.packmanager.R.id.install:
                        InstallUtil.J_install(MainActivity.this,"test","test.packmanager");//静默安装
                        break;
                    case test.packmanager.R.id.delete:
                        InstallUtil.J_uninstall(MainActivity.this,"test.client");//静默卸载
                        break;
                    case test.packmanager.R.id.install2:
                        InstallUtil.install(MainActivity.this,"test");//普通安装
                        break;
                    case test.packmanager.R.id.uninstall2:
                        InstallUtil.uninstall(MainActivity.this,"test.client");//普通卸载
                        break;
                    case test.packmanager.R.id.install_root:
                        InstallUtil.J_installRoot("test");//需要root的静默安装
                        break;
                    case test.packmanager.R.id.uninstall_root:
                        InstallUtil.J_uninstallRoot("test.client");
                        break;
                    case test.packmanager.R.id.install3:
                        InstallUtil.JF_install(MainActivity.this,"test","test.packmanager");//反射方式
                        break;
                    case test.packmanager.R.id.uninstall3:
                        InstallUtil.JF_uninstall(MainActivity.this,"test.client");//反射方式
                        break;
                }
            }
        }).start();

    }


}
