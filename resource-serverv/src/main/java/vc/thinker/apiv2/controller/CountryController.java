package vc.thinker.apiv2.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.dozer.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.Authorization;
import vc.thinker.apiv2.ResourceServerMain;
import vc.thinker.apiv2.bo.APICountryBO;
import vc.thinker.apiv2.bo.APIlanguagePackageBO;
import vc.thinker.apiv2.http.response.ListResponse;
import vc.thinker.apiv2.http.response.SingleResponse;
import vc.thinker.apiv2.utils.MapperUtils;
import vc.thinker.apiv2.web.BaseController;
import vc.thinker.apiv2.web.Responses;
import vc.thinker.apiv2.web.UserAuthentication;
import vc.thinker.cabbage.sys.ResourceContants;
import vc.thinker.cabbage.sys.bo.CountryBO;
import vc.thinker.cabbage.sys.bo.LanguageBO;
import vc.thinker.cabbage.sys.exception.CountryNotExitException;
import vc.thinker.cabbage.sys.service.CountryService;
import vc.thinker.cabbage.sys.service.LanguageService;

@RestController
@RequestMapping(value = "/api/country/", produces = { APPLICATION_JSON_VALUE })
@Api(value = "国家操作相关",description="国家操作相关")
public class CountryController extends BaseController{

//	private static Logger logger = LoggerFactory.getLogger(CountryController.class);
	
	@Autowired
	private CountryService countryService;
	
	@Autowired
	private MessageSource messageSource;
	
	@Autowired
	private Mapper mapper;
	
	@Autowired
	private LanguageService languageService;
	
	@RequestMapping(value = "/query_country_list", method = { RequestMethod.GET })
	@ApiOperation(value = "获取国家列表",notes = "获取国家列表",
	authorizations=@Authorization(value = ResourceServerMain.securitySchemaOAuth2Cc))
	@ResponseBody
	public ResponseEntity<ListResponse<CountryBO>> queryCountryList(
			HttpServletRequest request){
		ListResponse<CountryBO> resp = new ListResponse<>(messageSource,request);
		resp.setSuccess(true);
		List<CountryBO> list = countryService.findAll();
		resp.setItems(list);
		return Responses.ok(resp);
	}
	
	
	@RequestMapping(value = "/query_language_list", method = { RequestMethod.GET })
	@ApiOperation(value = "获取语言列表",notes = "获取语言列表",
	authorizations=@Authorization(value = ResourceServerMain.securitySchemaOAuth2Cc))
	@ResponseBody
	public ResponseEntity<ListResponse<APICountryBO>> queryLanguageList(
			HttpServletRequest request){
		ListResponse<APICountryBO> resp = new ListResponse<>(messageSource,request);
		resp.setSuccess(true);
		List<LanguageBO> list = languageService.findAll();
//		List<CountryBO> list = countryService.groupByLanguage();
		resp.setItems(MapperUtils.map(mapper, list, APICountryBO.class));
		return Responses.ok(resp);
	}
	
	@RequestMapping(value = "/query_language_pack", method = { RequestMethod.GET })
	@ApiOperation(value = "获取语言包",notes = "获取语言包",
			authorizations=@Authorization(value = ResourceServerMain.securitySchemaOAuth2Cc))
	public ResponseEntity<SingleResponse<APIlanguagePackageBO>> queryLanguagePack(
			HttpServletRequest request,
			@UserAuthentication User user,
			@ApiParam(value = "默认语言（defaultLanguage）", required = true) @RequestParam("language") String language ){
		
//		logger.info("获取语言包 客户端上送的 locale:[{}]",request.getLocale().getLanguage());
		
		SingleResponse<APIlanguagePackageBO> resp = new SingleResponse<>(messageSource,request);
		APIlanguagePackageBO item = new APIlanguagePackageBO();
		try{
			String languagePackage = countryService.queryLanguagePack(language);
			item.setLanguagePackage(languagePackage);
			Date lastUpdateTime = countryService.queryUpdateTimeByLanguage(
					ResourceContants.RESOURCE_PLAY_TYEP_APP, language);
			item.setLastUpdateTime(lastUpdateTime);
			resp.setItem(item);
			return Responses.ok(resp); 
		}catch (CountryNotExitException e) {
			resp.setSuccess(false);
			resp.setErrorInfo("406", "language.notSupport","Does not support this language");
			return Responses.ok(resp);
		}
	}
	
}