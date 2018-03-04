/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ballerinalang.swagger.model;

import io.swagger.oas.models.Components;
import io.swagger.oas.models.ExternalDocumentation;
import io.swagger.oas.models.OpenAPI;
import io.swagger.oas.models.PathItem;
import io.swagger.oas.models.info.Info;
import io.swagger.oas.models.media.Schema;
import io.swagger.oas.models.security.SecurityRequirement;
import io.swagger.oas.models.servers.Server;
import io.swagger.oas.models.tags.Tag;
import org.ballerinalang.swagger.exception.BalOpenApiException;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.AbstractMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Wrapper for <code>io.swagger.oas.models.OpenAPI</code>
 * <p>This class can be used to push additional context variables for handlebars</p>
 */
public class BalOpenApi {
    private String apiPackage;
    private String openapi = "3.0.0";
    private Info info = null;
    private ExternalDocumentation externalDocs = null;
    private List<Server> servers = null;
    private List<SecurityRequirement> security = null;
    private List<Tag> tags = null;
    private Set<Map.Entry<String, PathItem>> paths = null;
    private Set<Map.Entry<String, BalSchema>> schemas = null;
    private Components components = null;
    private Map<String, Object> extensions = null;
    private String host = null;
    private int port = 0;
    private int httpsPort = 0;
    private String basePath = null;
    private String url = null;

    /**
     * Build a {@link BalOpenApi} object from a {@link OpenAPI} object.
     * All non iterable objects using handlebars library is converted into
     * supported iterable object types.
     *
     * @param openAPI {@link OpenAPI} type object to be converted
     * @return Converted {@link BalOpenApi} object
     * @throws BalOpenApiException when OpenAPI to BalOpenApi parsing failed
     */
    public BalOpenApi buildFromOpenAPI(OpenAPI openAPI) throws BalOpenApiException {
        this.openapi = openAPI.getOpenapi();
        this.info = openAPI.getInfo();
        this.externalDocs = openAPI.getExternalDocs();
        this.servers = openAPI.getServers();
        this.security = openAPI.getSecurity();
        this.tags = openAPI.getTags();
        this.components = openAPI.getComponents();
        this.extensions = openAPI.getExtensions();
        this.paths = openAPI.getPaths().entrySet();

        try {
            setHostInfo(openAPI);
            setSchemas(openAPI);
        } catch (MalformedURLException e) {
            throw new BalOpenApiException("Failed to parse server information", e);
        }

        return this;
    }

    /**
     * Populate schemas into a "Set".
     */
    private void setSchemas(OpenAPI openAPI) {
        this.schemas = new LinkedHashSet<>();
        Map<String, Schema> schemaMap;
        if (openAPI.getComponents() == null) {
            return;
        }
        if (openAPI.getComponents().getSchemas() == null) {
            return;
        }

        schemaMap = openAPI.getComponents().getSchemas();
        for (Map.Entry entry : schemaMap.entrySet()) {
            BalSchema schema = new BalSchema().buildFromSchema((Schema) entry.getValue(), schemaMap);
            schemas.add(new AbstractMap.SimpleEntry<>((String) entry.getKey(), schema));
        }
    }

    /**
     * Extract host information from OpenAPI server list.
     */
    private void setHostInfo(OpenAPI openAPI) throws MalformedURLException {
        List<Server> serverList = openAPI.getServers();
        if (serverList == null) {
            this.host = "localhost";
            this.port = 80;
            this.httpsPort = 443;
            this.basePath = "/";

            return;
        }

        // Swagger parser returns a server object with "/" url when no servers are defined
        // this check is to overcome possible errors due to that
        if (serverList.size() > 1 || !"/".equals(serverList.get(0).getUrl())) {

            // We select the first server in the list as the Host of generated service
            // Other servers will be kept as extra information but will not be used within the service
            URL url = new URL(serverList.get(0).getUrl());
            this.url = serverList.get(0).getUrl();
            host = url.getHost();
            basePath = url.getPath();
            boolean isHttps = "https".equalsIgnoreCase(url.getProtocol());

            if (isHttps) {
                httpsPort = url.getPort();
                httpsPort = httpsPort == -1 ? 443 : httpsPort;
            } else {
                port = url.getPort();
                port = port == -1 ? 80 : port;
            }
        }
    }
    
    public String getApiPackage() {
        return apiPackage;
    }

    public BalOpenApi apiPackage(String apiPackage) {
        this.apiPackage = apiPackage;
        return this;
    }

    public String getOpenapi() {
        return openapi;
    }

    public Info getInfo() {
        return info;
    }

    public ExternalDocumentation getExternalDocs() {
        return externalDocs;
    }

    public List<Server> getServers() {
        return servers;
    }

    public List<SecurityRequirement> getSecurity() {
        return security;
    }

    public List<Tag> getTags() {
        return tags;
    }

    public Set<Map.Entry<String, PathItem>> getPaths() {
        return paths;
    }

    public Components getComponents() {
        return components;
    }

    public Map<String, Object> getExtensions() {
        return extensions;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getBasePath() {
        return basePath;
    }

    public int getHttpsPort() {
        return httpsPort;
    }

    public Set<Map.Entry<String, BalSchema>> getSchemas() {
        return schemas;
    }

    public String getUrl() {
        return url;
    }
}
