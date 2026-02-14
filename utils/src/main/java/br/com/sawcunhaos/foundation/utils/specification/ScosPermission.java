package br.com.sawcunhaos.foundation.utils.specification;

import java.util.List;

public interface ScosPermission {
    String getPermission();
    String getDescriptionPtBr();
    String getDescriptionEng();
    String getEndPoint();
    String getModule();
    List<ScosFeature> getFeatures();
}
