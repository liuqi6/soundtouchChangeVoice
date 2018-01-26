package com.example.liuqi.soundtouch.feature.soundtouch;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.liuqi.soundtouch.feature.ExampleActivity;
import com.example.liuqi.soundtouch.feature.R;
import com.example.liuqi.soundtouch.feature.audiorecord.WavRecord;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Main2Activity extends AppCompatActivity {
    private static final int REQUEST_PERMISSIONS_CODE = 3;
    private boolean writeExtStorage;
    private boolean readExtStorage;
    private boolean hasRecordAudio;
    private List<String> permissions;
    private Button button1;
    private Button button2;
    private String recordFilename;
    private WavRecord record;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        button1 = findViewById(R.id.button);
        button2 = findViewById(R.id.button2);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (writeExtStorage && readExtStorage && hasRecordAudio) {
                setOutputFilePath();
            } else {
                permissions = new ArrayList<>();
                if (!writeExtStorage) {
                    permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                }

                if (!readExtStorage) {
                    permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
                }

                if (!hasRecordAudio) {
                    permissions.add(Manifest.permission.RECORD_AUDIO);
                }

                //没有权限,去申请索要权限
                String[] strs = new String[permissions.size()];
                for (int i = 0; i < permissions.size(); i++) {
                    strs[i] = permissions.get(i);
                }
                ActivityCompat.requestPermissions(this, strs, REQUEST_PERMISSIONS_CODE);
            }
        } else {
            setOutputFilePath();
        }
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
             if(null!=record){
                 if(!record.ismIsAudioing()) {
                     button1.setText("停下");
                     record.startAudio();
                 }else {
                     button1.setText("录制");
                     record.stopAudio();
                 }
             }
            }
        });
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent it =new Intent(Main2Activity.this,ExampleActivity.class);
                it.putExtra("path",recordFilename);
                startActivity(it);
            }
        });
    }

    public void setOutputFilePath() {
        recordFilename = getFile("sountouch").getAbsolutePath()
                + "/rec_" + System.currentTimeMillis() + ".wav";
        record = new WavRecord(recordFilename);
    }

    /**
     * 获取缓存目录
     *
     * @param cacheKey 缓存的类型
     * @return
     */
    public File getFile(String cacheKey) {
        File file;
        if (android.os.Environment.getExternalStorageState().equals(
                android.os.Environment.MEDIA_MOUNTED)) {
            file = Environment.getExternalStorageDirectory();
        } else {
            file = getApplication().getApplicationContext().getFilesDir();
        }

        file = new File(file, cacheKey);

        if (!file.exists() || !file.isDirectory()) {
            file.mkdirs();
        }
        return file;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSIONS_CODE) {
            if (grantResults.length > 0) {
                //给予了权限
                for (int i = 0; i < permissions.length; i++) {
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                }
                //成功获取权限
                setOutputFilePath();
            } else {
                //拒绝了权限
                Toast.makeText(Main2Activity.this, "请在权限管理界面打开应用的存储和录音权限！", Toast.LENGTH_LONG).show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
