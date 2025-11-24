package com.example.myapplication;

import android.content.Intent;
import android.widget.RemoteViewsService;

public class ReminderWidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new ReminderWidgetFactory(this.getApplicationContext(), intent);
    }
}

