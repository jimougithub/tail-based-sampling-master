package com.alibaba.tailbase;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

import com.alibaba.tailbase.backendprocess.BackendController;
import com.alibaba.tailbase.backendprocess.CheckSumService;
import com.alibaba.tailbase.clientprocess.ClientProcessData;

@EnableAutoConfiguration
@ComponentScan(basePackages = "com.alibaba.tailbase")
@EnableAsync
public class MultiEntry {
    public static void main(String[] args) {
        if (Utils.isBackendProcess()) {
            BackendController.init();
            CheckSumService.init();
            CheckSumService.start();
        }
        if (Utils.isClientProcess()) {
            ClientProcessData.init();
        }
        
        String port = System.getProperty("server.port", "8080");
        System.out.println("\nCurrent port:" + port);
        
        ConfigurableApplicationContext context = SpringApplication.run(MultiEntry.class,"--server.port=" + port);
        context.getBean(ControlService.class).startUp();
    }
}
