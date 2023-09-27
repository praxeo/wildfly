/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jca.annorar;

import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Set;
import org.jboss.logging.Logger;
import jakarta.resource.ResourceException;
import jakarta.resource.spi.ConfigProperty;
import jakarta.resource.spi.ConnectionDefinition;
import jakarta.resource.spi.ConnectionManager;
import jakarta.resource.spi.ConnectionRequestInfo;
import jakarta.resource.spi.ManagedConnection;
import jakarta.resource.spi.ManagedConnectionFactory;
import jakarta.resource.spi.ResourceAdapter;
import jakarta.resource.spi.ResourceAdapterAssociation;
import javax.security.auth.Subject;

/**
 * AnnoManagedConnectionFactory
 *
 * @version $Revision: $
 */
@ConnectionDefinition(connectionFactory = AnnoConnectionFactory1.class, connectionFactoryImpl = AnnoConnectionFactoryImpl1.class, connection = AnnoConnection1.class, connectionImpl = AnnoConnectionImpl1.class)
public class AnnoManagedConnectionFactory1 implements ManagedConnectionFactory,
        ResourceAdapterAssociation {

    /**
     * The serial version UID
     */
    private static final long serialVersionUID = 1L;

    /**
     * The logger
     */
    private static Logger log = Logger
            .getLogger("AnnoManagedConnectionFactory");

    /**
     * The resource adapter
     */
    private ResourceAdapter ra;

    /**
     * The logwriter
     */
    private PrintWriter logwriter;

    /**
     * first
     */
    @ConfigProperty(defaultValue = "2", description = {"1st", "first"}, ignore = true, supportsDynamicUpdates = false, confidential = true)
    private Byte first;

    /**
     * second
     */
    private Short second;

    /**
     * Default constructor
     */
    public AnnoManagedConnectionFactory1() {

    }

    /**
     * Set first
     *
     * @param first The value
     */
    public void setFirst(Byte first) {
        this.first = first;
    }

    /**
     * Get first
     *
     * @return The value
     */
    public Byte getFirst() {
        return first;
    }

    /**
     * Set second
     *
     * @param second The value
     */
    @ConfigProperty(defaultValue = "1", description = {"2nd", "second"}, ignore = false, supportsDynamicUpdates = true, confidential = false)
    public void setSecond(Short second) {
        this.second = second;
    }

    /**
     * Get second
     *
     * @return The value
     */
    public Short getSecond() {
        return second;
    }

    /**
     * Creates a Connection Factory instance.
     *
     * @param cxManager ConnectionManager to be associated with created EIS connection
     *                  factory instance
     * @return EIS-specific Connection Factory instance or
     * jakarta.resource.cci.ConnectionFactory instance
     * @throws ResourceException Generic exception
     */
    public Object createConnectionFactory(ConnectionManager cxManager)
            throws ResourceException {
        log.trace("createConnectionFactory()");
        return new AnnoConnectionFactoryImpl1(this, cxManager);
    }

    /**
     * Creates a Connection Factory instance.
     *
     * @return EIS-specific Connection Factory instance or
     * jakarta.resource.cci.ConnectionFactory instance
     * @throws ResourceException Generic exception
     */
    public Object createConnectionFactory() throws ResourceException {
        throw new ResourceException(
                "This resource adapter doesn't support non-managed environments");
    }

    /**
     * Creates a new physical connection to the underlying EIS resource manager.
     *
     * @param subject       Caller's security information
     * @param cxRequestInfo Additional resource adapter specific connection request
     *                      information
     * @return ManagedConnection instance
     * @throws ResourceException generic exception
     */
    public ManagedConnection createManagedConnection(Subject subject,
                                                     ConnectionRequestInfo cxRequestInfo) throws ResourceException {
        log.trace("createManagedConnection()");
        return new AnnoManagedConnection1(this);
    }

    /**
     * Returns a matched connection from the candidate set of connections.
     *
     * @param connectionSet Candidate connection set
     * @param subject       Caller's security information
     * @param cxRequestInfo Additional resource adapter specific connection request
     *                      information
     * @return ManagedConnection if resource adapter finds an acceptable match
     * otherwise null
     * @throws ResourceException generic exception
     */
    public ManagedConnection matchManagedConnections(Set connectionSet,
                                                     Subject subject, ConnectionRequestInfo cxRequestInfo)
            throws ResourceException {
        log.trace("matchManagedConnections()");
        ManagedConnection result = null;
        Iterator it = connectionSet.iterator();
        while (result == null && it.hasNext()) {
            ManagedConnection mc = (ManagedConnection) it.next();
            if (mc instanceof AnnoManagedConnection1) {
                result = mc;
            }

        }
        return result;
    }

    /**
     * Get the log writer for this ManagedConnectionFactory instance.
     *
     * @return PrintWriter
     * @throws ResourceException generic exception
     */
    public PrintWriter getLogWriter() throws ResourceException {
        log.trace("getLogWriter()");
        return logwriter;
    }

    /**
     * Set the log writer for this ManagedConnectionFactory instance.
     *
     * @param out PrintWriter - an out stream for error logging and tracing
     * @throws ResourceException generic exception
     */
    public void setLogWriter(PrintWriter out) throws ResourceException {
        log.trace("setLogWriter()");
        logwriter = out;
    }

    /**
     * Get the resource adapter
     *
     * @return The handle
     */
    public ResourceAdapter getResourceAdapter() {
        log.trace("getResourceAdapter()");
        return ra;
    }

    /**
     * Set the resource adapter
     *
     * @param ra The handle
     */
    public void setResourceAdapter(ResourceAdapter ra) {
        log.trace("setResourceAdapter()");
        this.ra = ra;
    }

    /**
     * Returns a hash code value for the object.
     *
     * @return A hash code value for this object.
     */
    @Override
    public int hashCode() {
        int result = 17;
        if (first != null) { result += 31 * result + 7 * first.hashCode(); } else { result += 31 * result + 7; }
        if (second != null) { result += 31 * result + 7 * second.hashCode(); } else { result += 31 * result + 7; }
        return result;
    }

    /**
     * Indicates whether some other object is equal to this one.
     *
     * @param other The reference object with which to compare.
     * @return true if this object is the same as the obj argument, false
     * otherwise.
     */
    @Override
    public boolean equals(Object other) {
        if (other == null) { return false; }
        if (other == this) { return true; }
        if (!(other instanceof AnnoManagedConnectionFactory1)) { return false; }
        boolean result = true;
        AnnoManagedConnectionFactory1 obj = (AnnoManagedConnectionFactory1) other;
        if (result) {
            if (first == null) { result = obj.getFirst() == null; } else { result = first.equals(obj.getFirst()); }
        }
        if (result) {
            if (second == null) { result = obj.getSecond() == null; } else { result = second.equals(obj.getSecond()); }
        }
        return result;
    }

}
