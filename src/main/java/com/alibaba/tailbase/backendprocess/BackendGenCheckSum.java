package com.alibaba.tailbase.backendprocess;

import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.alibaba.tailbase.Global;
import com.alibaba.tailbase.Utils;

@Service
public class BackendGenCheckSum {
	private static final Logger LOGGER = LoggerFactory.getLogger(BackendGenCheckSum.class.getName());
	
	@Async("asyncBackendGenCheckSumExecutor")
	public void run() {
		LOGGER.warn("--------------BackendGenCheckSumThread started--------------");
		Map<String, Set<String>> map = null;
		while (true) {
			try {
				Long pos = Global.BACKEND_GEN_CHECKSUM_QUEUE.poll(60, TimeUnit.SECONDS);
				if (pos != null) {
					map = Global.BACKEND_CHECKSUM_BATCH_TRACE_LIST.get(pos.intValue());
					// Generating check sum ===========================================================================
	                for (Map.Entry<String, Set<String>> entry : map.entrySet()) {
	                    String traceId = entry.getKey();
	                    Set<String> spanSet = entry.getValue();
	                    // order span with startTime
	                    String spans = spanSet.stream().sorted(Comparator.comparing(CheckSumService::getStartTime)).collect(Collectors.joining("\n"));
	                    spans = spans + "\n";
	                    // output all span to check
	                    Global.TRACE_CHUCKSUM_MAP.put(traceId, Utils.MD5(spans));
	                }
	                // Release map
	                map.clear();
	                LOGGER.warn("released map: " + pos);
				}
			} catch (InterruptedException e) {
				map.clear();
				LOGGER.error("Poll BACKEND_GEN_CHECKSUM_QUEUE error: " + e.getMessage());
			}
		}
	}
    
}
