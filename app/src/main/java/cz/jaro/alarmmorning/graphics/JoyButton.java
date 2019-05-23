package cz.jaro.alarmmorning.graphics;

import android.content.Context;
import android.support.v7.widget.AppCompatImageButton;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

/**
 * JoyStickButton is a button. Moreover, while holding the button the user can move the button. In the Alarm Morning app, the posistion of where the button was
 * released denotes the snooze minutes.
 * <p>
 * Definition:
 * <ul>
 * <li>Move by less or equal to <strong>distanceMin</strong> is considered as no move. Releasing the touch invokes the onUp event with click=true.</li>
 * <li>Move by more than <strong>distanceMin</strong> and less or equal to than <strong>distanceCancel</strong> is considered as a standard move. Releasing the touch invokes the onUp event with click=false.</li>
 * <li>Move by more than <strong>distanceCancel</strong>. The action is cancelled, the button is moved back to original position, the onCancel event is invoked, no onUp event is invoked.
 * </ul>
 */
public class JoyButton extends AppCompatImageButton {

    private static final String TAG = JoyButton.class.getSimpleName();

    int distanceMin = 20;
    int distanceCancel = 164;

    private OnJoyClickListener listener = null;

    public JoyButton(Context context, AttributeSet attrs) {
        super(context, attrs);

        setOnTouchListener(new OnTouchListener() {
            private boolean isCancelled;

            private float motionEvent_original_x;
            private float motionEvent_original_y;

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                Log.w(TAG, "onTouch() action=" + motionEvent.getAction() + ", x,y=[" + motionEvent.getX() + ", " + motionEvent.getX() + "]");

                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        motionEvent_original_x = motionEvent.getX();
                        motionEvent_original_y = motionEvent.getY();

                        isCancelled = false;

                        if (listener != null)
                            listener.onDown(view);

                        return true;
                    case MotionEvent.ACTION_MOVE:
                    case MotionEvent.ACTION_UP:
                        if (isCancelled)
                            return true;

                        float current_x = motionEvent.getX();
                        float current_y = motionEvent.getY();

                        float delta_x = current_x - motionEvent_original_x;
                        float delta_y = current_y - motionEvent_original_y;
                        double distance = Math.sqrt(delta_x * delta_x + delta_y * delta_y);
                        boolean click = distance <= distanceMin;

                        Log.d(TAG, "delta=[" + delta_x + ", " + delta_y + "], current=[" + current_x + ", " + current_y + "], motionEvent_original=[" + motionEvent_original_x + ", " + motionEvent_original_y + "], distance=" + distance + ", click=" + click);

                        if (motionEvent.getAction() == MotionEvent.ACTION_MOVE) {
                            if (distance <= distanceCancel) {
                                if (listener != null)
                                    listener.onMove(view, delta_x, delta_y, click);
                            } else {
                                isCancelled = true;

                                if (listener != null)
                                    listener.onCancel(view);
                            }
                        } else {
                            if (listener != null)
                                listener.onUp(view, delta_x, delta_y, click);
                        }
                        return true;
                }
                return false;
            }
        });
    }

    /**
     * Register a callback to be invoked when an interaction happens clicked. If this view is not
     * clickable, it becomes clickable.
     *
     * @param l The callback that will run
     * @see #setClickable(boolean)
     */
    public void setOnJoyClickListener(OnJoyClickListener l) {
        listener = l;
    }

    public interface OnJoyClickListener {

        void onDown(View v);

        void onMove(View v, float dx, float dy, boolean click);

        /**
         * Called when a touch has been released.
         *
         * @param v The view that was released.
         */
        void onUp(View v, float dx, float dy, boolean click);

        void onCancel(View v);
    }
}

