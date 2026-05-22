package br.com.sawcunhaos.foundation.security.domain.repository;

import br.com.sawcunhaos.foundation.security.domain.entity.PermissionCache;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public interface PermissionCacheRepository extends JpaRepository<PermissionCache, String> {

    @Query("""
        SELECT pc.permissions
        FROM PermissionCache pc
        WHERE pc.feature = :feature
    """)
    Set<String> findPermissionByFeature(@NonNull @Param("feature") String feature);
}
