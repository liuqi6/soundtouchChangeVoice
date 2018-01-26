/////////////////////////////////////////////////////////////////////////////
///
/// Example Android Application/Activity that allows processing WAV 
/// audio files with SoundTouch library
///
/// Copyright (c) Olli Parviainen
///
////////////////////////////////////////////////////////////////////////////////
//
// $Id: SoundTouch.java 210 2015-05-14 20:03:56Z oparviai $
//
////////////////////////////////////////////////////////////////////////////////


package com.example.liuqi.soundtouch.feature;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.liuqi.soundtouch.feature.soundtouch.SoundTouch;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ExampleActivity extends Activity implements OnClickListener {
    private static final int FILE_SELECT_CODE1 = 1;
    private static final int FILE_SELECT_CODE2 = 2;
    private static final int REQUEST_PERMISSIONS_CODE = 3;
    private boolean writeExtStorage;
    private boolean readExtStorage;
    private boolean hasRecordAudio;
    private List<String> permissions;
    TextView textViewConsole = null;
    EditText editSourceFile = null;
    EditText editOutputFile = null;
    EditText editTempo = null;
    EditText editPitch = null;
    CheckBox checkBoxPlay = null;
    private Button playButton;
    MediaPlayer mediaPlayer;

    StringBuilder consoleText = new StringBuilder();
    String changeFilename;
    String path;

    /// Called when the activity is created
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_example);
        path = getIntent().getStringExtra("path");
        textViewConsole = (TextView) findViewById(R.id.textViewResult);
        editSourceFile = (EditText) findViewById(R.id.editTextSrcFileName);
        editOutputFile = (EditText) findViewById(R.id.editTextOutFileName);

        editTempo = (EditText) findViewById(R.id.editTextTempo);
        editPitch = (EditText) findViewById(R.id.editTextPitch);

        Button buttonFileSrc = (Button) findViewById(R.id.buttonSelectSrcFile);
        Button buttonFileOutput = (Button) findViewById(R.id.buttonSelectOutFile);
        Button buttonProcess = (Button) findViewById(R.id.buttonProcess);
        Button playButton = (Button) findViewById(R.id.play);
        buttonFileSrc.setOnClickListener(this);
        buttonFileOutput.setOnClickListener(this);
        buttonProcess.setOnClickListener(this);
        playButton.setOnClickListener(this);
        checkBoxPlay = (CheckBox) findViewById(R.id.checkBoxPlay);
        writeExtStorage = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;

        readExtStorage = ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
        hasRecordAudio = ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
        editSourceFile.setText(path);
        // Check soundtouch library presence & version
        checkLibVersion();
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
    }

    public void setOutputFilePath() {
        changeFilename = getFile("sountouch").getAbsolutePath()
                + "/change_" + System.currentTimeMillis() + ".wav";
        editOutputFile.setText(changeFilename);
    }

    /// Function to append status text onto "console box" on the Activity
    public void appendToConsole(final String text) {
        // run on UI thread to avoid conflicts
        runOnUiThread(new Runnable() {
            public void run() {
                consoleText.append(text);
                consoleText.append("\n");
                textViewConsole.setText(consoleText);
            }
        });
    }


    /// print SoundTouch native library version onto console
    protected void checkLibVersion() {
        String ver = SoundTouch.getVersionString();
        appendToConsole("SoundTouch native library version = " + ver);
    }


    /// Button click handler
    @Override
    public void onClick(View arg0) {
        if (arg0.getId() == R.id.buttonSelectSrcFile) {
            showFileChooser(FILE_SELECT_CODE1);
        } else if (arg0.getId() == R.id.buttonSelectOutFile) {

        } else if (arg0.getId() == R.id.buttonProcess) {
            process();
        } else if (arg0.getId() == R.id.play) {
            play();
        }

    }

    private void showFileChooser(int code) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(intent, "Select a File to Upload"), code);
        } catch (android.content.ActivityNotFoundException ex) {
            // Potentially direct the user to the Market with a Dialog
            Toast.makeText(this, "Please install a File Manager.", Toast.LENGTH_SHORT).show();
        }
    }


    /// Play audio file
    protected void playWavFile(String fileName) {
        File file2play = new File(fileName);
        Intent i = new Intent();
        i.setAction(Intent.ACTION_VIEW);
        i.setDataAndType(Uri.fromFile(file2play), "audio/wav");
        startActivity(i);
    }


    /// Helper class that will execute the SoundTouch processing. As the processing may take
    /// some time, run it in background thread to avoid hanging of the UI.
    protected class ProcessTask extends AsyncTask<ProcessTask.Parameters, Integer, Long> {
        /// Helper class to store the SoundTouch file processing parameters
        public final class Parameters {
            String inFileName;
            String outFileName;
            float tempo;
            float pitch;
        }


        /// Function that does the SoundTouch processing
        public final long doSoundTouchProcessing(Parameters params) {

            SoundTouch st = new SoundTouch();
            st.setTempo(params.tempo);
            st.setPitchSemiTones(params.pitch);
            Log.i("SoundTouch", "process file " + params.inFileName);
            long startTime = System.currentTimeMillis();
            int res = st.processFile(params.inFileName, params.outFileName);
            long endTime = System.currentTimeMillis();
            float duration = (endTime - startTime) * 0.001f;

            Log.i("SoundTouch", "process file done, duration = " + duration);
            appendToConsole("Processing done, duration " + duration + " sec.");
            if (res != 0) {
                String err = SoundTouch.getErrorString();
                appendToConsole("Failure: " + err);
                return -1L;
            }

            // Play file if so is desirable
            if (checkBoxPlay.isChecked()) {
                playWavFile(params.outFileName);
            }
            return 0L;
        }


        /// Overloaded function that get called by the system to perform the background processing
        @Override
        protected Long doInBackground(Parameters... aparams) {
            return doSoundTouchProcessing(aparams[0]);
        }

    }


    /// process a file with SoundTouch. Do the processing using a background processing
    /// task to avoid hanging of the UI
    protected void process() {
        try {
            ProcessTask task = new ProcessTask();
            ProcessTask.Parameters params = task.new Parameters();
            // parse processing parameters
            params.inFileName = editSourceFile.getText().toString();
            params.outFileName = editOutputFile.getText().toString();
            params.tempo = 0.01f * Float.parseFloat(editTempo.getText().toString());
            params.pitch = Float.parseFloat(editPitch.getText().toString());

            // update UI about status
            appendToConsole("Process audio file :" + params.inFileName + " => " + params.outFileName);
            appendToConsole("Tempo = " + params.tempo);
            appendToConsole("Pitch adjust = " + params.pitch);

            Toast.makeText(this, "Starting to process file " + params.inFileName + "...", Toast.LENGTH_SHORT).show();

            // start SoundTouch processing in a background thread
            task.execute(params);
//			task.doSoundTouchProcessing(params);	// this would run processing in main thread

        } catch (Exception exp) {
            exp.printStackTrace();
        }

    }

    private static final String TAG = "ChooseFile";

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case FILE_SELECT_CODE1:
                if (resultCode == RESULT_OK) {
                    // Get the Uri of the selected file
                    Uri uri = data.getData();
                    Log.d(TAG, "File Uri: " + uri.toString());
                    // Get the path
                    String path = getPath(ExampleActivity.this, uri);
                    editSourceFile.setText(uri.getPath());
                    Log.d(TAG, "File Path: " + path);
                    // Get the file instance
                    // File file = new File(path);
                    // Initiate the upload
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public static String getPath(Context context, Uri uri) {
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            String[] projection = {"_data"};
            Cursor cursor = null;
            try {
                cursor = context.getContentResolver().query(uri, null, null, null, null);
                int column_index = cursor.getColumnIndexOrThrow("_data");
                if (cursor.moveToFirst()) {
                    return cursor.getString(column_index);
                }
            } catch (Exception e) {
                // Eat it  Or Log it.
            }
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
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
                Toast.makeText(ExampleActivity.this, "请在权限管理界面打开应用的存储和录音权限！", Toast.LENGTH_LONG).show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void play() {
        if (null == mediaPlayer) {
            mediaPlayer = new MediaPlayer();
        }
        try {
            mediaPlayer.setDataSource(changeFilename);
            mediaPlayer.prepareAsync();
        } catch (Exception e) {
            try {
                mediaPlayer.setDataSource(path);
                mediaPlayer.prepareAsync();
            } catch (Exception ee) {
                return;
            }
        }
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.start();
            }
        });
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mp.reset();
            }
        });
    }
}