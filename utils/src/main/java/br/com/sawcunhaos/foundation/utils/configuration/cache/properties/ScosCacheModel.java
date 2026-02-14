package br.com.sawcunhaos.foundation.utils.configuration.cache.properties;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.validation.annotation.Validated;

/**
 * Modelo de configuração individual de cache
 */
@Data
@Validated
@RefreshScope
public class ScosCacheModel {

    /**
     * Nome único do cache
     */
    @NotBlank(message = "Nome do cache não pode ser vazio")
    private String name;

    /**
     * Tempo de vida em segundos para este cache específico
     */
    @NotNull(message = "TTL não pode ser nulo")
    @Min(value = 1, message = "TTL deve ser no mínimo 1 segundo")
    private Long timeToLiveSeconds;

    /**
     * Descrição opcional do cache (para documentação)
     */
    private String description;

    /**
     * Define se este cache aceita valores nulos
     * Default: false (não cacheia null)
     */
    private boolean allowNullValues = false;

    /**
     * Tamanho máximo estimado de entradas (apenas informativo/monitoramento)
     */
    private Integer maxSize;
}

