# **APIM Publisher Role Based Access Control Extension**

###### **A role-based API access control extension for API Manager Publisher 2.1.0.**

## Introduction

In API Manager Publisher, by default API Creators can edit any API available in the system. This extension is to restrict that and enable role based API modification control. After enabling this extension, only the users with particular roles specified per API can only edit the particular API. However, there won’t be any restrictions with viewing the API.

Once the extension is enabled, there will be a new UI element in Publisher to specify the Editing Allowed Roles.

## Configuration:

### Configuring the registry indexer:

1. Build the project using "mvn clean install" and get the org.wso2.apim.example.registry.indexer.rolebasedaccess-1.0.0.jar file from the target folder \(or you can use the jar file attached\)

2. Copy the jar file into &lt;APIM-HOME&gt;/repository/components/dropins

3. Add below to &lt;APIM-HOME&gt;/repository/conf/registry.xml under &lt;indexingConfiguration&gt; --&gt;&lt;indexers&gt; as the first &lt;indexer&gt; element.

`<indexer class="org.wso2.apim.example.registry.indexer.rolebasedaccess.CustomAPIIndexer"mediaTypeRegEx="application/vnd.wso2-api\+xml"profiles ="default,api-store,api-publisher"/>`

The registry indexer will be invoked when we created or edited any API. It will read the API’s registry artifact and update necessary permissions based on the artifact field value for edit allowed roles. For this, an unused field of API artifact is used which is “overview\_wadl”.

### Configuring Publisher subtheme:

1. Copy the "custom" folder into &lt;APIM-HOME&gt;/repository/deployment/server/jaggeryapps/publisher/site/themes/subthemes folder. If subthemes folder is not there already, please create it.

2. Set "subtheme" element as "custom" in &lt;APIM-HOME&gt;/repository/deployment/server/jaggeryapps/publisher/site/conf/site.json

`"theme": {    
    "base":"wso2",    
    "subtheme":"custom"    
 }`

1. Add below localization elements in &lt;APIM-HOME&gt;/repository/deployment/server/jaggeryapps/publisher/site/conf/locales/jaggery/locale\_default.json

`"Edit Permissions":"Edit Permissions",    
 "Edit Allowed Roles":"Edit Allowed Roles",`

Restart the server after both Registry indexer and Publisher subtheme configuration
