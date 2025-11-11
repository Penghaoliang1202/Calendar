package com.example.myapplication;

import java.io.Serializable;

public class Reminder implements Serializable {
    private final String id;
    private final String date; // Format: yyyy-MM-dd
    private String title;
    private String content;
    private String startTime; // Format: HH:mm
    private String endTime; // Format: HH:mm
    private long timestamp;
    private boolean isCompleted; // Whether completed
    private boolean isDeleted; // Whether deleted

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
    }

    public String getId() {
        return id;
    }

    public String getDate() {
        return date;
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
}

