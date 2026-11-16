/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: Date and time formatting utilities tailored to framework reporting, log stamping, and timestamp generation.
 * Created: 2026-09-28
 */
package framework.utils;

import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import framework.reporting.ReportUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TimeZone;
import java.util.stream.Collectors;


public class CustomTimeUtil {

    private CustomTimeUtil() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    private static DateTimeFormatter getFlexibleDateFormatter(String customFormat) {
        try {
            /**      if (customFormat != null && !customFormat.isEmpty()) {
             /          return DateTimeFormatter.ofPattern(customFormat, Locale.ENGLISH);
             }*/
        } catch (Exception e) {
            ReportUtils.logInfo("Invalid custom format: " + customFormat + " | Falling back to default formats.");
        }

        // Default fallback formatter with multiple formats
        return new DateTimeFormatterBuilder()

                .appendOptional(DateTimeFormatter.ofPattern("M/d/yyyy h:mm a"))  // 3/25/2025 11:00 AM
                .appendOptional(DateTimeFormatter.ofPattern("MM/dd/yyyy h:mm a")) // 03/25/2025 11:00 AM
                .appendOptional(DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm a")) // 03/25/2025 11:00 AM
                .appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) // 2025-03-25 23:15:00
                .appendOptional(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")) // 2025/03/25 23:15:00
                .appendOptional(DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss")) // 25-Mar-2025 23:15:00
                .appendOptional(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) // 20250325231500
                .appendOptional(DateTimeFormatter.ofPattern("dd/MM/yyyy")) // 25/03/2025
                .appendOptional(DateTimeFormatter.ofPattern("dd-MM-yyyy")) // 25-03-2025
                .appendOptional(DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss")) // 2025.03.25 23:15:00
                .appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")) // 2025-03-25T23:15:00
                .appendOptional(DateTimeFormatter.ofPattern("EEE, d MMM yyyy HH:mm:ss Z")) // Tue, 25 Mar 2025 23:15:00 +0530
                .appendOptional(DateTimeFormatter.ofPattern("d MMM yyyy")) // 25 Mar 2025
                .appendOptional(DateTimeFormatter.ofPattern("MMM d, yyyy")) // Mar 25, 2025
                .appendOptional(DateTimeFormatter.ofPattern("MMMM d, yyyy")) // March 25, 2025
                .appendOptional(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")) // 2025/03/25 23:15
                .appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")) // 2025-03-25T23:15:30.123Z (ISO 8601)

                // Ensure seconds default to 0 if missing
                .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
                .toFormatter(Locale.ENGLISH);
    }

    public static String getNextExecution(String cronExpression, String currentDateTime, String timeZone) {
        // Define a cron parser for QUARTZ format
        CronParser parser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(com.cronutils.model.CronType.QUARTZ));
        Cron cron = parser.parse(cronExpression);

        // Parse current date-time with time zone
        ZonedDateTime now = ZonedDateTime.parse(currentDateTime, DateTimeFormatter.ISO_ZONED_DATE_TIME)
                .withZoneSameInstant(ZoneId.of(timeZone));

        // Get the next execution time
        ExecutionTime executionTime = ExecutionTime.forCron(cron);
        Optional<ZonedDateTime> nextExecution = executionTime.nextExecution(now);

        return nextExecution.map(time -> time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z")))
                .orElse("No next execution found.");
    }

    public static String getNextExecution1(String cronExpression, String timeZone) {
        // Define a cron parser for Quartz format
        CronDefinition cronDefinition = CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ);
        CronParser parser = new CronParser(cronDefinition);
        Cron cron = parser.parse(cronExpression);

        // Get the current date-time in UTC
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));

        // Convert to the given time zone
        now = now.withZoneSameInstant(ZoneId.of(timeZone));

        // Get the next execution time
        ExecutionTime executionTime = ExecutionTime.forCron(cron);
        Optional<ZonedDateTime> nextExecution = executionTime.nextExecution(now);

        // Define new date format (M/d/yyyy, hh:mm:ss a)
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/d/yyyy, hh:mm:ss a");

        return nextExecution.map(time -> time.format(formatter))
                .orElse("No next execution found");
    }

    public static String getNextExecution2(String cronExpression, String timeZone, String startDateTime) {
        try {
            // Define a cron parser for Quartz format
            CronDefinition cronDefinition = CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ);
            CronParser parser = new CronParser(cronDefinition);
            Cron cron = parser.parse(cronExpression);

            // Define date-time formatters
            DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("M/d/yyyy h:mm a", Locale.ENGLISH);
            DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("M/d/yyyy, h:mm:ss a", Locale.ENGLISH);

            // Parse start date-time and apply the given time zone
            ZonedDateTime startDate = LocalDateTime.parse(startDateTime.trim(), inputFormatter)
                    .atZone(ZoneId.of(timeZone));

            // Get the current date-time in UTC, then convert to given time zone
            ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC")).withZoneSameInstant(ZoneId.of(timeZone));

            // Determine the reference date: Use the later of (current time OR start date)
            ZonedDateTime referenceDate = now.isAfter(startDate) ? now : startDate;

            // Get execution time based on cron expression
            ExecutionTime executionTime = ExecutionTime.forCron(cron);

            // Get last execution (if applicable)
            Optional<ZonedDateTime> lastExecution = executionTime.lastExecution(referenceDate);

            // Get next execution after last execution (or reference date if no last execution)
            Optional<ZonedDateTime> nextExecution = executionTime.nextExecution(lastExecution.orElse(referenceDate));

            return nextExecution.map(time -> time.format(outputFormatter))
                    .orElse("No next execution found");

        } catch (Exception e) {
            return "Error while calculating next execution: " + e.getMessage();
        }
    }

