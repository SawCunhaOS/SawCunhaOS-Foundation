package br.com.sawcunhaos.foundation.utils.utils.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Data
@Builder
public class TestDTO {

    private Long id;
    private String taxIdentifier;
    private String name;
    private String description;
    private boolean enable;
    private int age;

    private Set<TestDTO> testDTOs;

}
