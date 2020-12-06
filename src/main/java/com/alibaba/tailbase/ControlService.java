package com.alibaba.tailbase;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.tailbase.backendprocess.BackendGenCheckSum;
import com.alibaba.tailbase.socket.SocketReceive;
import com.alibaba.tailbase.socket.SocketSend;

@Service
public class ControlService {
	@Autowired
	BackendGenCheckSum backendGenCheckSum;
	
	@Autowired
	SocketReceive socketReceive;
	
	@Autowired
	SocketSend socketSend;

	@Autowired
	AsyncConfig asyncConfig;

	public void startUp(){
	  	if (Utils.isBackendProcess()) {
	  		//Start get wrong trace thread
	  		for (int i=0; i<asyncConfig.getBackendGenCheckSumThreadCount(); i++) {
	  			backendGenCheckSum.run();
	  		}
	  		//Start socket receive
	  		socketReceive.run(Constants.BACKEND_SOCKET_PORT1);
	  		socketReceive.run(Constants.BACKEND_SOCKET_PORT2);
	  	}
	  	
	  	if (Utils.getPort().equals(Constants.CLIENT_PROCESS_PORT1)) {
	  		//Start socket send
	  		socketSend.run(Constants.BACKEND_SOCKET_PORT1);
	  	}
	  	
	  	if (Utils.getPort().equals(Constants.CLIENT_PROCESS_PORT2)) {
	  		//Start socket send
	  		socketSend.run(Constants.BACKEND_SOCKET_PORT2);
	  	}
	  	
	  	//Sleep 10 seconds before mark system ready
	  	try {
			Thread.sleep(10000);
			Global.SYSTEM_READY = true;
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
