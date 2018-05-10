/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2018 Adobe
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.adobe.acs.commons.models.injectors.impl;

import com.adobe.acs.commons.models.injectors.annotation.SharedValueMapValue;
import com.adobe.acs.commons.wcm.PageRootProvider;
import com.adobe.acs.commons.wcm.properties.shared.SharedComponentProperties;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.wcm.api.Page;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.models.spi.DisposalCallbackRegistry;
import org.apache.sling.models.spi.Injector;
import org.osgi.framework.Constants;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Component
@Service
@Property(name = Constants.SERVICE_RANKING, intValue = 4500)
public class SharedValueMapValueInjectorImpl implements Injector {
    @Reference
    private PageRootProvider pageRootProvider;

    @Override
    public String getName() {
        return "shared-component-properties-valuemap";
    }

    @Override
    public Object getValue(Object adaptable, String name, Type declaredType, AnnotatedElement element, DisposalCallbackRegistry callbackRegistry) {
        // sanity check
        if (element.getAnnotation(SharedValueMapValue.class) == null) {
            return null;
        }

        // sanity check
        if (!(adaptable instanceof Resource || adaptable instanceof SlingHttpServletRequest)) {
            return null;
        }

        Resource resource = null;
        if (adaptable instanceof SlingHttpServletRequest) {
            resource = ((SlingHttpServletRequest) adaptable).getResource();
        } else if (adaptable instanceof Resource) {
            resource = (Resource) adaptable;
        }

        if (resource != null) {
            Page pageRoot = pageRootProvider.getRootPage(resource);
            if (pageRoot != null) {
                switch (element.getAnnotation(SharedValueMapValue.class).type()) {
                    case MERGED:
                        return getMergedProperties(pageRoot, resource).get(name);
                    case SHARED:
                        return getSharedProperties(pageRoot, resource).get(name);
                    case GLOBAL:
                        return getGlobalProperties(pageRoot, resource).get(name);
                }
            }
        }

        return null;
    }

    /**
     * Get global properties resource for the current resource.
     */
    protected ValueMap getGlobalProperties(Page pageRoot, Resource resource) {
        String globalPropsPath = pageRoot.getPath() + "/" + JcrConstants.JCR_CONTENT + "/" + SharedComponentProperties.NN_GLOBAL_COMPONENT_PROPERTIES;
        Resource globalPropsResource = resource.getResourceResolver().getResource(globalPropsPath);
        return globalPropsResource != null ? globalPropsResource.getValueMap() : new ValueMapDecorator(Collections.emptyMap());
    }

    /**
     * Get global properties resource for the current resource.
     */
    protected ValueMap getSharedProperties(Page pageRoot, Resource resource) {
        String sharedPropsPath = pageRoot.getPath() + "/" + JcrConstants.JCR_CONTENT + "/" + SharedComponentProperties.NN_SHARED_COMPONENT_PROPERTIES + "/" + resource.getResourceType();
        Resource sharedPropsResource = resource.getResourceResolver().getResource(sharedPropsPath);
        return sharedPropsResource != null ? sharedPropsResource.getValueMap() : new ValueMapDecorator(Collections.emptyMap());
    }

    /**
     * Get merged properties ValueMap for the current resource.
     */
    protected ValueMap getMergedProperties(Page pageRoot, Resource resource) {
        Map<String, Object> mergedProperties = new HashMap<String, Object>();

        mergedProperties.putAll(getGlobalProperties(pageRoot, resource));
        mergedProperties.putAll(getSharedProperties(pageRoot, resource));
        mergedProperties.putAll(resource.getValueMap());

        return new ValueMapDecorator(mergedProperties);
    }
}