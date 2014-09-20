package com.googlecode.jmxtrans.jmx;

import java.io.File;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import com.googlecode.jmxtrans.JmxTransConfiguration;
import com.googlecode.jmxtrans.JmxTransformer;
import com.googlecode.jmxtrans.util.LifecycleException;

/**
 * The Class ManagedJmxTransformerProcess.
 * TODO: Only start/stop working, the setters don't fire update on JmxProcess
 * @author marcos.lois
 */
public class ManagedJmxTransformerProcess implements ManagedJmxTransformerProcessMBean, ManagedObject {
    
    /** The object name. */
    private ObjectName objectName;
	
	/** The proc. */
	private JmxTransformer proc;

    private final JmxTransConfiguration configuration;

	/**
	 * The Constructor.
	 *
     * @param proc the proc
     * @param configuration
     */
	public ManagedJmxTransformerProcess(JmxTransformer proc, JmxTransConfiguration configuration) {
		this.proc = proc;
        this.configuration = configuration;
    }

	/* (non-Javadoc)
	 * @see com.googlecode.jmxtrans.jmx.ManagedJmxTransformerProcessMBean#start()
	 */
	@Override
	public void start() throws LifecycleException {
		this.proc.start();
	}

	/* (non-Javadoc)
	 * @see com.googlecode.jmxtrans.jmx.ManagedJmxTransformerProcessMBean#stop()
	 */
	@Override
	public void stop() throws LifecycleException {
		this.proc.stop();
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.jmxtrans.jmx.ManagedJmxTransformerProcessMBean#getQuartPropertiesFile()
	 */
	@Override
	public String getQuartPropertiesFile() {
		return configuration.getQuartPropertiesFile();
	}

	/* (non-Javadoc)
	 * @see com.googlecode.jmxtrans.jmx.ManagedJmxTransformerProcessMBean#setQuartPropertiesFile(java.lang.String)
	 */
	@Override
	public void setQuartPropertiesFile(String quartPropertiesFile) {
		configuration.setQuartPropertiesFile(quartPropertiesFile);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.jmxtrans.jmx.ManagedJmxTransformerProcessMBean#getRunPeriod()
	 */
	@Override
	public int getRunPeriod() {
		return configuration.getRunPeriod();
	}

	/* (non-Javadoc)
	 * @see com.googlecode.jmxtrans.jmx.ManagedJmxTransformerProcessMBean#setRunPeriod(int)
	 */
	@Override
	public void setRunPeriod(int runPeriod) {
		configuration.setRunPeriod(runPeriod);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.jmxtrans.jmx.ManagedJmxTransformerProcessMBean#setJsonDirOrFile(java.io.File)
	 */
	@Override
	public void setJsonDirOrFile(File jsonDirOrFile) {
		configuration.setJsonDirOrFile(jsonDirOrFile);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.jmxtrans.jmx.ManagedJmxTransformerProcessMBean#getJsonDirOrFile()
	 */
	@Override
	public File getJsonDirOrFile() {
		return configuration.getJsonDirOrFile();
	}

	/* (non-Javadoc)
	 * @see com.googlecode.jmxtrans.jmx.ManagedObject#getObjectName()
	 */
	@Override
	public ObjectName getObjectName() throws MalformedObjectNameException {
        if (objectName == null) {
            objectName = new ObjectName("com.googlecode.jmxtrans:Type=JmxTransformerProcess,Name=JmxTransformerProcess");
        }
        return objectName;
    }

	/* (non-Javadoc)
	 * @see com.googlecode.jmxtrans.jmx.ManagedObject#setObjectName(javax.management.ObjectName)
	 */
	@Override
    public void setObjectName(ObjectName objectName) throws MalformedObjectNameException {
        this.objectName = objectName;
    }

	/* (non-Javadoc)
	 * @see com.googlecode.jmxtrans.jmx.ManagedObject#setObjectName(java.lang.String)
	 */
	@Override
    public void setObjectName(String objectName) throws MalformedObjectNameException {
        this.objectName = ObjectName.getInstance(objectName);
    }

}
