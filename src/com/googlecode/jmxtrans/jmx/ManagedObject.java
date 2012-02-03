package com.googlecode.jmxtrans.jmx;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

/**
 * The Interface ManagedObject represents a MBean managed object,
 * force some ObjectName methods, used on register / unregister
 * in the MBeanServer.
 * 
 * @author marcos.lois
 */
public interface ManagedObject {
	
	/**
	 * Gets the object name.
	 *
	 * @return the object name
	 * @throws MalformedObjectNameException if the object name is not valid
	 */
	public ObjectName getObjectName() throws MalformedObjectNameException;
    
    /**
     * Sets the object name.
     *
     * @param objectName the object name
     * @throws MalformedObjectNameException if the object name is not valid
     */
    public void setObjectName(ObjectName objectName) throws MalformedObjectNameException;
    
    /**
     * Sets the object name.
     *
     * @param objectName the object name
     * @throws MalformedObjectNameException if the object name is not valid
     */
    public void setObjectName(String objectName) throws MalformedObjectNameException;
}
