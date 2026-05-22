package br.com.sawcunhaos.foundation.security.service;


import br.com.sawcunhaos.foundation.security.domain.entity.Permission;
import br.com.sawcunhaos.foundation.security.domain.repository.PermissionRepository;
import br.com.sawcunhaos.foundation.utils.specification.ScosFeature;
import br.com.sawcunhaos.foundation.utils.specification.ScosPermission;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
@RequiredArgsConstructor
public class ScosPermissionService {


    private final PermissionRepository permissionRepository;

    public void createAndUpdatePermission() {
        log.info("Starting update permissions");

        Reflections reflections = new Reflections("br.com.sawcunhaos");

        Set<Class<? extends ScosPermission>> classes =
                reflections.getSubTypesOf(ScosPermission.class);

        Set<Permission> permissions = classes.stream()
                .filter(Class::isEnum)
                .flatMap(c -> Stream.of(((Class<? extends Enum<?>>) c).getEnumConstants()))
                .map(p -> (ScosPermission) p)
                .map(p -> Permission.builder()
                        .permission(p.getPermission())
                        .descriptionPtBr(p.getDescriptionPtBr())
                        .descriptionEng(p.getDescriptionEng())
                        .module(p.getModule())
                        .endPoint(p.getEndPoint())
                        .features(p.getFeatures().stream()
                                .map(ScosFeature::getFeature)
                                .collect(Collectors.toList()))
                        .build())
                .collect(Collectors.toSet());

        permissionRepository.saveAll(permissions);

        log.info("Finished update permission with {} permissions", permissions.size());
    }

}
