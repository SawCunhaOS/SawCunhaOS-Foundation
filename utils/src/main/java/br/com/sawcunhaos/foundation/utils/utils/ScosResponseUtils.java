package br.com.sawcunhaos.foundation.utils.utils;

import br.com.sawcunhaos.foundation.utils.dto.response.ScosResponseDTO;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ScosResponseUtils {

    public static <T> ScosResponseDTO<T> wrapResponse(final T data){
        return ScosResponseDTO.<T>builder()
                .data(data)
                .build();
    }

    public static <T> ScosResponseDTO<T> wrapResponse(
            final T data,
            final int totalPages,
            final long totalElements,
            final long totalElementsPerPage,
            final int sizePerPage
    ){
        return ScosResponseDTO.<T>builder()
                .data(data)
                .scosPaginatedDTO(PaginationUtils.createPaginated(totalPages, totalElements, totalElementsPerPage, sizePerPage))
                .build();
    }

}
