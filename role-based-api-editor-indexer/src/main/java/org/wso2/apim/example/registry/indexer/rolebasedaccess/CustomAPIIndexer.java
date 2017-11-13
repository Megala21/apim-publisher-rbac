package org.wso2.apim.example.registry.indexer.rolebasedaccess;

import org.wso2.apim.example.registry.indexer.rolebasedaccess.internal.CustomDataHolder;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.common.SolrException;
import org.wso2.carbon.governance.api.common.dataobjects.GovernanceArtifact;
import org.wso2.carbon.governance.api.generic.GenericArtifactManager;
import org.wso2.carbon.governance.api.util.GovernanceArtifactConfiguration;
import org.wso2.carbon.governance.api.util.GovernanceUtils;
import org.wso2.carbon.governance.registry.extensions.indexers.RXTIndexer;
import org.wso2.carbon.registry.core.ActionConstants;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.jdbc.realm.RegistryAuthorizationManager;
import org.wso2.carbon.registry.core.utils.RegistryUtils;
import org.wso2.carbon.registry.indexing.AsyncIndexer;
import org.wso2.carbon.registry.indexing.IndexingManager;
import org.wso2.carbon.registry.indexing.solr.IndexDocument;
import org.wso2.carbon.user.api.AuthorizationManager;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.api.UserStoreManager;
import org.wso2.carbon.user.core.UserRealm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import javax.xml.stream.XMLStreamException;

public class CustomAPIIndexer extends RXTIndexer {

    public static final Log LOGGER = LogFactory.getLog(CustomAPIIndexer.class);
    private static final String ALLOWED_ROLES_REGISTRY_FIELD = "overview_wadl";
    public static final String EVERYONE_ROLE = "INTERNAL/everyone";
    public static final String ANONYMOUS_ROLE = "SYSTEM/wso2.anonymous.role";
    public static final String REGISTRY_ARTIFACT_API = "api";
    public static final String REGISTRY_ARTIFACT_SWAGGER = "swagger.json";

    public IndexDocument getIndexedDocument(AsyncIndexer.File2Index fileData) throws SolrException,

            RegistryException {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.info("Triggering custom API Indexer for publisher access control");
        }

