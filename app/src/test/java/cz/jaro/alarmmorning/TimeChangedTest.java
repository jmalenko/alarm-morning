package cz.jaro.alarmmorning;

import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlarmManager;

import java.util.Calendar;
import java.util.GregorianCalendar;

import cz.jaro.alarmmorning.app.CalendarWithDayAlarmTest;
import cz.jaro.alarmmorning.clock.FixedClock;
import cz.jaro.alarmmorning.model.AlarmDataSource;
import cz.jaro.alarmmorning.model.DayTest;
import cz.jaro.alarmmorning.model.Defaults;
import cz.jaro.alarmmorning.receivers.AlarmReceiver;
import cz.jaro.alarmmorning.receivers.TimeChangedReceiver;
import cz.jaro.alarmmorning.shadows.ShadowGlobalManager;
import cz.jaro.alarmmorning.wizard.Wizard;

import static cz.jaro.alarmmorning.Analytics.Channel.Test;
import static cz.jaro.alarmmorning.model.DayTest.DAY;
import static cz.jaro.alarmmorning.model.DayTest.MINUTE;
import static cz.jaro.alarmmorning.model.DayTest.MONTH;
import static cz.jaro.alarmmorning.model.DayTest.YEAR;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * Tests of time and time zone changes.
 * <p>
 * Tests are performed by changing time; the time zone is not changed.
 * <p>
 * Tests only system alarm; other features are assumed to be covered in {@link CalendarWithDayAlarmTest}.
 */
