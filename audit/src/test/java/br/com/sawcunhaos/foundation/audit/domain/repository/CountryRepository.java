package br.com.sawcunhaos.foundation.audit.domain.repository;


import br.com.sawcunhaos.foundation.audit.domain.entity.Country;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CountryRepository extends JpaRepository<Country, UUID> {

}