        try {
            Registry registry =
                    GovernanceUtils.getGovernanceSystemRegistry(
                            IndexingManager.getInstance().getRegistry(fileData.tenantId));

            GovernanceArtifactConfiguration configuration =
                    GovernanceUtils.findGovernanceArtifactConfigurationByMediaType(fileData.mediaType, registry);
            GenericArtifactManager manager = new GenericArtifactManager(registry, configuration.getKey());
            String xmlAsStr = RegistryUtils.decodeBytes(fileData.data);
            GovernanceArtifact governanceArtifact = manager.newGovernanceArtifact(AXIOMUtil.stringToOM(xmlAsStr));

            String allowedRoles = governanceArtifact.getAttribute(ALLOWED_ROLES_REGISTRY_FIELD);
            UserRealm userRealm = (UserRealm) CustomDataHolder.getRealmService()
                    .getTenantUserRealm(fileData.tenantId);

            RegistryAuthorizationManager authorizationManager = new RegistryAuthorizationManager(userRealm);

            String resourcePath = authorizationManager.computePathOnMount(fileData.path);
            String swaggerResourcePath = authorizationManager
                    .computePathOnMount(replaceLast(fileData.path, REGISTRY_ARTIFACT_API, REGISTRY_ARTIFACT_SWAGGER));
            AuthorizationManager authManager =
                    CustomDataHolder.getRealmService().getTenantUserRealm(fileData.tenantId)
                            .getAuthorizationManager();

            if (StringUtils.isNotEmpty(allowedRoles)) {
                UserStoreManager userStoreManager = CustomDataHolder.getRealmService()
                        .getTenantUserRealm(fileData.tenantId).getUserStoreManager();

                List<String> roleToAddArr = new ArrayList<String>(
                        Arrays.asList(governanceArtifact.getAttribute(ALLOWED_ROLES_REGISTRY_FIELD).split(",")));

                //validate the provided roles from the UI whether they are existing roles.
                for (int i = 0; i < roleToAddArr.size(); i++) {
                    if (!userStoreManager.isExistingRole(roleToAddArr.get(i))) {
                        log.error("Invalid role added for Publisher visible role " + roleToAddArr.get(i));
                        roleToAddArr.remove(i);
                    }
                }

                //Deny all available roles for API artifact and swagger.json artifact 
                // other than internal everyone and anonymous
                denyCurrentAvailableRoles(fileData, resourcePath, authManager);
                denyCurrentAvailableRoles(fileData, swaggerResourcePath, authManager);

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Roles going to authorize permissions : " + roleToAddArr.toString());
                }

                //Setting allowed roles
                for (String roleToAdd : roleToAddArr) {
                    if (!authManager.isRoleAuthorized(roleToAdd, resourcePath, ActionConstants.GET)) {
                        authorizeRoleToResourcePath(authManager, roleToAdd, resourcePath);
                        authorizeRoleToResourcePath(authManager, roleToAdd, swaggerResourcePath);
                    }
                }

            } else {
                // Authorize to Publisher and Creator role if there is no any Publisher visible roles
                resetDefaultRoles(resourcePath, authManager);
                resetDefaultRoles(swaggerResourcePath, authManager);
            }
        } catch (XMLStreamException e) {
            LOGGER.error("Unable to parse XML.", e);
        } catch (UserStoreException e) {
            LOGGER.error("Unable to read user store." + e);
        }
        IndexDocument indexedDocument = super.getIndexedDocument(fileData);
        return indexedDocument;

    }

    private void resetDefaultRoles(String resourcePath, AuthorizationManager authManager)
            throws UserStoreException {
        String[] roleToAddArr = new String[] { "INTERNAL/publisher", "INTERNAL/creator" };
        for (String roleToAdd : roleToAddArr) {
            if (!authManager.isRoleAuthorized(roleToAdd, resourcePath, ActionConstants.GET)) {
                authorizeRoleToResourcePath(authManager, roleToAdd, resourcePath);
            }

        }
    }

    private void denyCurrentAvailableRoles(AsyncIndexer.File2Index fileData, String resourcePath,
            AuthorizationManager authManager) throws UserStoreException {

        String[] currentRoleArr = authManager.getAllowedRolesForResource(resourcePath, ActionConstants.GET);
        List<String> currentRoleList = new LinkedList<String>(Arrays.asList(currentRoleArr));

        String adminRoleName = CustomDataHolder.getRealmService().getTenantUserRealm(
                fileData.tenantId).getRealmConfiguration().getAdminRoleName();

        currentRoleList.remove(adminRoleName);
        currentRoleList.remove(EVERYONE_ROLE);
        currentRoleList.remove(ANONYMOUS_ROLE);

        String[] roleToRemoveArr = currentRoleList.toArray(new String[currentRoleList.size()]);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Roles going to deny permissions : " + roleToRemoveArr.toString());
        }

        for (String roleToRemove : roleToRemoveArr) {
            if (authManager.isRoleAuthorized(roleToRemove, resourcePath, ActionConstants.GET)) {
                denyRoleToResourcePath(authManager, roleToRemove, resourcePath);
            }
        }
    }

    private String replaceLast(String string, String from, String to) {
        int lastIndex = string.lastIndexOf(from);
        if (lastIndex < 0) return string;
        String tail = string.substring(lastIndex).replaceFirst(from, to);
        return string.substring(0, lastIndex) + tail;
    }

    private void authorizeRoleToResourcePath(AuthorizationManager authManager, String roleToAdd, String resourcePath)
            throws UserStoreException {
        authManager.authorizeRole(roleToAdd, resourcePath, ActionConstants.GET);
        authManager.authorizeRole(roleToAdd, resourcePath, ActionConstants.PUT);
        authManager.authorizeRole(roleToAdd, resourcePath, ActionConstants.DELETE);
    }

    private void denyRoleToResourcePath(AuthorizationManager authManager, String roleToDeny, String resourcePath)
            throws UserStoreException {
        authManager.denyRole(roleToDeny, resourcePath, ActionConstants.GET);
        authManager.denyRole(roleToDeny, resourcePath, ActionConstants.PUT);
        authManager.denyRole(roleToDeny, resourcePath, ActionConstants.DELETE);
    }
    
    
}
