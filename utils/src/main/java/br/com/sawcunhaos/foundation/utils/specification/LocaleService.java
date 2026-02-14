package br.com.sawcunhaos.foundation.utils.specification;

import java.util.List;
import java.util.Locale;

public interface LocaleService {
    Locale getLocale();
    String getMessage(String code, Object... args);
    String getMessage(String code, List<Object> args);
}
