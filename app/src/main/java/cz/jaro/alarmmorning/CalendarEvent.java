package cz.jaro.alarmmorning;

import android.database.Cursor;

import java.util.Calendar;

import static cz.jaro.alarmmorning.CalendarHelper.PROJECTION_ALL_DAY_INDEX;
import static cz.jaro.alarmmorning.CalendarHelper.PROJECTION_BEGIN_INDEX;
import static cz.jaro.alarmmorning.CalendarHelper.PROJECTION_ID_INDEX;
import static cz.jaro.alarmmorning.CalendarHelper.PROJECTION_LOCATION_INDEX;
import static cz.jaro.alarmmorning.CalendarHelper.PROJECTION_TITLE_INDEX;

/**
 * This class represents a calendar event.
 */
public class CalendarEvent {

    public long eventID;
    public Calendar begin;
    public String title;
    public String location;
    public int allDay;

    CalendarEvent() {
    }

    static CalendarEvent load(Cursor cur) {
        CalendarEvent event = new CalendarEvent();

        event.eventID = cur.getLong(PROJECTION_ID_INDEX);

        long beginMS = cur.getLong(PROJECTION_BEGIN_INDEX);
        event.begin = Calendar.getInstance();
        event.begin.setTimeInMillis(beginMS);

        event.title = cur.getString(PROJECTION_TITLE_INDEX);

        event.location = cur.getString(PROJECTION_LOCATION_INDEX);

        event.allDay = cur.getInt(PROJECTION_ALL_DAY_INDEX);

        return event;
    }

}
