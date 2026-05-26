package com.example.chatrt.utils;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class ReminderParser {
    
    public static class ReminderData {
        public String content;
        public Date dueDate;

        public ReminderData(String content, Date dueDate) {
            this.content = content;
            this.dueDate = dueDate;
        }
    }

    /**
     * Parse cú pháp: /reminder {DD/MM/YYYY} {todo-content}
     */
    public static ReminderData parse(String text) {
        if (text == null || !text.trim().startsWith("/reminder")) return null;
        
        String[] parts = text.trim().split("\\s+", 3);
        if (parts.length < 3) return null; // Thiếu ngày hoặc nội dung

        String dateStr = parts[1];
        String content = parts[2];

        Date dueDate = parseDate(dateStr);
        if (dueDate == null) return null;

        return new ReminderData(content, dueDate);
    }

    private static Date parseDate(String dateStr) {
        Calendar cal = Calendar.getInstance();
        String[] dateParts = dateStr.split("[/-]");
        if (dateParts.length < 2) return null;

        try {
            int day = Integer.parseInt(dateParts[0].trim());
            int month = Integer.parseInt(dateParts[1].trim()) - 1;
            int year = cal.get(Calendar.YEAR);
            
            if (dateParts.length == 3) {
                year = Integer.parseInt(dateParts[2].trim());
                if (year < 100) year += 2000;
            }

            cal.set(year, month, day, 9, 0, 0); // Mặc định 9h sáng
            cal.set(Calendar.MILLISECOND, 0);
            
            return cal.getTime();
        } catch (Exception e) {
            return null;
        }
    }
}
