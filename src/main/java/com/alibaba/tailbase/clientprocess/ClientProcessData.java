package com.alibaba.tailbase.clientprocess;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.tailbase.CommonController;
import com.alibaba.tailbase.Constants;
import com.alibaba.tailbase.Global;
import com.alibaba.tailbase.Utils;

import okhttp3.Request;
import okhttp3.Response;


public class ClientProcessData implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientProcessData.class.getName());

    // an list of trace map,like ring buffer. key is traceId, value is spans
    private static List<Map<String,List<String>>> BATCH_TRACE_LIST = new ArrayList<>();
    
    public static  void init() {
        for (int i = 0; i < Constants.BATCH_COUNT; i++) {
            BATCH_TRACE_LIST.add(new ConcurrentHashMap<>(Constants.BATCH_SIZE));
        }
    }

    public static void start() {
        Thread t = new Thread(new ClientProcessData(), "ProcessDataThread");
        t.start();
    }

    @Override
    public void run() {
        try {
            String path = getPath();
            // process data on client, not server
            if (StringUtils.isEmpty(path)) {
                LOGGER.warn("path is empty");
                return;
            }
            URL url = new URL(path);
            LOGGER.info("data path:" + path);
            HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection(Proxy.NO_PROXY);
            InputStream input = httpConnection.getInputStream();
            BufferedReader bf = new BufferedReader(new InputStreamReader(input));
            String line;
            long count = 0;
            int pos = 0;
            int batchPos = 0;
            Set<String> badTraceIdList = new HashSet<>(1000);
            Map<String, List<String>> traceMap = BATCH_TRACE_LIST.get(pos);
            while ((line = bf.readLine()) != null) {
                count++;
                String[] cols = line.split("\\|");
                if (cols != null && cols.length > 1 ) {
                    String traceId = cols[0];
                    List<String> spanList = traceMap.get(traceId);
                    if (spanList == null) {
                        spanList = new ArrayList<>();
                        traceMap.put(traceId, spanList);
                    }
                    spanList.add(line);
                    if (cols.length > 8) {
                        String tags = cols[8];
                        if (tags != null) {
                            if (tags.contains("error=1")) {
                                badTraceIdList.add(traceId);
                            } else if (tags.contains("http.status_code=") && tags.indexOf("http.status_code=200") < 0) {
                                badTraceIdList.add(traceId);
                            }
                        }
                    }
                }
                if (count % Constants.BATCH_SIZE == 0) {
                	batchPos = pos;
                    pos++;
                    LOGGER.info("******************************************************** pos changed: " + pos);
                    // loop cycle
                    if (pos >= Constants.BATCH_COUNT) {
                        pos = 0;
                    }
                    traceMap = BATCH_TRACE_LIST.get(pos);
                    // donot produce data, wait backend to consume data
                    // TODO to use lock/notify
                    if (traceMap.size() > 0) {
                        while (true) {
                        	LOGGER.warn("------------------- Completed count: {}. Waiting for pos release: {}", count, pos);
                            Thread.sleep(10);
                            if (traceMap.size() == 0) {
                                break;
                            }
                        }
                    }
                    // batchPos begin from 0, so need to minus 1
                    updateWrongTraceId(badTraceIdList, batchPos);
                    badTraceIdList.clear();
                    LOGGER.info("suc to updateBadTraceId, batchPos:" + batchPos);
                }
            }
            if (badTraceIdList.size() > 0) {
            	updateWrongTraceId(badTraceIdList, pos);
            }
            bf.close();
            input.close();
            callFinish();
        } catch (Exception e) {
            LOGGER.warn("fail to process data", e);
        }
    }

    //call backend controller to update wrong tradeId list.
    private void updateWrongTraceId(Set<String> badTraceIdList, int batchPos) {
        String json = JSON.toJSONString(badTraceIdList);
        if (badTraceIdList.size() == 0) {
        	badTraceIdList.add("NULL");
        }
        try {
            LOGGER.info("updateBadTraceId, json:" + json + ", batch:" + batchPos);
            /*RequestBody body = new FormBody.Builder()
                    .add("traceIdListJson", json).add("batchPos", batchPos + "").build();
            Request request = new Request.Builder().url("http://localhost:8002/setWrongTraceId").post(body).build();
            Response response = Utils.callHttp(request);
            response.close();*/
            //Use socket to send data
            Global.SOCKET_SEND_QUEUE0.put("setWrongTraceId|" + json + "|" + batchPos);
        } catch (Exception e) {
            LOGGER.warn("fail to updateBadTraceId, json:" + json + ", batch:" + batchPos);
        }
    }

    // notify backend process when client process has finished.
    private void callFinish() {
        try {
            Request request = new Request.Builder().url("http://localhost:8002/finish").build();
            Response response = Utils.callHttp(request);
            response.close();
            LOGGER.warn("total get wrong data cost time: " + Global.total_cost_time);
        } catch (Exception e) {
            LOGGER.warn("fail to callFinish");
        }
    }


    public static String getWrongTracing(String wrongTraceIdList, int batchPos) {
        LOGGER.info(String.format("getWrongTracing, batchPos:%d, wrongTraceIdList:\n %s" ,
                batchPos, wrongTraceIdList));
        List<String> traceIdList = JSON.parseObject(wrongTraceIdList, new TypeReference<List<String>>(){});
        Map<String,List<String>> wrongTraceMap = new HashMap<>();
        int pos = batchPos % Constants.BATCH_COUNT;
        int previous = pos - 1;
        if (previous == -1) {
            previous = Constants.BATCH_COUNT -1;
        }
        int next = pos + 1;
        if (next == Constants.BATCH_COUNT) {
            next = 0;
        }
        
        // EMGJH Start
        // getWrongTraceWithBatch(previous, pos, traceIdList, wrongTraceMap);
        // getWrongTraceWithBatch(pos, pos, traceIdList,  wrongTraceMap);
        // getWrongTraceWithBatch(next, pos, traceIdList, wrongTraceMap);
        getWrongTraceWithThreeBatch(previous, pos, next, traceIdList, wrongTraceMap);
        // EMGJH End
        
        // to clear spans, don't block client process thread. TODO to use lock/notify
        BATCH_TRACE_LIST.get(previous).clear();
        return JSON.toJSONString(wrongTraceMap);
    }
    
    
    // EMGJH Start
    private static void getWrongTraceWithThreeBatch(int previous, int currentPos, int next, List<String> traceIdList, Map<String,List<String>> wrongTraceMap) {
        Map<String, List<String>> currentTraceMap = BATCH_TRACE_LIST.get(currentPos);

        Map<String, List<String>> previousTraceMap = BATCH_TRACE_LIST.get(previous);
        Map<String, List<String>> nextTraceMap = BATCH_TRACE_LIST.get(next);

        for (String traceId : traceIdList) {
            List<String> spanListCurrentTraceMap = currentTraceMap.get(traceId);
            List<String> spanListPreviousTraceMap = previousTraceMap.get(traceId);

            // Firstly, try to locate the traceId in spanListCurrentTraceMap
            if (spanListCurrentTraceMap != null) {
                // one trace may cross to batch (e.g batch size 20000, span1 in line 19999, span2 in line 20001)
                List<String> existSpanList = wrongTraceMap.get(traceId);
                if (existSpanList != null) {
                    existSpanList.addAll(spanListCurrentTraceMap);
                } else {
                    wrongTraceMap.put(traceId, spanListCurrentTraceMap);
                }
            }

            // If the traceId is located in spanListPreviousTraceMap, then it should Not be located spanListNextTraceMap
            if (spanListPreviousTraceMap != null) {
                // one trace may cross to batch (e.g batch size 20000, span1 in line 19999, span2 in line 20001)
                List<String> existSpanList = wrongTraceMap.get(traceId);
                if (existSpanList != null) {
                    existSpanList.addAll(spanListPreviousTraceMap);
                } else {
                    wrongTraceMap.put(traceId, spanListPreviousTraceMap);
                }
            } else {
                List<String> spanListNextTraceMap = nextTraceMap.get(traceId);
                if (spanListNextTraceMap != null) {
                    // one trace may cross to batch (e.g batch size 20000, span1 in line 19999, span2 in line 20001)
                    List<String> existSpanList = wrongTraceMap.get(traceId);
                    if (existSpanList != null) {
                        existSpanList.addAll(spanListNextTraceMap);
                    } else {
                        wrongTraceMap.put(traceId, spanListNextTraceMap);
                    }
                }
            }
        }
    }
    // EMGJH End

	/*
	 * private static void getWrongTraceWithBatch(int batchPos, int pos,
	 * List<String> traceIdList, Map<String,List<String>> wrongTraceMap) { // donot
	 * lock traceMap, traceMap may be clear anytime. Map<String, List<String>>
	 * traceMap = BATCH_TRACE_LIST.get(batchPos); for (String traceId : traceIdList)
	 * { if (traceId.equals("NULL")) { continue; } List<String> spanList =
	 * traceMap.get(traceId); if (spanList != null) { // one trace may cross to
	 * batch (e.g batch size 20000, span1 in line 19999, span2 in line 20001)
	 * List<String> existSpanList = wrongTraceMap.get(traceId); if (existSpanList !=
	 * null) { existSpanList.addAll(spanList); } else { wrongTraceMap.put(traceId,
	 * spanList); } // output spanlist to check String spanListString =
	 * spanList.stream().collect(Collectors.joining("\n")); LOGGER.info(String.
	 * format("getWrongTracing, batchPos:%d, pos:%d, traceId:%s, spanList:\n %s",
	 * batchPos, pos, traceId, spanListString)); } } }
	 */

    private String getPath(){
        String port = System.getProperty("server.port", "8080");
        Integer targetPort = CommonController.getDataSourcePort();
        targetPort = 8080;															//remove before promote to production
        if ("8000".equals(port)) {
            return "http://localhost:" + targetPort + "/trace1.data";
        } else if ("8001".equals(port)){
            return "http://localhost:" + targetPort + "/trace2.data";
        } else {
            return null;
        }
    }

}
