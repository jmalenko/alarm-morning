package cz.jaro.alarmmorning;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowAlarmManager;
import org.robolectric.shadows.ShadowPendingIntent;

import java.util.Calendar;

import cz.jaro.alarmmorning.clock.Clock;
import cz.jaro.alarmmorning.model.AlarmDataSource;
import cz.jaro.alarmmorning.model.GlobalManager1NextAlarm0NoAlarmTest;
import cz.jaro.alarmmorning.receivers.AlarmReceiver;
import cz.jaro.alarmmorning.shadows.ShadowAlarmManagerAPI21;
import cz.jaro.alarmmorning.shadows.ShadowGlobalManager;

import static cz.jaro.alarmmorning.model.DayTest.DAY;
import static cz.jaro.alarmmorning.model.DayTest.MONTH;
import static cz.jaro.alarmmorning.model.DayTest.YEAR;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test "when there is no alarm..."
 */
@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21, shadows = {ShadowAlarmManagerAPI21.class, ShadowGlobalManager.class})
public class AlarmMorningActivity0Test {

    private GlobalManager globalManager;

    @Before
    public void before() {
        globalManager = GlobalManager.getInstance();
        globalManager.reset();
    }

    @After
    public void after() {
        GlobalManager1NextAlarm0NoAlarmTest.resetSingleton(GlobalManager.class, "instance");
    }

    @Test
    public void clockSetToFixedTime() {
        Clock clock = globalManager.clock();
        Calendar now = clock.now();

        assertThat(now.get(Calendar.YEAR)).isEqualTo(YEAR);
        assertThat(now.get(Calendar.MONTH)).isEqualTo(MONTH);
        assertThat(now.get(Calendar.DAY_OF_MONTH)).isEqualTo(DAY);
    }

    @Test
    public void noAlarmInDefaults() {
        DefaultsActivity activity = Robolectric.setupActivity(DefaultsActivity.class);
        ShadowActivity shadowActivity = Shadows.shadowOf(activity);

        RecyclerView recyclerView = (RecyclerView) shadowActivity.findViewById(R.id.defaults_recycler_view);

        // Hack: RecyclerView needs to be measured and layed out manually in Robolectric.
        // Source: http://stackoverflow.com/questions/27052866/android-robolectric-click-recyclerview-item
        recyclerView.measure(0, 0);
        recyclerView.layout(0, 0, 100, 10000);

        assertThat(recyclerView.getChildCount()).isEqualTo(AlarmDataSource.allDaysOfWeek.length);

        for (int position = 0; position < recyclerView.getChildCount(); position++) {
            View item = recyclerView.getChildAt(position);

            TextView textTime = (TextView) item.findViewById(R.id.textTime);
            assertThat(textTime.getText()).isEqualTo(activity.getResources().getString(R.string.alarm_unset));
        }
    }

    @Test
    public void noAlarm() {
        AlarmMorningActivity activity = Robolectric.setupActivity(AlarmMorningActivity.class);
        ShadowActivity shadowActivity = Shadows.shadowOf(activity);

        Resources res = activity.getResources();

        RecyclerView recyclerView = (RecyclerView) shadowActivity.findViewById(R.id.calendar_recycler_view);

        // Hack: RecyclerView needs to be measured and layed out manually in Robolectric.
        // Source: http://stackoverflow.com/questions/27052866/android-robolectric-click-recyclerview-item
        recyclerView.measure(0, 0);
        recyclerView.layout(0, 0, 100, 10000);

        assertThat(recyclerView.getChildCount()).isEqualTo(GlobalManager.HORIZON_DAYS);

        for (int position = 0; position < recyclerView.getChildCount(); position++) {
            View item = recyclerView.getChildAt(position);

            Clock clock = globalManager.clock();
            Calendar today = CalendarFragment.getToday(clock);
            Calendar date = CalendarFragment.addDays(today, position);

            TextView textDayOfWeekCal = (TextView) item.findViewById(R.id.textDayOfWeekCal);
            int dayOfWeek = date.get(Calendar.DAY_OF_WEEK);
            String dayOfWeekText = Localization.dayOfWeekToStringShort(res, dayOfWeek);
            assertThat(textDayOfWeekCal.getText()).isEqualTo(dayOfWeekText);

            TextView alarmTimeText = (TextView) item.findViewById(R.id.textTimeCal);
            assertThat(alarmTimeText.getText()).isEqualTo(activity.getResources().getString(R.string.alarm_unset));

            TextView textDate = (TextView) item.findViewById(R.id.textDate);
            String dateText = Localization.dateToStringVeryShort(res, date.getTime());
            assertThat(textDate.getText()).isEqualTo(dateText);

            TextView textState = (TextView) item.findViewById(R.id.textState);
            if (position == 0) {
                assertThat(textState.getText().equals("") || textState.getText().equals(res.getString(R.string.alarm_state_passed))).isTrue();
            } else {
                assertThat(textState.getText()).isEmpty();
            }

            TextView textComment = (TextView) item.findViewById(R.id.textComment);
            assertThat(textComment.getText()).isEmpty();
        }
    }

    @Test
    public void noAlarmCheckNextSystemAlarm() {
        Context context = RuntimeEnvironment.application.getApplicationContext();

        AlarmManager alarmManager = (AlarmManager) RuntimeEnvironment.application.getSystemService(Context.ALARM_SERVICE);
        ShadowAlarmManager shadowAlarmManager = Shadows.shadowOf(alarmManager);

        ShadowAlarmManager.ScheduledAlarm scheduledAlarm = shadowAlarmManager.getNextScheduledAlarm();

        assertThat(scheduledAlarm).isNotNull();

        // time
        Clock clock = globalManager.clock();
        Calendar now = clock.now();
        Calendar expectedResetTime = SystemAlarm.getResetTime(now);

        assertThat(scheduledAlarm.triggerAtTime).isEqualTo(expectedResetTime.getTimeInMillis());

        // action
        PendingIntent operation = scheduledAlarm.operation;
        ShadowPendingIntent shadowOperation = Shadows.shadowOf(operation);

        assertThat(shadowOperation.isBroadcastIntent()).isEqualTo(true);
        assertThat(shadowOperation.getSavedIntents().length).isEqualTo(1);

        Intent intent = shadowOperation.getSavedIntents()[0];

        assertThat(intent.getAction()).isEqualTo(SystemAlarm.ACTION_SET_SYSTEM_ALARM);

        Intent expectedIntent = new Intent(context, AlarmReceiver.class);
        assertThat(intent.getComponent()).isEqualTo(expectedIntent.getComponent());

        // type
        assertThat(scheduledAlarm.type).isEqualTo(AlarmManager.RTC_WAKEUP);
    }
}
