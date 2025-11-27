package com.example.myapplication;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public class ReminderWidgetProvider extends AppWidgetProvider {
    private static final String ACTION_ADD_REMINDER = "com.example.myapplication.ACTION_ADD_REMINDER";
    private static final String ACTION_OPEN_APP = "com.example.myapplication.ACTION_OPEN_APP";
    private static final String ACTION_CLICK_REMINDER = "com.example.myapplication.ACTION_CLICK_REMINDER";
    static final String EXTRA_REMINDER_ID = "reminder_id";

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
            // Open reminders list activity to show all reminders
            Intent remindersIntent = new Intent(context, RemindersListActivity.class);
            remindersIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
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
        
        // Set up the RemoteViewsService for scrollable list
        Intent serviceIntent = new Intent(context, ReminderWidgetService.class);
        serviceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        // Use a unique URI for each widget to ensure proper caching
        serviceIntent.setData(android.net.Uri.parse("widget://" + appWidgetId));
        views.setRemoteAdapter(R.id.remindersList, serviceIntent);
        
        // Set empty view
        views.setEmptyView(R.id.remindersList, R.id.emptyText);
        
        // Set click template for list items
        Intent clickIntent = new Intent(context, ReminderWidgetProvider.class);
        clickIntent.setAction(ACTION_CLICK_REMINDER);
        clickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        PendingIntent clickPendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            clickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        views.setPendingIntentTemplate(R.id.remindersList, clickPendingIntent);
        
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
        
        // Notify the RemoteViewsService to refresh the list data
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.remindersList);
        
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}

