
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

package br.com.sawcunhaos.foundation.utils.specification;

import java.util.List;
import java.util.Locale;

public interface LocaleService {
    Locale getLocale();
    String getMessage(String code, Object... args);
    String getMessage(String code, List<Object> args);
}
