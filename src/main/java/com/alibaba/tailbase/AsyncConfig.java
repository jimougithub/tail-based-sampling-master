package com.alibaba.tailbase;

import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class AsyncConfig implements AsyncConfigurer {
	@Value("${thread.backend.gen.checksum}")
	int BACKEDN_GEN_CHECKSUM_THREAD;
	
	public int getBackendGenCheckSumThreadCount() {
		return BACKEDN_GEN_CHECKSUM_THREAD;
	}
	
	@Bean
	public Executor asyncBackendGenCheckSumExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(BACKEDN_GEN_CHECKSUM_THREAD);
		executor.setMaxPoolSize(BACKEDN_GEN_CHECKSUM_THREAD);
		executor.setQueueCapacity(BACKEDN_GEN_CHECKSUM_THREAD);
		executor.setThreadNamePrefix("backend_gen_checksum_thread-");
		executor.initialize();
		return executor;
	}
	
	@Bean
	public Executor asyncSocketReceiveExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(2);
		executor.setMaxPoolSize(2);
		executor.setQueueCapacity(2);
		executor.setThreadNamePrefix("socket_receive_thread-");
		executor.initialize();
		return executor;
	}
	
	@Bean
	public Executor asyncSocketSendExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(1);
		executor.setMaxPoolSize(1);
		executor.setQueueCapacity(1);
		executor.setThreadNamePrefix("socket_send_thread-");
		executor.initialize();
		return executor;
	}
}
