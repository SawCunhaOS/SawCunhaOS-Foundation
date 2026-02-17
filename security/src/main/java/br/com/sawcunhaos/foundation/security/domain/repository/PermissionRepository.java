package br.com.sawcunhaos.foundation.security.domain.repository;

import br.com.sawcunhaos.foundation.security.domain.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, String> {

}
