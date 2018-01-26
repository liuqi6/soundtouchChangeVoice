package com.example.liuqi.soundtouch.feature.audiorecord;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.AutomaticGainControl;
import android.media.audiofx.NoiseSuppressor;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;

/**
 * Description:
 * Author: liuqi
 * Version: 1.0
 * Create Date Time: 2018/1/26 下午1:06.
 * Update Date Time:
 *
 * @see
 */
public class WavRecord {
    private static final String LOG_TAG = "WavRecord";
    private int mSampleRate = 44100;
    private int minBufferSize;
    private int recodeMaxTime=1000*60;
    private int fileLength;
    private AudioRecord mRecord;
    private int mAudioSessionId;
    private NoiseSuppressor mNoiseSuppressor;
    private AcousticEchoCanceler mAcousticEchoCanceler;
    private AutomaticGainControl mAutomaticGainControl;
    private String basePath;
    private String outputFilePath;
    ByteArrayOutputStream os;
    private boolean mIsAudioing;
    public WavRecord(String outputFilePath) {
        this.outputFilePath = outputFilePath;
        init();
    }

    private void init() {
        int channelConfig = AudioFormat.CHANNEL_IN_MONO;
        int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
        minBufferSize = AudioRecord.getMinBufferSize(mSampleRate, channelConfig, audioEncoding);
        Log.i(LOG_TAG, "minBufferSize: " + minBufferSize);

        mRecord = new AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                mSampleRate,
                channelConfig,
                audioEncoding,
                minBufferSize * 2);
        mAudioSessionId = mRecord.getAudioSessionId();
        setNoiseSuppressor();
    }
    public void setOutputFilePath(String outputFilePath) {
        this.outputFilePath = outputFilePath;
    }

    public void setSampleRate(int sampleRate) {
        this.mSampleRate = sampleRate;
    }

    public void setRecodeMaxTime(int recodeMaxTime) {
        this.recodeMaxTime = recodeMaxTime;
    }


    public void setNoiseSuppressor() {
        if (mAudioSessionId != 0 && android.os.Build.VERSION.SDK_INT >= 16) {
            if (NoiseSuppressor.isAvailable()) {
                if (mNoiseSuppressor != null) {
                    mNoiseSuppressor.release();
                    mNoiseSuppressor = null;
                }

                mNoiseSuppressor = NoiseSuppressor.create(mAudioSessionId);
                if (mNoiseSuppressor != null) {
                    mNoiseSuppressor.setEnabled(true);
                } else {
                    Log.i(LOG_TAG, "Failed to create NoiseSuppressor.");
                }
            } else {
                Log.i(LOG_TAG, "Doesn't support NoiseSuppressor");
            }

            if (AcousticEchoCanceler.isAvailable()) {
                if (mAcousticEchoCanceler != null) {
                    mAcousticEchoCanceler.release();
                    mAcousticEchoCanceler = null;
                }

                mAcousticEchoCanceler = AcousticEchoCanceler.create(mAudioSessionId);
                if (mAcousticEchoCanceler != null) {
                    mAcousticEchoCanceler.setEnabled(true);
                    // mAcousticEchoCanceler.setControlStatusListener(listener)setEnableStatusListener(listener)
                } else {
                    Log.i(LOG_TAG, "Failed to initAEC.");
                    mAcousticEchoCanceler = null;
                }
            } else {
                Log.i(LOG_TAG, "Doesn't support AcousticEchoCanceler");
            }

            if (AutomaticGainControl.isAvailable()) {
                if (mAutomaticGainControl != null) {
                    mAutomaticGainControl.release();
                    mAutomaticGainControl = null;
                }

                mAutomaticGainControl = AutomaticGainControl.create(mAudioSessionId);
                if (mAutomaticGainControl != null) {
                    mAutomaticGainControl.setEnabled(true);
                } else {
                    Log.i(LOG_TAG, "Failed to create AutomaticGainControl.");
                }

            } else {
                Log.i(LOG_TAG, "Doesn't support AutomaticGainControl");
            }
        }
    }

    public boolean ismIsAudioing() {
        return mIsAudioing;
    }

    public void startAudio() {
        mIsAudioing = true;
        new AudioThread().start();
    }


    public void stopAudio() {
        mIsAudioing = false;
    }
    private class AudioThread extends Thread {

        @Override
        public void run() {
            try {
                android.os.Process
                        .setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
            } catch (Exception e) {

            }
            try {
                mRecord.startRecording();
                byte[] buffer = new byte[minBufferSize];
                os =  new ByteArrayOutputStream();
                while (mIsAudioing) {
                    int read = mRecord.read(buffer, 0, minBufferSize);
                    if (read > 0) {
                        os.write(buffer);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            byte[] wavDatas=os.toByteArray();
            fileLength=wavDatas.length;
                try {
                    // 保存文件
                    FileOutputStream out = new FileOutputStream(outputFilePath);
                    out.write(getWavHeader(wavDatas.length));
                    out.write(wavDatas);
                    out.close();
                    mRecord.stop();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

    public byte[] getWavHeader(long totalAudioLen){
        int mChannels = 1;
        long totalDataLen = totalAudioLen + 36;
        long longSampleRate = mSampleRate;
        long byteRate = mSampleRate * 2 * mChannels;
        byte[] header = new byte[44];
        header[0] = 'R';  // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f';  // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16;  // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1;  // format = 1
        header[21] = 0;
        header[22] = (byte) mChannels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (2 * mChannels);  // block align
        header[33] = 0;
        header[34] = 16;  // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

        return header;
    }
}
