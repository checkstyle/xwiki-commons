/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.xwiki.extension.handler.internal;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.extension.ExtensionDependency;
import org.xwiki.extension.ExtensionException;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.handler.ExtensionHandlerManager;
import org.xwiki.extension.handler.ExtensionInitializer;
import org.xwiki.extension.job.internal.ExtensionPlanContext;
import org.xwiki.extension.repository.CoreExtensionRepository;
import org.xwiki.extension.repository.InstalledExtensionRepository;

/**
 * Default implementation of {@link org.xwiki.extension.handler.ExtensionInitializer}.
 *
 * @version $Id$
 * @since 4.0M1
 */
@Component
@Singleton
public class DefaultExtensionInitializer implements ExtensionInitializer, Initializable
{
    /**
     * The local extension repository from which extension are initialized.
     */
    @Inject
    private InstalledExtensionRepository installedExtensionRepository;

    /**
     * The extension manager to launch extension initialization.
     */
    @Inject
    private ExtensionHandlerManager extensionHandlerManager;

    /**
     * The core extension repository to check extension dependency availability.
     */
    @Inject
    private CoreExtensionRepository coreExtensionRepository;

    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    @Override
    public void initialize()
    {
        initialize(null, null);
    }

    @Override
    public void initialize(String namespaceToLoad)
    {
        initialize(namespaceToLoad, null);
    }

    @Override
    public void initialize(String namespaceToInitialize, String type)
    {
        Map<String, Map<InstalledExtension, Boolean>> initializedExtensions = new HashMap<>();

        // Load extensions from local repository
        Collection<InstalledExtension> installedExtensions;
        if (namespaceToInitialize != null) {
            installedExtensions = this.installedExtensionRepository.getInstalledExtensions(namespaceToInitialize);
        } else {
            installedExtensions = this.installedExtensionRepository.getInstalledExtensions();
        }
        for (InstalledExtension installedExtension : installedExtensions) {
            if (type == null || type.equals(installedExtension.getType())) {
                try {
                    initializeExtension(installedExtension, namespaceToInitialize, initializedExtensions);
                } catch (Throwable t) {
                    this.logger.error("Failed to initialize local extension [{}]", installedExtension.getId(), t);
                }
            }
        }
    }

    private void initializeExtension(InstalledExtension installedExtension, String namespaceToLoad,
        Map<String, Map<InstalledExtension, Boolean>> initializedExtensions) throws ExtensionException
    {
        if (installedExtension.getNamespaces() != null) {
            if (namespaceToLoad == null) {
                for (String namespace : installedExtension.getNamespaces()) {
                    initializeExtensionInNamespace(installedExtension, namespace, initializedExtensions,
                        new ExtensionPlanContext());
                }
            } else if (installedExtension.getNamespaces().contains(namespaceToLoad)) {
                initializeExtensionInNamespace(installedExtension, namespaceToLoad, initializedExtensions,
                    new ExtensionPlanContext());
            }
        } else if (namespaceToLoad == null) {
            initializeExtensionInNamespace(installedExtension, null, initializedExtensions, new ExtensionPlanContext());
        }
    }

    private boolean initializeExtensionInNamespace(InstalledExtension installedExtension, String namespace,
        Map<String, Map<InstalledExtension, Boolean>> initializedExtensions, ExtensionPlanContext extensionContext)
        throws ExtensionException
    {
        // Check if the extension can be available from this namespace
        if (!installedExtension.isValid(namespace)) {
            return false;
        }

        Map<InstalledExtension, Boolean> initializedExtensionsInNamespace = initializedExtensions.get(namespace);

        if (initializedExtensionsInNamespace == null) {
            initializedExtensionsInNamespace = new HashMap<>();
            initializedExtensions.put(namespace, initializedExtensionsInNamespace);
        }

        Boolean initialized = initializedExtensionsInNamespace.get(installedExtension);
        if (initialized == null) {
            initialized = initializeExtensionInNamespace(installedExtension, namespace, initializedExtensions,
                initializedExtensionsInNamespace, extensionContext);
        }

        return initialized;
    }

    private boolean initializeExtensionInNamespace(InstalledExtension installedExtension, String namespace,
        Map<String, Map<InstalledExtension, Boolean>> initializedExtensions,
        Map<InstalledExtension, Boolean> initializedExtensionsInNamespace, ExtensionPlanContext extensionContext)
        throws ExtensionException
    {
        if (namespace != null && installedExtension.getNamespaces() == null) {
            // This extension is supposed to be installed on root namespace only so redirecting to null namespace
            // initialization
            return initializeExtensionInNamespace(installedExtension, null, initializedExtensions, extensionContext);
        } else {
            boolean intialized = false;

            try {
                // Initialize dependencies
                for (ExtensionDependency dependency : installedExtension.getDependencies()) {
                    initializeExtensionDependencyInNamespace(installedExtension, dependency, namespace,
                        initializedExtensions, new ExtensionPlanContext(extensionContext, installedExtension));
                }

                // Initialize the extension
                this.extensionHandlerManager.initialize(installedExtension, namespace);

                intialized = true;
            } finally {
                // Cache the extension to not initialize several times
                initializedExtensionsInNamespace.put(installedExtension, intialized);
            }

            return intialized;
        }
    }

    private void initializeExtensionDependencyInNamespace(InstalledExtension installedExtension,
        ExtensionDependency dependency, String namespace,
        Map<String, Map<InstalledExtension, Boolean>> initializedExtensions, ExtensionPlanContext extensionContext)
        throws ExtensionException
    {
        if (!this.coreExtensionRepository.exists(dependency.getId())) {
            InstalledExtension dependencyExtension =
                this.installedExtensionRepository.getInstalledExtension(dependency.getId(), namespace);

            if (dependencyExtension != null) {
                if (dependencyExtension == installedExtension) {
                    throw new ExtensionException(String.format(
                        "Extension [%s] has itself as a dependency ([%s]). "
                            + "It usually means an extension is installed along with one of its features.",
                        installedExtension, dependency));
                }

                try {
                    if (!initializeExtensionInNamespace(dependencyExtension, namespace, initializedExtensions,
                        new ExtensionPlanContext(extensionContext, dependency))) {
                        throw new ExtensionException(String.format(
                            "Extension [%s] cannot be initialized because its dependency ([%s]) could not.",
                            installedExtension, dependency));
                    }
                } catch (Throwable e) {
                    if (dependency.isOptional()) {
                        this.logger.warn("Failed to initialize dependency [{}]: {}", dependency,
                            ExceptionUtils.getRootCauseMessage(e));
                    } else {
                        throw new ExtensionException(String.format("Failed to initialize dependency [%s]", dependency),
                            e);
                    }
                }
            }
        }
    }
}
