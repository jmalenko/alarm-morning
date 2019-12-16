package cz.jaro.alarmmorning;

import android.media.MediaRecorder;

import java.io.IOException;

public class SoundMeter {

    private MediaRecorder mRecorder;

    public void start() throws RuntimeException, IOException {
        if (mRecorder == null) {
            mRecorder = new MediaRecorder();
            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mRecorder.setOutputFile("/dev/null/");
            mRecorder.prepare();
            mRecorder.start();
        }
    }

    public void stop() {
        if (mRecorder != null) {
            mRecorder.stop();
            mRecorder.release();
            mRecorder = null;
        }
    }

    /**
     * @return Amplitude. Max amplitude is 32768.
     */
    public double getMaxAmplitude() {
        return mRecorder.getMaxAmplitude();
    }

}