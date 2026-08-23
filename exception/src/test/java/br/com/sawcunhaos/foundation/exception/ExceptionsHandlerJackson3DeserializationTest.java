
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

package br.com.sawcunhaos.foundation.exception;

import br.com.sawcunhaos.foundation.core.enums.ScosExceptionCode;
import br.com.sawcunhaos.foundation.core.specification.LocaleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.json.JsonMapper;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Story 2.5: {@code handleHttpMessageNotReadable} used to extract {@code field}/{@code typesEnum}
 * by regexing {@code ex.getMessage()} — fragile because {@code Matcher.find()} stops at the FIRST
 * bracketed segment in the message, not necessarily the one describing the property that actually
 * failed. Confirmed here against real Jackson 3 ({@code tools.jackson.databind.exc}) messages.
 *
 * <p>Task 1 tests ({@code oldRegex*}) are a standalone baseline: they run the OLD regex directly
 * against a real captured Jackson 3 message, independent of the handler, and document the bug that
 * motivates Task 2. Task 2 tests ({@code extracts*}/{@code fallsBack*}) exercise the rewritten
 * handler.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ExceptionsHandlerJackson3DeserializationTest {

	@Mock LocaleService localeService;
	@InjectMocks ExceptionsHandler handler;

	private enum Color { RED, GREEN, BLUE }
	private record Leaf(Color color) {}
	private record Middle(Leaf leaf) {}
	private record Outer(Middle middle) {}

	private static final JsonMapper MAPPER = JsonMapper.builder().build();

	private static MismatchedInputException invalidEnum(String json, Class<?> type) {
		try {
			MAPPER.readValue(json, type);
		} catch (MismatchedInputException e) {
			return e;
		}
		throw new IllegalStateException("expected a MismatchedInputException from " + json);
	}

	// --- Task 1: baseline against the OLD regex, run directly against a real Jackson 3 message ---

	private static final String OLD_PATTERN_FIELD = "(\\[\\\"[\\w,\\s]+\\\"\\])";
	private static final String OLD_PATTERN_TYPE = "(\\[[\\w,\\s]+\\])";

	@Test
	@DisplayName("baseline: old regex still matches Jackson 3's message for a top-level enum field")
	void oldRegexMatchesTopLevelField() {
		MismatchedInputException ex = invalidEnum("{\"color\":\"PURPLE\"}", Leaf.class);
		String message = ex.getMessage();

		Matcher fieldMatcher = Pattern.compile(OLD_PATTERN_FIELD).matcher(message);
		Matcher typeMatcher = Pattern.compile(OLD_PATTERN_TYPE).matcher(message);

		assertTrue(fieldMatcher.find());
		assertEquals("[\"color\"]", fieldMatcher.group());
		assertTrue(typeMatcher.find());
		// Note: this is Jackson's internal (hash-based) ordering of the enum's valid values,
		// NOT declaration order (RED, GREEN, BLUE) — another reason not to parse it from text.
		assertEquals("[RED, BLUE, GREEN]", typeMatcher.group());
	}

	@Test
	@DisplayName("baseline: old regex grabs the WRONG (outermost) field for a nested enum - this is the bug")
	void oldRegexGrabsWrongFieldForNestedPath() {
		MismatchedInputException ex = invalidEnum(
				"{\"middle\":{\"leaf\":{\"color\":\"PURPLE\"}}}", Outer.class
		);
		String message = ex.getMessage();

		Matcher fieldMatcher = Pattern.compile(OLD_PATTERN_FIELD).matcher(message);
		assertTrue(fieldMatcher.find());
		// BUG: find() stops at the FIRST bracketed segment in the reference chain ("middle"),
		// not the leaf property that actually failed ("color").
		assertEquals("[\"middle\"]", fieldMatcher.group());
		assertNotEquals("[\"color\"]", fieldMatcher.group());

		// getPath() gives the correct leaf field - this is what Task 2 uses instead.
		assertEquals("color", ex.getPath().get(ex.getPath().size() - 1).getPropertyName());
	}

	// --- Task 2: the rewritten handler navigates the cause instead of regexing the message ---

	private void handle(HttpMessageNotReadableException ex) {
		when(localeService.getMessage(anyString(), any(Object[].class))).thenReturn("erro");
		ServletWebRequest request = new ServletWebRequest(new MockHttpServletRequest("POST", "/api/things"));
		handler.handleHttpMessageNotReadable(ex, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
	}

	@Test
	@DisplayName("extracts the leaf field (not the outermost one) and declaration-ordered enum values for a nested cause")
	void extractsLeafFieldForNestedCause() {
		MismatchedInputException cause = invalidEnum(
				"{\"middle\":{\"leaf\":{\"color\":\"PURPLE\"}}}", Outer.class
		);
		HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
				"bad request body", cause, mock(HttpInputMessage.class)
		);

		handle(ex);

		verify(localeService).getMessage(eq(ScosExceptionCode.ATTRIBUTE_NOT_VALID.getCode()), eq("color"), eq("[RED, GREEN, BLUE]"));
	}

	@Test
	@DisplayName("extracts the field for a top-level enum cause")
	void extractsFieldForTopLevelCause() {
		MismatchedInputException cause = invalidEnum("{\"color\":\"PURPLE\"}", Leaf.class);
		HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
				"bad request body", cause, mock(HttpInputMessage.class)
		);

		handle(ex);

		verify(localeService).getMessage(eq(ScosExceptionCode.ATTRIBUTE_NOT_VALID.getCode()), eq("color"), eq("[RED, GREEN, BLUE]"));
	}

	@Test
	@DisplayName("falls back to empty field/typesEnum when the cause isn't a Jackson deserialization exception")
	void fallsBackWhenCauseIsNotJacksonException() {
		HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
				"bad request body", new RuntimeException("boom"), mock(HttpInputMessage.class)
		);

		handle(ex);

		verify(localeService).getMessage(eq(ScosExceptionCode.ATTRIBUTE_NOT_VALID.getCode()), eq(""), eq(""));
	}

	@Test
	@DisplayName("falls back to empty field/typesEnum when there is no cause at all")
	void fallsBackWhenNoCause() {
		HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
				"bad request body", mock(HttpInputMessage.class)
		);

		handle(ex);

		verify(localeService).getMessage(eq(ScosExceptionCode.ATTRIBUTE_NOT_VALID.getCode()), eq(""), eq(""));
	}

	@Test
	@DisplayName("falls back to empty field (not null) when the last path reference is an array index, not a named property")
	void fallsBackToEmptyFieldForIndexOnlyPath() {
		// getPath()'s last Reference here has getPropertyName()==null/getIndex()==1 (an
		// invalid enum value inside an array element, not a named field) — field must
		// fall back to "", not the raw null, while typesEnum is still derived from the
		// array's element type.
		MismatchedInputException cause = invalidEnum("[\"RED\",\"PURPLE\"]", Color[].class);
		HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
				"bad request body", cause, mock(HttpInputMessage.class)
		);

		handle(ex);

		verify(localeService).getMessage(eq(ScosExceptionCode.ATTRIBUTE_NOT_VALID.getCode()), eq(""), eq("[RED, GREEN, BLUE]"));
	}

	@Test
	@DisplayName("bare top-level enum body: empty path but enum targetType -> field empty, typesEnum still populated")
	void emptyPathStillPopulatesTypesEnumForBareTopLevelEnum() {
		MismatchedInputException cause = invalidEnum("\"PURPLE\"", Color.class);
		HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
				"bad request body", cause, mock(HttpInputMessage.class)
		);

		handle(ex);

		verify(localeService).getMessage(eq(ScosExceptionCode.ATTRIBUTE_NOT_VALID.getCode()), eq(""), eq("[RED, GREEN, BLUE]"));
	}

	@Test
	@DisplayName("non-enum target type: field is extracted but typesEnum stays empty")
	void nonEnumTargetTypeLeavesTypesEnumEmpty() {
		MismatchedInputException cause = invalidEnum("{\"n\":\"notanumber\"}", NumHolder.class);
		HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
				"bad request body", cause, mock(HttpInputMessage.class)
		);

		handle(ex);

		verify(localeService).getMessage(eq(ScosExceptionCode.ATTRIBUTE_NOT_VALID.getCode()), eq("n"), eq(""));
	}

	private record NumHolder(int n) {}
}
