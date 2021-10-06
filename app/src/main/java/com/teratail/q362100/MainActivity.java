package com.teratail.q362100;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.*;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import android.Manifest;
import android.app.Dialog;
import android.content.pm.PackageManager;
import android.media.*;
import android.os.*;
import android.util.Log;
import android.view.Gravity;
import android.widget.*;

public class MainActivity extends AppCompatActivity {
  @SuppressWarnings("UnusedDeclaration")
  private static final String TAG = "MainActivity";

  private static final int AUDIO_SAMPLE_FREQ = 44100;//サンプリング周波数
  private static final int FRAME_RATE = 10;
  private static final String START_TEXT = "START";
  private static final String STOP_TEXT = "STOP";

  private SurfaceDrawer surfaceDrawer;
  private Button button;
  private HandlerThread listenerThread;
  private AudioRecord recorder;

  public void onCreate(Bundle bundle) {
    super.onCreate(bundle);
    setContentView(R.layout.activity_main);

    surfaceDrawer = new SurfaceDrawer(findViewById(R.id.surfaceView));

    button = findViewById(R.id.button);
    button.setEnabled(true);
    button.setText(START_TEXT);
    button.setOnClickListener(v -> {
      switch(button.getText().toString()) { //トグル
        case START_TEXT:
          permissionCheckAndStart();
          break;
        case STOP_TEXT:
          recorder.stop();
          button.setText(START_TEXT);
          break;
      }
    });
  }

  //権限要求ダイアログ表示・返答受付
  private final ActivityResultLauncher<String> requestPermissionLauncher =
          registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
              start();
            } else {
              Toast t = Toast.makeText(this, "権限が許可されなかった為、実行できません。", Toast.LENGTH_LONG);
              t.setGravity(Gravity.CENTER, 0, 0);
              t.show();
            }
          });

  //権限説明ダイアログ  ok 押下 → 権限要求ダイアログ
  public static class ExplainDialogFragment extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
      MainActivity activity = (MainActivity)requireActivity();
      return new AlertDialog.Builder(activity)
              .setTitle("使用許可の説明")
              .setMessage("音声波形を表示するにはマイクの使用(「音声の録音」)の許可が必要です")
              .setNeutralButton(android.R.string.cancel, (dialog,which)->{})
              .setPositiveButton(android.R.string.ok, (dialog,which)->{
                  activity.requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
              })
              .create();
    }
  }

  //権限チェック
  private void permissionCheckAndStart() {
    if(ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
      start();
    } else if (shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)) {
      new ExplainDialogFragment().show(getSupportFragmentManager(), "Explain");
    } else {
      requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
    }
  }

  //波形表示開始
  private void start() {
    if(recorder == null) initialize();
    recorder.startRecording();
    button.setText(STOP_TEXT);
  }

  private void initialize() {
    int frameBufferSize = AUDIO_SAMPLE_FREQ / FRAME_RATE;
    short[] audioData = new short[frameBufferSize];

    recorder = settingRecorder(AUDIO_SAMPLE_FREQ, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, frameBufferSize);

    listenerThread = new HandlerThread("RecordPositionUpdateListenerThread");
    listenerThread.start();

    recorder.setRecordPositionUpdateListener(new AudioRecord.OnRecordPositionUpdateListener() {
      @Override
      public void onMarkerReached(AudioRecord recorder) {}

      @Override
      public void onPeriodicNotification(AudioRecord recorder) {
        recorder.read(audioData, 0, audioData.length);
        surfaceDrawer.draw(audioData);
      }
    }, new Handler(listenerThread.getLooper())); //HandlerThread でリスナを実行
  }

  private void terminate() {
    if(recorder != null) {
      recorder.stop();
      recorder.release();
    }
    recorder = null;

    if(listenerThread != null) listenerThread.quit();
    listenerThread = null;
  }

  private AudioRecord settingRecorder(int sampleRateInHz, int channelConfig, int audioFormat, int frameBufferSize) {
    int minBufferSize = AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat);
    if(minBufferSize == AudioRecord.ERROR_BAD_VALUE) throw new IllegalArgumentException();
    if(minBufferSize == AudioRecord.ERROR) throw new IllegalStateException("minBufferSize");

    int bufferSizeInBytes = Math.max(minBufferSize, frameBufferSize*10); //"*10"は余裕分

    AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRateInHz, channelConfig, audioFormat, bufferSizeInBytes);
    if(recorder.getState() != AudioRecord.STATE_INITIALIZED) throw new IllegalStateException("recorder.getState");

    if(recorder.setPositionNotificationPeriod(frameBufferSize) != AudioRecord.SUCCESS) throw new IllegalStateException("setPositionNotificationPeriod frameBufferSize="+frameBufferSize);

    return recorder;
  }

  @Override
  protected void onPause() { //停止
    super.onPause();
    terminate();
  }

  @Override
  protected void onResume() { //再開
    super.onResume();
    if(button.getText().toString().equals(STOP_TEXT)) { //実行中だった
      permissionCheckAndStart();
    }
  }
}