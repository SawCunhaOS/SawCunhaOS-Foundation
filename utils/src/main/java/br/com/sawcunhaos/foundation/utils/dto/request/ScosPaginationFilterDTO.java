package br.com.sawcunhaos.foundation.utils.dto.request;

import lombok.Builder;
import org.springframework.data.domain.Sort;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

@Builder
public record ScosPaginationFilterDTO(
        Integer page,
        Integer sizePerPage,
        Sort.Direction direction,
        String order
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 7366806307844227277L;

    public int getPage() {
        return Objects.isNull(page) ? 1 : page;
    }

    public int getSizePerPage() {
        return Objects.isNull(sizePerPage) ? 10 : sizePerPage;
    }

    public Sort.Direction getDirection() {
        return Objects.isNull(direction) ? Sort.Direction.ASC : direction;
    }
}
