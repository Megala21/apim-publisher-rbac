package org.wso2.apim.example.registry.indexer.rolebasedaccess.internal;

import org.apache.axis2.context.ConfigurationContext;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.CarbonUtils;

/**
 * Cache invalidating data holder
 */
public class CustomDataHolder {

    private static ConfigurationContext configContext;
    private static RegistryService registryService;
    private static RealmService realmService;

    public static void setConfigContext(ConfigurationContext configContext) {
        CustomDataHolder.configContext = configContext;
    }

    public static ConfigurationContext getConfigContext() {
        CarbonUtils.checkSecurity();
        return configContext;
    }

    public static RegistryService getRegistryService() {
        return registryService;
    }

    public static void setRegistryService(RegistryService registryService) {
        CustomDataHolder.registryService = registryService;
    }

    public static RealmService getRealmService() {
        return realmService;
    }

    public static void setRealmService(RealmService realmService) {
        CustomDataHolder.realmService = realmService;
    }
}