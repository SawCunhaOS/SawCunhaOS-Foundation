package br.com.sawcunhaos.foundation.exception;


import br.com.sawcunhaos.foundation.exception.error.ScosException;
import br.com.sawcunhaos.foundation.exception.error.ScosNoContentException;
import br.com.sawcunhaos.foundation.exception.error.ScosNoRollbackException;
import br.com.sawcunhaos.foundation.exception.model.AttributeNotValid;
import br.com.sawcunhaos.foundation.exception.model.ExceptionResponse;
import br.com.sawcunhaos.foundation.exception.utils.ExceptionUtils;
import br.com.sawcunhaos.foundation.utils.dto.response.ScosResponseDTO;
import br.com.sawcunhaos.foundation.utils.enums.ScosExceptionCode;
import br.com.sawcunhaos.foundation.utils.specification.LocaleService;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
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

		return ResponseEntity.status(status).body(
				createResponse(ScosExceptionCode.ENUM_ERROR.getCode(), message)
		);
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(
			MethodArgumentNotValidException ex,
			HttpHeaders headers,
			HttpStatusCode status,
			WebRequest request
	) {
		log.error("handleSecurity - handleMethodArgumentNotValid: ", ex);

		List<AttributeNotValid> validationErrorsDTO = new ArrayList<>();

		ex.getBindingResult().getFieldErrors()
				.forEach(
						e -> validationErrorsDTO.add(
								new AttributeNotValid(
										e.getField(),
										localeService.getMessage(e.getDefaultMessage(), getArgsValidation(e.getArguments()))
								)
						)
				);

		String message = localeService.getMessage(
				ScosExceptionCode.ATTRIBUTE_NOT_VALID.getCode(),
				((ServletWebRequest)request).getRequest().getRequestURI()
		);

		return ResponseEntity.status(status).body(
				createResponse(ScosExceptionCode.ATTRIBUTE_NOT_VALID.getCode(), message, validationErrorsDTO)
		);
	}

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        log.error("handleSecurity - handleHandlerMethodValidationException: ", ex);

        List<AttributeNotValid> validationErrorsDTO = new ArrayList<>();

        ex.getBeanResults().get(0).getFieldErrors()
                .forEach(
                        e -> validationErrorsDTO.add(
                                new AttributeNotValid(
                                        e.getField(),
                                        localeService.getMessage(e.getDefaultMessage(), getArgsValidation(e.getArguments()))
                                )
                        )
                );

        String message = localeService.getMessage(
                ScosExceptionCode.ATTRIBUTE_NOT_VALID.getCode(),
                ((ServletWebRequest)request).getRequest().getRequestURI()
        );

        return ResponseEntity.status(status).body(
                createResponse(ScosExceptionCode.ATTRIBUTE_NOT_VALID.getCode(), message, validationErrorsDTO)
        );
    }



    @ExceptionHandler(ConstraintViolationException.class)
	@ResponseBody
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	protected ScosResponseDTO<ExceptionResponse> handleConstraintViolationException(ConstraintViolationException exception) {
		log.error("handleSecurity - ConstraintViolationException: ", exception);

		List<AttributeNotValid> validationErrorsDTO = new ArrayList<>();

		exception.getConstraintViolations().forEach(
				e -> {
					String[] path = e.getPropertyPath().toString().split("\\.");
					List<String> attributes = new ArrayList<>();
					attributes.add(path[path.length-1]);
					attributes.addAll(
							ExceptionUtils.findValuesAnnotation(e.getConstraintDescriptor().getAnnotation())
					);

					validationErrorsDTO.add(
							new AttributeNotValid(
									attributes.get(0),
									localeService.getMessage(e.getMessage(), attributes.toArray(Object[]::new))
							)
					);
				}
		);

		String message = localeService.getMessage(ScosExceptionCode.ATTRIBUTE_NOT_VALID.getCode());

		return createResponse(ScosExceptionCode.ATTRIBUTE_NOT_VALID.getCode(), message, validationErrorsDTO);
	}

	@ExceptionHandler(ScosException.class)
	@ResponseBody
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	protected ScosResponseDTO<ExceptionResponse> handleScosException(ScosException exception){
		log.error("handleSecurity - ScosException: ", exception);
		return createResponse(exception.getCode(), exception.getArgs());
	}

	@ExceptionHandler(ScosNoRollbackException.class)
	@ResponseBody
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	protected ScosResponseDTO<ExceptionResponse> handleScosNoRollbackException(ScosNoRollbackException exception){
		log.error("handleSecurity - ScosNoRollbackException: ", exception);
		return createResponse(exception.getCode());
	}

	@ExceptionHandler(ScosNoContentException.class)
	@ResponseBody
	@ResponseStatus(HttpStatus.NO_CONTENT)
	protected void handleScosNoContentException(ScosNoContentException exception){
		log.error("handleSecurity - ScosNoContentException: ", exception);
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<String> handleAccessDeniedException(AccessDeniedException ex) {
		return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Acesso negado: " + ex.getMessage());
	}

	private ScosResponseDTO<ExceptionResponse> createResponse(
			String code,
			String message
	){
		return createResponse(code, message, null);
	}

	private ScosResponseDTO<ExceptionResponse> createResponse(
			String code,
			String message,
			List<AttributeNotValid> validationErrorsDTO
	){
		ExceptionResponse exceptionResponse = ExceptionResponse.builder().codeError(code)
				.message(message)
				.validationErrors(validationErrorsDTO).build();
		return ScosResponseDTO.<ExceptionResponse>builder()
				.data(exceptionResponse)
				.build();
	}

	private ScosResponseDTO<ExceptionResponse> createResponse(String code, Object... args){
		ExceptionResponse exceptionResponse = ExceptionResponse.builder()
				.codeError(code)
				.message(localeService.getMessage(code, args))
				.build();
		return ScosResponseDTO.<ExceptionResponse>builder()
				.data(exceptionResponse)
				.build();
	}
}
