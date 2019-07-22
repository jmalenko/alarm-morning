package cz.jaro.alarmmorning.checkalarmtime;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowNotificationManager;

import java.util.Calendar;
import java.util.GregorianCalendar;

import cz.jaro.alarmmorning.FixedTimeTest;
import cz.jaro.alarmmorning.app.CalendarWithDayAlarmTest;
import cz.jaro.alarmmorning.calendar.CalendarEvent;
import cz.jaro.alarmmorning.clock.FixedClock;

import static cz.jaro.alarmmorning.app.AlarmMorningAppTest.assertNotification;
import static cz.jaro.alarmmorning.app.AlarmMorningAppTest.assertNotificationActionCount;
import static cz.jaro.alarmmorning.model.DayTest.DAY;
import static cz.jaro.alarmmorning.model.DayTest.HOUR_DEFAULT;
import static cz.jaro.alarmmorning.model.DayTest.MINUTE_DEFAULT;
import static cz.jaro.alarmmorning.model.DayTest.MONTH;
import static cz.jaro.alarmmorning.model.DayTest.YEAR;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;


/**
 * Tests of "check alarm time in the evening".
 */
@RunWith(RobolectricTestRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CheckAlarmTimeTest extends FixedTimeTest {

    private Context context;

    private NotificationManager notificationManager;
    private ShadowNotificationManager shadowNotificationManager;

    private CheckAlarmTime checkAlarmTime;

    @Before
    public void before() {
        super.before();

        context = RuntimeEnvironment.application.getApplicationContext();

        checkAlarmTime = spy(CheckAlarmTime.getInstance(context));

        notificationManager = (NotificationManager) RuntimeEnvironment.application.getSystemService(Context.NOTIFICATION_SERVICE);
        shadowNotificationManager = Shadows.shadowOf(notificationManager);
    }

    @Test
    public void t001_checkMock_MorningInfo() {
        // Mock calendar event
        CalendarEvent meeting = new CalendarEvent();
        meeting.setBegin(new GregorianCalendar(YEAR, MONTH, DAY + 1, HOUR_DEFAULT, MINUTE_DEFAULT));
        meeting.setTitle("Dentist");

        MorningInfo morningInfo = spy(MorningInfo.class);
        when(morningInfo.getEarliestEvent(anyObject(), anyObject())).thenReturn(meeting);
        morningInfo.setContext(context);

        // Run the business logic
        morningInfo.init();

        // Check
        assertNotNull(morningInfo.event);
    }

    @Test
    public void t101_checkAlarmTime_withAlarm_meetingDoesNotTriggerNotification() {
        // Mock calendar event
        CalendarEvent meeting = new CalendarEvent();
        meeting.setBegin(new GregorianCalendar(YEAR, MONTH, DAY + 1, HOUR_DEFAULT + 1, MINUTE_DEFAULT));
        meeting.setTitle("Dentist");

        MorningInfo morningInfo = spy(MorningInfo.class);
        when(morningInfo.getEarliestEvent(anyObject(), anyObject())).thenReturn(meeting);
        morningInfo.setContext(RuntimeEnvironment.application.getApplicationContext());

        // Set alarm
        setAlarm(new GregorianCalendar(YEAR, MONTH, DAY + 1, HOUR_DEFAULT, MINUTE_DEFAULT));

        // Do the action
        doCheckInTheEvening(morningInfo);

        // Check notification
        assertNotificationCount(0);
    }

    @Test
    public void t102_checkAlarmTime_withAlarm_meetingTriggersNotification() {
        // Mock calendar event
        CalendarEvent meeting = new CalendarEvent();
        meeting.setBegin(new GregorianCalendar(YEAR, MONTH, DAY + 1, HOUR_DEFAULT + 1, MINUTE_DEFAULT));
        meeting.setTitle("Dentist");

        MorningInfo morningInfo = spy(MorningInfo.class);
        when(morningInfo.getEarliestEvent(anyObject(), anyObject())).thenReturn(meeting);
        morningInfo.setContext(RuntimeEnvironment.application.getApplicationContext());

        // Set alarm
        setAlarm(new GregorianCalendar(YEAR, MONTH, DAY + 1, HOUR_DEFAULT, MINUTE_DEFAULT + 1));

        // Do the action
        doCheckInTheEvening(morningInfo);

        // Check notification
        assertNotificationCount(1);

        Notification notification = shadowNotificationManager.getAllNotifications().get(0);

        assertNotification(notification, "Alarm clock currently at 7:01 AM", "First appointment: 8:00 AM Dentist");
        assertNotificationActionCount(notification, 2);
        assertNotificationAction(notification, 0, "Set to 7:00 AM", CheckAlarmTimeNotificationReceiver.ACTION_CHECK_ALARM_TIME_SET_TO);
        assertNotificationAction(notification, 1, "Set…", CheckAlarmTimeNotificationReceiver.ACTION_CHECK_ALARM_TIME_SET_DIALOG);
    }

    @Test
    public void t103_checkAlarmTime_noAlarm_meetingTriggersNotification() {
        // Mock calendar event
        CalendarEvent meeting = new CalendarEvent();
        meeting.setBegin(new GregorianCalendar(YEAR, MONTH, DAY + 1, HOUR_DEFAULT + 1, MINUTE_DEFAULT));
        meeting.setTitle("Dentist");

        MorningInfo morningInfo = spy(MorningInfo.class);
        when(morningInfo.getEarliestEvent(anyObject(), anyObject())).thenReturn(meeting);
        morningInfo.setContext(RuntimeEnvironment.application.getApplicationContext());

        doCheckInTheEvening(morningInfo);

        // Check notification
        assertNotificationCount(1);

        Notification notification = shadowNotificationManager.getAllNotifications().get(0);

        assertNotification(notification, "Alarm clock currently disabled", "First appointment: 8:00 AM Dentist");
        assertNotificationActionCount(notification, 2);
        assertNotificationAction(notification, 0, "Set to 7:00 AM", CheckAlarmTimeNotificationReceiver.ACTION_CHECK_ALARM_TIME_SET_TO);
        assertNotificationAction(notification, 1, "Set…", CheckAlarmTimeNotificationReceiver.ACTION_CHECK_ALARM_TIME_SET_DIALOG);
    }

    @Test
    public void t104_checkAlarmTime_noMeeting() {
        // Mock calendar event
        CalendarEvent meeting = null;

        MorningInfo morningInfo = spy(MorningInfo.class);
        when(morningInfo.getEarliestEvent(anyObject(), anyObject())).thenReturn(meeting);
        morningInfo.setContext(RuntimeEnvironment.application.getApplicationContext());

        doCheckInTheEvening(morningInfo);

        // Check notification
        assertNotificationCount(0);
    }

    private void doCheckInTheEvening(MorningInfo morningInfo) {
        // Shift clock
        shadowGlobalManager.setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, 22, MINUTE_DEFAULT)));

        // Do the business logic
        morningInfo.init();

        // Trigger the check
        checkAlarmTime.onCheckAlarmTime(morningInfo);
    }

    private void setAlarm(Calendar cal) {
        CalendarWithDayAlarmTest.setAlarm(cal, globalManager);
    }

    private void assertNotificationCount(int count) {
        CalendarWithDayAlarmTest.assertNotificationCount(shadowNotificationManager, count);
    }

    private void assertNotificationAction(Notification notification, int index, String title, String actionString) {
        CalendarWithDayAlarmTest.assertNotificationAction(context, notification, index, title, actionString, CheckAlarmTimeNotificationReceiver.class);
    }

}