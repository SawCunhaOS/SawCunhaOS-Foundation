package br.com.sawcunhaos.foundation.security.domain.repository;

import br.com.sawcunhaos.foundation.security.domain.cache.LoginCacheDTO;
import br.com.sawcunhaos.foundation.security.domain.entity.LoginCache;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoginCacheRepository extends JpaRepository<LoginCache, String> {

    @Query("""
           SELECT new  br.com.sawcunhaos.foundation.security.domain.cache.LoginCacheDTO(
               lc.login,
               lc.profileId,
               lc.status,
               lc.active,
               lc.features
           )
           FROM LoginCache lc
           """)
    List<LoginCacheDTO> findAllLoginCache();

    @Query("""
           SELECT new  br.com.sawcunhaos.foundation.security.domain.cache.LoginCacheDTO(
               lc.login,
               lc.profileId,
               lc.status,
               lc.active,
               lc.features
           )
           FROM LoginCache lc
           WHERE lc.login = :login
           """)
    LoginCacheDTO findByLogin(@NonNull @Param("login") String login);

}
