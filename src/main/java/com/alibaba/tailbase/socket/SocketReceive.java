package com.alibaba.tailbase.socket;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.alibaba.tailbase.Global;
import com.alibaba.tailbase.backendprocess.BackendController;

@Service
public class SocketReceive {
	private static final Logger LOGGER = LoggerFactory.getLogger(SocketReceive.class.getName());
	
	@Async("asyncSocketReceiveExecutor")
	public void run(int port) {
		try {
			LOGGER.warn("--------------asyncSocketReceiveExecutor start. Listening: {}", port);
			ServerSocket server = new ServerSocket(port);
            Socket socket = server.accept();
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream());
            String receiveData = null;
            String replyData = null;
            while (!Global.ALL_FINISHED){
            	//Receive data from client
            	receiveData = in.readLine();
            	LOGGER.info("BackendSocketReceive received: " + receiveData);
            	String[] cols = receiveData.split("\\|");
            	replyData = "suc";
            	if (cols.length > 2 && cols[0].equals("setWrongTraceId")) {
            		replyData = BackendController.setWrongTraceId(cols[1], Integer.valueOf(cols[2]));
            	}
            	
            	//Send reply to client
                out.println(replyData);
                out.flush();
            }
            in.close();
            out.close();
            socket.close();
            server.close();
		} catch (Exception e) {
			LOGGER.error("Get socket data exception: " + e.getMessage());
		}
	}
	
}
