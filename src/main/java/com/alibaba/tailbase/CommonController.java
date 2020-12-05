package com.alibaba.tailbase;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.tailbase.clientprocess.ClientProcessData;


@RestController
public class CommonController {

  private static Integer DATA_SOURCE_PORT = 0;

  public static Integer getDataSourcePort() {
    return DATA_SOURCE_PORT;
  }

  @GetMapping(value = "/ready")
  public ResponseEntity<String> ready() {
    if (Global.ALL_SYSTEM_READY) {
    	return ResponseEntity
    			.status(HttpStatus.OK)
    			.body("suc");
    } else {
    	return ResponseEntity
    			.status(HttpStatus.SERVICE_UNAVAILABLE)
    			.body("Not ready");
    }
  }
  
  @RequestMapping("/getready")
  public String getReady() {
	  if (Global.SYSTEM_READY) {
		  return "yes";
	  } else {
		  return "no";
	  }
  }
  
  @RequestMapping("/setready")
  public String setReady() {
	  Global.ALL_SYSTEM_READY = true;
	  return "suc";
  }

  @RequestMapping("/setParameter")
  public String setParamter(@RequestParam Integer port) {
    DATA_SOURCE_PORT = port;
    if (Utils.isClientProcess()) {
      ClientProcessData.start();
    }
    return "suc";
  }

  @RequestMapping("/start")
  public String start() {
    return "suc";
  }



}
