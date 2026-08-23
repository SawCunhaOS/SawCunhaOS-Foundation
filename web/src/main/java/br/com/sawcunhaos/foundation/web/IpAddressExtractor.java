
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

package br.com.sawcunhaos.foundation.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

@Component
public class IpAddressExtractor {

    /**
     * Headers para verificar (em ordem de prioridade)
     */
    private static final List<String> IP_HEADERS = Arrays.asList(
            "CF-Connecting-IP",        // CloudFlare
            "True-Client-IP",          // Akamai
            "X-Real-IP",               // Nginx
            "X-Forwarded-For",         // Padrão
            "X-Client-IP",             // Outros proxies
            "Forwarded",               // RFC 7239
            "Proxy-Client-IP",         // Apache
            "WL-Proxy-Client-IP",      // WebLogic
            "HTTP_X_FORWARDED_FOR",    // Variante
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
    );

    /**
     * Lista de IPs privados/locais para ignorar
     */
    private static final List<String> PRIVATE_IP_PREFIXES = Arrays.asList(
            "10.",           // Classe A privada
            "172.16.",       // Classe B privada
            "172.17.",
            "172.18.",
            "172.19.",
            "172.20.",
            "172.21.",
            "172.22.",
            "172.23.",
            "172.24.",
            "172.25.",
            "172.26.",
            "172.27.",
            "172.28.",
            "172.29.",
            "172.30.",
            "172.31.",
            "192.168.",      // Classe C privada
            "127.",          // Loopback
            "169.254.",      // Link-local
            "::1",           // IPv6 loopback
            "fc00:",         // IPv6 private
            "fd00:",         // IPv6 private
            "fe80:"          // IPv6 link-local
    );

    /**
     * Extrai o IP real do cliente
     */
    public String extractClientIp(HttpServletRequest request) {
        // 1. Tentar headers conhecidos
        for (String header : IP_HEADERS) {
            String ip = extractIpFromHeader(request, header);
            if (isValidPublicIp(ip)) {
                return ip;
            }
        }

        // 2. Fallback para remoteAddr
        String remoteAddr = request.getRemoteAddr();
        if (isValidPublicIp(remoteAddr)) {
            return remoteAddr;
        }

        // 3. Se tudo falhar, retornar remoteAddr mesmo que privado
        return remoteAddr != null ? remoteAddr : "unknown";
    }

    /**
     * Extrai IP de um header específico
     */
    private String extractIpFromHeader(HttpServletRequest request, String headerName) {
        String headerValue = request.getHeader(headerName);

        if (!StringUtils.hasText(headerValue) ||
                "unknown".equalsIgnoreCase(headerValue)) {
            return null;
        }

        // X-Forwarded-For pode ter múltiplos IPs: "client, proxy1, proxy2"
        // Queremos o primeiro (cliente real)
        if ("X-Forwarded-For".equalsIgnoreCase(headerName)) {
            String[] ips = headerValue.split(",");
            for (String ip : ips) {
                String trimmedIp = ip.trim();
                if (isValidPublicIp(trimmedIp)) {
                    return trimmedIp;
                }
            }
            return null;
        }

        // Forwarded header (RFC 7239): "for=192.0.2.60;proto=http;by=203.0.113.43"
        if ("Forwarded".equalsIgnoreCase(headerName)) {
            return extractIpFromForwardedHeader(headerValue);
        }

        return headerValue.trim();
    }

    /**
     * Extrai IP do header Forwarded (RFC 7239)
     */
    private String extractIpFromForwardedHeader(String headerValue) {
        // Formato: for=192.0.2.60 ou for="[2001:db8:cafe::17]"
        String[] parts = headerValue.split(";");
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.startsWith("for=")) {
                String ip = trimmed.substring(4)
                        .replaceAll("\"", "")
                        .replaceAll("\\[", "")
                        .replaceAll("\\]", "");
                return ip;
            }
        }
        return null;
    }

    /**
     * Valida se é um IP público válido
     */
    private boolean isValidPublicIp(String ip) {
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            return false;
        }

        // Remover porta se presente (ex: "192.168.1.1:8080")
        ip = ip.split(":")[0];

        // Validar formato
        if (!isValidIpFormat(ip)) {
            return false;
        }

        // Rejeitar IPs privados/locais
        if (isPrivateIp(ip)) {
            return false;
        }

        return true;
    }

    /**
     * Valida formato de IP (IPv4 ou IPv6)
     */
    private boolean isValidIpFormat(String ip) {
        try {
            InetAddress.getByName(ip);
            return true;
        } catch (UnknownHostException e) {
            return false;
        }
    }

    /**
     * Verifica se é IP privado/local
     */
    private boolean isPrivateIp(String ip) {
        for (String prefix : PRIVATE_IP_PREFIXES) {
            if (ip.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
