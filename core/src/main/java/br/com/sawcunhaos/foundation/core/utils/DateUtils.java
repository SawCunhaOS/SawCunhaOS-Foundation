
/*
 *
 *  * Copyright 2026 SawCunha Open System - SawCunhaOS-Foundation
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *
 */

package br.com.sawcunhaos.foundation.core.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

/**
 * Date helpers used across the foundation for weekend checks, year shifting,
 * naive day/month validation and default string formatting.
 *
 * @since 1.2.0
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DateUtils {

	private static final int MONTH_31_DAY = 31;
	private static final int MONTH_30_DAY = 30;
	private static final int FEBRUARY = 2;
	private static final int FEBRUARY_DAY = 29;
	private static final List<Integer> MONTHS_31_DAYS = List.of(1,3,5,7,8,10,12);
	private static final List<Integer> MONTHS_30_DAYS = List.of(4,6,9,11);
	private static final DateTimeFormatter FORMATER_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
	private static final DateTimeFormatter FORMATER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Whether {@code ld} falls on a Saturday or Sunday.
     *
     * <p>Note: the method name has a historical typo ({@code isWeenkend}, not
     * {@code isWeekend}). It is kept as-is because it is public API and no
     * consumer of it was found in this reactor to confirm a safe rename.
     *
     * @param ld the date to check
     * @return {@code true} if {@code ld} is a Saturday or Sunday
     */
    public static boolean isWeenkend(LocalDate ld) {
        DayOfWeek d = ld.getDayOfWeek();
        return d == DayOfWeek.SATURDAY || d == DayOfWeek.SUNDAY;
    }

    /**
     * Adds {@code year} years to {@code date}.
     *
     * @param date the base date
     * @param year the number of years to add
     * @return {@code date} plus {@code year} years
     */
    public static LocalDate plusYear(LocalDate date, int year){
        return date.plusYears(year);
    }

    /**
     * Subtracts {@code year} years from {@code date}.
     *
     * @param date the base date
     * @param year the number of years to subtract
     * @return {@code date} minus {@code year} years
     */
    public static LocalDate minusYear(LocalDate date, int year){
        return date.minusYears(year);
    }

    /**
     * The year of {@code date} plus {@code year} years, as a string.
     *
     * @param date the base date
     * @param year the number of years to add
     * @return the resulting year, as a string
     */
    public static String returnYearPlus(LocalDate date, int year){
        return String.format("%s",plusYear(date, year).getYear());
    }

    /**
     * The year of {@code date} minus {@code year} years, as a string.
     *
     * @param date the base date
     * @param year the number of years to subtract
     * @return the resulting year, as a string
     */
    public static String returnYearMinus(LocalDate date, int year){
        return String.format("%s",minusYear(date, year).getYear());
    }

    /**
     * Same as {@link #returnYearPlus(LocalDate, int)}, taking {@code year} as a string.
     *
     * @param date the base date
     * @param year the number of years to add, as a string
     * @return the resulting year, as a string
     */
    public static String returnYearPlus(LocalDate date, String year){
        return returnYearPlus(date,Integer.parseInt(year));
    }

    /**
     * Same as {@link #returnYearMinus(LocalDate, int)}, taking {@code year} as a string.
     *
     * @param date the base date
     * @param year the number of years to subtract, as a string
     * @return the resulting year, as a string
     */
    public static String returnYearMinus(LocalDate date, String year){
        return returnYearMinus(date,Integer.parseInt(year));
    }

    /**
     * Naively checks whether {@code day}/{@code month} could be a valid calendar
     * date, without a year.
     *
     * <p>Because there is no year, February 29 is <strong>always</strong> accepted,
     * regardless of leap years — unlike {@link #isDateValid(Integer, Integer, Integer)},
     * which does know the year and is leap-year-aware. Prefer the 3-argument overload
     * whenever a year is available.
     *
     * <p><strong>No lower-bound check</strong>: {@code day} is only checked against an
     * upper bound per month, so {@code 0} or a negative {@code day} is accepted as valid
     * (e.g. {@code isDateValid(0, 1)} and {@code isDateValid(-5, 3)} both return
     * {@code true}). Callers that need a real calendar date should use
     * {@link #isDateValid(Integer, Integer, Integer)} instead.
     *
     * @param day   the day of month
     * @param month the month (1-12)
     * @return {@code true} if {@code day} is a plausible day for {@code month}
     */
    public static boolean isDateValid(Integer day, Integer month){
        if(day <= (MONTH_31_DAY) && MONTHS_31_DAYS.contains(month)) return true;
        // Unreachable: day <= MONTH_30_DAY implies day <= MONTH_31_DAY, already handled above.
        if(day <= (MONTH_30_DAY) && MONTHS_31_DAYS.contains(month)) return true;
        if(day <= (MONTH_30_DAY) && MONTHS_30_DAYS.contains(month)) return true;
        return month.equals(FEBRUARY) && day <= FEBRUARY_DAY;
    }

    /**
     * Checks whether {@code day}/{@code month}/{@code year} is a real calendar date,
     * via {@link LocalDate#of}, so leap years are handled correctly.
     *
     * @param day   the day of month
     * @param month the month (1-12)
     * @param year  the year
     * @return {@code true} if the combination is a valid calendar date
     */
    public static boolean isDateValid(Integer day, Integer month, Integer year){
        try {
            LocalDate.of(year, month, day);
        }catch (DateTimeException dateTimeException){
            return false;
        }
        return true;
    }

	/**
	 * The current date and time, formatted as {@code yyyy-MM-dd HH:mm:ss.SSS}.
	 *
	 * @return the current timestamp, formatted
	 */
	public static String returnDateCurrent(){
		return LocalDateTime.now().format(FORMATER_TIME);
	}

    /**
     * The current date.
     *
     * @return today, as a {@link LocalDate}
     */
    public static LocalDate getDateCurrent(){
        return LocalDate.now();
    }

    /**
     * Formats {@code date} as {@code yyyy-MM-dd}.
     *
     * @param date the date to format, may be {@code null}
     * @return the formatted date, or {@code null} if {@code date} is {@code null}
     */
    public static String returnDate(final LocalDate date){
        if(Objects.isNull(date)) return null;
        return date.format(FORMATER);
    }

    /**
     * Formats {@code date} as {@code yyyy-MM-dd HH:mm:ss.SSS}.
     *
     * @param date the date/time to format, may be {@code null}
     * @return the formatted date/time, or {@code null} if {@code date} is {@code null}
     */
    public static String returnDate(final LocalDateTime date){
        if(Objects.isNull(date)) return null;
        return date.format(FORMATER_TIME);
    }
}
