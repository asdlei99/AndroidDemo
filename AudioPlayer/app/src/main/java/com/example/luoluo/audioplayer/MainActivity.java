package com.example.luoluo.audioplayer;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.media.AudioManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    // Used to load the 'native-lib' library on application startup.
    private static final String TAG = "MainActivity";
    static AssetManager assetManager;
    static boolean isPlayingAsset = false;
    int sampleRate = 0;
    int bufSize = 0;
    private static final int AUDIO_ECHO_REQUEST = 0;
    static {
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        assetManager = getAssets();
        //初始化ffmpeg
        initffmpeg();
        //初始化opensl引擎
        createEngine();

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            AudioManager myAudioMgr = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

            String nativeParam = myAudioMgr.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE);
            sampleRate = Integer.parseInt(nativeParam);//48000 16bit存储方式
            nativeParam = myAudioMgr.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER);
            bufSize = Integer.parseInt(nativeParam);
        }
        Log.d(TAG, "onCreate: smpleRate="+sampleRate +"buffSize="+bufSize);//默认采样率 48000 buffsize=960
        //创建播放器
        createBufferQueueAudioPlayer(sampleRate, bufSize);

        // Example of a call to a native method
//        TextView tv = (TextView) findViewById(R.id.sample_text);
//        tv.setText(stringFromJNI());


        Button button = findViewById(R.id.b_recode);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: start recode button ");
                //开始录制
                try {
                    startRecode();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        Button playButton = findViewById(R.id.b_play);
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //播放音频文件
//                startPlay();
                Log.d(TAG, "start play audio.");
                //录音的--播放
                selectClip(3);
            }
        });
    }

    public void startPlay(){
        boolean created = false;
        if (!created) {
            created = createAssetAudioPlayer(assetManager, "background.mp3");
        }

        if (created) {
            isPlayingAsset = !isPlayingAsset;
            setPlayingAssetAudioPlayer(isPlayingAsset);
        }
    }

    public void popAlertDialog(){
        //    通过AlertDialog.Builder这个类来实例化我们的一个AlertDialog的对象
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        //    设置Title的图标
//        builder.setIcon(R.drawable.ic_launcher);
        builder.setTitle("提示");
        //    设置Content来显示一个信息
        builder.setMessage("正在编码、编码时长约8秒");
        //    设置一个PositiveButton
        builder.setPositiveButton("我知道了", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                Toast.makeText(MainActivity.this, "positive: " + which, Toast.LENGTH_SHORT).show();
            }
        });

        //显示出该对话框
        builder.show();
    }

    public  void  startRecode() throws IOException {

        Toast.makeText(MainActivity.this,"正在录制、点击播放会在编码后进行播放",Toast.LENGTH_LONG).show();

        int status = ActivityCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.RECORD_AUDIO);
        if (status != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    MainActivity.this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    AUDIO_ECHO_REQUEST);
            return;
        }
        //录制
        recordAudio();

    }

    static boolean created = false;
    private void recordAudio() throws IOException {
        if (!created) {
            created = createAudioRecorder();
        }
        if (created) {
            Log.d(TAG, "recordAudio: start recod");
            startRecording();
        }

        //保存到沙盒目录
//        this.saveString("i will change my life.");

    }


    public  void saveString(String string) throws IOException {
        File file = new File(getFilesDir(), "hello_luoluo2");
        if(!file.exists()){
            file.createNewFile();
        }
        FileOutputStream fos  = new FileOutputStream(file);
        fos.write(string.getBytes());
        fos.close();


    }


    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
    //创建引擎
    public static native void createEngine();
    //将音频文件绑定到音频播放器
    public static native boolean createAssetAudioPlayer(AssetManager assetManager, String filename);
    // true == PLAYING, false == PAUSED
    public static native void setPlayingAssetAudioPlayer(boolean isPlaying);
    public  native void shutDown();

    //创建录音对象
    public static native boolean createAudioRecorder();
    //开始录音
    public static native void startRecording();
    //裁剪录制的音频buff
    public static native boolean selectClip(int count);
    //播放录音buff播放器
    public static native void createBufferQueueAudioPlayer(int sampleRate, int samplesPerBuf);

    public static native void initffmpeg();




}


/*
    android默认采集使用的 48000 16bit存储方式
    而ffmpeg转码的时候采用 48000 32bit存储方式
 */