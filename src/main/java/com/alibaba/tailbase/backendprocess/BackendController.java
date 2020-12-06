package com.alibaba.tailbase.backendprocess;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.tailbase.Constants;
import com.alibaba.tailbase.Global;

@RestController
public class BackendController {

    private static final Logger LOGGER = LoggerFactory.getLogger(BackendController.class.getName());

    // FINISH_PROCESS_COUNT will add one, when process call finish();
    private static volatile Integer FINISH_PROCESS_COUNT = 0;

    // single thread to run, do not use lock
    private static volatile Integer CURRENT_BATCH = 0;
    
    private static Lock lock = new ReentrantLock();

    // save number of batch for wrong trace
    private static List<TraceIdBatch> TRACEID_BATCH_LIST= new ArrayList<>();
    public static  void init() {
        for (int i = 0; i < Constants.BATCH_COUNT; i++) {
            TRACEID_BATCH_LIST.add(new TraceIdBatch());
        }
    }

    @RequestMapping("/setWrongTraceId")
    public String setWrongTraceIdRequest(@RequestParam String traceIdListJson, @RequestParam int batchPos) {
        return setWrongTraceId(traceIdListJson, batchPos);
    }
    
    public static String setWrongTraceId(@RequestParam String traceIdListJson, @RequestParam int batchPos) {
        lock.lock();
        List<String> traceIdList = JSON.parseObject(traceIdListJson, new TypeReference<List<String>>() {});
        LOGGER.info(String.format("setWrongTraceId had called, batchPos:%d, traceIdList:%s", batchPos, traceIdListJson));
        TraceIdBatch traceIdBatch = TRACEID_BATCH_LIST.get(batchPos);
        if (traceIdBatch.getBatchPos() != 0 && traceIdBatch.getBatchPos() != batchPos) {
            LOGGER.warn("overwrite traceId batch when call setWrongTraceId");
        }
        if (traceIdList != null && traceIdList.size() > 0) {
            traceIdBatch.setBatchPos(batchPos);
            traceIdBatch.addProcessCount();
            traceIdBatch.getTraceIdList().addAll(traceIdList);
        }
        lock.unlock();
        return "suc";
    }

    @RequestMapping("/finish")
    public String finish() {
        FINISH_PROCESS_COUNT++;
        LOGGER.warn("receive call 'finish', count:" + FINISH_PROCESS_COUNT);
        return "suc";
    }

    /**
     * trace batch will be finished, when client process has finished.(FINISH_PROCESS_COUNT == PROCESS_COUNT)
     * @return
     */
   public static boolean isFinished() {
       for (int i = 0; i < Constants.BATCH_COUNT; i++) {
           TraceIdBatch currentBatch = TRACEID_BATCH_LIST.get(i);
           if (currentBatch.getBatchPos() != 0) {
               return false;
           }
       }
       for (int i = 0; i < Global.BACKEND_CHECKSUM_BATCH_COUNT; i++) {
           if (!Global.BACKEND_CHECKSUM_BATCH_TRACE_LIST.get(i).isEmpty()) {
               return false;
           }
       }
       if (FINISH_PROCESS_COUNT < Constants.PROCESS_COUNT) {
           return false;
       }
       Global.ALL_FINISHED = true;
       return true;
   }

    /**
     * get finished bath when current and next batch has all finished
     * @return
     */
    public static TraceIdBatch getFinishedBatch() {
        int next = CURRENT_BATCH + 1;
        if (next >= Constants.BATCH_COUNT) {
            next = 0;
        }
        TraceIdBatch nextBatch = TRACEID_BATCH_LIST.get(next);
        TraceIdBatch currentBatch = TRACEID_BATCH_LIST.get(CURRENT_BATCH);
        // when client process is finished, or then next trace batch is finished. to get checksum for wrong traces.
        if ((FINISH_PROCESS_COUNT >= Constants.PROCESS_COUNT && currentBatch.getProcessCount() > 0) ||
                (nextBatch.getProcessCount() >= Constants.PROCESS_COUNT && currentBatch.getProcessCount() >= Constants.PROCESS_COUNT)) {
            // reset
            TraceIdBatch newTraceIdBatch = new TraceIdBatch();
            TRACEID_BATCH_LIST.set(CURRENT_BATCH, newTraceIdBatch);
            CURRENT_BATCH = next;
            return currentBatch;
        }

        return null;
    }

}
