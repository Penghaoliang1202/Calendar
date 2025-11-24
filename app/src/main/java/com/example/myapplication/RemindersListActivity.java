package com.example.myapplication;

import android.annotation.SuppressLint;
import android.app.TimePickerDialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import androidx.appcompat.app.AlertDialog;
import android.content.Context;
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
    private MaterialButton clearHistoryButton;
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
            clearHistoryButton = findViewById(R.id.clearHistoryButton);

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

            if (clearHistoryButton != null) {
                clearHistoryButton.setOnClickListener(v -> showClearHistoryConfirmDialog());
            }

            loadReminders();
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
            if (clearHistoryButton != null) {
                clearHistoryButton.setVisibility(reminders.isEmpty() ? View.GONE : View.VISIBLE);
            }
        } else {
            reminders = reminderManager.getActiveReminders();
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(getString(R.string.all_reminders));
            }
            if (historyButton != null) {
                historyButton.setText(getString(R.string.view_history));
            }
            if (clearHistoryButton != null) {
                clearHistoryButton.setVisibility(View.GONE);
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
        if (reminder == null) {
            Log.e(TAG, "Cannot show reminder dialog: reminder is null");
            return;
        }
        
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_reminder, null);
        TextView dialogTitle = dialogView.findViewById(R.id.dialogTitle);
        TextInputEditText titleEdit = dialogView.findViewById(R.id.reminderTitleEdit);
        TextInputEditText contentEdit = dialogView.findViewById(R.id.reminderContentEdit);
        TextInputEditText startTimeEdit = dialogView.findViewById(R.id.startTimeEdit);
        TextInputEditText endTimeEdit = dialogView.findViewById(R.id.endTimeEdit);
        SwitchMaterial notificationSwitch = dialogView.findViewById(R.id.notificationSwitch);
        LinearLayout notificationTimeLayout = dialogView.findViewById(R.id.notificationTimeLayout);
        TextInputEditText notificationTimeEdit = dialogView.findViewById(R.id.notificationTimeEdit);

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
                    Reminder reminderToSave = finalReminder;
                    
                    reminderManager.saveReminder(reminderToSave);
                    
                    // Set alarm if notification is enabled
                    if (enableNotification && !startTime.isEmpty()) {
                        boolean alarmSet = AlarmHelper.setAlarm(RemindersListActivity.this, reminderToSave);
                        if (!alarmSet) {
                            // Alarm time is in the past
                            String reminderDateTime = reminderToSave.getDate() + " " + startTime;
                            Toast.makeText(RemindersListActivity.this, getString(R.string.reminder_time_past_detail, reminderDateTime), Toast.LENGTH_LONG).show();
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
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
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
                    updateWidgets();
                })
                .setNegativeButton(getString(R.string.cancel), null);
        builder.show();
    }

    private void showClearHistoryConfirmDialog() {
        List<Reminder> completedReminders = reminderManager.getCompletedReminders();
        if (completedReminders.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_history_reminders), Toast.LENGTH_SHORT).show();
            return;
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.CustomDialogTheme)
                .setTitle(getString(R.string.clear_all_history))
                .setMessage(getString(R.string.clear_all_history_message))
                .setPositiveButton(getString(R.string.clear_all), (dialog, which) -> {
                    // Cancel alarms for all completed reminders before clearing
                    for (Reminder reminder : completedReminders) {
                        if (reminder != null && reminder.isEnableNotification()) {
                            AlarmHelper.cancelAlarm(RemindersListActivity.this, reminder);
                        }
                    }
                    // Permanently delete all completed reminders
                    reminderManager.clearAllCompletedReminders();
                    Toast.makeText(RemindersListActivity.this, getString(R.string.all_history_cleared), Toast.LENGTH_SHORT).show();
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

}

