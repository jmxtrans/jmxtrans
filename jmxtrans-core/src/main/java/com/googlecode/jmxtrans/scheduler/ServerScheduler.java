/**
 * The MIT License
 * Copyright © 2010 JmxTrans team
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.googlecode.jmxtrans.scheduler;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.googlecode.jmxtrans.cli.JmxTransConfiguration;
import com.googlecode.jmxtrans.executors.ExecutorRepository;
import com.googlecode.jmxtrans.jmx.ResultProcessor;
import com.googlecode.jmxtrans.model.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ServerScheduler {
	private static final Logger log = LoggerFactory.getLogger(ServerScheduler.class);

	private final JmxTransConfiguration configuration;
	private final ScheduledExecutorService scheduledExecutor;
	private final List<ScheduledServerCommand> scheduledServerCommands = new ArrayList<>();
	private final ExecutorRepository queryExecutorRepository;
	private final ResultProcessor resultProcessor;

	@Inject
	public ServerScheduler(
			@Nonnull JmxTransConfiguration configuration,
			@Nonnull ScheduledExecutorService scheduledExecutor,
			@Nonnull @Named("queryExecutorRepository") ExecutorRepository queryExecutorRepository,
			@Nonnull ResultProcessor resultProcessor) {
		this.configuration = configuration;
		this.scheduledExecutor = scheduledExecutor;
		this.queryExecutorRepository = queryExecutorRepository;
		this.resultProcessor = resultProcessor;
	}

	public void start() {
	}

	private static class ScheduledServerCommand {
		private final ServerCommand serverCommand;
		private final ScheduledFuture<?> scheduledFuture;

		private ScheduledServerCommand(ServerCommand serverCommand, ScheduledFuture<?> scheduledFuture) {
			this.serverCommand = serverCommand;
			this.scheduledFuture = scheduledFuture;
		}

		private String getName() {
			return serverCommand.getName();
		}

		private void cancel() {
			scheduledFuture.cancel(true);
		}
	}

	public void schedule(Server server) {
		ServerCommand serverCommand = new ServerCommand(server, queryExecutorRepository, resultProcessor);
		long runPeriod = serverCommand.getRunPeriodSeconds(configuration.getRunPeriod());
		ScheduledFuture<?> scheduledFuture = scheduledExecutor.scheduleAtFixedRate(serverCommand, runPeriod, runPeriod, TimeUnit.SECONDS);
		synchronized (this.scheduledServerCommands) {
			this.scheduledServerCommands.add(new ScheduledServerCommand(serverCommand, scheduledFuture));
		}

		log.debug("Scheduled job for server {}", serverCommand.getName());
	}

	public void unscheduleAll() {
		synchronized (this.scheduledServerCommands) {
			for (Iterator<ScheduledServerCommand> commandsIterator = scheduledServerCommands.iterator(); commandsIterator.hasNext(); ) {
				ScheduledServerCommand command = commandsIterator.next();
				command.cancel();
				commandsIterator.remove();
				log.debug("Deleted scheduled job for server {}", command.getName());
			}
		}
	}

	public void stop() {
		if (!scheduledExecutor.isShutdown()) {
			scheduledExecutor.shutdown();
			log.debug("Shutdown scheduler");
		}
	}

}
