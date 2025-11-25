package com.example.myapplication;

import java.io.Serializable;

public class Reminder implements Serializable {
    private final String id;
    private String date; // Format: yyyy-MM-dd
    private String title;
    private String content;
    private String startTime; // Format: HH:mm
    private String endTime; // Format: HH:mm
    private long timestamp;
    private boolean isCompleted; // Whether completed
    private boolean isDeleted; // Whether deleted
    private boolean enableNotification; // Whether to enable system notification
    private int notificationMinutesBefore; // Minutes before reminder time to notify (e.g., 5, 10, 15)
    private String repeatType; // Repeat type: NONE, DAILY, WEEKLY, MONTHLY, YEARLY

    public Reminder(String id, String date, String title, String content, String startTime, String endTime, long timestamp) {
        this.id = id;
        this.date = date;
        this.title = title;
        this.content = content;
        this.startTime = startTime;
        this.endTime = endTime;
        this.timestamp = timestamp;
        this.isCompleted = false;
        this.isDeleted = false;
        this.enableNotification = false;
        this.notificationMinutesBefore = 5; // Default: 5 minutes before
        this.repeatType = "NONE"; // Default: no repeat
    }

    public String getId() {
        return id;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }

    public boolean isEnableNotification() {
        return enableNotification;
    }

    public void setEnableNotification(boolean enableNotification) {
        this.enableNotification = enableNotification;
    }

    public int getNotificationMinutesBefore() {
        return notificationMinutesBefore;
    }

    public void setNotificationMinutesBefore(int notificationMinutesBefore) {
        this.notificationMinutesBefore = notificationMinutesBefore;
    }

    public String getRepeatType() {
        return repeatType != null ? repeatType : "NONE";
    }

    public void setRepeatType(String repeatType) {
        this.repeatType = repeatType != null ? repeatType : "NONE";
    }
}

