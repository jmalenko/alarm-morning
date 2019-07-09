package cz.jaro.alarmmorning;


import android.os.Handler;
import android.util.Log;

import java.util.Calendar;

import cz.jaro.alarmmorning.clock.Clock;
import cz.jaro.alarmmorning.clock.SystemClock;

import static cz.jaro.alarmmorning.calendar.CalendarUtils.roundDown;
import static cz.jaro.alarmmorning.calendar.CalendarUtils.subField;

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

    private static final String TAG = GlobalManager.createLogTag(HandlerOnClockChange.class);

    private Integer unit;

    private Runnable runnable;

    private final Runnable runnableOnClockChange = () -> {
        Log.d(TAG, "Clock (and time unit) changed");

        registerNextClockChange();

        runRunnable();
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
        this(null, unit);
    }

    /**
     * Constructor that sets the Runnable.
     *
     * @param runnable Runnable
     */
    public HandlerOnClockChange(Runnable runnable) {
        this(runnable, Calendar.SECOND);
    }

    /**
     * Constructor that sets the Unit and Runnable.
     *
     * @param runnable Runnable
     * @param unit     Clock unit identifier
     */
    public HandlerOnClockChange(Runnable runnable, int unit) {
        super();
        setRunnable(runnable);
        setUnit(unit);
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
        Log.v(TAG, "                                               Delta is ms " + String.format("%13d", delta));

        return postDelayed(runnableOnClockChange, delta);
    }

    private Calendar now() {
        Clock clock = new SystemClock();
        return clock.now();
    }

    private Calendar calcNextClockChange(Calendar now) {
        Calendar cal = (Calendar) now.clone();

        cal.add(unit, 1);
        roundDown(cal, subField(unit));

        return cal;
    }

    private void runRunnable() {
        if (runnable != null) {
            runnable.run();
        }
    }

}
