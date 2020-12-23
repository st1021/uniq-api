//package vc.thinker.apiv2.controller;
//
//import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
//
//import java.util.Date;
//import java.util.List;
//
//import javax.servlet.http.HttpServletRequest;
//
//import org.dozer.Mapper;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.context.MessageSource;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.core.userdetails.User;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestMethod;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.ResponseBody;
//import org.springframework.web.bind.annotation.RestController;
//
//import io.swagger.annotations.Api;
//import io.swagger.annotations.ApiOperation;
//import io.swagger.annotations.ApiParam;
//import io.swagger.annotations.Authorization;
//import vc.thinker.apiv2.ResourceServerMain;
//import vc.thinker.apiv2.bo.APIlanguagePackageBO;
//import vc.thinker.apiv2.bo.ApiLanguageBO;
//import vc.thinker.apiv2.http.response.ListResponse;
//import vc.thinker.apiv2.http.response.SingleResponse;
//import vc.thinker.apiv2.utils.MapperUtils;
//import vc.thinker.apiv2.web.BaseController;
//import vc.thinker.apiv2.web.Responses;
//import vc.thinker.apiv2.web.UserAuthentication;
//import vc.thinker.cabbage.sys.ResourceContants;
//import vc.thinker.cabbage.sys.bo.LanguageBO;
//import vc.thinker.cabbage.sys.exception.CountryNotExitException;
//import vc.thinker.cabbage.sys.service.CountryService;
//import vc.thinker.cabbage.sys.service.LanguageService;
//
///**
// *
// * @author ZhangGaoXiang
// * @time Jan 7, 20205:41:45 PM
// */
//@RestController
//@RequestMapping(value = "/api/country/", produces = { APPLICATION_JSON_VALUE })
//@Api(value = "语言包 - API",description="语言包 - API")
//public class LanguageController extends BaseController{
//
//	@Autowired
//	private Mapper mapper;
//	
//	@Autowired
//	private MessageSource messageSource;
//	
//	@Autowired
//	private LanguageService languageService;
//	
//	@RequestMapping(value = "/languages", method = { RequestMethod.GET })
//	@ApiOperation(value = "获取支持的语言列表",notes = "获取支持的语言列表",
//	authorizations=@Authorization(value = ResourceServerMain.securitySchemaOAuth2Cc))
//	@ResponseBody
//	public ResponseEntity<ListResponse<ApiLanguageBO>> languages(
//			HttpServletRequest request){
//		ListResponse<ApiLanguageBO> resp = new ListResponse<>(messageSource,request);
//		resp.setSuccess(true);
//		List<LanguageBO> languages = languageService.findAll();
//		resp.setItems(MapperUtils.map(mapper, languages, ApiLanguageBO.class));
//		return Responses.ok(resp);
//	}
//	

//}
