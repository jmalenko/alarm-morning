package cz.jaro.alarmmorning.graphics;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.SeekBar;

/**
 * SlideButton is a button that needs to be slided instead of clicked. This is useful for sensitive actions where a click can be done too easily and a more
 * intentional action (like slide) is required.
 */
public class SlideButton extends androidx.appcompat.widget.AppCompatSeekBar {

    private Drawable thumb;

    public SlideButton(Context context, AttributeSet attrs) {
        super(context, attrs);

        setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
                // nothing
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // nothing
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int progress = seekBar.getProgress();
                if (progress < 100) {
                    ObjectAnimator anim = ObjectAnimator.ofInt(seekBar, "progress", 0);
                    anim.setInterpolator(new AccelerateDecelerateInterpolator());
                    anim.setDuration(getResources().getInteger(android.R.integer.config_shortAnimTime));
                    anim.start();
                } else {
                    performClick();
                    seekBar.setProgress(0);
                }
            }
        });

        setOnTouchListener(new View.OnTouchListener() {
            private boolean isInvalidMove;

            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        return isInvalidMove = motionEvent.getX() > getThumb().getIntrinsicWidth();
                    case MotionEvent.ACTION_MOVE:
                    case MotionEvent.ACTION_UP:
                        return isInvalidMove;
                }
                return false;
            }
        });
    }

    @Override
    public void setThumb(Drawable thumb) {
        super.setThumb(thumb);
        this.thumb = thumb;
    }

    @Override
    public Drawable getThumb() {
        return thumb;
    }

}