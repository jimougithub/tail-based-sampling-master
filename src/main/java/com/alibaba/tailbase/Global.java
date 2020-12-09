package com.alibaba.tailbase;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class Global {
	public static boolean SYSTEM_READY = false;
	public static boolean ALL_SYSTEM_READY = false;
	public static boolean ALL_FINISHED = false;
	public static long total_cost_time = 0;
	
	// save 90 batch for wrong trace
	public static int BACKEND_CHECKSUM_BATCH_COUNT = 10;
	public static List<Map<String, Set<String>>> BACKEND_CHECKSUM_BATCH_TRACE_LIST = new ArrayList<>();
    
	// save chuckSum for the total wrong trace
    public static Map<String, String> TRACE_CHUCKSUM_MAP = new ConcurrentHashMap<>();
    public static BlockingQueue<Long> BACKEND_GEN_CHECKSUM_QUEUE = new LinkedBlockingQueue<Long>();
    
    // socket send queue
    public static BlockingQueue<String> SOCKET_SEND_QUEUE0 = new LinkedBlockingQueue<String>();
    public static BlockingQueue<String> SOCKET_SEND_QUEUE1 = new LinkedBlockingQueue<String>();
    public static BlockingQueue<String> SOCKET_SEND_QUEUE2 = new LinkedBlockingQueue<String>();
    // socket receive quque
    public static BlockingQueue<String> SOCKET_RESPONSE_QUEUE = new LinkedBlockingQueue<String>();
}