@Config(shadows = {ShadowGlobalManager.class})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TimeChangedTest extends FixedTimeTest {

    private Context context;

    private AlarmManager alarmManager;
    private ShadowAlarmManager shadowAlarmManager;

    @Before
    public void before() {
        super.before();

        AlarmMorningActivityTest.saveWizardPreference(!Wizard.PREF_WIZARD_DEFAULT);

        context = RuntimeEnvironment.application.getApplicationContext();

        alarmManager = (AlarmManager) RuntimeEnvironment.application.getSystemService(Context.ALARM_SERVICE);
        shadowAlarmManager = Shadows.shadowOf(alarmManager);

        // Consume the alarm with action ACTION_SET_SYSTEM_ALARM
        assertThat(shadowAlarmManager.getScheduledAlarms().size(), is(1));
        BootReceiverTest.consumeSystemAlarm(shadowAlarmManager);

        CalendarWithDayAlarmTest.setNearFuturePeriodPreferenceToZero(context);

        setDefaultAlarms();
    }

    private void setDefaultAlarms() {
        for (int dayOfWeek : AlarmDataSource.allDaysOfWeek) {
            Defaults defaults = new Defaults();
            defaults.setDayOfWeek(dayOfWeek);
            defaults.setState(Defaults.STATE_ENABLED);
            defaults.setHour(DayTest.HOUR_DEFAULT - 5 + (dayOfWeek + (dayOfWeek == Calendar.SUNDAY ? 7 : 0)));
            defaults.setMinute(DayTest.MINUTE_DEFAULT - 1 + (dayOfWeek + (dayOfWeek == Calendar.SUNDAY ? 7 : 0))); // Mnemotechnic hint: hours on monday = 4, minutes = day of week

            Analytics analytics = new Analytics(Test, Analytics.ChannelName.Defaults);

            globalManager.modifyDefault(defaults, analytics);
        }
    }

    @Test
    public void t00_prerequisities() {
        // Current time
        assertEquals(YEAR, 2016);
        assertEquals(MONTH, Calendar.FEBRUARY);
        assertEquals(DAY, 1);
        assertEquals(DayTest.HOUR, 1);
        assertEquals(MINUTE, 0);

        // Check system alarm
        assertAndConsumeSystemAlarm(YEAR, MONTH, DAY, 4, 1, SystemAlarm.ACTION_RING);
    }

    @Test
    public void t10_changeTimeBy1Hour() {
        // Set clock
        shadowGlobalManager.setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, DayTest.HOUR + 1, MINUTE)));

        // Call the receiver
        Intent intent = new Intent();
        TimeChangedReceiver timeChangedReceiver = new TimeChangedReceiver();
        timeChangedReceiver.onReceive(context, intent);

        // Check system alarm
        assertAndConsumeSystemAlarm(YEAR, MONTH, DAY, 4, 1, SystemAlarm.ACTION_RING);
    }

    @Test
    public void t11_changeTimeJustBeforePreviousAlarm() {
        // Set clock
        shadowGlobalManager.setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY - 1, 10, 6)));

        // Call the receiver
        Intent intent = new Intent();
        TimeChangedReceiver timeChangedReceiver = new TimeChangedReceiver();
        timeChangedReceiver.onReceive(context, intent);

        // Check system alarm
        assertAndConsumeSystemAlarm(YEAR, MONTH - 1, 31, 10, 7, SystemAlarm.ACTION_RING);
    }

    @Test
    public void t12_changeTimeJustAfterPreviousAlarm() {
        // Set clock
        shadowGlobalManager.setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY - 1, 10, 8)));

        // Call the receiver
        Intent intent = new Intent();
        TimeChangedReceiver timeChangedReceiver = new TimeChangedReceiver();
        timeChangedReceiver.onReceive(context, intent);

        // Check system alarm
        assertAndConsumeSystemAlarm(YEAR, MONTH, DAY, 4, 1, SystemAlarm.ACTION_RING);
    }

    @Test
    public void t13_changeTimeJustBeforeNextAlarm() {
        // Set clock
        shadowGlobalManager.setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, 4, 0)));

        // Call the receiver
        Intent intent = new Intent();
        TimeChangedReceiver timeChangedReceiver = new TimeChangedReceiver();
        timeChangedReceiver.onReceive(context, intent);

        // Check system alarm
        assertAndConsumeSystemAlarm(YEAR, MONTH, DAY, 4, 1, SystemAlarm.ACTION_RING);
    }

    @Test
    public void t14_changeTimeJustAfterNextAlarm() {
        // Set clock
        shadowGlobalManager.setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, 4, 2)));

        // Call the receiver
        Intent intent = new Intent();
        TimeChangedReceiver timeChangedReceiver = new TimeChangedReceiver();
        timeChangedReceiver.onReceive(context, intent);

        // Check system alarm
        assertAndConsumeSystemAlarm(YEAR, MONTH, DAY + 1, 5, 2, SystemAlarm.ACTION_RING);
    }

    @Test
    public void t21_changeTimeJustBefore2PreviousAlarm() {
        // Set clock
        shadowGlobalManager.setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY - 2, 9, 5)));

        // Call the receiver
        Intent intent = new Intent();
        TimeChangedReceiver timeChangedReceiver = new TimeChangedReceiver();
        timeChangedReceiver.onReceive(context, intent);

        // Check system alarm
        assertAndConsumeSystemAlarm(YEAR, MONTH - 1, 30, 9, 6, SystemAlarm.ACTION_RING);
    }

    @Test
    public void t22_changeTimeJustAfter2NextAlarm() {
        // Set clock
        shadowGlobalManager.setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY + 1, 5, 3)));

        // Call the receiver
        Intent intent = new Intent();
        TimeChangedReceiver timeChangedReceiver = new TimeChangedReceiver();
        timeChangedReceiver.onReceive(context, intent);

        // Check system alarm
        assertAndConsumeSystemAlarm(YEAR, MONTH, DAY + 2, 6, 3, SystemAlarm.ACTION_RING);
    }

    private void assertAndConsumeSystemAlarm(int year, int month, int day, int hour, int minute, String action) {
        BootReceiverTest.assertSystemAlarm(context, shadowAlarmManager, year, month, day, hour, minute, AlarmReceiver.class, action);
        BootReceiverTest.consumeSystemAlarm(shadowAlarmManager);
    }

}
