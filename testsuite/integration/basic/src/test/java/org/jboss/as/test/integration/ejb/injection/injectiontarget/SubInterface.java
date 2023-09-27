/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.injection.injectiontarget;

import jakarta.ejb.Local;

/**
 * @author Stuart Douglas
 */
@Local
public interface SubInterface extends SuperInterface {
}
