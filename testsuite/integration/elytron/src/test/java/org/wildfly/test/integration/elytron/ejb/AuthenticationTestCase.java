/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.elytron.ejb;

import static org.wildfly.test.integration.elytron.util.HttpUtil.get;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.SocketPermission;
import java.net.URL;
import java.security.Principal;
import java.util.concurrent.Callable;

import jakarta.ejb.EJB;
import javax.security.auth.AuthPermission;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.categories.CommonCriteria;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.as.test.shared.integration.ejb.security.Util;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.wildfly.security.permission.ElytronPermission;
import org.wildfly.test.integration.elytron.ejb.authentication.EntryBean;
import org.wildfly.test.integration.elytron.ejb.base.WhoAmIBean;
import org.wildfly.test.integration.elytron.util.HttpUtil;
import org.wildfly.test.security.common.elytron.EjbElytronDomainSetup;
import org.wildfly.test.security.common.elytron.ElytronDomainSetup;
import org.wildfly.test.security.common.elytron.ServletElytronDomainSetup;

/**
 * Test case to hold the authentication scenarios, these range from calling a servlet which calls a bean to calling a bean which
 * calls another bean to calling a bean which re-authenticated before calling another bean.
 *
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 *
 * NOTE: References in this file to Enterprise JavaBeans (EJB) refer to the Jakarta Enterprise Beans unless otherwise noted.
 */
@RunWith(Arquillian.class)
@ServerSetup({ AuthenticationTestCase.ElytronDomainSetupOverride.class, EjbElytronDomainSetup.class, ServletElytronDomainSetup.class })
@Category(CommonCriteria.class)
public class AuthenticationTestCase {

    /*
     * Authentication Scenarios
     *
     * Client -> Bean
     * Client -> Bean -> Bean
     * Client -> Bean (Re-auth) -> Bean
     * Client -> Servlet -> Bean
     * Client -> Servlet (Re-auth) -> Bean
     * Client -> Servlet -> Bean -> Bean
     * Client -> Servlet -> Bean (Re Auth) -> Bean
     */

    @ArquillianResource
    private URL url;

