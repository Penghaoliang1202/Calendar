package com.example.myapplication;

import android.annotation.SuppressLint;
import android.app.TimePickerDialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import androidx.appcompat.app.AlertDialog;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class RemindersListActivity extends AppCompatActivity {
    private static final String TAG = "RemindersListActivity";
    private RecyclerView remindersRecyclerView;
    private TextView emptyRemindersText;
    private MaterialButton historyButton;
    private ReminderManager reminderManager;
    private ReminderAdapter reminderAdapter;
    private boolean showingHistory = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            EdgeToEdge.enable(this);
            setContentView(R.layout.activity_reminders_list);

            // Handle window insets for Toolbar
            MaterialToolbar toolbar = findViewById(R.id.toolbar);
            if (toolbar != null) {
                ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
                    Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(
                        v.getPaddingStart(),
                        systemBars.top,
                        v.getPaddingEnd(),
                        v.getPaddingBottom()
                    );
                    return insets;
                });
            }
            
            setSupportActionBar(toolbar);
            
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle(getString(R.string.all_reminders));
            }

            remindersRecyclerView = findViewById(R.id.remindersRecyclerView);
            emptyRemindersText = findViewById(R.id.emptyRemindersText);
            historyButton = findViewById(R.id.historyButton);

            reminderManager = new ReminderManager(this);
            reminderAdapter = new ReminderAdapter(new ArrayList<>(), new ReminderAdapter.OnReminderClickListener() {
                @Override
                public void onEditClick(Reminder reminder) {
                    if (reminder != null && !reminder.isCompleted()) {
                        showReminderDialog(reminder);
                    }
                }

                @Override
                public void onDeleteClick(Reminder reminder) {
                    showDeleteConfirmDialog(reminder);
                }

                @Override
                public void onCompleteClick(Reminder reminder) {
                    // Cancel alarm if notification is enabled
                    if (reminder.isEnableNotification()) {
                        AlarmHelper.cancelAlarm(RemindersListActivity.this, reminder);
                    }
                    reminderManager.markAsCompleted(reminder.getId());
                    loadReminders();
                    updateWidgets();
                }

                @Override
                public void onRestoreClick(Reminder reminder) {
                    reminderManager.restoreReminder(reminder.getId());
                    // Restore alarm if notification is enabled
                    Reminder restoredReminder = null;
                    for (Reminder r : reminderManager.getAllReminders()) {
                        if (r != null && reminder.getId().equals(r.getId())) {
                            restoredReminder = r;
                            break;
                        }
                    }
                    if (restoredReminder != null && restoredReminder.isEnableNotification() && !restoredReminder.getStartTime().isEmpty()) {
                        boolean alarmSet = AlarmHelper.setAlarm(RemindersListActivity.this, restoredReminder);
                        if (!alarmSet) {
                            String reminderDateTime = restoredReminder.getDate() + " " + restoredReminder.getStartTime();
                            Toast.makeText(RemindersListActivity.this, getString(R.string.reminder_time_past_detail, reminderDateTime), Toast.LENGTH_LONG).show();
                        }
                    }
                    Toast.makeText(RemindersListActivity.this, getString(R.string.reminder_restored), Toast.LENGTH_SHORT).show();
                    loadReminders();
                    updateWidgets();
                }
            });

            remindersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            remindersRecyclerView.setAdapter(reminderAdapter);

            if (historyButton != null) {
                historyButton.setOnClickListener(v -> toggleHistoryView());
            }

            loadReminders();
            
            // Check if we need to scroll to a specific reminder (from widget click)
            Intent intent = getIntent();
            if (intent != null) {
                String reminderId = intent.getStringExtra(ReminderWidgetProvider.EXTRA_REMINDER_ID);
                if (reminderId != null && !reminderId.isEmpty()) {
                    // Delay scrolling to ensure RecyclerView is fully laid out
                    remindersRecyclerView.postDelayed(() -> scrollToReminder(reminderId), 100);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize RemindersListActivity", e);
            Toast.makeText(this, getString(R.string.app_initialization_failed, e.getMessage()), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("SetTextI18n")
    private void loadReminders() {
        List<Reminder> reminders;
        if (showingHistory) {
            reminders = reminderManager.getCompletedReminders();
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(getString(R.string.history));
            }
            if (historyButton != null) {
                historyButton.setText(getString(R.string.view_all_reminders));
            }
        } else {
            reminders = reminderManager.getActiveReminders();
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(getString(R.string.all_reminders));
            }
            if (historyButton != null) {
                historyButton.setText(getString(R.string.view_history));
            }
        }
        
        reminderAdapter.updateReminders(reminders);
        if (reminders.isEmpty()) {
            emptyRemindersText.setVisibility(View.VISIBLE);
            remindersRecyclerView.setVisibility(View.GONE);
            emptyRemindersText.setText(showingHistory ? getString(R.string.no_history_reminders) : getString(R.string.no_reminders));
        } else {
            emptyRemindersText.setVisibility(View.GONE);
            remindersRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void toggleHistoryView() {
        showingHistory = !showingHistory;
        loadReminders();
    }

    private void showReminderDialog(Reminder reminder) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_reminder, null);
        TextView dialogTitle = dialogView.findViewById(R.id.dialogTitle);
        TextInputEditText titleEdit = dialogView.findViewById(R.id.reminderTitleEdit);
        TextInputEditText contentEdit = dialogView.findViewById(R.id.reminderContentEdit);
        TextInputEditText startTimeEdit = dialogView.findViewById(R.id.startTimeEdit);
        TextInputEditText endTimeEdit = dialogView.findViewById(R.id.endTimeEdit);
        SwitchMaterial notificationSwitch = dialogView.findViewById(R.id.notificationSwitch);
        LinearLayout notificationTimeLayout = dialogView.findViewById(R.id.notificationTimeLayout);
        TextInputEditText notificationTimeEdit = dialogView.findViewById(R.id.notificationTimeEdit);
        TextInputEditText repeatTypeEdit = dialogView.findViewById(R.id.repeatTypeEdit);

        final Reminder finalReminder = reminder;
        
        if (dialogTitle != null) {
            dialogTitle.setText(getString(R.string.edit_reminder));
        }
        
        // Set reminder values
        titleEdit.setText(finalReminder.getTitle());
        contentEdit.setText(finalReminder.getContent());
        startTimeEdit.setText(finalReminder.getStartTime());
        endTimeEdit.setText(finalReminder.getEndTime());
        if (notificationSwitch != null) {
            notificationSwitch.setChecked(finalReminder.isEnableNotification());
        }
        if (notificationTimeEdit != null) {
            notificationTimeEdit.setText(String.valueOf(finalReminder.getNotificationMinutesBefore()));
        }
        if (repeatTypeEdit != null) {
            repeatTypeEdit.setText(getRepeatTypeDisplayName(finalReminder.getRepeatType()));
        }

        // Show/hide notification time layout based on switch state
        if (notificationSwitch != null && notificationTimeLayout != null) {
            notificationTimeLayout.setVisibility(notificationSwitch.isChecked() ? View.VISIBLE : View.GONE);
            notificationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> notificationTimeLayout.setVisibility(isChecked ? View.VISIBLE : View.GONE));
        }

        startTimeEdit.setOnClickListener(v -> showTimePicker(startTimeEdit));
        endTimeEdit.setOnClickListener(v -> showTimePicker(endTimeEdit));

        // Set up notification time picker
        if (notificationTimeEdit != null) {
            notificationTimeEdit.setOnClickListener(v -> showNotificationTimePicker(notificationTimeEdit));
        }

        // Set up repeat type picker
        if (repeatTypeEdit != null) {
            repeatTypeEdit.setOnClickListener(v -> showRepeatTypePicker(repeatTypeEdit));
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.CustomDialogTheme)
                .setView(dialogView)
                .setPositiveButton(getString(R.string.save), null)
                .setNegativeButton(getString(R.string.cancel), null);

        AlertDialog dialog = builder.create();
        
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                (int) (getResources().getDisplayMetrics().widthPixels * 0.9),
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
        dialog.setOnShowListener(dialogInterface -> {
            // Auto focus on title edit and show keyboard
            titleEdit.requestFocus();
            titleEdit.postDelayed(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                try {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null && titleEdit.hasFocus()) {
                        imm.showSoftInput(titleEdit, InputMethodManager.SHOW_IMPLICIT);
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Failed to show keyboard", e);
                }
            }, 100);
            
            android.widget.Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (positiveButton != null) {
                positiveButton.setOnClickListener(v -> {
                    // Optimize: reduce null checks and string operations
                    CharSequence titleSeq = titleEdit.getText();
                    CharSequence contentSeq = contentEdit.getText();
                    CharSequence startTimeSeq = startTimeEdit.getText();
                    CharSequence endTimeSeq = endTimeEdit.getText();
                    String title = titleSeq != null ? titleSeq.toString().trim() : "";
                    String content = contentSeq != null ? contentSeq.toString().trim() : "";
                    String startTime = startTimeSeq != null ? startTimeSeq.toString().trim() : "";
                    String endTime = endTimeSeq != null ? endTimeSeq.toString().trim() : "";

                    if (TextUtils.isEmpty(title) && TextUtils.isEmpty(content)) {
                        Toast.makeText(RemindersListActivity.this, getString(R.string.please_enter_title_or_content), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Get notification settings
                    // notificationSwitch may be null if layout changes, but lint thinks it's always non-null
                    @SuppressWarnings("ConstantConditions")
                    boolean enableNotification = notificationSwitch != null && notificationSwitch.isChecked();
                    int notificationMinutesBefore = 5; // Default
                    if (enableNotification && notificationTimeEdit != null) {
                        CharSequence minutesSeq = notificationTimeEdit.getText();
                        String minutesStr = minutesSeq != null ? minutesSeq.toString().trim() : "";
                        if (!minutesStr.isEmpty()) {
                            try {
                                notificationMinutesBefore = Integer.parseInt(minutesStr);
                                if (notificationMinutesBefore < 0) {
                                    notificationMinutesBefore = 0;
                                } else if (notificationMinutesBefore > 1440) { // Max 24 hours
                                    notificationMinutesBefore = 1440;
                                }
                            } catch (NumberFormatException e) {
                                // Invalid notification minutes, using default
                            }
                        }
                    }

                    // Get repeat type
                    String repeatType = "NONE";
                    if (repeatTypeEdit != null) {
                        CharSequence repeatSeq = repeatTypeEdit.getText();
                        String repeatStr = repeatSeq != null ? repeatSeq.toString().trim() : "";
                        repeatType = getRepeatTypeFromDisplayName(repeatStr);
                    }

                    // Cancel old alarm if exists
                    if (finalReminder.isEnableNotification()) {
                        AlarmHelper.cancelAlarm(RemindersListActivity.this, finalReminder);
                    }
                    
                    finalReminder.setTitle(title);
                    finalReminder.setContent(content);
                    finalReminder.setStartTime(startTime);
                    finalReminder.setEndTime(endTime);
                    finalReminder.setTimestamp(System.currentTimeMillis());
                    finalReminder.setEnableNotification(enableNotification);
                    finalReminder.setNotificationMinutesBefore(notificationMinutesBefore);
                    finalReminder.setRepeatType(repeatType);
                    
                    reminderManager.saveReminder(finalReminder);
                    
                    // Set alarm if notification is enabled
                    if (enableNotification && !startTime.isEmpty()) {
                        boolean alarmSet = AlarmHelper.setAlarm(RemindersListActivity.this, finalReminder);
                        if (!alarmSet) {
                            // Check if it's a permission issue
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                android.app.AlarmManager alarmManager = (android.app.AlarmManager) getSystemService(Context.ALARM_SERVICE);
                                if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                                    // Show dialog to guide user to settings
                                    showExactAlarmPermissionDialog();
                                } else {
                                    // Alarm time is in the past
                                    String reminderDateTime = finalReminder.getDate() + " " + startTime;
                                    Toast.makeText(RemindersListActivity.this, getString(R.string.reminder_time_past_detail, reminderDateTime), Toast.LENGTH_LONG).show();
                                }
                            } else {
                                // Alarm time is in the past
                                String reminderDateTime = finalReminder.getDate() + " " + startTime;
                                Toast.makeText(RemindersListActivity.this, getString(R.string.reminder_time_past_detail, reminderDateTime), Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                    
                    // Hide keyboard before dismissing dialog
                    hideKeyboard(dialogView);
                    
                    loadReminders();
                    updateWidgets();
                    dialog.dismiss();
                });
            }
        });

        dialog.setOnDismissListener(dialogInterface -> hideKeyboard(dialogView));

        dialog.show();
    }
    
    /**
     * Hide the soft keyboard
     */
    private void hideKeyboard(View view) {
        if (view == null || isFinishing() || isDestroyed()) {
            return;
        }
        try {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                android.view.Window window = getWindow();
                if (window != null) {
                    android.view.View currentFocus = window.getCurrentFocus();
                    if (currentFocus != null) {
                        imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
                    } else if (view.getWindowToken() != null) {
                        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to hide keyboard", e);
        }
    }

    private void showTimePicker(TextInputEditText timeEdit) {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        CharSequence currentTimeSeq = timeEdit.getText();
        String currentTime = currentTimeSeq != null ? currentTimeSeq.toString() : "";
        if (!currentTime.isEmpty()) {
            try {
                String[] parts = currentTime.split(":");
                if (parts.length == 2) {
                    hour = Integer.parseInt(parts[0]);
                    minute = Integer.parseInt(parts[1]);
                }
            } catch (Exception e) {
                // Failed to parse time, using default values
            }
        }

        TimePickerDialog timePickerDialog = new TimePickerDialog(this, (view, hourOfDay, minuteOfHour) -> {
            String time = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minuteOfHour);
            timeEdit.setText(time);
        }, hour, minute, true);

        timePickerDialog.show();
    }

    private void showRepeatTypePicker(TextInputEditText repeatTypeEdit) {
        String[] displayOptions = {
            getString(R.string.repeat_none),
            getString(R.string.repeat_daily),
            getString(R.string.repeat_weekly),
            getString(R.string.repeat_monthly),
            getString(R.string.repeat_yearly)
        };

        CharSequence currentValueSeq = repeatTypeEdit.getText();
        String currentValue = currentValueSeq != null ? currentValueSeq.toString().trim() : getString(R.string.repeat_none);
        int selectedIndex = 0; // Default to NONE
        for (int i = 0; i < displayOptions.length; i++) {
            if (displayOptions[i].equals(currentValue)) {
                selectedIndex = i;
                break;
            }
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.select_repeat_type))
                .setSingleChoiceItems(displayOptions, selectedIndex, (dialog, which) -> {
                    repeatTypeEdit.setText(displayOptions[which]);
                    dialog.dismiss();
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private String getRepeatTypeDisplayName(String repeatType) {
        if (repeatType == null) {
            return getString(R.string.repeat_none);
        }
        switch (repeatType) {
            case "DAILY":
                return getString(R.string.repeat_daily);
            case "WEEKLY":
                return getString(R.string.repeat_weekly);
            case "MONTHLY":
                return getString(R.string.repeat_monthly);
            case "YEARLY":
                return getString(R.string.repeat_yearly);
            default:
                return getString(R.string.repeat_none);
        }
    }

    private String getRepeatTypeFromDisplayName(String displayName) {
        if (displayName == null) {
            return "NONE";
        }
        if (displayName.equals(getString(R.string.repeat_daily))) {
            return "DAILY";
        } else if (displayName.equals(getString(R.string.repeat_weekly))) {
            return "WEEKLY";
        } else if (displayName.equals(getString(R.string.repeat_monthly))) {
            return "MONTHLY";
        } else if (displayName.equals(getString(R.string.repeat_yearly))) {
            return "YEARLY";
        } else {
            return "NONE";
        }
    }

    private void showNotificationTimePicker(TextInputEditText timeEdit) {
        String[] options = {"0", "5", "10", "15", "30", "60"};
        String[] displayOptions = {
            getString(R.string.at_time),
            String.format(Locale.getDefault(), getString(R.string.minutes_before_hint), "5"),
            String.format(Locale.getDefault(), getString(R.string.minutes_before_hint), "10"),
            String.format(Locale.getDefault(), getString(R.string.minutes_before_hint), "15"),
            String.format(Locale.getDefault(), getString(R.string.minutes_before_hint), "30"),
            String.format(Locale.getDefault(), getString(R.string.minutes_before_hint), "60")
        };

        CharSequence currentValueSeq = timeEdit.getText();
        String currentValue = currentValueSeq != null ? currentValueSeq.toString().trim() : "5";
        int selectedIndex = 1; // Default to 5 minutes
        for (int i = 0; i < options.length; i++) {
            if (options[i].equals(currentValue)) {
                selectedIndex = i;
                break;
            }
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.select_reminder_time))
                .setSingleChoiceItems(displayOptions, selectedIndex, (dialog, which) -> {
                    timeEdit.setText(options[which]);
                    dialog.dismiss();
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void showDeleteConfirmDialog(Reminder reminder) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.CustomDialogTheme)
                .setTitle(getString(R.string.confirm_delete))
                .setMessage(getString(R.string.confirm_delete_message))
                .setPositiveButton(getString(R.string.delete), (dialog, which) -> {
                    // Cancel alarm if notification is enabled
                    if (reminder.isEnableNotification()) {
                        AlarmHelper.cancelAlarm(RemindersListActivity.this, reminder);
                    }
                    
                    // If in history view, permanently delete; otherwise mark as deleted
                    if (showingHistory) {
                        reminderManager.permanentlyDeleteReminder(reminder.getId());
                    } else {
                        reminderManager.deleteReminder(reminder.getId());
                    }
                    loadReminders();
                    updateWidgets();
                })
                .setNegativeButton(getString(R.string.cancel), null);
        builder.show();
    }


    private void updateWidgets() {
        Intent intent = new Intent(this, ReminderWidgetProvider.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        int[] ids = AppWidgetManager.getInstance(this).getAppWidgetIds(
            new ComponentName(this, ReminderWidgetProvider.class)
        );
        if (ids.length > 0) {
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
            sendBroadcast(intent);
        }
    }

    private void showExactAlarmPermissionDialog() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            new MaterialAlertDialogBuilder(this, R.style.CustomDialogTheme)
                    .setTitle(getString(R.string.exact_alarm_permission_required))
                    .setMessage(getString(R.string.exact_alarm_permission_required))
                    .setPositiveButton(getString(R.string.open_settings), (dialog, which) -> {
                        try {
                            Intent intent = new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                            startActivity(intent);
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to open exact alarm settings", e);
                            Toast.makeText(this, getString(R.string.alarm_set_failed), Toast.LENGTH_LONG).show();
                        }
                    })
                    .setNegativeButton(getString(R.string.cancel), null)
                    .show();
        }
    }

    /**
     * Scroll to a specific reminder in the list
     * @param reminderId The ID of the reminder to scroll to
     */
    private void scrollToReminder(String reminderId) {
        if (reminderId == null || reminderAdapter == null || remindersRecyclerView == null) {
            return;
        }
        
        // Wait for the RecyclerView to be laid out before scrolling
        remindersRecyclerView.post(() -> {
            List<Reminder> reminders = reminderAdapter.getReminders();
            if (reminders == null) {
                return;
            }
            
            // Find the position of the reminder with the given ID
            for (int i = 0; i < reminders.size(); i++) {
                Reminder reminder = reminders.get(i);
                if (reminder != null && reminderId.equals(reminder.getId())) {
                    // Scroll to the position
                    LinearLayoutManager layoutManager = (LinearLayoutManager) remindersRecyclerView.getLayoutManager();
                    if (layoutManager != null) {
                        layoutManager.scrollToPositionWithOffset(i, 0);
                    }
                    break;
                }
            }
        });
    }

}

