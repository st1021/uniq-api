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
import vc.thinker.cabbage.se.CabinetStatusService;
import vc.thinker.cabbage.se.exception.CabinetStatusNotFindException;
import vc.thinker.cabbage.se.model.CabinetStatus;

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
	private CabinetStatusService cabinetStatusService;
	@Autowired
	private CabinetRemoteHandle cabinetRemoteHandle;
	
	@RequestMapping(value= "/borrow/{sysCode}/{slot}", method = RequestMethod.GET)
	@ApiOperation(value = "borrow", notes = "borrow")
	public SimpleResponse borrow(@PathVariable("sysCode")String sysCode, @PathVariable("slot") String slot) {
		SimpleResponse resp = new SimpleResponse();
		try {
			CabinetStatus cs = cabinetStatusService.findBySysCode(sysCode);
			if(null == cs) {
				resp.setErrorInfo("400", "sysCode not found.");
				return resp;
			}
			cabinetRemoteHandle.out(cs.getCabinetCode(), cs.getServiceCode(), slot);
		} catch (CabinetStatusNotFindException e) {
			resp.setErrorInfo("400", e.getMessage());
		}
		return resp;
	}
	
	@RequestMapping(value= "/sysOut/{sysCode}/{slot}", method = RequestMethod.GET)
	@ApiOperation(value = "sysOut", notes = "sysOut")
	public SimpleResponse sysOut(@PathVariable("sysCode")String sysCode, @PathVariable("slot") Integer slot) {
		SimpleResponse resp = new SimpleResponse();
		try {
			cabinetRemoteHandle.sysOut(sysCode, slot);
		} catch (CabinetStatusNotFindException e) {
			resp.setErrorInfo("400", e.getMessage());
		}
		return resp;
	}
	
	@RequestMapping(value= "/getServerIp/{sysCode}", method = RequestMethod.GET)
	@ApiOperation(value = "getServerIp", notes = "getServerIp")
	public SimpleResponse getServerIp(@PathVariable("sysCode")String sysCode) {
		SimpleResponse resp = new SimpleResponse();
		try {
			cabinetRemoteHandle.getServerIp(sysCode);
		} catch (CabinetStatusNotFindException e) {
			resp.setErrorInfo("400", e.getMessage());
		}
		return resp;
	}
	
	
	@RequestMapping(value= "/sync/{sysCode}", method = RequestMethod.GET)
	@ApiOperation(value = "sync", notes = "sync")
	public SimpleResponse sync(@PathVariable("sysCode")String sysCode) {
		SimpleResponse resp = new SimpleResponse();
		try {
			cabinetRemoteHandle.sync(sysCode);
		} catch (CabinetStatusNotFindException e) {
			resp.setErrorInfo("400", e.getMessage());
		}
		return resp;
	}
	
	@RequestMapping(value = "/setServerIp/{sysCode}/{ip}/{port}/{hearbet}", method = RequestMethod.GET)
	@ApiOperation(value = "setServerIp", notes = "setServerIp")
	public SimpleResponse setServerIp(@PathVariable("sysCode") String sysCode, @PathVariable("ip") String ip,
			@PathVariable("port") String port, @PathVariable("hearbet") Integer hearbet) {
		SimpleResponse resp = new SimpleResponse();
		try {
			cabinetRemoteHandle.synServer(sysCode, ip, port, hearbet);
		} catch (CabinetStatusNotFindException e) {
			resp.setErrorInfo("400", e.getMessage());
		}
		return resp;
	}
}
