
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

package br.com.sawcunhaos.foundation.privacy.core;

/**
 * Check-digit validators for numeric documents.
 *
 * <p>Builtin patterns such as {@code credit-card}, {@code cpf} and {@code cnpj} are matched by a broad regex
 * first (cheap) and then confirmed by the relevant check-digit here. This second step removes most false
 * positives (random digit runs) before anything is masked, keeping the hot path correct and quiet.</p>
 */
public final class DocumentValidators {

    private DocumentValidators() {
    }

    /**
     * Validates a credit-card number using the Luhn algorithm (ignoring spaces/hyphens).
     *
     * @param value the candidate string
     * @return {@code true} when the digits pass Luhn
     */
    public static boolean isLuhnValid(final String value) {
        final String digits = digitsOnly(value);
        if (digits.length() < 13 || digits.length() > 19) {
            return false;
        }
        int sum = 0;
        boolean alternate = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int d = digits.charAt(i) - '0';
            if (alternate) {
                d *= 2;
                if (d > 9) {
                    d -= 9;
                }
            }
            sum += d;
            alternate = !alternate;
        }
        return sum % 10 == 0;
    }

    /**
     * Validates a Brazilian CPF (11 digits) by its two check digits.
     *
     * @param value the candidate string (formatting is ignored)
     * @return {@code true} when the CPF check digits are valid
     */
    public static boolean isCpfValid(final String value) {
        final String d = digitsOnly(value);
        if (d.length() != 11 || allSame(d)) {
            return false;
        }
        final int dv1 = cpfDigit(d, 9, 10);
        final int dv2 = cpfDigit(d, 10, 11);
        return dv1 == (d.charAt(9) - '0') && dv2 == (d.charAt(10) - '0');
    }

    /**
     * Validates a Brazilian CNPJ (14 digits) by its two check digits.
     *
     * @param value the candidate string (formatting is ignored)
     * @return {@code true} when the CNPJ check digits are valid
     */
    public static boolean isCnpjValid(final String value) {
        final String d = digitsOnly(value);
        if (d.length() != 14 || allSame(d)) {
            return false;
        }
        final int[] w1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        final int[] w2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        final int dv1 = cnpjDigit(d, w1);
        final int dv2 = cnpjDigit(d, w2);
        return dv1 == (d.charAt(12) - '0') && dv2 == (d.charAt(13) - '0');
    }

    /**
     * Dispatches to the named validator. Unknown / null validator means "no check" (always valid).
     *
     * @param validator one of {@code "luhn"}, {@code "cpf"}, {@code "cnpj"} or {@code null}
     * @param value the candidate string
     * @return {@code true} when the value passes (or when no validator applies)
     */
    public static boolean passes(final String validator, final String value) {
        if (validator == null) {
            return true;
        }
        return switch (validator) {
            case "luhn" -> isLuhnValid(value);
            case "cpf" -> isCpfValid(value);
            case "cnpj" -> isCnpjValid(value);
            default -> true;
        };
    }

    private static int cpfDigit(final String d, final int len, final int startWeight) {
        int sum = 0;
        for (int i = 0; i < len; i++) {
            sum += (d.charAt(i) - '0') * (startWeight - i);
        }
        final int mod = sum % 11;
        return mod < 2 ? 0 : 11 - mod;
    }

    private static int cnpjDigit(final String d, final int[] weights) {
        int sum = 0;
        for (int i = 0; i < weights.length; i++) {
            sum += (d.charAt(i) - '0') * weights[i];
        }
        final int mod = sum % 11;
        return mod < 2 ? 0 : 11 - mod;
    }

    private static boolean allSame(final String d) {
        for (int i = 1; i < d.length(); i++) {
            if (d.charAt(i) != d.charAt(0)) {
                return false;
            }
        }
        return true;
    }

    private static String digitsOnly(final String value) {
        if (value == null) {
            return "";
        }
        final StringBuilder sb = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            final char c = value.charAt(i);
            if (c >= '0' && c <= '9') {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
