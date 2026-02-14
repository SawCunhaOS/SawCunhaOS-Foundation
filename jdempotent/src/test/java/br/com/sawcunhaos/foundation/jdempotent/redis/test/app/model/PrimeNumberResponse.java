package br.com.sawcunhaos.foundation.jdempotent.redis.test.app.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PrimeNumberResponse implements Serializable {

    private List<Long> primesNumber;

}
