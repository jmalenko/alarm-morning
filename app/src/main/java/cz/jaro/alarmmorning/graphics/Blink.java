package cz.jaro.alarmmorning.graphics;

import android.os.Handler;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import cz.jaro.alarmmorning.R;

/**
 * Blinks a message, waits a moment and then finishes the activity.
 */
public class Blink {
    private static final int DELAY_MILLIS = 500;

    private final AppCompatActivity activity;

    private TextView messageText;
    private TextView timeText;

    public Blink(AppCompatActivity activity) {
        this.activity = activity;
        init();
    }

    private void init() {
        activity.setContentView(R.layout.activity_blink);

        messageText = activity.findViewById(R.id.message);
        timeText = activity.findViewById(R.id.time);
    }

    /**
     * Redraw the activity and initialize the finish.
     */
    public void initiateFinish() {
        Handler disappearHandler = new Handler();
        disappearHandler.postDelayed(this::disappearActivity, DELAY_MILLIS);
    }

    private void disappearActivity() {
        activity.finish();
        activity.overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    public void setTimeText(int resid) {
        timeText.setVisibility(View.VISIBLE);
        timeText.setText(resid);
    }

    public void setTimeText(String text) {
        timeText.setVisibility(View.VISIBLE);
        timeText.setText(text);
    }

    public void setMessageText(int resid) {
        messageText.setText(resid);
    }

    public void setMessageText(String text) {
        messageText.setText(text);
    }
}
