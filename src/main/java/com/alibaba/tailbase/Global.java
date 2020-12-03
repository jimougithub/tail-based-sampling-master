package com.alibaba.tailbase;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class Global {
	// save 90 batch for wrong trace
	public static int BACKEND_CHECKSUM_BATCH_COUNT = 10;
	public static List<Map<String, Set<String>>> BACKEND_CHECKSUM_BATCH_TRACE_LIST = new ArrayList<>();
    
	// save chuckSum for the total wrong trace
    public static Map<String, String> TRACE_CHUCKSUM_MAP = new ConcurrentHashMap<>();
    public static BlockingQueue<Long> BACKEND_GEN_CHECKSUM_QUEUE = new LinkedBlockingQueue<Long>();
}
