package com.example.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.amqp.AmqpItemReader;
import org.springframework.batch.item.amqp.builder.AmqpItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableBatchProcessing
public class JobConfig {
	private final JobBuilderFactory jobBuilderFactory;

	private final StepBuilderFactory stepBuilderFactory;

	public JobConfig(JobBuilderFactory jobBuilderFactory, StepBuilderFactory stepBuilderFactory) {
		this.jobBuilderFactory = jobBuilderFactory;
		this.stepBuilderFactory = stepBuilderFactory;
	}


	@Bean
	public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, Jackson2JsonMessageConverter jackson2JsonMessageConverter) {
		final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
		rabbitTemplate.setMessageConverter(jackson2JsonMessageConverter);
		rabbitTemplate.setDefaultReceiveQueue("hello.demo");
		rabbitTemplate.setReceiveTimeout(5_000);
		return rabbitTemplate;
	}

	@Bean
	public Jackson2JsonMessageConverter producerJackson2MessageConverter() {
		return new Jackson2JsonMessageConverter();
	}

	@Bean
	public AmqpItemReader<Hello> amqpItemReader(AmqpTemplate amqpTemplate) {
		return new AmqpItemReaderBuilder<Hello>()
				.itemType(Hello.class)
				.amqpTemplate(amqpTemplate)
				.build();
	}

	@Bean
	public Step step(AmqpItemReader<Hello> itemReader) {
		final Logger logger = LoggerFactory.getLogger("demo");
		return stepBuilderFactory.get("step").<Hello, String>chunk(30)
				.reader(itemReader)
				.processor((ItemProcessor<? super Hello, ? extends String>) Hello::message)
				.writer(items -> {
					logger.info("{}", items);
				})
				.build();
	}

	@Bean
	public Job job(ShutdownJobExecutionListener shutdownJobExecutionListener) {
		return this.jobBuilderFactory.get("job").incrementer(new RunIdIncrementer())
				.start(step(null))
				.listener(shutdownJobExecutionListener)
				.build();
	}
}
