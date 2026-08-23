
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

import br.com.sawcunhaos.foundation.core.exception.ScosException;
import br.com.sawcunhaos.foundation.core.specification.ExceptionCode;
import br.com.sawcunhaos.foundation.core.specification.LocaleService;
import br.com.sawcunhaos.foundation.exception.error.ScosNoContentException;
import br.com.sawcunhaos.foundation.exception.error.ScosNoRollbackException;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies the status-range log policy (Story 2.4): fixed-{@code 4xx} handlers log
 * {@code WARN} without a stack trace, dynamic-status handlers ({@code ScosException}/
 * {@code ScosNoRollbackException}) resolve {@code WARN}/{@code ERROR} from their HTTP
 * status, and {@code ScosNoContentException} logs {@code DEBUG}. Captured via a
 * {@link ListAppender} attached to {@link ExceptionsHandler}'s {@code @Slf4j} logger.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ExceptionsHandlerLogLevelTest {

    @Mock LocaleService localeService;
    @InjectMocks ExceptionsHandler handler;

    private ListAppender<ILoggingEvent> appender;
    private Logger logger;
    private Level originalLevel;

    @BeforeEach
    void attachAppender() {
        logger = (Logger) LoggerFactory.getLogger(ExceptionsHandler.class);
        originalLevel = logger.getLevel();
        logger.setLevel(Level.DEBUG);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        when(localeService.getMessage(anyString(), any(Object[].class))).thenReturn("erro");
    }

    @AfterEach
    void detachAppender() {
        logger.detachAppender(appender);
        logger.setLevel(originalLevel);
    }

    private void assertLogged(Level expectedLevel, boolean expectStackTrace) {
        assertFalse(appender.list.isEmpty(), "expected a log event but none was captured");
        ILoggingEvent event = appender.list.get(appender.list.size() - 1);
        assertEquals(expectedLevel, event.getLevel());
        if (expectStackTrace) {
            assertNotNull(event.getThrowableProxy());
        } else {
            assertNull(event.getThrowableProxy());
        }
    }

    @Test
    @DisplayName("4xx ScosException -> WARN without stack trace")
    void fourXxLogsWarnWithoutStackTrace() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/persons");
        ScosException exception = new ScosException(code("T-400", 400));

        handler.handleScosException(exception, request);

        assertLogged(Level.WARN, false);
    }

    @Test
    @DisplayName("5xx ScosException -> ERROR with stack trace")
    void fiveXxLogsErrorWithStackTrace() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/persons");
        ScosException exception = new ScosException(code("T-500", 500));

        handler.handleScosException(exception, request);

        assertLogged(Level.ERROR, true);
    }

    @Test
    @DisplayName("unresolvable HTTP code falls back to 400 -> WARN without stack trace")
    void unresolvableCodeFallsBackToWarn() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/persons");
        ScosException exception = new ScosException(code("T-999", 999));

        handler.handleScosException(exception, request);

        assertLogged(Level.WARN, false);
    }

    @Test
    @DisplayName("4xx ScosNoRollbackException -> WARN without stack trace")
    void noRollbackFourXxLogsWarnWithoutStackTrace() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/persons");
        ScosNoRollbackException exception = new ScosNoRollbackException(code("T-400", 400));

        handler.handleScosNoRollbackException(exception, request);

        assertLogged(Level.WARN, false);
    }

    @Test
    @DisplayName("5xx ScosNoRollbackException -> ERROR with stack trace")
    void noRollbackFiveXxLogsErrorWithStackTrace() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/persons");
        ScosNoRollbackException exception = new ScosNoRollbackException(code("T-500", 500));

        handler.handleScosNoRollbackException(exception, request);

        assertLogged(Level.ERROR, true);
    }

    @Test
    @DisplayName("ScosNoContentException -> DEBUG with stack trace kept")
    void noContentLogsDebug() {
        handler.handleScosNoContentException(new ScosNoContentException());

        assertLogged(Level.DEBUG, true);
    }

    @Test
    @DisplayName("handleHttpMessageNotReadable -> WARN without stack trace")
    void httpMessageNotReadableLogsWarn() {
        HttpMessageNotReadableException ex = mock(HttpMessageNotReadableException.class);
        when(ex.getMessage()).thenReturn("Cannot deserialize value");
        ServletWebRequest request = new ServletWebRequest(new MockHttpServletRequest("POST", "/api/persons"));

        handler.handleHttpMessageNotReadable(ex, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);

        assertLogged(Level.WARN, false);
    }

    @Test
    @DisplayName("handleMethodArgumentNotValid -> WARN without stack trace")
    void methodArgumentNotValidLogsWarn() {
        BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "target");
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        ServletWebRequest request = new ServletWebRequest(new MockHttpServletRequest("POST", "/api/persons"));

        handler.handleMethodArgumentNotValid(ex, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);

        assertLogged(Level.WARN, false);
    }

    @Test
    @DisplayName("handleHandlerMethodValidationException -> WARN without stack trace")
    void handlerMethodValidationLogsWarn() {
        HandlerMethodValidationException ex = mock(HandlerMethodValidationException.class);
        when(ex.getBeanResults()).thenReturn(List.of());
        when(ex.getValueResults()).thenReturn(List.of());
        ServletWebRequest request = new ServletWebRequest(new MockHttpServletRequest("GET", "/api/persons"));

        handler.handleHandlerMethodValidationException(ex, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);

        assertLogged(Level.WARN, false);
    }

    @Test
    @DisplayName("handleConstraintViolationException -> WARN without stack trace")
    void constraintViolationLogsWarn() {
        ConstraintViolationException ex = new ConstraintViolationException(Collections.emptySet());
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/persons");

        handler.handleConstraintViolationException(ex, request);

        assertLogged(Level.WARN, false);
    }

    @Test
    @DisplayName("handleAccessDeniedException(AccessDeniedException) -> WARN without stack trace")
    void accessDeniedLogsWarn() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin");

        handler.handleAccessDeniedException(new AccessDeniedException("denied"), request);

        assertLogged(Level.WARN, false);
    }

    @Test
    @DisplayName("handleAccessDeniedException(AuthorizationDeniedException) -> WARN without stack trace")
    void authorizationDeniedLogsWarn() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin");

        handler.handleAccessDeniedException(new AuthorizationDeniedException("denied"), request);

        assertLogged(Level.WARN, false);
    }

    private ExceptionCode code(String code, int httpCode) {
        return new ExceptionCode() {
            @Override
            public String getCode() {
                return code;
            }

            @Override
            public int getHttpCode() {
                return httpCode;
            }
        };
    }
}
