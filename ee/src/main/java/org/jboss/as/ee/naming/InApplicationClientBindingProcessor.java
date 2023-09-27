/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.naming;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.ComponentNamingMode;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.ValueManagedReferenceFactory;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.BinderService;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

/**
 * Processor responsible for binding java:comp/InAppClientContainer
 *
 * @author Stuart Douglas
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class InApplicationClientBindingProcessor implements DeploymentUnitProcessor {

    private final boolean appclient;

    public InApplicationClientBindingProcessor(final boolean appclient) {
        this.appclient = appclient;
    }

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final EEModuleDescription moduleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);

        if(moduleDescription == null) {
            return;
        }

        final ServiceTarget serviceTarget = phaseContext.getServiceTarget();
        //if this is a war we need to bind to the modules comp namespace
        if(DeploymentTypeMarker.isType(DeploymentType.WAR,deploymentUnit) ||
                DeploymentTypeMarker.isType(DeploymentType.APPLICATION_CLIENT, deploymentUnit)) {
            final ServiceName moduleContextServiceName = ContextNames.contextServiceNameOfModule(moduleDescription.getApplicationName(),moduleDescription.getModuleName());
            bindServices(deploymentUnit, serviceTarget, moduleContextServiceName);
        }

        for(ComponentDescription component : moduleDescription.getComponentDescriptions()) {
            if(component.getNamingMode() == ComponentNamingMode.CREATE) {
                final ServiceName compContextServiceName = ContextNames.contextServiceNameOfComponent(moduleDescription.getApplicationName(),moduleDescription.getModuleName(),component.getComponentName());
                bindServices(deploymentUnit, serviceTarget, compContextServiceName);
            }
        }

    }

    private void bindServices(DeploymentUnit deploymentUnit, ServiceTarget serviceTarget, ServiceName contextServiceName) {
        BinderService inAppClientContainerService = new BinderService("InAppClientContainer");
        final ServiceName inAppClientServiceName = contextServiceName.append("InAppClientContainer");
        inAppClientContainerService.getManagedObjectInjector().inject(new ValueManagedReferenceFactory(appclient));
        serviceTarget.addService(inAppClientServiceName, inAppClientContainerService)
            .addDependency(contextServiceName, ServiceBasedNamingStore.class, inAppClientContainerService.getNamingStoreInjector())
            .install();
        final Map<ServiceName, Set<ServiceName>> jndiComponentDependencies = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.COMPONENT_JNDI_DEPENDENCIES);
        Set<ServiceName> jndiDependencies = jndiComponentDependencies.get(contextServiceName);
        if (jndiDependencies == null) {
            jndiComponentDependencies.put(contextServiceName, jndiDependencies = new HashSet<>());
        }
        jndiDependencies.add(inAppClientServiceName);
    }
}
