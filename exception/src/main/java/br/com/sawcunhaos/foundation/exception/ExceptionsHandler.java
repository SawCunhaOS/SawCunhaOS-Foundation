
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
import br.com.sawcunhaos.foundation.core.exception.MethodNotImplementedException;
import br.com.sawcunhaos.foundation.core.exception.ScosNoContentException;
import br.com.sawcunhaos.foundation.core.exception.ScosNoRollbackException;
import br.com.sawcunhaos.foundation.exception.model.ScosFieldError;
import br.com.sawcunhaos.foundation.exception.model.ScosProblemDetails;
import br.com.sawcunhaos.foundation.exception.utils.ExceptionUtils;
import br.com.sawcunhaos.foundation.core.enums.ScosExceptionCode;
import br.com.sawcunhaos.foundation.core.specification.LocaleService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.MismatchedInputException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
@Slf4j
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
		log.warn("handleSecurity - handleHttpMessageNotReadable: {}", ex.getMessage());

		String field = "", typesEnum = "";
		// Jackson 3 (tools.jackson.databind.exc): InvalidFormatException IS-A
		// MismatchedInputException, so this one check covers both. Navigates the
		// structured cause (getPath()/getTargetType()) instead of regexing
		// ex.getMessage(), which was fragile (e.g. it matched the FIRST bracketed
		// segment in a nested reference chain, not the leaf field that actually failed).
		if (ex.getCause() instanceof MismatchedInputException mie) {
			List<JacksonException.Reference> path = mie.getPath();
			if (!path.isEmpty()) {
				String propertyName = path.get(path.size() - 1).getPropertyName();
				field = propertyName != null ? propertyName : "";
			}
			Class<?> targetType = mie.getTargetType();
			if (targetType != null && targetType.isEnum()) {
				// Enum::name, not Object::toString: an enum overriding toString() for a
				// human-readable label would otherwise report the wrong literal — Jackson
				// matches enum deserialization against the constant name, not toString().
				typesEnum = Arrays.stream(targetType.getEnumConstants())
						.map(constant -> ((Enum<?>) constant).name())
						.collect(Collectors.joining(", ", "[", "]"));
			}
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
		log.warn("handleSecurity - handleMethodArgumentNotValid: {}", ex.getMessage());

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
		log.warn("handleSecurity - handleHandlerMethodValidationException: {}", ex.getMessage());

		List<ScosFieldError> errors = new ArrayList<>();
		// getBeanResults(): violações em parâmetro anotado @Valid (bean), expõe getFieldErrors()
		ex.getBeanResults().forEach(
				beanResult -> beanResult.getFieldErrors().forEach(
						e -> errors.add(ScosFieldError.of(
								e.getField(),
								localeService.getMessage(e.getDefaultMessage(), getArgsValidation(e.getArguments())),
								e.getDefaultMessage()
						))
				)
		);
		// getValueResults(): violações em parâmetro simples anotado direto (ex.: @RequestParam @Min(1) int page),
		// sem getFieldErrors() — expõe o parâmetro e a lista de MessageSourceResolvable das violações
		ex.getValueResults().forEach(valueResult -> {
			String field = valueResult.getMethodParameter().getParameterName();
			valueResult.getResolvableErrors().forEach(
					e -> errors.add(ScosFieldError.of(
							field != null ? field : String.valueOf(valueResult.getMethodParameter().getParameterIndex()),
							localeService.getMessage(e.getDefaultMessage(), getArgsValidation(e.getArguments())),
							e.getDefaultMessage()
					))
			);
		});

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

	/**
	 * Catch-all hook of {@link ResponseEntityExceptionHandler}: every exception it
	 * handles internally that does NOT have a more specific {@code @Override} in
	 * this class (e.g. {@code NoResourceFoundException} for an unmatched route,
	 * {@code HttpRequestMethodNotSupportedException}, {@code HttpMediaTypeNotAcceptableException})
	 * funnels through here. Without this override, those cases fell back to Spring's
	 * default rendering instead of {@link ScosProblemDetails}. Reuses the {@code statusCode}
	 * and {@code ex.getMessage()} Spring already resolved instead of re-deriving them.
	 */
	@Override
	protected ResponseEntity<Object> handleExceptionInternal(
			Exception ex,
			Object body,
			HttpHeaders headers,
			HttpStatusCode statusCode,
			WebRequest request
	) {
		// Mirrors the isCommitted() guard in ResponseEntityExceptionHandler's own default
		// implementation, which this override replaces entirely: writing a body to an
		// already-committed response throws IllegalStateException.
		if (request instanceof ServletWebRequest servletWebRequest) {
			HttpServletResponse response = servletWebRequest.getResponse();
			if (response != null && response.isCommitted()) {
				log.warn("handleSecurity - handleExceptionInternal: response already committed, ignoring {}", ex.toString());
				return null;
			}
		}

		HttpStatus status = HttpStatus.resolve(statusCode.value());
		if (status != null) {
			logByStatus(status, "handleExceptionInternal", ex);
		} else {
			log.warn("handleSecurity - handleExceptionInternal: {}", ex.getMessage());
		}

		String title = status != null ? status.getReasonPhrase() : ScosExceptionCode.GENERIC.getTitle();
		String rawDetail = ex.getMessage();
		String detail = (rawDetail != null && !rawDetail.isBlank()) ? rawDetail : title;

		ProblemDetail problem = enrich(
				ScosProblemDetails.of(
						statusCode, ScosExceptionCode.GENERIC.getCode(), title, detail, requestUri(request)
				)
		);
		return ResponseEntity.status(statusCode).headers(headers).body(problem);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	protected ResponseEntity<ProblemDetail> handleConstraintViolationException(
			ConstraintViolationException exception,
			HttpServletRequest request
	) {
		log.warn("handleSecurity - ConstraintViolationException: {}", exception.getMessage());

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
		HttpStatus status = resolveHttpCode(exception.getHttpCode());
		logByStatus(status, "ScosException", exception);
		String detail = localeService.getMessage(exception.getCode(), exception.getArgs());
		ProblemDetail problem = enrich(
				ScosProblemDetails.of(
						status, exception.getCode(), resolveTitle(exception.getTitle()),
						detail, request.getRequestURI()
				)
		);
		return ResponseEntity.status(status).body(problem);
	}

	@ExceptionHandler(ScosNoRollbackException.class)
	protected ResponseEntity<ProblemDetail> handleScosNoRollbackException(
			ScosNoRollbackException exception, HttpServletRequest request
	){
		HttpStatus status = resolveHttpCode(exception.getHttpCode());
		logByStatus(status, "ScosNoRollbackException", exception);
		String detail = localeService.getMessage(exception.getCode());
		ProblemDetail problem = enrich(
				ScosProblemDetails.of(
						status, exception.getCode(), resolveTitle(exception.getTitle()),
						detail, request.getRequestURI()
				)
		);
		return ResponseEntity.status(status).body(problem);
	}

	@ExceptionHandler(ScosNoContentException.class)
	@ResponseStatus(HttpStatus.NO_CONTENT)
	protected void handleScosNoContentException(ScosNoContentException exception){
		log.debug("handleSecurity - ScosNoContentException: ", exception);
	}

	@ExceptionHandler(AccessDeniedException.class)
	protected ResponseEntity<ProblemDetail> handleAccessDeniedException(
			AccessDeniedException ex, HttpServletRequest request
	) {
		log.warn("handleSecurity - AccessDeniedException: {}", ex.getMessage());
		String detail = localeService.getMessage(ScosExceptionCode.ACCESS_DENIED.getCode());
		ProblemDetail problem = enrich(
				ScosProblemDetails.of(
						HttpStatus.FORBIDDEN, ScosExceptionCode.ACCESS_DENIED, detail, request.getRequestURI()
				)
		);
		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem);
	}

	@ExceptionHandler(AuthorizationDeniedException.class)
	protected ResponseEntity<ProblemDetail> handleAccessDeniedException(
            AuthorizationDeniedException ex, HttpServletRequest request
	) {
		log.warn("handleSecurity - AuthorizationDeniedException: {}", ex.getMessage());
		String detail = localeService.getMessage(ScosExceptionCode.ACCESS_DENIED.getCode());
		ProblemDetail problem = enrich(
				ScosProblemDetails.of(
						HttpStatus.FORBIDDEN, ScosExceptionCode.ACCESS_DENIED, detail, request.getRequestURI()
				)
		);
		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem);
	}



	@ExceptionHandler(MethodNotImplementedException.class)
	protected ResponseEntity<ProblemDetail> handleMethodNotImplementedException(
			MethodNotImplementedException ex, HttpServletRequest request
	) {
		log.error("handleSecurity - MethodNotImplementedException: ", ex);
		String detail = localeService.getMessage(ScosExceptionCode.NOT_IMPLEMENTED.getCode());
		ProblemDetail problem = enrich(
				ScosProblemDetails.of(
						HttpStatus.NOT_IMPLEMENTED, ScosExceptionCode.NOT_IMPLEMENTED, detail, request.getRequestURI()
				)
		);
		return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(problem);
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

	/**
	 * Decides the log level for a {@link ScosException}/{@link ScosNoRollbackException},
	 * whose HTTP status is dynamic ({@code resolveHttpCode(exception.getHttpCode())}):
	 * {@code WARN} without stack trace for {@code 4xx} (including the fallback status
	 * {@link #resolveHttpCode(int)} returns for an unresolvable code, {@code 400}),
	 * {@code ERROR} with stack trace for {@code 5xx}.
	 */
	private void logByStatus(HttpStatus status, String context, Throwable ex) {
		if (status.is4xxClientError()) {
			log.warn("handleSecurity - {}: {}", context, ex.getMessage());
		} else {
			log.error("handleSecurity - {}: ", context, ex);
		}
	}

	private HttpStatus resolveHttpCode(int httpCode) {
		HttpStatus httpStatus = HttpStatus.resolve(httpCode);

		return httpStatus != null ? httpStatus : HttpStatus.BAD_REQUEST;
	}

	/**
	 * Resolves {@code title} (the {@code ScosException}'s own title, from
	 * {@code ExceptionCode.getTitle()}) as an i18n key, translating it when a
	 * bundle entry exists. When it doesn't, {@code title} itself is the default —
	 * never a hardcoded literal that would mask a missing translation.
	 */
	private String resolveTitle(String title) {
		return localeService.getMessageOrDefault(title, title);
	}
}
