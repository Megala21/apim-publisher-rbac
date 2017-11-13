# APIM Publisher Role Based Access Control Extension

A role-based API access control extension for API Manager Publisher 2.1.0.

## Introduction

In API Manager Publisher, by default API Creators can edit any API available in the system. This extension is to restrict that and enable role based API modification control. After enabling this extension, only the users with particular roles specified per API can only edit the particular API. However, there won’t be any restrictions with viewing the API.

Once the extension is enabled, there will be a new UI element in Publisher to specify the Editing Allowed Roles.

## ![](https://github.com/malinthaprasan/apim-publisher-rbac/raw/master/docs/new-ui-element.jpg)

## Configuration:

### Configuring the registry indexer: 

* Build the [project](https://github.com/malinthaprasan/apim-publisher-rbac/tree/master/role-based-api-editor-indexer) using "mvn clean install" and get the org.wso2.apim.example.registry.indexer.rolebasedaccess-1.0.0.jar file from the target folder \(or you can use the jar file attached\)

* Copy the jar file into &lt;APIM-HOME&gt;/repository/components/dropins

* Add below to &lt;APIM-HOME&gt;/repository/conf/registry.xml under &lt;indexingConfiguration&gt; --&gt;&lt;indexers&gt; as the first &lt;indexer&gt; element.

`<indexer class="org.wso2.apim.example.registry.indexer.rolebasedaccess.CustomAPIIndexer"mediaTypeRegEx="application/vnd.wso2-api\+xml"profiles ="default,api-store,api-publisher"/>`

The registry indexer will be invoked when we created or edited any API. It will read the API’s registry artifact and update necessary permissions based on the artifact field value for edit allowed roles. For this, an unused field of API artifact is used which is “overview\_wadl”.

### Configuring Publisher subtheme:

* Copy the "custom" folder into &lt;APIM-HOME&gt;/repository/deployment/server/jaggeryapps/publisher/site/themes/subthemes folder. If subthemes folder is not there already, please create it.

* Set "subtheme" element as "custom" in &lt;APIM-HOME&gt;/repository/deployment/server/jaggeryapps/publisher/site/conf/site.json

`"theme": {
    "base":"wso2",  
    "subtheme":"custom"  
 }`

* Add below localization elements in &lt;APIM-HOME&gt;/repository/deployment/server/jaggeryapps/publisher/site/conf/locales/jaggery/locale\_default.json

`"Edit Permissions":"Edit Permissions",  
 "Edit Allowed Roles":"Edit Allowed Roles",`

Restart the server after both Registry indexer and Publisher subtheme configuration

## Limitations:

* When we set an invalid role, there is no error message popping in the UI. There will be an error log in the server logs.

`"ERROR - RXTIndexer Invalid role added for Publisher visible role invalidRole1"`

* Edit button is not disabled for users who does not have edit rights for the particular API.

  



