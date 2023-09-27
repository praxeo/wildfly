/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ws.wsa;

import jakarta.jws.HandlerChain;
import jakarta.jws.WebService;
import jakarta.xml.ws.soap.Addressing;

@WebService
        (
                portName = "AddressingServicePort",
                serviceName = "AddressingService",
                targetNamespace = "http://www.jboss.org/jbossws/ws-extensions/wsaddressing",
                endpointInterface = "org.jboss.as.test.integration.ws.wsa.ServiceIface"
        )
@Addressing(enabled = true, required = true)
@HandlerChain(file = "ws-handler.xml")
public class ServiceImplAddressingRequired implements ServiceIface {

    public String sayHello(String name) {
        return "Hello " + name + "!";
    }
}
