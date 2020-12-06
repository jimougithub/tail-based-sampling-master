package com.alibaba.tailbase.socket;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.alibaba.tailbase.Global;

@Service
public class SocketSend {
	private static final Logger LOGGER = LoggerFactory.getLogger(SocketSend.class.getName());
	
	@Async("asyncSocketSendExecutor")
	public void run(int port) {
		try {
			while (!Global.ALL_SYSTEM_READY) {
				Thread.sleep(100);
			}
			LOGGER.warn("--------------asyncSocketSendExecutor start. target port: {}", port);
			Socket socket = new Socket("localhost", port);
        	BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        	PrintWriter out = new PrintWriter(socket.getOutputStream());
        	String sendData = "";
        	String receiveData = "";
        	while (!Global.ALL_FINISHED){
        		sendData = Global.SOCKET_SEND_QUEUE.poll(5, TimeUnit.SECONDS);
        		if (sendData != null && sendData.trim().length()>0) {
        			//Send data to server
                    out.println(sendData);
                    out.flush();
                    //Receive data from server
                    receiveData = in.readLine();
                    LOGGER.info("Received: " + receiveData);
        		}
        	}
            out.close();
            in.close();
            out.close();
            socket.close();
		} catch (Exception e) {
			LOGGER.error("Get socket data exception: " + e.getMessage());
		}
	}
	
}
