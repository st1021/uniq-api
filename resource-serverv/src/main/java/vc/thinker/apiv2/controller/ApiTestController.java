package vc.thinker.apiv2.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import vc.thinker.apiv2.http.response.SimpleResponse;
import vc.thinker.apiv2.web.BaseController;
import vc.thinker.cabbage.se.CabinetRemoteHandle;
import vc.thinker.cabbage.se.exception.CabinetStatusNotFindException;

/**
 * 
 * @description MyWebConfig.java
 *
 * @author ZhangGaoXiang
 * @date 2020年12月29日 上午11:23:48
 */
@RestController
@RequestMapping(value = "/api/test", produces = { APPLICATION_JSON_VALUE })
@Api(value = "个人用户操作类", description = "提供与个人用户相关的api")
public class ApiTestController extends BaseController{

	@Autowired
	private CabinetRemoteHandle cabinetRemoteHandle;
	
	@RequestMapping(value= "/out/{bosId}/{slot}", method = RequestMethod.GET)
	@ApiOperation(value = "out", notes = "out")
	public SimpleResponse out(@PathVariable("boxId")String boxId, @PathVariable("slot") Integer slot) {
		SimpleResponse resp = new SimpleResponse();
		try {
			cabinetRemoteHandle.out(boxId, "", String.valueOf(slot));
		} catch (CabinetStatusNotFindException e) {
			resp.setErrorInfo("400", e.getMessage());
		}
		
		return resp;
	}
	
	@RequestMapping(value= "/sysOut/{boxId}/{slot}", method = RequestMethod.GET)
	@ApiOperation(value = "sysOut", notes = "sysOut")
	public SimpleResponse sysOut(@PathVariable("boxId")String boxId, @PathVariable("slot") Integer slot) {
		SimpleResponse resp = new SimpleResponse();
		try {
			cabinetRemoteHandle.sysOut(boxId, slot);
		} catch (CabinetStatusNotFindException e) {
			resp.setErrorInfo("400", e.getMessage());
		}
		return resp;
	}
	
	@RequestMapping(value= "/getServerIp/{boxId}", method = RequestMethod.GET)
	@ApiOperation(value = "getServerIp", notes = "getServerIp")
	public SimpleResponse getServerIp(@PathVariable("boxId")String boxId) {
		SimpleResponse resp = new SimpleResponse();
		try {
			cabinetRemoteHandle.getServerIp(boxId);
		} catch (CabinetStatusNotFindException e) {
			resp.setErrorInfo("400", e.getMessage());
		}
		return resp;
	}
	
	
	@RequestMapping(value= "/sync/{boxId}", method = RequestMethod.GET)
	@ApiOperation(value = "sync", notes = "sync")
	public SimpleResponse sync(@PathVariable("boxId")String boxId) {
		SimpleResponse resp = new SimpleResponse();
		try {
			cabinetRemoteHandle.sync(boxId);
		} catch (CabinetStatusNotFindException e) {
			resp.setErrorInfo("400", e.getMessage());
		}
		return resp;
	}
}
