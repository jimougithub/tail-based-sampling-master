package com.alibaba.tailbase.clientprocess;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.tailbase.Global;


@RestController
public class ClientController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientProcessData.class.getName());

    @RequestMapping("/getWrongTrace")
    public String getWrongTrace(@RequestParam String traceIdList, @RequestParam Integer batchPos) {
    	long  startTime = System.currentTimeMillis();
        String json = ClientProcessData.getWrongTracing(traceIdList, batchPos);
        long costTime = System.currentTimeMillis() - startTime;
        Global.total_cost_time = Global.total_cost_time + costTime;
        if (costTime>2) {
        	LOGGER.warn("getWrongTracing consume time: " + costTime);
        }
        LOGGER.info("suc to getWrongTrace, batchPos:" + batchPos);
        return json;
    }
}
