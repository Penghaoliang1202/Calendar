package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReminderWidgetFactory implements RemoteViewsService.RemoteViewsFactory {
    private final Context context;
    private List<Reminder> reminders;
    private final SimpleDateFormat dateFormat;
    private final SimpleDateFormat displayDateFormat;

    public ReminderWidgetFactory(Context context, @SuppressWarnings("unused") Intent intent) {
        this.context = context;
        this.reminders = new ArrayList<>();
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        this.displayDateFormat = new SimpleDateFormat("MM/dd", Locale.getDefault());
    }

    @Override
    public void onCreate() {
        // Data source initialization is done in constructor
    }

    @Override
    public void onDataSetChanged() {
        // Load reminders from ReminderManager
        ReminderManager reminderManager = new ReminderManager(context);
        reminders = reminderManager.getActiveReminders();
    }

    @Override
    public void onDestroy() {
        if (reminders != null) {
            reminders.clear();
        }
    }

    @Override
    public int getCount() {
        return reminders.size();
    }

    @Override
    public RemoteViews getViewAt(int position) {
        if (position < 0 || position >= reminders.size()) {
            return null;
        }

        Reminder reminder = reminders.get(position);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_reminder_item);

        // Set title
        String title = reminder.getTitle();
        if (TextUtils.isEmpty(title)) {
            title = reminder.getContent();
        }
        if (TextUtils.isEmpty(title)) {
            title = context.getString(R.string.no_title);
        }
        views.setTextViewText(R.id.itemTitle, title);

        // Set date
        String dateStr = reminder.getDate();
        String displayDate = dateStr;
        try {
            Date date = dateFormat.parse(dateStr);
            String today = dateFormat.format(new Date());
            if (date != null) {
                if (dateStr.equals(today)) {
                    displayDate = context.getString(R.string.today);
                } else {
                    displayDate = displayDateFormat.format(date);
                }
            }
        } catch (Exception e) {
            // Use original date string
        }
        views.setTextViewText(R.id.itemDate, displayDate);

        // Set time
        String timeStr = "";
        if (!TextUtils.isEmpty(reminder.getStartTime())) {
            timeStr = reminder.getStartTime();
            if (!TextUtils.isEmpty(reminder.getEndTime())) {
                timeStr += " - " + reminder.getEndTime();
            }
        }
        views.setTextViewText(R.id.itemTime, timeStr);

        // Set click intent - directly create PendingIntent for each item
        Intent clickIntent = new Intent(context, ReminderWidgetProvider.class);
        clickIntent.setAction(ReminderWidgetProvider.ACTION_CLICK_REMINDER);
        clickIntent.putExtra(ReminderWidgetProvider.EXTRA_REMINDER_ID, reminder.getId());
        
        // Use position and reminder ID hash to create unique request code
        // This ensures each item has a unique PendingIntent
        int requestCode = (position * 1000) + Math.abs(reminder.getId().hashCode() % 1000);
        int flags = android.app.PendingIntent.FLAG_UPDATE_CURRENT;
        // minSdk is 27, so FLAG_IMMUTABLE is always available
        // FLAG_MUTABLE is required for Android 12+ (API 31+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            flags |= android.app.PendingIntent.FLAG_MUTABLE;
        } else {
            flags |= android.app.PendingIntent.FLAG_IMMUTABLE;
        }
        
        android.app.PendingIntent pendingIntent = android.app.PendingIntent.getBroadcast(
            context,
            requestCode,
            clickIntent,
            flags
        );
        
        // Set click listener on the root layout
        views.setOnClickPendingIntent(android.R.id.content, pendingIntent);

        return views;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        if (position >= 0 && position < reminders.size()) {
            return reminders.get(position).getId().hashCode();
        }
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }
}

