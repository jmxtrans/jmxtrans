package com.googlecode.jmxtrans.model.filter;

import java.util.Arrays;
import java.util.List;

import weblogic.health.HealthState;

/**
 * The WeblogicHealthFilter convert weblogic.health.HealthState 
 * records to its numerical values:
 * 
 * HEALTH_OK         = 0
 * HEALTH_WARN       = 1
 * HEALTH_CRITICAL   = 2
 * HEALTH_FAILED     = 3
 * HEALTH_OVERLOADED = 4
 * 
 * @author marcos.lois
 */
public class WeblogicHealthFilter implements Filter {
	
	/* (non-Javadoc)
	 * @see com.googlecode.jmxtrans.model.filter.Filter#doFilter(java.lang.Object)
	 */
	@Override
	public Object doFilter(Object obj) {
		if (obj instanceof HealthState) {
			return ((HealthState) obj).getState();
		} else {
			return obj;
		}
	}

	/* (non-Javadoc)
	 * @see com.googlecode.jmxtrans.model.filter.Filter#getClassNames()
	 */
	@Override
	public List<String> getClassNames() {
		return Arrays.asList(new String[]{weblogic.health.HealthState.class.getName()});
	}

}
