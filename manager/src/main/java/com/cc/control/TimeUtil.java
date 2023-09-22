package com.cc.control;

import android.text.TextUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * 日期时间管理类
 */
public class TimeUtil {
    public static final SimpleDateFormat DEFAULT_DATE_FORMAT = new SimpleDateFormat("HH:mm:ss", Locale.CHINESE);
    public static final SimpleDateFormat DEFAULT_MIN_FORMAT = new SimpleDateFormat("mm:ss", Locale.CHINESE);
    public static final SimpleDateFormat DATE_FORMAT_DATE = new SimpleDateFormat("yyyy/MM/dd", Locale.CHINESE);
    public static final SimpleDateFormat DEL_DATE = new SimpleDateFormat("MM/dd", Locale.CHINESE);
    public static final SimpleDateFormat MONTH = new SimpleDateFormat("MM", Locale.CHINESE);
    public static final SimpleDateFormat DAY = new SimpleDateFormat("dd", Locale.CHINESE);
    public static final SimpleDateFormat DEL_FORMAT_DATE = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINESE);
    public static final SimpleDateFormat DEL_FORMAT_DATE2 = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINESE);
    public static final SimpleDateFormat DEL_FORMAT_DATE1 = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINESE);
    public static final SimpleDateFormat DEL_FORMAT_MONTH = new SimpleDateFormat("yyyy-MM", Locale.CHINESE);
    public static final SimpleDateFormat DEL_FORMAT_MDHM = new SimpleDateFormat("MM-dd HH:mm", Locale.CHINESE);

    public static final SimpleDateFormat INT_HOUR_FORMAT = new SimpleDateFormat("HH:mm", Locale.CHINESE);
    public static final SimpleDateFormat INT_MOMTH_FORMAT = new SimpleDateFormat("MM/dd HH:mm", Locale.CHINESE);
    public static final SimpleDateFormat INT_MONTH_FORMAT = new SimpleDateFormat("MM月dd日 HH:mm", Locale.CHINESE);
    public static final SimpleDateFormat INT_MONTH_DAY_FORMAT = new SimpleDateFormat("MM月dd日", Locale.CHINESE);
    public static final SimpleDateFormat INT_YEAR_MONTH_FORMAT = new SimpleDateFormat("yyyy年MM月", Locale.CHINESE);
    public static final Date todayDate = new Date();


    /**
     * long time to int
     *
     * @param timeInMillis
     * @param dateFormat
     * @return
     */
    public static int getCurrentHourInInt(long timeInMillis, SimpleDateFormat dateFormat) {
        String date = dateFormat.format(new Date(timeInMillis));
        int time = 0;
        if (!TextUtils.isEmpty(date)) {
            time = Integer.parseInt(date);
        }
        return time;
    }

    /**
     * 毫秒 转 日期
     *
     * @param time
     * @return
     */
    public static Calendar millisToDate(long time) {
        Calendar date = Calendar.getInstance();
        date.setTimeInMillis(time);
        return date;
    }

    public static String dateToString(Calendar date) {
        String stringBuilder = intToString(date.get(Calendar.MINUTE)) + ":" + intToString(date.get(Calendar.SECOND));
        return stringBuilder;
    }

    public static String intToString(int i) {
        return String.format("%02d", i);
    }

    /**
     * @param y_m_d_h_m 年月日时分
     * @return yyyy-MM-dd HH:mm 转 MM月dd日
     */
    public static String stringDateFormat(String y_m_d_h_m) {
        return INT_MONTH_DAY_FORMAT.format(stringToDate2(y_m_d_h_m));
    }

    /**
     * @param y_m_d 年月日
     * @return yyyy-MM-dd 转 MM月dd日
     */
    public static String stringDateFormat1(String y_m_d) {
        String date = INT_MONTH_DAY_FORMAT.format(stringToDate1(y_m_d));
        return date;
    }

    // strTime 要转换的string类型的时间，
    // formatType 要转换的格式yyyy-MM-dd HH:mm:ss//yyyy年MM月dd日HH时mm分ss秒，
    // strTime的时间格式必须要与formatType的时间格式相同

    /**
     * @param strTime yyyy-MM-dd HH:mm:ss
     * @return
     */
    public static Date stringToDate(String strTime) {
        Date date = todayDate;
        try {
            date = DEL_FORMAT_DATE.parse(strTime);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;
    }

    /**
     * @param strTime yyyy-MM-dd
     * @return
     */
    public static Date stringToDate1(String strTime) {
        Date date = todayDate;
        try {
            date = DEL_FORMAT_DATE1.parse(strTime);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;
    }

    /**
     * @param strTime yyyy-MM-dd HH:mm
     * @return
     */
    public static Date stringToDate2(String strTime) {
        Date date = todayDate;
        try {
            date = DEL_FORMAT_DATE2.parse(strTime);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;
    }

    public static Date stringToDate(String strTime, SimpleDateFormat simpleDateFormat) {
        Date date = todayDate;
        try {
            date = simpleDateFormat.parse(strTime);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;
    }

    public static Date stringToDateMonth(String strTime) {
        Date date = todayDate;
        try {
            date = DEL_FORMAT_MONTH.parse(strTime);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;
    }

    public static long getLongTime(String strTime) {
        return stringToDate(strTime).getTime();
    }

    /**
     * long time to string
     *
     * @param timeInMillis
     * @param dateFormat
     * @return
     */
    public static String getTime(long timeInMillis, SimpleDateFormat dateFormat) {
        return dateFormat.format(new Date(timeInMillis));
    }

    /**
     * long time to string
     *
     * @return
     */
    public static String getTime() {
        return getTime(new Date());
    }


    public static String getTime(Date date) {
        return DEL_FORMAT_DATE1.format(date);
    }

    public static String getYMDHMS(Date date) {
        return DEL_FORMAT_DATE.format(date);
    }

    public static String getYM(Date date) {
        return INT_YEAR_MONTH_FORMAT.format(date);
    }

    public static String getTimeHHMM(Date date) {
        return INT_HOUR_FORMAT.format(date);
    }

    public static String getTimeSeconds(Date date) {
        return DEFAULT_DATE_FORMAT.format(date);
    }

    public static String getTimeDay(Date date) {
        return DAY.format(date);
    }

    /**
     * long time to int
     *
     * @param timeInMillis
     * @param dateFormat
     * @return
     */
    public static long getCurrentDateInLong(long timeInMillis, SimpleDateFormat dateFormat) {
        String date = dateFormat.format(new Date(timeInMillis));
        long time = 0;
        if (!TextUtils.isEmpty(date)) {
            time = Long.parseLong(date);
        }
        return time;
    }

    /**
     * long time to int
     *
     * @param timeInMillis
     * @return
     */
    public static String getFormatDateToMMss(long timeInMillis) {
        if (timeInMillis <= 0) {
            return "00:00";
        }
        return DEFAULT_MIN_FORMAT.format(new Date(timeInMillis));
    }

    /**
     * long time to int
     *
     * @param time
     * @return
     */
    public static String getFormatDateToMDHM(String time, int duration) {
        try {
            Date date1 = DEL_FORMAT_DATE2.parse(time);
            long endTime = date1.getTime() + duration * 60000L;
            if (date1 != null) {
                return DEL_FORMAT_MDHM.format(date1) + " - " + INT_HOUR_FORMAT.format(new Date(endTime));
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * long time to string, format is {@link #DEFAULT_DATE_FORMAT}
     *
     * @param timeInMillis
     * @return
     */
    public static String getTime(long timeInMillis) {
        return getTime(timeInMillis, DEFAULT_DATE_FORMAT);
    }

    public static Calendar getCalendar(String time) {
        Calendar calendar = Calendar.getInstance();
        if (TextUtils.isEmpty(time) || time.equals("0")) {
            calendar.set(0, 0, 0, 0, 0, 0);
            return calendar;
        }
        try {
            Date date1 = DEFAULT_DATE_FORMAT.parse(time);
            if (date1 != null)
                calendar.setTime(date1);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return calendar;
    }

    public static Calendar getYYMMDD(String time) {
        Calendar calendar = Calendar.getInstance();
        if (TextUtils.isEmpty(time) || time.equals("0")) {
            calendar.set(0, 0, 0, 0, 0, 0);
            return calendar;
        }
        try {
            Date date1 = DEL_FORMAT_DATE1.parse(time);
            if (date1 != null) {
                calendar.setTime(date1);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return calendar;
    }

    public static int getSeconds(String time) {
        if (time.equals("0")) {
            return 0;
        }
        Calendar calendar = getCalendar(time);
        return calendar.get(Calendar.SECOND) + calendar.get(Calendar.MINUTE) * 60 + calendar.get(Calendar.HOUR) * 3600;
    }

    public static String getHourTime(String date) {
        return getTime(stringToDate(date).getTime(), INT_HOUR_FORMAT);
    }

    public static String getHourTime2(String date) {
        return getTime(stringToDate2(date).getTime(), INT_HOUR_FORMAT);
    }

    public static String getMonthHourTime(String date) {
        return getTime(stringToDate(date).getTime(), INT_MOMTH_FORMAT);
    }

    public static String getMonthTime(String date) {
        return getTime(stringToDate(date).getTime(), INT_MONTH_FORMAT);
    }


    //MM/dd
    public static String getMonth(String date) {
        return getTime(stringToDate(date).getTime(), DEL_DATE);
    }

    public static String getMonth2(String date) {
        return getTime(stringToDate(date, DEL_FORMAT_DATE1).getTime(), DEL_DATE);
    }


    public static int getMonth(Date date) {
        int month = 0;
        try {
            Calendar c = Calendar.getInstance();
            c.setTime(date);
            month = c.get(Calendar.MONTH) + 1;//函数返回0~11，结果要+1
        } catch (Exception e) {
            e.printStackTrace();
        }
        return month;
    }


    public static String getMonth1(String date) {
        return getTime(stringToDateMonth(date).getTime(), MONTH);
    }

    public static String getYearMoth(String date) {
        return getTime(stringToDateMonth(date).getTime(), MONTH);
    }

    public static String getYearTime(String date) {
        return getTime(stringToDate(date).getTime(), DEL_FORMAT_DATE1);
    }

    /**
     * get current time in milliseconds
     *
     * @return
     */
    public static long getCurrentTimeInLong() {
        return System.currentTimeMillis();
    }

    /**
     * get current time in milliseconds, format is {@link #DEFAULT_DATE_FORMAT}
     *
     * @return
     */
    public static String getCurrentTimeInString() {
        return getTime(getCurrentTimeInLong(), DEL_FORMAT_DATE);
    }

    public static String getCurrentTimeInString(Long time) {
        return getTime(time, DEL_FORMAT_DATE);
    }

    /**
     * get current time in milliseconds
     *
     * @return
     */
    public static String getCurrentTimeInString(SimpleDateFormat dateFormat) {
        return getTime(getCurrentTimeInLong(), dateFormat);
    }

    //时间转化成时分秒 00:00:00

    public static String secondToHMS(long time) {
        if (time == 0)
            return "00:00:00";
        long hours = time / 3600;
        long minute = (time - hours * 3600) / 60;
        long second = time % 60;
        String h = hours < 10 ? "0" + hours : hours + "";
        String min = minute < 10 ? "0" + minute : minute + "";
        String sec = second < 10 ? "0" + second : second + "";
        return h + ":" + min + ":" + sec;
    }

    public static String secondToMS(long time) {
        if (time <= 0)
            return "00:00";
        long minute = time / 60;
        long second = time % 60;
        String min = minute < 10 ? "0" + minute : minute + "";
        String sec = second < 10 ? "0" + second : second + "";
        return min + ":" + sec;
    }

    /**
     * @param time 时间
     * @return 20min29s
     */
    public static String second2MS_format(long time) {
        if (time <= 0)
            return "0min0s";
        long minute = time / 60;
        long second = time % 60;
        return minute + "min" + second + "s";
    }

    /**
     * @param time 时间
     * @return 20min29s
     */
    public static String second2M_format(long time) {
        if (time <= 0) {
            return "0min";
        } else if (time <= 60000) {
            return "1min";
        }
        long minute = time / 60000;
        return minute + "min";
    }

    /**
     * @param time 时间
     * @return 20'29''
     */
    public static String second2MS_format2(long time) {
        if (time <= 0)
            return "0'0''";
        long minute = time / 60;
        long second = time % 60;
        return minute + "'" + second + "''";
    }

    /**
     * 获取星期几
     *
     * @param date
     * @return
     */
    public static String getWeekOfDate(Date date) {
        String[] weekDays = {"日", "一", "二", "三", "四", "五", "六"};
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int w = cal.get(Calendar.DAY_OF_WEEK) - 1;
        if (w < 0)
            w = 0;
        return weekDays[w];
    }

    public static int getWeek(String date) {
        Date date1 = stringToDate(date);
        Calendar c = Calendar.getInstance();
        c.setTime(date1);
        return c.get(Calendar.DAY_OF_WEEK) - 1;
    }


    /**
     * 判断是否是今天
     *
     * @param date
     * @return
     */
    public static boolean isThisTime(Date date, Date todayDate) {
        String param = DEL_FORMAT_DATE1.format(date);//参数时间
        String now = DEL_FORMAT_DATE1.format(todayDate);//当前时间
        if (param.equals(now)) {
            return true;
        }
        return false;
    }

    public static Date parseServerTime(String serverTime) {
        DEL_FORMAT_DATE1.setTimeZone(TimeZone.getTimeZone("GMT+8:00"));
        Date date = null;
        try {
            date = DEL_FORMAT_DATE1.parse(serverTime);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return date;
    }

    /**
     * 2021-09-01 转换成 09-01
     *
     * @param serverTime 2021-09-01
     * @return
     */
    public static String parseDateYmd2md(String serverTime) {
        DEL_FORMAT_DATE1.setTimeZone(TimeZone.getTimeZone("GMT+8:00"));
        String dateMd = serverTime;
        try {
            dateMd = INT_MONTH_DAY_FORMAT.format(DEL_FORMAT_DATE1.parse(serverTime));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return dateMd;
    }

    //出生年、月、日
    private static int year;
    private static int month;
    private static int day;

    /**
     * 根据生日获取年龄
     *
     * @param birthday
     * @return
     */
    public static Integer BirthdayToAge(String birthday) {
        stringToInt(birthday);
        // 得到当前时间的年、月、日
        Calendar cal = Calendar.getInstance();
        int yearNow = cal.get(Calendar.YEAR);
        int monthNow = cal.get(Calendar.MONTH) + 1;
        int dayNow = cal.get(Calendar.DATE);
        // 用当前年月日减去出生年月日
        int yearMinus = yearNow - year;
        int monthMinus = monthNow - month;
        int dayMinus = dayNow - day;
        int age = yearMinus;// 先大致赋值
        if (yearMinus <= 0) {
            age = 0;
            return age;
        }
        if (monthMinus < 0) {
            age = age - 1;
        } else if (monthMinus == 0) {
            if (dayMinus < 0) {
                age = age - 1;
            }
        }
        return age;
    }

    /**
     * String类型转换为long类型
     * .............................
     * strTime为要转换的String类型时间
     * formatType时间格式
     * formatType格式为yyyy-MM-dd HH:mm:ss//yyyy年MM月dd日 HH时mm分ss秒
     * strTime的时间格式和formatType的时间格式必须相同
     */
    private static void stringToInt(String strTime) {
        //String类型转换为date类型
        Calendar calendar = getYYMMDD(strTime);
        //date类型转成long类型
        year = calendar.get(Calendar.YEAR);
        month = calendar.get(Calendar.MONTH) + 1;
        day = calendar.get(Calendar.DAY_OF_MONTH);
    }

    /**
     * 2021-09-01 09:00 转换成 09:00
     *
     * @param serverTime 2021-09-01
     * @return
     */
    public static String parseDateYmdHm2Hm(String serverTime) {
        DEL_FORMAT_DATE2.setTimeZone(TimeZone.getTimeZone("GMT+8:00"));
        String dateMd = serverTime;
        try {
            dateMd = INT_HOUR_FORMAT.format(DEL_FORMAT_DATE2.parse(serverTime));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return dateMd;
    }


    /**
     * 时间戳转为 2022年4月
     *
     * @return string 格式
     */
    public static String long2YearMothChinese(long time) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time);
        return calendar.get(Calendar.YEAR) + "年" + (calendar.get(Calendar.MONTH) + 1) + "月";
    }

    /**
     * 时间戳转为 2022年4月
     *
     * @return
     */
    public static String long2MothDayChinese(long time) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time);
        return calendar.get(Calendar.MONTH) + 1 + "月" + calendar.get(Calendar.DAY_OF_MONTH) + "日";
    }

    /**
     * 年月日
     *
     * @param time 时间戳
     * @return
     */
    public static String long2YearMothDay(long time) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time);
        return calendar.get(Calendar.YEAR) + "-" + (calendar.get(Calendar.MONTH) + 1) + "-" + calendar.get(Calendar.DAY_OF_MONTH);
    }

    public static String long2HourMinute(long time) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time);
        String minute = calendar.get(Calendar.MINUTE) + "";
        if (calendar.get(Calendar.MINUTE) < 10) {
            minute = String.format("0%s", calendar.get(Calendar.MINUTE));
        }
        return calendar.get(Calendar.HOUR_OF_DAY) + ":" + minute;
    }

    /**
     * 是否大于等于今天
     */
    public static boolean isToday(String time) {
        Calendar todayCalendar = Calendar.getInstance();
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(stringToDate2(time).getTime());
        if (calendar.get(Calendar.YEAR) == (todayCalendar.get(Calendar.YEAR))) {
            int diffDay = todayCalendar.get(Calendar.DAY_OF_YEAR) - calendar.get(Calendar.DAY_OF_YEAR);
            return diffDay <= 0;
        }
        return false;
    }

    /**
     * 获取明日凌晨时间
     *
     * @return
     */
    public static long getNextEarlyMorning() {
        Calendar cal = Calendar.getInstance();
        //日期加1
        cal.add(Calendar.DAY_OF_YEAR, 1);
        //时间设定到8点整
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }


    public static int getTimeIsFutureOrOld(String time) {
        Date d = new Date();
        Date q = stringToDate(time);
        if (d.getTime() > q.getTime()) {
            return 1;//过去
        } else if (q.getTime() > d.getTime()) {
            return 2;//将来
        } else {
            return 3;//当天
        }
    }


    public static int getTimeIsFutureOrOldNYmd(String time) {
        Date d = new Date();
        String taday = getTime(d);
        Date q = stringToDate1(time);
        Date tadaySec = stringToDate1(taday);
        if (tadaySec.getTime() > q.getTime()) {
            return 1;//过去
        } else if (q.getTime() > d.getTime()) {
            return 2;//将来
        } else {
            return 3;//当天
        }
    }


    public static int getTimeIsFutureOrOld(Date time) {
        Date d = new Date();
        Date q = time;
        if (d.getTime() > q.getTime()) {
            return 1;//过去
        } else if (q.getTime() > d.getTime()) {
            return 2;//将来
        } else {
            return 3;//当天
        }
    }


}

