package cz.jaro.alarmmorning;

import android.app.Activity;
import android.text.format.DateFormat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Created by jmalenko on 17.12.2015.
 */
public class Localization {

    public static String dayOfWeekToString(int dayOfWeek) {
        Calendar date = new GregorianCalendar();
        date.set(Calendar.DAY_OF_WEEK, dayOfWeek);
        SimpleDateFormat sdf = new SimpleDateFormat("E");
        sdf.setCalendar(date);
        return sdf.format(date.getTime());
    }

    public static String timeToString(int hours, int minutes, Activity activity) {
        Calendar date = new GregorianCalendar();
        date.set(Calendar.HOUR_OF_DAY, hours);
        date.set(Calendar.MINUTE, minutes);
        return timeToString(date.getTime(), activity);
    }

    public static String timeToString(Date date, Activity activity) {
        java.text.DateFormat dateFormat = DateFormat.getTimeFormat(activity);
        return dateFormat.format(date);
    }

    public static String dateToString(Date date) {
        SimpleDateFormat dateFormat = (SimpleDateFormat) java.text.DateFormat.getDateInstance(java.text.DateFormat.SHORT);
        // Trick: use regex to trim off all y's and any non-alphabetic characters before and after
        dateFormat.applyPattern(dateFormat.toPattern().replaceAll("[^\\p{Alpha}]*y+[^\\p{Alpha}]*", ""));
        return dateFormat.format(date);
    }

}
