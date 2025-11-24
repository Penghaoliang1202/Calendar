package com.example.myapplication;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.widget.RemoteViews;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReminderWidgetProvider extends AppWidgetProvider {
    private static final String TAG = "ReminderWidgetProvider";
    private static final String ACTION_ADD_REMINDER = "com.example.myapplication.ACTION_ADD_REMINDER";
    private static final String ACTION_OPEN_APP = "com.example.myapplication.ACTION_OPEN_APP";
    private static final String ACTION_CLICK_REMINDER = "com.example.myapplication.ACTION_CLICK_REMINDER";
    private static final String EXTRA_REMINDER_ID = "reminder_id";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        
        String action = intent.getAction();
        if (ACTION_ADD_REMINDER.equals(action)) {
            // Open quick add reminder activity
            Intent addIntent = new Intent(context, QuickAddReminderActivity.class);
            addIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(addIntent);
        } else if (ACTION_OPEN_APP.equals(action)) {
            // Open main app
            Intent mainIntent = new Intent(context, MainActivity.class);
            mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(mainIntent);
        } else if (ACTION_CLICK_REMINDER.equals(action)) {
            // Open reminders list activity directly
            Intent remindersIntent = new Intent(context, RemindersListActivity.class);
            remindersIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(remindersIntent);
        } else if (AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(action)) {
            // Update all widgets
            int[] appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
            if (appWidgetIds != null) {
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                for (int appWidgetId : appWidgetIds) {
                    updateAppWidget(context, appWidgetManager, appWidgetId);
                }
            }
        }
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_reminder);
        
        ReminderManager reminderManager = new ReminderManager(context);
        List<Reminder> activeReminders = reminderManager.getActiveReminders();
        
        // Limit display to maximum 5 reminders
        int maxReminders = Math.min(activeReminders.size(), 5);
        
        if (maxReminders == 0) {
            // Show empty state
            views.setViewVisibility(R.id.emptyText, android.view.View.VISIBLE);
            views.setViewVisibility(R.id.remindersList, android.view.View.GONE);
        } else {
            views.setViewVisibility(R.id.emptyText, android.view.View.GONE);
            views.setViewVisibility(R.id.remindersList, android.view.View.VISIBLE);
            
            // Remove all child views
            views.removeAllViews(R.id.remindersList);
            
            // Add reminder items
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat displayDateFormat = new SimpleDateFormat("MM/dd", Locale.getDefault());
            String today = dateFormat.format(new Date());
            
            for (int i = 0; i < maxReminders; i++) {
                Reminder reminder = activeReminders.get(i);
                if (reminder == null) continue;
                
                RemoteViews itemViews = new RemoteViews(context.getPackageName(), R.layout.widget_reminder_item);
                
                // Set title
                String title = reminder.getTitle();
                if (TextUtils.isEmpty(title)) {
                    title = reminder.getContent();
                }
                if (TextUtils.isEmpty(title)) {
                    title = context.getString(R.string.no_title);
                }
                itemViews.setTextViewText(R.id.itemTitle, title);
                
                // Set date
                String dateStr = reminder.getDate();
                String displayDate = dateStr;
                try {
                    Date date = dateFormat.parse(dateStr);
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
                itemViews.setTextViewText(R.id.itemDate, displayDate);
                
                // Set time
                String timeStr = "";
                if (!TextUtils.isEmpty(reminder.getStartTime())) {
                    timeStr = reminder.getStartTime();
                    if (!TextUtils.isEmpty(reminder.getEndTime())) {
                        timeStr += " - " + reminder.getEndTime();
                    }
                }
                itemViews.setTextViewText(R.id.itemTime, timeStr);
                
                // Set click event - open reminders list activity
                // Use a unique request code for each reminder to ensure different PendingIntents
                Intent clickIntent = new Intent(context, ReminderWidgetProvider.class);
                clickIntent.setAction(ACTION_CLICK_REMINDER);
                clickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                clickIntent.putExtra(EXTRA_REMINDER_ID, reminder.getId());
                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    reminder.getId().hashCode() + i, // Use index to ensure uniqueness
                    clickIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );
                // Set click on all text views to make the entire item clickable
                itemViews.setOnClickPendingIntent(R.id.itemTitle, pendingIntent);
                itemViews.setOnClickPendingIntent(R.id.itemDate, pendingIntent);
                itemViews.setOnClickPendingIntent(R.id.itemTime, pendingIntent);
                
                views.addView(R.id.remindersList, itemViews);
            }
        }
        
        // Set click event for add reminder button
        Intent addIntent = new Intent(context, ReminderWidgetProvider.class);
        addIntent.setAction(ACTION_ADD_REMINDER);
        addIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        PendingIntent addPendingIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId,
            addIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        views.setOnClickPendingIntent(R.id.addReminderButton, addPendingIntent);
        
        // Set click event for title - open app
        Intent titleIntent = new Intent(context, ReminderWidgetProvider.class);
        titleIntent.setAction(ACTION_OPEN_APP);
        titleIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        PendingIntent titlePendingIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId + 1000,
            titleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        views.setOnClickPendingIntent(R.id.widgetTitle, titlePendingIntent);
        
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}

