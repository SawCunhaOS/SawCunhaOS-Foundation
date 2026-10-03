
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

package br.com.sawcunhaos.foundation.web.filter;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Wrapper de requisição que permite ler o corpo mais de uma vez. O {@code ServletInputStream} do
 * container só pode ser consumido uma vez; como {@link LoggingInitialFilter} precisa ler o corpo
 * para logá-lo e o controller precisa lê-lo de novo para desserializar, o primeiro acesso copia
 * todos os bytes para a memória e cada chamada seguinte recebe um stream novo sobre essa cópia.
 *
 * <p>Limitações: o corpo inteiro fica em memória (sem limite de tamanho) e leitura assíncrona não é
 * suportada ({@code setReadListener} lança {@link RuntimeException}) — por isso os filtros ignoram
 * requisições {@code application/grpc}.</p>
 *
 * @since 1.2.0
 */
public class MultiReadHttpServletRequest extends HttpServletRequestWrapper {
    private ByteArrayOutputStream cachedBytes;

    /**
     * @param request requisição original, ainda não lida
     */
    public MultiReadHttpServletRequest(HttpServletRequest request) {
        super(request);
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        // Cache lazy: só consome o stream original na primeira leitura; as demais reutilizam os bytes.
        if (cachedBytes == null) cacheInputStream();

        return new CachedServletInputStream(cachedBytes.toByteArray());
    }

    @Override
    public BufferedReader getReader() throws IOException{
        final String encoding = getCharacterEncoding();
        final Charset charset = (encoding != null) ? Charset.forName(encoding) : StandardCharsets.UTF_8;
        return new BufferedReader(new InputStreamReader(getInputStream(), charset));
    }

    private void cacheInputStream() throws IOException {
        /* Cache the inputstream in order to read it multiple times. */
        cachedBytes = new ByteArrayOutputStream();
        super.getInputStream().transferTo(cachedBytes);
    }


    /* An input stream which reads the cached request body */
    private static class CachedServletInputStream extends ServletInputStream {

        private final ByteArrayInputStream buffer;

        public CachedServletInputStream(byte[] contents) {
            this.buffer = new ByteArrayInputStream(contents);
        }

        @Override
        public int read() {
            return buffer.read();
        }

        @Override
        public boolean isFinished() {
            return buffer.available() == 0;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener listener) {
            throw new RuntimeException("Not implemented");
        }
    }
}