    @Deployment
    public static Archive<?> deployment() {
        final String SERVER_HOST_PORT = TestSuiteEnvironment.getHttpAddress() + ":" + TestSuiteEnvironment.getHttpPort();
        final Package currentPackage = AuthenticationTestCase.class.getPackage();
        // using JavaArchive doesn't work, because of a bug in Arquillian, it only deploys wars properly
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "ejb3security.war")
                .addPackage(WhoAmIBean.class.getPackage()).addPackage(EntryBean.class.getPackage())
                .addClass(WhoAmI.class).addClass(Util.class).addClass(Entry.class).addClass(HttpUtil.class)
                .addClasses(WhoAmIServlet.class, AuthenticationTestCase.class)
                .addClasses(ElytronDomainSetup.class, EjbElytronDomainSetup.class, ServletElytronDomainSetup.class)
                .addAsResource(currentPackage, "users.properties", "users.properties")
                .addAsResource(currentPackage, "roles.properties", "roles.properties")
                .addAsWebInfResource(currentPackage, "web.xml", "web.xml")
                .addAsWebInfResource(currentPackage, "jboss-web.xml", "jboss-web.xml")
                .addAsWebInfResource(currentPackage, "jboss-ejb3.xml", "jboss-ejb3.xml")
                .addAsManifestResource(new StringAsset("Manifest-Version: 1.0\nDependencies: org.jboss.as.controller-client,org.jboss.dmr\n"), "MANIFEST.MF")
                .addAsManifestResource(createPermissionsXmlAsset(
                        // login module needs to modify principal to commit logging in
                        new AuthPermission("modifyPrincipals"),
                        // AuthenticationTestCase#execute calls ExecutorService#shutdownNow
                        new RuntimePermission("modifyThread"),
                        // AuthenticationTestCase#execute calls sun.net.www.http.HttpClient#openServer under the hood
                        new SocketPermission(SERVER_HOST_PORT, "connect,resolve"),
                        // TestSuiteEnvironment reads system properties
                        new ElytronPermission("getSecurityDomain"),
                        new ElytronPermission("authenticate")
                        ),
                        "permissions.xml");
        war.addPackage(CommonCriteria.class.getPackage());
        return war;
    }

    @EJB(mappedName = "java:global/ejb3security/WhoAmIBean!org.wildfly.test.integration.elytron.ejb.WhoAmI")
    private WhoAmI whoAmIBean;

    @EJB(mappedName = "java:global/ejb3security/EntryBean!org.wildfly.test.integration.elytron.ejb.Entry")
    private Entry entryBean;

    @Test
    public void testAuthentication() throws Exception {
        final Callable<Void> callable = () -> {
            String response = entryBean.whoAmI();
            assertEquals("user1", response);
            return null;
        };
        Util.switchIdentity("user1", "password1", callable);
    }

    @Test
    public void testAuthentication_BadPwd() throws Exception {
        Util.switchIdentity("user1", "wrong_password", () -> entryBean.whoAmI(), true);
    }

    @Test
    public void testAuthentication_TwoBeans() throws Exception {
        final Callable<Void> callable = () -> {
            String[] response = entryBean.doubleWhoAmI();
            assertEquals("user1", response[0]);
            assertEquals("user1", response[1]);
            return null;
        };
        Util.switchIdentity("user1", "password1", callable);
    }

    @Test
    public void testAuthentication_TwoBeans_ReAuth() throws Exception {
        final Callable<Void> callable = () -> {
            String[] response = entryBean.doubleWhoAmI("user2", "password2");
            assertEquals("user1", response[0]);
            assertEquals("user2", response[1]);
            return null;
        };
        Util.switchIdentity("user1", "password1", callable);
    }

    // TODO - Similar test with first bean @RunAs - does it make sense to also manually switch?
    @Test
    public void testAuthentication_TwoBeans_ReAuth_BadPwd() throws Exception {
        Util.switchIdentity("user1", "password1", () -> entryBean.doubleWhoAmI("user2", "wrong_password"), true);
    }

    @Test
    public void testAuthenticatedCall() throws Exception {
        // TODO: this is not spec
        final Callable<Void> callable = () -> {
            try {
                final Principal principal = whoAmIBean.getCallerPrincipal();
                assertNotNull("EJB 3.1 FR 17.6.5 The container must never return a null from the getCallerPrincipal method.",
                        principal);
                assertEquals("user1", principal.getName());
            } catch (RuntimeException e) {
                e.printStackTrace();
                fail("EJB 3.1 FR 17.6.5 The EJB container must provide the caller’s security context information during the execution of a business method ("
                        + e.getMessage() + ")");
            }
            return null;
        };
        Util.switchIdentitySCF("user1", "password1", callable);
    }

    @Test
    public void testUnauthenticated() throws Exception {
        try {
            final Principal principal = whoAmIBean.getCallerPrincipal();
            assertNotNull("EJB 3.1 FR 17.6.5 The container must never return a null from the getCallerPrincipal method.",
                    principal);
            // TODO: where is 'anonymous' configured?
            assertEquals("anonymous", principal.getName());
        } catch (RuntimeException e) {
            e.printStackTrace();
            fail("EJB 3.1 FR 17.6.5 The EJB container must provide the caller’s security context information during the execution of a business method ("
                    + e.getMessage() + ")");
        }
    }

    @Test
    public void testAuthentication_ViaServlet() throws Exception {
        final String result = getWhoAmI("?method=whoAmI");
        assertEquals("user1", result);
    }

    @Test
    public void testAuthentication_ReAuth_ViaServlet() throws Exception {
        final String result = getWhoAmI("?method=whoAmI&username=user2&password=password2");
        assertEquals("user2", result);
    }

    @Test
    public void testAuthentication_TwoBeans_ViaServlet() throws Exception {
        final String result = getWhoAmI("?method=doubleWhoAmI");
        assertEquals("user1,user1", result);
    }

    @Test
    public void testAuthentication_TwoBeans_ReAuth_ViaServlet() throws Exception {
        final String result = getWhoAmI("?method=doubleWhoAmI&username=user2&password=password2");
        assertEquals("user1,user2", result);
    }

    @Test
    @RunAsClient
    public void testAuthentication_TwoBeans_ReAuth__BadPwd_ViaServlet() throws Exception {
        try {
            getWhoAmI("?method=doubleWhoAmI&username=user2&password=bad_password");
            fail("Expected IOException");
        } catch (IOException e) {
            final String message = e.getMessage();
            assertTrue("Response should contain 'ELY01151: Evidence Verification Failed'", message.contains("ELY01151:"));
            assertTrue("Response should contain 'jakarta.ejb.EJBException' or 'jakarta.ejb.EJBException'",
                    message.contains("jakarta.ejb.EJBException") || message.contains("jakarta.ejb.EJBException"));
        }
    }

    /*
     * isCallerInRole Scenarios
     */

    @Test
    public void testICIRSingle() throws Exception {
        final Callable<Void> callable = () -> {
            assertTrue(entryBean.doIHaveRole("Users"));
            assertTrue(entryBean.doIHaveRole("Role1"));
            assertFalse(entryBean.doIHaveRole("Role2"));
            return null;
        };
        Util.switchIdentity("user1", "password1", callable);
    }

    @Test
    public void testICIR_TwoBeans() throws Exception {
        final Callable<Void> callable = () -> {
            boolean[] response;
            response = entryBean.doubleDoIHaveRole("Users");
            assertTrue(response[0]);
            assertTrue(response[1]);

            response = entryBean.doubleDoIHaveRole("Role1");
            assertTrue(response[0]);
            assertTrue(response[1]);

            response = entryBean.doubleDoIHaveRole("Role2");
            assertFalse(response[0]);
            assertFalse(response[1]);
            return null;
        };
        Util.switchIdentity("user1", "password1", callable);
    }

    @Test
    public void testICIR_TwoBeans_ReAuth() throws Exception {
        final Callable<Void> callable = () -> {
            boolean[] response;
            response = entryBean.doubleDoIHaveRole("Users", "user2", "password2");
            assertTrue(response[0]);
            assertTrue(response[1]);

            response = entryBean.doubleDoIHaveRole("Role1", "user2", "password2");
            assertTrue(response[0]);
            assertFalse(response[1]);

            response = entryBean.doubleDoIHaveRole("Role2", "user2", "password2");
            assertFalse(response[0]);
            assertTrue(response[1]);
            return null;
        };
        Util.switchIdentity("user1", "password1", callable);
    }

    private String getWhoAmI(String queryString) throws Exception {
        return get(url + "whoAmI" + queryString, "user1", "password1", 10, SECONDS);
    }

    @Test
    public void testICIR_ViaServlet() throws Exception {
        String result = getWhoAmI("?method=doIHaveRole&role=Users");
        assertEquals("true", result);
        result = getWhoAmI("?method=doIHaveRole&role=Role1");
        assertEquals("true", result);
        result = getWhoAmI("?method=doIHaveRole&role=Role2");
        assertEquals("false", result);
    }

    @Test
    public void testICIR_ReAuth_ViaServlet() throws Exception {
        String result = getWhoAmI("?method=doIHaveRole&role=Users&username=user2&password=password2");
        assertEquals("true", result);
        result = getWhoAmI("?method=doIHaveRole&role=Role1&username=user2&password=password2");
        assertEquals("false", result);
        result = getWhoAmI("?method=doIHaveRole&role=Role2&username=user2&password=password2");
        assertEquals("true", result);
    }

    @Test
    public void testICIR_TwoBeans_ViaServlet() throws Exception {
        String result = getWhoAmI("?method=doubleDoIHaveRole&role=Users");
        assertEquals("true,true", result);
        result = getWhoAmI("?method=doubleDoIHaveRole&role=Role1");
        assertEquals("true,true", result);
        result = getWhoAmI("?method=doubleDoIHaveRole&role=Role2");
        assertEquals("false,false", result);
    }

    @Test
    public void testICIR_TwoBeans_ReAuth_ViaServlet() throws Exception {
        String result = getWhoAmI("?method=doubleDoIHaveRole&role=Users&username=user2&password=password2");
        assertEquals("true,true", result);
        result = getWhoAmI("?method=doubleDoIHaveRole&role=Role1&username=user2&password=password2");
        assertEquals("true,false", result);
        result = getWhoAmI("?method=doubleDoIHaveRole&role=Role2&username=user2&password=password2");
        assertEquals("false,true", result);
    }

    /*
     * isCallerInRole Scenarios with @RunAs Defined
     *
     * EJB 3.1 FR 17.2.5.2 isCallerInRole tests the principal that represents the caller of the enterprise bean, not the
     * principal that corresponds to the run-as security identity for the bean.
     */

    // 17.2.5 - Programatic Access to Caller's Security Context
    // Include tests for methods not implemented to pick up if later they are implemented.
    // 17.2.5.1 - Use of getCallerPrincipal
    // 17.6.5 - Security Methods on EJBContext
    // 17.2.5.2 - Use of isCallerInRole
    // 17.2.5.3 - Declaration of Security Roles Referenced from the Bean's Code
    // 17.3.1 - Security Roles
    // 17.3.2.1 - Specification of Method Permissions with Metadata Annotation
    // 17.3.2.2 - Specification of Method Permissions in the Deployment Descriptor
    // 17.3.2.3 - Unspecified Method Permission
    // 17.3.3 - Linking Security Role References to Security Roles
    // 17.3.4 - Specification on Security Identities in the Deployment Descriptor
    // (Include permutations for overrides esp where deployment descriptor removes access)
    // 17.3.4.1 - Run-as
    // 17.5 EJB Client Responsibilities
    // A transactional client can not change principal association within transaction.
    // A session bean client must not change the principal association for the duration of the communication.
    // If transactional requests within a single transaction arrive from multiple clients all must be associated
    // with the same security context.

    // 17.6.3 - Security Mechanisms
    // 17.6.4 - Passing Principals on EJB Calls
    // 17.6.6 - Secure Access to Resource Managers
    // 17.6.7 - Principal Mapping
    // 17.6.9 - Runtime Security Enforcement
    // 17.6.10 - Audit Trail

    static class ElytronDomainSetupOverride extends ElytronDomainSetup {
        public ElytronDomainSetupOverride() {
            super(new File(AuthenticationTestCase.class.getResource("users.properties").getFile()).getAbsolutePath(),
                    new File(AuthenticationTestCase.class.getResource("roles.properties").getFile()).getAbsolutePath());
        }
    }

}
