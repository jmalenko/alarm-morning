package cz.jaro.alarmmorning;


import android.os.Handler;
import android.util.Log;

import java.util.Calendar;

import cz.jaro.alarmmorning.clock.Clock;
import cz.jaro.alarmmorning.clock.SystemClock;

/**
 * HandlerOnClockChange runs a Runnable whenever the clock changes. A Unit (hour, minute, second...) defines on which clock unit change the Runnable method
 * {@link Runnable#run()} is called.
 * <p/>
 * The Unit and Runnable may be changed while the HandlerOnClockChange is running.
 * <p/>
 * The {@link Runnable#run()} is also on {@link #start()}, {@link #stop()} and whenever the Unit or Runnable changes.
 * <p/>
 * The Unit identifiers are reused from {@link Calendar}. The default unit is {@link Calendar#SECOND}.
 */
public class HandlerOnClockChange extends Handler {

    private static final String TAG = HandlerOnClockChange.class.getSimpleName();

    private Integer unit;

    private Runnable runnable;

    private final Runnable runnableOnClockChange = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "Clock (and time unit) changed");

            runRunnable();

            registerNextClockChange();
        }
    };

    private boolean isRunning;

    public HandlerOnClockChange() {
        this(Calendar.SECOND);
    }

    /**
     * Constructor that sets the Unit.
     *
     * @param unit Clock unit identifier
     */
    public HandlerOnClockChange(int unit) {
        this(unit, null);
    }

    /**
     * Constructor that sets the Runnable.
     *
     * @param runnable Runnable
     */
    public HandlerOnClockChange(Runnable runnable) {
        this(Calendar.SECOND, runnable);
    }

    /**
     * Constructor that sets the Unit and Runnable.
     *
     * @param unit     Clock unit identifier
     * @param runnable Runnable
     */
    public HandlerOnClockChange(int unit, Runnable runnable) {
        super();
        setUnit(unit);
        setRunnable(runnable);
    }

    /**
     * Get the Unit.
     *
     * @return Clock unit identifier
     */
    public int getUnit() {
        return unit;
    }

    /**
     * Set the Unit.
     *
     * @param unit Clock unit identifier
     */
    public void setUnit(int unit) {
        if (!(unit == Calendar.YEAR ||
                unit == Calendar.MONTH ||
                unit == Calendar.WEEK_OF_YEAR ||
                unit == Calendar.DATE ||
                unit == Calendar.HOUR_OF_DAY ||
                unit == Calendar.MINUTE ||
                unit == Calendar.SECOND)) {
            throw new RuntimeException("Unsupported unit " + unit);
        }

        this.unit = unit;

        if (isRunning)
            runRunnable();
    }

    /**
     * Get the Runnable.
     *
     * @return Runnable
     */
    public Runnable getRunnable() {
        return runnable;
    }

    /**
     * Set the Runnable.
     *
     * @param runnable Runnable
     */
    public void setRunnable(Runnable runnable) {
        this.runnable = runnable;

        if (isRunning)
            runRunnable();
    }

    /**
     * Checks whether the HandlerOnClockChange is running.
     *
     * @return true if the handler is running
     */
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * First, set the Unit and Runnable.
     * Second, start running the Runnable on the change of clock, whenever the clock unit changes.
     *
     * @param runnable Runnable
     * @param unit     Unit
     */
    public void start(Runnable runnable, int unit) {
        setUnit(unit);
        start(runnable);
    }

    /**
     * First, set the Runnable.
     * Second, start running the Runnable on the change of clock, whenever the clock unit changes.
     *
     * @param runnable Runnable
     */
    public void start(Runnable runnable) {
        setRunnable(runnable);
        start();
    }

    /**
     * Start running the Runnable on the change of clock, whenever the clock unit changes.
     */
    public void start() {
        if (unit == null) {
            throw new RuntimeException("Unit not set");
        }

        isRunning = true;

        runRunnable();

        registerNextClockChange();
    }

    /**
     * Stop running the Runnable on the change of clock.
     */
    public void stop() {
        Log.d(TAG, "Stopping");
        removeCallbacks(runnableOnClockChange);
        isRunning = false;
    }

    /**
     * @return Returns true if the Runnable was successfully placed in to the message queue. Returns false on failure, usually because the looper processing the
     * message queue is exiting. Note that a result of true does not mean the Runnable will be processed -- if the looper is quit before the delivery time of
     * the message occurs then the message will be dropped.
     */
    private boolean registerNextClockChange() {
        Calendar now = now();

        Log.v(TAG, "                Now is " + now.getTime() + ". In ms " + now.getTimeInMillis() + ".");

        Calendar cal = calcNextClockChange(now);

        Log.v(TAG, "Register next event at " + cal.getTime() + ". In ms " + cal.getTimeInMillis() + ".");

        long delta = cal.getTimeInMillis() - now.getTimeInMillis();
        Log.v(TAG, "                                                        Delta is " + String.format("%13d", delta));

        return postDelayed(runnableOnClockChange, delta);
    }

    private Calendar now() {
        Clock clock = new SystemClock();
        return clock.now();
    }

    private Calendar calcNextClockChange(Calendar now) {
        Calendar cal = (Calendar) now.clone();

        switch (unit) {
            case Calendar.YEAR:
                cal.add(Calendar.YEAR, 1);
                cal.set(Calendar.MONTH, 0);
                cal.set(Calendar.WEEK_OF_YEAR, 0);
                cal.set(Calendar.DATE, 0);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                break;
            case Calendar.MONTH:
                cal.add(Calendar.MONTH, 1);
                cal.set(Calendar.WEEK_OF_YEAR, 0);
                cal.set(Calendar.DATE, 0);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                break;
            case Calendar.WEEK_OF_YEAR:
                cal.add(Calendar.WEEK_OF_YEAR, 1);
                cal.set(Calendar.DATE, 0);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                break;
            case Calendar.DATE:
                cal.add(Calendar.DATE, 1);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                break;
            case Calendar.HOUR_OF_DAY:
                cal.add(Calendar.HOUR_OF_DAY, 1);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                break;
            case Calendar.MINUTE:
                cal.add(Calendar.MINUTE, 1);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                break;
            case Calendar.SECOND:
                cal.add(Calendar.SECOND, 1);
                cal.set(Calendar.MILLISECOND, 0);
                break;
        }
        return cal;
    }

    private void runRunnable() {
        if (runnable != null) {
            runnable.run();
        }
    }

}
