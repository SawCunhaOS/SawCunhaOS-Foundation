
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


import br.com.sawcunhaos.foundation.utils.exception.ScosException;
import br.com.sawcunhaos.foundation.exception.error.ScosNoContentException;
import br.com.sawcunhaos.foundation.exception.error.ScosNoRollbackException;
import br.com.sawcunhaos.foundation.exception.model.ScosFieldError;
import br.com.sawcunhaos.foundation.exception.model.ScosProblemDetails;
import br.com.sawcunhaos.foundation.exception.utils.ExceptionUtils;
import br.com.sawcunhaos.foundation.utils.enums.ScosExceptionCode;
import br.com.sawcunhaos.foundation.utils.specification.LocaleService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.nio.file.AccessDeniedException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static br.com.sawcunhaos.foundation.exception.utils.ExceptionUtils.getArgsValidation;

/**
 * Global error handler that renders every error as an RFC 9457
 * {@link ProblemDetail} ({@code Content-Type: application/problem+json}),
 * replacing the legacy {@code ScosResponseDTO} error wrapper.
 *
 * <p>Field mapping from the previous proprietary format (no data is lost):</p>
 * <ul>
 *   <li>{@code data.message} → {@code detail}</li>
 *   <li>{@code data.codeError} → {@code code} (extension)</li>
 *   <li>{@code data.validationErrors[].attribute} → {@code errors[].pointer} (JSON Pointer)</li>
 *   <li>{@code data.validationErrors[].message} → {@code errors[].detail}</li>
 * </ul>
 *
 * <p>Plus the additions {@code type}, {@code title}, {@code status},
 * {@code instance}, {@code requestId} and {@code timestamp}.</p>
 */
@ControllerAdvice
@Log4j2
@RequiredArgsConstructor
public class ExceptionsHandler extends ResponseEntityExceptionHandler {

	private final LocaleService localeService;

