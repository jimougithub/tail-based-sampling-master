package com.alibaba.tailbase.backendprocess;

import static com.alibaba.tailbase.Constants.CLIENT_PROCESS_PORT1;
import static com.alibaba.tailbase.Constants.CLIENT_PROCESS_PORT2;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestParam;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.tailbase.CommonController;
import com.alibaba.tailbase.Constants;
import com.alibaba.tailbase.Global;
import com.alibaba.tailbase.Utils;
import com.alibaba.tailbase.clientprocess.ClientProcessData;

import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CheckSumService implements Runnable{
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientProcessData.class.getName());
    
    public static void init() {
        for (int i = 0; i < Global.BACKEND_CHECKSUM_BATCH_COUNT; i++) {
        	Global.BACKEND_CHECKSUM_BATCH_TRACE_LIST.add(new HashMap<>());
        }
    }

    public static void start() {
        Thread t = new Thread(new CheckSumService(), "CheckSumServiceThread");
        t.start();
    }

    @Override
    public void run() {
    	//Ensure all system ready together
		while (!Global.ALL_SYSTEM_READY) {
			try {
				if (Global.SYSTEM_READY) {
					boolean client1Ready = checkClientReady(Constants.CLIENT_PROCESS_PORT1);
					boolean client2Ready = checkClientReady(Constants.CLIENT_PROCESS_PORT2);
					if (client1Ready && client2Ready) {
						setClientReady(Constants.CLIENT_PROCESS_PORT1);
						setClientReady(Constants.CLIENT_PROCESS_PORT2);
						Global.ALL_SYSTEM_READY = true;
					}
				}
				Thread.sleep(200);
			} catch (Exception e) {
				LOGGER.error("checkClientReady error: " + e.getMessage());
			}
		}
    	
    	LOGGER.warn("--------------CheckSumService started. Batch size: {}--------------", Constants.BATCH_SIZE);
        TraceIdBatch traceIdBatch = null;
        String[] ports = new String[]{CLIENT_PROCESS_PORT1, CLIENT_PROCESS_PORT2};
        int pos = 0;
        long startTime;
        long costTime;
        //String response = "";
        Map<String, Set<String>> map = Global.BACKEND_CHECKSUM_BATCH_TRACE_LIST.get(pos);
        while (true) {
            try {
            	
                traceIdBatch = BackendController.getFinishedBatch();
                if (traceIdBatch == null) {
                    // send checksum when client process has all finished.
                    if (BackendController.isFinished()) {
                    	LOGGER.warn("--------------Finlished");
                        if (sendCheckSum()) {
                            break;
                        }
                    }
                    Thread.sleep(10);
                    continue;
                }
                
                // Getting wrong trace data from clients ===========================================================================
                int batchPos = traceIdBatch.getBatchPos();
                // to get all spans from remote
                startTime = System.currentTimeMillis();
                for (String port : ports) {
                    Map<String, List<String>> processMap = getWrongTrace(JSON.toJSONString(traceIdBatch.getTraceIdList()), port, batchPos);
                    if (processMap != null) {
                        for (Map.Entry<String, List<String>> entry : processMap.entrySet()) {
                            String traceId = entry.getKey();
                            Set<String> spanSet = map.get(traceId);
                            if (spanSet == null) {
                                spanSet = new HashSet<>();
                                map.put(traceId, spanSet);
                            }
                            spanSet.addAll(entry.getValue());
                        }
                    }
                }
                costTime = System.currentTimeMillis() - startTime;
                Global.total_cost_time = Global.total_cost_time + costTime;
                if (costTime>2) {
                	LOGGER.warn("getWrongTrace batchPos: {} consume time: {}", batchPos, costTime);
                }
                //LOGGER.info("getWrong:" + batchPos + ", traceIdsize:" + traceIdBatch.getTraceIdList().size() + ", result:" + map.size());
                
                // trigger generate checksum
                Global.BACKEND_GEN_CHECKSUM_QUEUE.put((long) pos);
                
                // loop cycle
                pos++;
                if (pos >= Global.BACKEND_CHECKSUM_BATCH_COUNT) {
                	pos = 0;
                	//System.gc();
                }
                map = Global.BACKEND_CHECKSUM_BATCH_TRACE_LIST.get(pos);
                while (!map.isEmpty()) {
                	LOGGER.warn("-------------------- Waiting for backend pos release: "+ pos);
                    Thread.sleep(10);
                }
                
            } catch (Exception e) {
                // record batchPos when an exception  occurs.
                int batchPos = 0;
                if (traceIdBatch != null) {
                    batchPos = traceIdBatch.getBatchPos();
                }
                LOGGER.warn(String.format("fail to getWrongTrace, batchPos:%d", batchPos), e);
            } finally {
                if (traceIdBatch == null) {
                    try {
                        Thread.sleep(100);
                    } catch (Throwable e) {
                        // quiet
                    }
                }
            }
        }
    }

    //call client process, to get all spans of wrong traces.
    private Map<String,List<String>> getWrongTrace(@RequestParam String traceIdList, String port, int batchPos) {
        try {
            RequestBody body = new FormBody.Builder().add("traceIdList", traceIdList).add("batchPos", batchPos + "").build();
            String url = String.format("http://localhost:%s/getWrongTrace", port);
            Request request = new Request.Builder().url(url).post(body).build();
            Response response = Utils.callHttp(request);
            Map<String,List<String>> resultMap = JSON.parseObject(response.body().string(), new TypeReference<Map<String, List<String>>>() {});
            response.close();
            return resultMap;
        } catch (Exception e) {
            LOGGER.warn("fail to getWrongTrace, json:" + traceIdList + ",batchPos:" + batchPos, e);
        }
        return null;
    }


    private boolean sendCheckSum() {
        try {
            String result = JSON.toJSONString(Global.TRACE_CHUCKSUM_MAP);
            RequestBody body = new FormBody.Builder().add("result", result).build();
            String url = String.format("http://localhost:%s/api/finished", CommonController.getDataSourcePort());
            Request request = new Request.Builder().url(url).post(body).build();
            Response response = Utils.callHttp(request);
            if (response.isSuccessful()) {
                response.close();
                LOGGER.warn("suc to sendCheckSum, result:" + result);
                LOGGER.warn("total get trace time: " + Global.total_cost_time);
                return true;
            }
            LOGGER.warn("fail to sendCheckSum:" + response.message());
            response.close();
            return false;
        } catch (Exception e) {
            LOGGER.warn("fail to call finish", e);
        }
        return false;
    }

    public static long getStartTime(String span) {
        if (span != null) {
            String[] cols = span.split("\\|");
            if (cols.length > 8) {
                return Utils.toLong(cols[1], -1);
            }
        }
        return -1;
    }
    
    private boolean checkClientReady(String port) {
        try {
        	boolean systemReady = false;
            String url = String.format("http://localhost:%s/getready", port);
            Request request = new Request.Builder().url(url).get().build();
            Response response = Utils.callHttp(request);
            if (response.body().string().equals("yes")) {
            	systemReady = true;
            }
            response.close();
            return systemReady;
        } catch (Exception e) {
            LOGGER.error("checkClientReady port: {} error: {}", port, e.getMessage());
        }
        return false;
    }
    
    private void setClientReady(String port) {
        try {
            String url = String.format("http://localhost:%s/setready", port);
            Request request = new Request.Builder().url(url).get().build();
            Response response = Utils.callHttp(request);
            response.close();
        } catch (Exception e) {
            LOGGER.error("setClientReady port: {} error: {}", port, e.getMessage());
        }
    }

}
