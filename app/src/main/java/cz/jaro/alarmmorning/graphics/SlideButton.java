package cz.jaro.alarmmorning.graphics;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.SeekBar;

/**
 * Created by jmalenko on 19.2.2016.
 */
public class SlideButton extends SeekBar {

    private Drawable thumb;
    private OnClickListener listener;

    public SlideButton(Context context, AttributeSet attrs) {
        super(context, attrs);

        setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
                //label.setAlpha(1f - progress * 0.02f);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

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
                    handleSlide();
                    seekBar.setProgress(0);
                }
            }
        });
        setOnTouchListener(new View.OnTouchListener() {
            private boolean isInvalidMove;

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        return isInvalidMove = motionEvent.getX() > getThumb().getIntrinsicWidth();
                    case MotionEvent.ACTION_MOVE:
                        return isInvalidMove;
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

//    @Override
//    public boolean onTouchEvent(MotionEvent event) {
//        if (event.getAction() == MotionEvent.ACTION_DOWN) {
//            if (thumb.getBounds().contains((int) event.getX(), (int) event.getY())) {
//                super.onTouchEvent(event);
//            } else
//                return false;
//        } else if (event.getAction() == MotionEvent.ACTION_UP) {
//            if (getProgress() > 70)
//                handleSlide();
//
//            setProgress(0);
//        } else
//            super.onTouchEvent(event);
//
//        return true;
//    }

    private void handleSlide() {
        listener.onClick(this);
    }

    public void setSlideButtonListener(OnClickListener listener) {
        this.listener = listener;
    }

}