package br.com.sawcunhaos.foundation.audit.configuration;

import br.com.sawcunhaos.foundation.utils.specification.LocaleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
public class LocaleUtilsBean implements LocaleService {

	@Autowired
	private MessageSource messageSource;

	@Override
	public Locale getLocale(){
		return LocaleContextHolder.getLocale(LocaleContextHolder.getLocaleContext());
	}

	@Override
	public String getMessage(String code, Object... args){
		return messageSource.getMessage(code, args, getLocale());
	}

    @Override
    public String getMessage(String code, List<Object> args) {
        return messageSource.getMessage(code, args.toArray(), getLocale());
    }

}
