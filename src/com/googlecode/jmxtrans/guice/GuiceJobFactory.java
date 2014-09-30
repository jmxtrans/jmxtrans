package com.googlecode.jmxtrans.guice;

import com.google.inject.Injector;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import org.quartz.spi.JobFactory;
import org.quartz.spi.TriggerFiredBundle;

import javax.inject.Inject;

public class GuiceJobFactory implements JobFactory {

	private final Injector injector;

	@Inject
	public GuiceJobFactory(Injector injector) {
		this.injector = injector;
	}

	@Override
	public Job newJob(TriggerFiredBundle bundle) throws SchedulerException {
		JobDetail jobDetail = bundle.getJobDetail();
		Class<Job> jobClass = jobDetail.getJobClass();
		return injector.getInstance(jobClass);
	}
}