	@Override
	protected ResponseEntity<Object> handleHttpMessageNotReadable(
			HttpMessageNotReadableException ex,
			HttpHeaders headers,
			HttpStatusCode status,
			WebRequest request
	) {
		log.error("handleSecurity - handleHttpMessageNotReadable: ", ex);

		String field = "", typesEnum = "";
		String patternField = "(\\[\\\"[\\w,\\s]+\\\"\\])";
		String patternType = "(\\[[\\w,\\s]+\\])";

		Pattern pattern = Pattern.compile(patternField);
		Matcher matcher = pattern.matcher(ex.getMessage());
		if(matcher.find()){
			field = matcher.group().replaceAll("([\\[\\\"\\]])","");
		}
		pattern = Pattern.compile(patternType);
		matcher = pattern.matcher(ex.getMessage());
		if(matcher.find()){
			typesEnum = matcher.group();
		}

		String message = localeService.getMessage(ScosExceptionCode.ATTRIBUTE_NOT_VALID.getCode(), field, typesEnum);

		ProblemDetail problem = enrich(
				ScosProblemDetails.of(status, ScosExceptionCode.ENUM_ERROR, message, requestUri(request))
		);
		return ResponseEntity.status(status).body(problem);
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(
			MethodArgumentNotValidException ex,
			HttpHeaders headers,
			HttpStatusCode status,
			WebRequest request
	) {
		log.error("handleSecurity - handleMethodArgumentNotValid: ", ex);

		List<ScosFieldError> errors = new ArrayList<>();
		ex.getBindingResult().getFieldErrors().forEach(
				e -> errors.add(ScosFieldError.of(
						e.getField(),
						localeService.getMessage(e.getDefaultMessage(), getArgsValidation(e.getArguments())),
						e.getDefaultMessage()
				))
		);

		String message = localeService.getMessage(
				ScosExceptionCode.ATTRIBUTE_NOT_VALID.getCode(),
				requestUri(request)
		);

		ProblemDetail problem = enrich(
				ScosProblemDetails.ofValidation(
						status, ScosExceptionCode.ATTRIBUTE_NOT_VALID, message, requestUri(request), errors
				)
		);
		return ResponseEntity.status(status).body(problem);
	}

	@Override
	protected ResponseEntity<Object> handleHandlerMethodValidationException(
			HandlerMethodValidationException ex,
			HttpHeaders headers,
			HttpStatusCode status,
			WebRequest request
	) {
		log.error("handleSecurity - handleHandlerMethodValidationException: ", ex);

		List<ScosFieldError> errors = new ArrayList<>();
		ex.getBeanResults().get(0).getFieldErrors().forEach(
				e -> errors.add(ScosFieldError.of(
						e.getField(),
						localeService.getMessage(e.getDefaultMessage(), getArgsValidation(e.getArguments())),
						e.getDefaultMessage()
				))
		);

		String message = localeService.getMessage(
				ScosExceptionCode.ATTRIBUTE_NOT_VALID.getCode(),
				requestUri(request)
		);

		ProblemDetail problem = enrich(
				ScosProblemDetails.ofValidation(
						status, ScosExceptionCode.ATTRIBUTE_NOT_VALID, message, requestUri(request), errors
				)
		);
		return ResponseEntity.status(status).body(problem);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	protected ResponseEntity<ProblemDetail> handleConstraintViolationException(
			ConstraintViolationException exception,
			HttpServletRequest request
	) {
		log.error("handleSecurity - ConstraintViolationException: ", exception);

		List<ScosFieldError> errors = new ArrayList<>();
		exception.getConstraintViolations().forEach(
				e -> {
					String[] path = e.getPropertyPath().toString().split("\\.");
					List<String> attributes = new ArrayList<>();
					attributes.add(path[path.length-1]);
					attributes.addAll(
							ExceptionUtils.findValuesAnnotation(e.getConstraintDescriptor().getAnnotation())
					);

					errors.add(ScosFieldError.of(
							attributes.get(0),
							localeService.getMessage(e.getMessage(), attributes.toArray(Object[]::new)),
							e.getMessage()
					));
				}
		);

		String message = localeService.getMessage(ScosExceptionCode.ATTRIBUTE_NOT_VALID.getCode());

		ProblemDetail problem = enrich(
				ScosProblemDetails.ofValidation(
						HttpStatus.BAD_REQUEST, ScosExceptionCode.ATTRIBUTE_NOT_VALID,
						message, request.getRequestURI(), errors
				)
		);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
	}

	@ExceptionHandler(ScosException.class)
	protected ResponseEntity<ProblemDetail> handleScosException(ScosException exception, HttpServletRequest request){
		log.error("handleSecurity - ScosException: ", exception);
		String detail = localeService.getMessage(exception.getCode(), exception.getArgs());
		ProblemDetail problem = enrich(
				ScosProblemDetails.of(
						HttpStatus.BAD_REQUEST, exception.getCode(), "Business Error",
						detail, request.getRequestURI()
				)
		);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
	}

	@ExceptionHandler(ScosNoRollbackException.class)
	protected ResponseEntity<ProblemDetail> handleScosNoRollbackException(
			ScosNoRollbackException exception, HttpServletRequest request
	){
		log.error("handleSecurity - ScosNoRollbackException: ", exception);
		String detail = localeService.getMessage(exception.getCode());
		ProblemDetail problem = enrich(
				ScosProblemDetails.of(
						HttpStatus.BAD_REQUEST, exception.getCode(), "Business Error",
						detail, request.getRequestURI()
				)
		);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
	}

	@ExceptionHandler(ScosNoContentException.class)
	@ResponseStatus(HttpStatus.NO_CONTENT)
	protected void handleScosNoContentException(ScosNoContentException exception){
		log.error("handleSecurity - ScosNoContentException: ", exception);
	}

	@ExceptionHandler(AccessDeniedException.class)
	protected ResponseEntity<ProblemDetail> handleAccessDeniedException(
			AccessDeniedException ex, HttpServletRequest request
	) {
		log.error("handleSecurity - AccessDeniedException: ", ex);
		String detail = localeService.getMessage(ScosExceptionCode.ACCESS_DENIED.getCode());
		ProblemDetail problem = enrich(
				ScosProblemDetails.of(
						HttpStatus.FORBIDDEN, ScosExceptionCode.ACCESS_DENIED, detail, request.getRequestURI()
				)
		);
		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem);
	}

	@ExceptionHandler(Exception.class)
	protected ResponseEntity<ProblemDetail> handleGenericException(Exception ex, HttpServletRequest request) {
		log.error("handleSecurity - Unhandled exception: ", ex);
		String detail = localeService.getMessage(ScosExceptionCode.GENERIC.getCode());
		ProblemDetail problem = enrich(
				ScosProblemDetails.of(
						HttpStatus.INTERNAL_SERVER_ERROR, ScosExceptionCode.GENERIC, detail, request.getRequestURI()
				)
		);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
	}

	/**
	 * Centralizes correlation/traceability enrichment of every problem produced
	 * by this advice: copies the MDC {@code X-Request-ID} into {@code requestId}
	 * (when present) and stamps an ISO-8601 UTC {@code timestamp}. Delegates to
	 * {@link ScosProblemDetails#enrich(ProblemDetail)} so the web advice and the
	 * security handlers stay byte-for-byte consistent.
	 */
	private ProblemDetail enrich(ProblemDetail problem) {
		return ScosProblemDetails.enrich(problem);
	}

	private String requestUri(WebRequest request) {
		return ((ServletWebRequest) request).getRequest().getRequestURI();
	}
}
