package com.alibaba.tailbase;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.tailbase.backendprocess.BackendGenCheckSum;

@Service
public class ControlService {
	@Autowired
	BackendGenCheckSum backendGenCheckSum;

	@Autowired
	AsyncConfig asyncConfig;

	public void startUp(){
	  	if (Utils.isBackendProcess()) {
	  		//Start get wrong trace thread
	  		for (int i=0; i<asyncConfig.getBackendGenCheckSumThreadCount(); i++) {
	  			backendGenCheckSum.run();
	  		}
	  	}
	}
}
