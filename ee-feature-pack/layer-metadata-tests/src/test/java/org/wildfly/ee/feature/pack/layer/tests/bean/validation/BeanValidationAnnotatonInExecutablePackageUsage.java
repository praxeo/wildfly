/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.ee.feature.pack.layer.tests.bean.validation;

import jakarta.validation.executable.ValidateOnExecution;

public class BeanValidationAnnotatonInExecutablePackageUsage {
    @ValidateOnExecution
    BeanValidationAnnotatonInExecutablePackageUsage() {

    }
}
