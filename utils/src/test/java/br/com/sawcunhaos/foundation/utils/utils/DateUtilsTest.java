package br.com.sawcunhaos.foundation.utils.utils;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DateUtilsTest {

    private final static LocalDate DATE_WEENKEND_SATURDAY = LocalDate.of(2022,8,27);
    private final static LocalDate DATE_WEENKEND_SUNDAY = LocalDate.of(2022,8,28);
    private final static LocalDateTime DATE_WEENKEND_SUNDAY_TIME = LocalDateTime.of(2022,8,28, 18, 59, 25);
    private final static LocalDate DATE_NOT_WEENKEND = LocalDate.of(2022,8,26);
    private final static LocalDateTime DATE_NOW = LocalDateTime.of(2022,8,26, 15, 25, 55);

    @Test
    void isWeenkend() {
        assertTrue(DateUtils.isWeenkend(DATE_WEENKEND_SATURDAY));
        assertTrue(DateUtils.isWeenkend(DATE_WEENKEND_SUNDAY));
        assertFalse(DateUtils.isWeenkend(DATE_NOT_WEENKEND));
    }

    @Test
    void plusYear() {
        LocalDate localDateExpected = DATE_WEENKEND_SATURDAY.plusYears(1);
        LocalDate localDateResult = DateUtils.plusYear(DATE_WEENKEND_SATURDAY, 1);

        assertEquals(localDateExpected, localDateResult);
        assertEquals(localDateExpected.getYear(), localDateResult.getYear());
    }

    @Test
    void minusYear() {
        LocalDate localDateExpected = DATE_NOT_WEENKEND.minusYears(1);
        LocalDate localDateResult = DateUtils.minusYear(DATE_NOT_WEENKEND, 1);

        assertEquals(localDateExpected, localDateResult);
        assertEquals(localDateExpected.getYear(), localDateResult.getYear());
    }

    @Test
    void returnYearPlus() {
        String yearExpected = String.format("%s",DATE_NOT_WEENKEND.plusYears(1).getYear());
        String yearResult = DateUtils.returnYearPlus(DATE_NOT_WEENKEND, 1);

        assertEquals(yearExpected, yearResult);

        String yearStringExpected = String.format("%s",DATE_NOT_WEENKEND.plusYears(1).getYear());
        String yearStringResult = DateUtils.returnYearPlus(DATE_NOT_WEENKEND, "1");

        assertEquals(yearStringExpected, yearStringResult);
    }

    @Test
    void returnYearMinus() {
        String yearExpected = String.format("%s",DATE_NOT_WEENKEND.minusYears(1).getYear());
        String yearResult = DateUtils.returnYearMinus(DATE_NOT_WEENKEND, 1);

        assertEquals(yearExpected, yearResult);

        String yearStringExpected = String.format("%s",DATE_NOT_WEENKEND.minusYears(1).getYear());
        String yearStringResult = DateUtils.returnYearMinus(DATE_NOT_WEENKEND, "1");

        assertEquals(yearStringExpected, yearStringResult);
    }

    @Test
    void isDateValidWithDay20AndMoth4() {
        assertTrue(DateUtils.isDateValid(20, 4));
    }

    @Test
    void isDateValidWithDay31AndMoth5() {
        assertTrue(DateUtils.isDateValid(31, 5));
    }

    @Test
    void isDateValidWithDay30AndMoth5() {
        assertTrue(DateUtils.isDateValid(30, 5));
    }

    @Test
    void isDateValidWithDay30AndMoth2() {
        assertFalse(DateUtils.isDateValid(30, 2));
    }

    @Test
    void isDateValidWithDay31AndMoth4() {
        assertFalse(DateUtils.isDateValid(31, 4));
    }

    @Test
    void returnDateCurrent() {
        try (MockedStatic<LocalDateTime> utilities = Mockito.mockStatic(LocalDateTime.class)) {
            utilities.when(LocalDateTime::now).thenReturn(DATE_NOW);

            String dateExpected = DATE_NOW.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
            String dateResult = DateUtils.returnDateCurrent();
            assertEquals(dateExpected, dateResult);
        }
    }

    @Test
    void returnDate() {
        String dateExpected = "2022-08-28";
        String dateResult = DateUtils.returnDate(DATE_WEENKEND_SUNDAY);

        assertEquals(dateExpected, dateResult);
    }

    @Test
    void returnCurrentDate() {
        LocalDate dateExpected = LocalDate.now();
        LocalDate dateResult = DateUtils.getDateCurrent();

        assertEquals(dateExpected, dateResult);
    }

    @Test
    void returnDateTime() {
        String dateExpected = "2022-08-28 18:59:25.000";
        String dateResult = DateUtils.returnDate(DATE_WEENKEND_SUNDAY_TIME);

        assertEquals(dateExpected, dateResult);
    }

    @Test
    void returnIsValidDate() {
        boolean isValid = DateUtils.isDateValid(29,2,2024);

        assertTrue(isValid);
    }

    @Test
    void returnIsNotValidDate() {
        boolean isValid = DateUtils.isDateValid(29,2,2023);

        assertFalse(isValid);
    }
}