    public static String getFutureTime(String format, int minute) {
        String ftValue = "";
        try {

            SimpleDateFormat sdf =  new SimpleDateFormat(format);

            Calendar calendar = Calendar.getInstance();
            int currentMinutes = calendar.get(Calendar.MINUTE);

            // Check for specific edge cases
            if (currentMinutes == 59 || currentMinutes == 0 || currentMinutes == 29 || currentMinutes == 30) {
                calendar.add(Calendar.MINUTE, 2);
            }

            int minutesToAdd = minute==1 ? (30 - (calendar.get(Calendar.MINUTE) % 30)) % 30 : minute; // Get the next 30-min interval

            // Add one minute
            calendar.add(Calendar.MINUTE, minutesToAdd);

            // Format the updated time
            ftValue =  sdf.format(calendar.getTime());

        }catch(Exception e){
            ReportUtils.logError(e,"Error occur while getFutureTime : "+e.getMessage());
        }
        return ftValue;

    }

    public static String getFutureTime(String format, int offset, ChronoUnit unit) {
        String ftValue = "";
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
            LocalDateTime futureTime = LocalDateTime.now().plus(offset, unit);
            ftValue = futureTime.format(formatter);
        } catch (Exception e) {
            ReportUtils.logError(e, "Error occurred while getting future time: " + e.getMessage());
        }
        return ftValue;
    }

    public static String getFutureTime(String format, int days, int hours, int minutes) {
        String ftValue = "";
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);

            // Add days, hours, and minutes to current time
            LocalDateTime futureTime = LocalDateTime.now()
                    .plusDays(days)
                    .plusHours(hours)
                    .plusMinutes(minutes);

            ftValue = futureTime.format(formatter);
        } catch (Exception e) {
            ReportUtils.logError(e, "Error occurred while getting future time: " + e.getMessage());
        }
        return ftValue;
    }

    public static String getFutureTimeAsPerUTC(String format, int days, int hours, int minutes) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);

            // Current time in IST -> UTC
            ZonedDateTime utcNow = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"))
                    .withZoneSameInstant(ZoneOffset.UTC);

            // Calculate based on UTC DATE
            LocalDate futureDate = utcNow.toLocalDate()
                    .plusDays(days);

            // Set time explicitly to 00:00:00 UTC
            ZonedDateTime futureUtcDateTime = futureDate
                    .atStartOfDay(ZoneOffset.UTC)
                    .plusHours(hours)
                    .plusMinutes(minutes);

            return futureUtcDateTime.format(formatter);

        } catch (Exception e) {
            ReportUtils.logError(
                    e,
                    "Error occurred while getting future time: " + e.getMessage()
            );
            return "";
        }
    }

    public static String getNextWeekDateTime(int afterDays,  String dateFormat) {
        // Get the current date and time
        LocalDateTime now = LocalDateTime.now();

        // Calculate days until the next occurrence of the same day next week
        LocalDateTime nextDay = now.plusDays(afterDays);

        // Format the date and time
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateFormat);
        return nextDay.format(formatter);
    }


    public static boolean compareTimeStamp(String timestamp1 , String timestamp2 , String format1, String format2) {

        boolean flag =true;
        // Define the date formats
        SimpleDateFormat sformat = new SimpleDateFormat(format1);
        SimpleDateFormat tformat = new SimpleDateFormat(format2);

        try {
            // Parse timestamp into Date objects
            Date date1 = sformat.parse(timestamp1);
            Date date2 = tformat.parse(timestamp2);

            ReportUtils.logInfo("Expected Timestamp : "+timestamp1);
            ReportUtils.logInfo("Retrieved Timestamp : "+timestamp2);
            // Calculate the time difference in milliseconds
            long timeDiff = Math.abs(date1.getTime() - date2.getTime());

            // If the difference is within 5 seconds (5000ms), consider them equal
            if (timeDiff <= 5000) {
                ReportUtils.logPass("Both timestamps are equal.");
                return true;
            } else if (date1.before(date2)) {
                ReportUtils.logFail("Exepected timestamp is earlier.", new Throwable());
                return false;
            } else {
                ReportUtils.logFail("Retrieved timestamp is earlier.", new Throwable());
                return false;
            }
        } catch (Exception e) {
            ReportUtils.logError(e,"Error parsing the timestamps: " + e.getMessage());
            flag=false;
        }

        return flag;

    }

    public static String getDayOfWeekString(String input) {
        // Array mapping numbers (1-7) to days of the week
        String[] dayNames = {"", "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};

        // Convert input string to day names
        return Arrays.stream(input.split(",")) // Split by comma
                .map(String::trim) // Remove any spaces
                .map(Integer::parseInt) // Convert to integer
                .filter(day -> day >= 1 && day <= 7) // Ensure valid range (1-7)
                .map(day -> dayNames[day]) // Get day name from array
                .collect(Collectors.joining(", ")); // Join results
    }

    public static String getCronExpression(String type, String reoccur, String hours, String minutes, String dayOfWeek, String startyr, String endyr, String startDate, String format) {

        String cronString = "";
        try {

            String hourPart = hours.isEmpty() ? getDatePart(format, startDate, "hour") : hours;
            hourPart = hourPart.isEmpty() ? "*" : hourPart; // Default to '*'

            String minutePart = minutes.isEmpty() ? getDatePart(format, startDate, "minute") : minutes;
            minutePart = minutePart.isEmpty() ? "*" : minutePart; // Default to '*'

            String dayOfMonthPart;
            if((type.trim().equalsIgnoreCase("daily"))){

                dayOfMonthPart = reoccur.isEmpty() ? "*" : "*/"+reoccur;

            }else {
                dayOfMonthPart = reoccur.isEmpty() ? "?" : "*/"+reoccur;
            }

            String dayOfWeekPart = dayOfWeek.isEmpty() ? "?" : dayOfWeek;

            String yearPart = startyr+"-"+endyr;

            cronString =  String.format("0 %s %s %s * %s %s", minutePart, hourPart, dayOfMonthPart, dayOfWeekPart, yearPart);

        }catch(Exception e) {
            ReportUtils.logError(e,"Error occur while getDatePart : "+e.getMessage()+ " ::::::- "+startDate+" :::::: "+format+" :::::: "+type);
        }
        return cronString;
    }

    public static String generateCronExpression(String type, String format, String reoccur, String dayOfWeek,  String startDateStr, String endDateStr) {

        // Define date format
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);

        // Parse start date
        LocalDateTime startDate = LocalDateTime.parse(startDateStr, formatter);

        // Parse end date if provided, otherwise set to 2099
        LocalDateTime endDate = Optional.ofNullable(endDateStr)
                .map(date -> LocalDateTime.parse(date, formatter))
                .orElse(LocalDateTime.of(2099, 12, 31, 23, 59));

        // Extract time details
        int minute = startDate.getMinute();
        int hour = startDate.getHour();
        int startYear = startDate.getYear();
        int endYear = endDate.getYear();

        String dayOfMonthPart = reoccur.isEmpty() ? "?" : "*/"+reoccur;

        String dayOfWeekPart = dayOfWeek.isEmpty() ? "?" : dayOfWeek;

        if(type.trim().equalsIgnoreCase("daily")) {

            return String.format("0 %d %d %s * ? %d-%d", minute, hour, dayOfMonthPart, startYear, endYear);

        }else {

            return String.format("0 %d %d ? * %s %d-%d", minute, hour, dayOfWeekPart, startYear, endYear);
        }

    }

    // Map for converting day names to Cron numeric values
    private static final Map<String, String> dayOfWeekMap = new HashMap<>();

    static {
        dayOfWeekMap.put("sunday", "1");
        dayOfWeekMap.put("monday", "2");
        dayOfWeekMap.put("tuesday", "3");
        dayOfWeekMap.put("wednesday", "4");
        dayOfWeekMap.put("thursday", "5");
        dayOfWeekMap.put("friday", "6");
        dayOfWeekMap.put("saturday", "7");
    }


    public static String getCronDayOfWeek(String dayNames) {

        if (dayNames == null || dayNames.trim().isEmpty()) {
            return "*"; // Default to '*' if no days are provided
        }

        // Split input, trim spaces, convert to lowercase, map to numbers
        return Arrays.stream(dayNames.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .map(dayOfWeekMap::get)
                .filter(Objects::nonNull) // Remove invalid entries
                .sorted(Comparator.comparingInt(Integer::parseInt)) // Sort numerically
                .collect(Collectors.joining(","));
    }

    public static String getCurrentDateInfo(String type) {
        LocalDate today = LocalDate.now();

        switch (type.toLowerCase()) {
            case "year":
                return String.valueOf(today.getYear()); // Get the current year
            case "month":
                return today.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH); // Get full month name (e.g., "March")
            case "month_number":
                return String.valueOf(today.getMonthValue()); // Get month number (e.g., 3 for March)
            case "date":
                return String.valueOf(today.getDayOfMonth()); // Get the day of the month
            default:
                return "Invalid input! Please use 'year', 'month', 'month_number', or 'date'.";
        }
    }

    public static String getDatePart(String format, String dateStr, String part) {
        String output = "";
        try {
            if(!dateStr.isEmpty()) {


                LocalDateTime dateTime = LocalDateTime.parse(dateStr, getFlexibleDateFormatter(format));

                switch (part.toLowerCase()) {
                    case "year":
                        output= String.valueOf(dateTime.getYear()); break;
                    case "month":
                        output= dateTime.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);break; // Full month name
                    case "day":
                        output= String.valueOf(dateTime.getDayOfMonth());break;
                    case "hour":
                        output= String.valueOf(dateTime.getHour()); break;// 24-hour format
                    case "minute":
                        output= String.valueOf(dateTime.getMinute());break;
                    case "ampm":
                        output= dateTime.getHour() < 12 ? "AM" : "PM";break; // AM/PM extraction
                    default:
                        throw new IllegalArgumentException("Invalid part. Use 'day', 'month', 'year', 'hour', 'minute', or 'ampm'.");
                }
            }
        } catch (Exception e) {
            ReportUtils.logError(e,"Error occur while getDatePart : "+e.getMessage()+ " ::: "+dateStr+" ::::: "+format+" ::::: "+part);
        }

        return output;
    }

    public static String getCurrentTime(String format, String timezone) {
        SimpleDateFormat formatter = new SimpleDateFormat(format);
        formatter.setTimeZone(TimeZone.getTimeZone(timezone)); // Set timezone to IST
        Date date = new Date();
        return formatter.format(date);
    }

    public static void waitForExecution(String format, String targetTime) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(format);

        try {
            Date targetDate = dateFormat.parse(targetTime);
            long targetTimeMillis = targetDate.getTime();

            ReportUtils.logInfo("Waiting for execution until: " + targetTime);

            // Keep checking the current time until target time is reached
            while (System.currentTimeMillis() < targetTimeMillis) {
                // Busy wait loop 
            }

            ReportUtils.logInfo("Execution time reached: " + targetTime);

        } catch (ParseException e) {
            ReportUtils.logInfo("Invalid date format. Please provide date in format.");
            e.printStackTrace();
        }
    }

    public static String returnMonthName(String monthNum) {
        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

        try {
            int monthIndex = Integer.parseInt(monthNum.trim());
            return (monthIndex >= 1 && monthIndex <= 12) ? months[monthIndex - 1] : "Jan";
        } catch (NumberFormatException e) {
            return "Jan"; // Default value for invalid input
        }
    }

    public static String returnDay(String day) {
        try {
            return String.valueOf(Integer.parseInt(day.trim()));
        } catch (NumberFormatException e) {
            return day; // Return as is if parsing fails
        }
    }

    public static String getUTCTimeFormat(String format) {

        // Create a SimpleDateFormat object with the desired format
        SimpleDateFormat sdf = new SimpleDateFormat(format);

        // Set the time zone to UTC
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

        // Get the current date and time
        return  sdf.format(new Date());

    }

    public static boolean isDateTimeListSorted(List<String> dateList, String dateFormat, String expectedOrder){
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
            sdf.setLenient(false);

            for (int i = 0; i < dateList.size() - 1; i++) {
                Date current = sdf.parse(dateList.get(i));
                Date next = sdf.parse(dateList.get(i + 1));

                if ("asc".equalsIgnoreCase(expectedOrder) && current.after(next)) {
                    ReportUtils.logInfo("List : "+dateList);
                    ReportUtils.logFail("Date list sorted in asc order ? " + dateList.get(i), new Throwable());
                    return false;
                } else if ("desc".equalsIgnoreCase(expectedOrder) && current.before(next)) {
                    ReportUtils.logInfo("List : "+dateList);
                    ReportUtils.logFail("Date list sorted in desc order ? " + dateList.get(i), new Throwable());
                    return false;
                }
            }
        } catch (ParseException e) {
            ReportUtils.logError(e, "Invalid date format. Please provide date in format.");
        } catch (Exception ex) {
            ReportUtils.logError(ex, "Expection Occours at isDateTimeListSorted");
        }
        return true;
    }

    public static String getPastTime(String format, int minute) {
        String ptValue = "";
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(format);
            Calendar calendar = Calendar.getInstance();
            int currentMinutes = calendar.get(Calendar.MINUTE);

            // Check for specific edge cases
            if (currentMinutes == 59 || currentMinutes == 0 || currentMinutes == 29 || currentMinutes == 30) {
                calendar.add(Calendar.MINUTE, -2); // Go back 2 minutes to avoid exact edges
            }

            int value = calendar.get(Calendar.MINUTE) % 30 == 0 ? 30 : calendar.get(Calendar.MINUTE) % 30;

            int minutesToSubtract = minute == 1 ? value : minute;

            // Subtract minutes
            calendar.add(Calendar.MINUTE, -minutesToSubtract);

            // Format the updated time
            ptValue = sdf.format(calendar.getTime());

        } catch (Exception e) {
            ReportUtils.logError(e, "Error occurred while getPastTime: " + e.getMessage());
        }
        return ptValue;
    }

    public static boolean compareTimeDate(String timestamp1 , String timestamp2 , String format1, String format2) {

        boolean flag =true;
        // Define the date formats
        SimpleDateFormat sformat = new SimpleDateFormat(format1);
        SimpleDateFormat tformat = new SimpleDateFormat(format2);

        try {
            // Parse timestamp into Date objects
            Date date1 = sformat.parse(timestamp1);
            Date date2 = tformat.parse(timestamp2);

            ReportUtils.logInfo("Expected Time : "+timestamp1);
            ReportUtils.logInfo("Retrieved Time : "+timestamp2);
            // Calculate the time difference in milliseconds
            long timeDiff = Math.abs(date1.getTime() - date2.getTime());

            flag = timeDiff <= 1000;

        } catch (Exception e) {
            ReportUtils.logError(e,"Error while the compareTimeDate: " + e.getMessage());
            flag=false;
        }

        return flag;

    }

    public static boolean compareFlexibleTimeDate(String timestamp1, String timestamp2) {
        try {
            DateTimeFormatter flexibleFormatter = getFlexibleDateFormatter("");

            // Parse both timestamps using flexible formatter
            LocalDateTime dt1 = LocalDateTime.parse(timestamp1, flexibleFormatter);
            LocalDateTime dt2 = LocalDateTime.parse(timestamp2, flexibleFormatter);

            // Convert to epoch milliseconds
            long millis1 = Date.from(dt1.atZone(ZoneId.systemDefault()).toInstant()).getTime();
            long millis2 = Date.from(dt2.atZone(ZoneId.systemDefault()).toInstant()).getTime();

            ReportUtils.logInfo("Expected Time : " + timestamp1);
            ReportUtils.logInfo("Retrieved Time : " + timestamp2);

            long timeDiff = Math.abs(millis1 - millis2);
            return timeDiff <= 1000;

        } catch (Exception e) {
            ReportUtils.logError(e, "Error in compareFlexibleTimeDate: " + e.getMessage());
            return false;
        }
    }

    public static String getTimeDifference(String startStr, String endStr, String format) {
        if (startStr == null || endStr == null || startStr.isEmpty() || endStr.isEmpty()) {
            return "Invalid input: start or end time is null/empty.";
        }

        DateTimeFormatter formatter = getFlexibleDateFormatter(format);

        try {
            LocalDateTime start = LocalDateTime.parse(startStr, formatter);
            LocalDateTime end = LocalDateTime.parse(endStr, formatter);

            Duration duration = Duration.between(start, end).abs();

            long totalSeconds = duration.getSeconds();
            long days = totalSeconds / (24 * 3600);
            long hours = (totalSeconds % (24 * 3600)) / 3600;
            long minutes = (totalSeconds % 3600) / 60;
            long seconds = totalSeconds % 60;

            StringBuilder result = new StringBuilder();

            if (days > 0) result.append(days).append(" days, ");
            if (hours > 0) result.append(hours).append(" hours, ");
            if (minutes > 0) result.append(minutes).append(" minutes, ");
            if (seconds > 0) result.append(seconds).append(" seconds, ");

            // Remove last comma and space
            if (result.length() > 2) {
                result.setLength(result.length() - 2);
            } else {
                return "0 seconds"; // All values were 0
            }

            return result.toString();

        } catch (DateTimeParseException e) {
            return "Error parsing date: " + e.getMessage();
        } catch (Exception e) {
            return "Unexpected error: " + e.getMessage();
        }
    }

    //compare two dates
    public static boolean compareTwoDates(boolean flag, String dateformat, String startDate,String endDate) {

        try {
            if(!startDate.isEmpty() || !endDate.isEmpty()) {

                if(startDate.equalsIgnoreCase("today")) {
                    String timestamp = new SimpleDateFormat(dateformat).format(new Date());

                    startDate = timestamp;
                }
                if(endDate.equalsIgnoreCase("today")) {
                    String timestamp = new SimpleDateFormat(dateformat).format(new Date());

                    endDate = timestamp;
                }

                SimpleDateFormat sdformat = new SimpleDateFormat(dateformat);
                Date d1 = sdformat.parse(startDate);
                Date d2 = sdformat.parse(endDate);
                ReportUtils.logInfo("The date 1 is: " + sdformat.format(d1));
                ReportUtils.logInfo("The date 2 is: " + sdformat.format(d2));
                if(d1.compareTo(d2) > 0) {
                    ReportUtils.logFail("start date occurs after end date ", new Throwable(""));
                    ReportUtils.logInfo("Date 1 occurs after Date 2");
                    flag=false;
                } else if(d1.compareTo(d2) < 0) {
                    ReportUtils.logInfo("Date 1 occurs before Date 2");
                } else if(d1.compareTo(d2) == 0) {
                    ReportUtils.logInfo("Both dates are equal");
                }
            }
        } catch (Exception e) {
            ReportUtils.logError(e,"error while compareTwoDates :: "+e.toString());
        }
        return flag;
    }

    public static LocalDate parseDate(String date) {

        String input = date.trim();

        DateTimeFormatter[] formatters = {

                DateTimeFormatter.ofPattern(
                        "dd-MMM-yyyy",
                        Locale.ENGLISH
                ),

                DateTimeFormatter.ofPattern(
                        "dd-MMM-yy",
                        Locale.ENGLISH
                ),

                DateTimeFormatter.ofPattern(
                        "dd/MM/yyyy",
                        Locale.ENGLISH
                ),

                DateTimeFormatter.ofPattern(
                        "dd-MM-yyyy",
                        Locale.ENGLISH
                ),

                DateTimeFormatter.ofPattern(
                        "yyyy-MM-dd",
                        Locale.ENGLISH
                ),

                DateTimeFormatter.ofPattern(
                        "yyyy/MM/dd",
                        Locale.ENGLISH
                ),

                DateTimeFormatter.ofPattern(
                        "MMM dd, yyyy",
                        Locale.ENGLISH
                ),

                DateTimeFormatter.ofPattern(
                        "MMMM dd, yyyy",
                        Locale.ENGLISH
                )
        };

        for (DateTimeFormatter formatter : formatters) {

            try {

                return LocalDate.parse(input, formatter);

            } catch (DateTimeParseException e) {
                // Try next format
            }
        }

        throw new IllegalArgumentException(
                "Unsupported date format: " + date +
                        ". Supported formats: " +
                        "dd-MMM-yyyy, dd-MMM-yy, dd/MM/yyyy, " +
                        "dd-MM-yyyy, yyyy-MM-dd, yyyy/MM/dd, " +
                        "MMM dd, yyyy, MMMM dd, yyyy"
        );
    }

    public static String formatDate(String date) {

        LocalDate localDate = parseDate(date);

        return localDate.format(
                DateTimeFormatter.ofPattern(
                        "dd MMMM yyyy",
                        Locale.ENGLISH
                )
        );
    }

}
