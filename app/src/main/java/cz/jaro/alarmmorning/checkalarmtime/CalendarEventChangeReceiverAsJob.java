package cz.jaro.alarmmorning.checkalarmtime;

import android.annotation.TargetApi;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.provider.CalendarContract;
import android.util.Log;

import cz.jaro.alarmmorning.GlobalManager;

@TargetApi(Build.VERSION_CODES.N)
public class CalendarEventChangeReceiverAsJob extends JobService {

    private static final String TAG = GlobalManager.createLogTag(CalendarEventChangeReceiverAsJob.class);

    private static final int JOB_ID = 100;

    static void schedule(Context context) {
        Log.v(TAG, "schedule()");

        final Uri CALENDAR_URI = Uri.parse("content://" + CalendarContract.AUTHORITY + "/");

        ComponentName oComponentName = new ComponentName(context, CalendarEventChangeReceiverAsJob.class);
        JobInfo.Builder jobInfoBuilder = new JobInfo.Builder(JOB_ID, oComponentName);
        jobInfoBuilder.addTriggerContentUri(new JobInfo.TriggerContentUri(CalendarContract.CONTENT_URI, JobInfo.TriggerContentUri.FLAG_NOTIFY_FOR_DESCENDANTS));
        jobInfoBuilder.addTriggerContentUri(new JobInfo.TriggerContentUri(CALENDAR_URI, 0));
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        jobScheduler.schedule(jobInfoBuilder.build());
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        Log.v(TAG, "onStartJob(...)");

        // Reschedule to receive future changes
        schedule(this);

        // Do check
        CheckAlarmTime checkAlarmTime = CheckAlarmTime.getInstance(this);
        checkAlarmTime.onCalendarUpdated();

        return false;
    }

    @Override
    synchronized public boolean onStopJob(JobParameters params) {
        Log.v(TAG, "onStopJob(...)");
        return false;
    }
}
