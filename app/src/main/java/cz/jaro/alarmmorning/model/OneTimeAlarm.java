package cz.jaro.alarmmorning.model;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import cz.jaro.alarmmorning.Analytics;
import cz.jaro.alarmmorning.calendar.CalendarUtils;

/**
 * Represents a one-time alarm, specifically a time on a particular date.
 * <p/>
 * The alarm time is internally represented in UTC time zone in the {@link #alarmTime} field. As the main feature of the app is that the alarm rings with
 * respect to the default time zone, most methods (those that don't contain UTC in the name) operate with default time zone.
 * <p/>
 * Example:
 * <br/>
 * The {@link #alarmTime} field contains value "1516690800000" (milliseconds since epoch).
 * <br/>
 * The method {@link #getDateTimeUTC()} return s the value "2017-01-23 7:00 UTC".
 * <br/>
 * As the user is located in Germany, the default time zone is CET (Central European Time) and the method {@link #getDateTime()} returns the value
 * "2017-01-23 7:00 CET".
 */
public class OneTimeAlarm extends AppAlarm {

    /**
     * Locale id in which the alarm time is stored as milliseconds since epoch.
     */
    public static final String UTC = "UTC";

    private long id;

    private long alarmTime;

    private String name;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getAlarmTime() {
        return alarmTime;
    }

    public void setAlarmTime(long alarmTime) {
        this.alarmTime = alarmTime;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setAlarmTime(Calendar alarmTime) {
        setAlarmTime(alarmTime.getTimeInMillis());
    }

    /**
     * Return the alarm time in UTC time zone.
     *
     * @return Alarm time in UTC time zone.
     */
    private Calendar getDateTimeUTC() {
        TimeZone utcTZ = TimeZone.getTimeZone(UTC);
        Calendar dateTimeUTC = Calendar.getInstance(utcTZ);
        dateTimeUTC.setTimeInMillis(alarmTime);
        return dateTimeUTC;
    }

    /**
     * Return the alarm time (in default time zone).
     *
     * @return Alarm time (in default time zone).
     */
    public Calendar getDateTime() {
        Calendar dateTimeUTC = getDateTimeUTC();

        Calendar dateTime = new GregorianCalendar();
        CalendarUtils.copyAllFields(dateTimeUTC, dateTime);

        return dateTime;
    }

    public Calendar getDate() {
        Calendar dateTimeUTC = getDateTimeUTC();
        return dateTimeUTC;
    }

    public void setDate(Calendar date) {
        Calendar dateTimeUTC = getDateTimeUTC();
        dateTimeUTC.set(Calendar.YEAR, date.get(Calendar.YEAR));
        dateTimeUTC.set(Calendar.MONTH, date.get(Calendar.MONTH));
        dateTimeUTC.set(Calendar.DATE, date.get(Calendar.DATE));
        setAlarmTime(dateTimeUTC);
    }

    public int getHour() {
        Calendar dateTimeUTC = getDateTimeUTC();
        return dateTimeUTC.get(Calendar.HOUR_OF_DAY);
    }

    public void setHour(int hour) {
        Calendar dateTimeUTC = getDateTimeUTC();
        dateTimeUTC.set(Calendar.HOUR_OF_DAY, hour);
        setAlarmTime(dateTimeUTC);
    }

    public int getMinute() {
        Calendar dateTimeUTC = getDateTimeUTC();
        return dateTimeUTC.get(Calendar.MINUTE);
    }

    public void setMinute(int minute) {
        Calendar dateTimeUTC = getDateTimeUTC();
        dateTimeUTC.set(Calendar.MINUTE, minute);
        setAlarmTime(dateTimeUTC);
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        Calendar dateTime = getDateTime();

        str.append(Analytics.calendarToDate(dateTime));
        str.append(" at ");
        str.append(Analytics.calendarToTime(dateTime));

        if (name != null) {
            str.append(" ");
            str.append(name);
        }

        return str.toString();
    }

}