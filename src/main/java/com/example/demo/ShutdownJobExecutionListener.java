package com.example.demo;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

/**
 * https://stackoverflow.com/questions/47273106/rabbittemplate-not-ending-the-spring-boot-spring-batch-job-app
 */
@Component
public class ShutdownJobExecutionListener implements JobExecutionListener {
	private final Logger log = LoggerFactory.getLogger(ShutdownJobExecutionListener.class);

	private final ConfigurableApplicationContext context;

	public ShutdownJobExecutionListener(ConfigurableApplicationContext context) {
		this.context = context;
	}

	@Override
	public void beforeJob(JobExecution jobExecution) {
		// NO-OP
	}

	@Override
	public void afterJob(JobExecution jobExecution) {
		log.info("Shutdown...");
		// https://github.com/spring-projects/spring-boot/blob/v2.7.0/spring-boot-project/spring-boot-actuator/src/main/java/org/springframework/boot/actuate/context/ShutdownEndpoint.java#L56-L71
		Thread thread = new Thread(this::performShutdown);
		thread.setContextClassLoader(getClass().getClassLoader());
		thread.start();
	}

	private void performShutdown() {
		try {
			TimeUnit.MILLISECONDS.sleep(500);
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
		this.context.close();
	}
}
