package br.com.sawcunhaos.foundation.utils.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ScosResponseDTO<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 4302753512896834219L;

    private T data;
    private ScosPaginatedDTO scosPaginatedDTO;

}
