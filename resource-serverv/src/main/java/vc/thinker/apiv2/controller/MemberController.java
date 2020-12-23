package vc.thinker.apiv2.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.apache.commons.lang3.StringUtils;
import org.dozer.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.RequestContextUtils;

import com.alibaba.fastjson.JSON;
import com.github.kevinsawicki.http.HttpRequest;
import com.sinco.common.utils.IPUtil;
import com.sinco.common.word.SensitivewordFilter;
import com.sinco.data.core.Page;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;
import net.weedfs.client.RequestResult;
import net.weedfs.client.WeedFSClient;
import redis.clients.jedis.JedisPool;
import vc.thinker.apiv2.ResourceServerMain;
import vc.thinker.apiv2.bo.APIBasicCostBO;
import vc.thinker.apiv2.bo.APIDepositBO;
import vc.thinker.apiv2.bo.APIIntegralLogBO;
import vc.thinker.apiv2.bo.APIMembershipCardBO;
import vc.thinker.apiv2.bo.APIUserCouponBO;
import vc.thinker.apiv2.bo.APIUserDepositPayLogBo;
import vc.thinker.apiv2.bo.APIUserMoneyBO;
import vc.thinker.apiv2.bo.APIVipPayLogBO;
import vc.thinker.apiv2.bo.ApplePaymentBO;
import vc.thinker.apiv2.bo.CashfreePaymentBO;
import vc.thinker.apiv2.bo.FondyPaymetBO;
import vc.thinker.apiv2.bo.MemberBO;
import vc.thinker.apiv2.bo.PayDetailsBO;
import vc.thinker.apiv2.bo.ThirdPartyBoundModileBO;
import vc.thinker.apiv2.http.response.ListResponse;
import vc.thinker.apiv2.http.response.PageResponse;
import vc.thinker.apiv2.http.response.SimpleResponse;
import vc.thinker.apiv2.http.response.SingleResponse;
import vc.thinker.apiv2.utils.MapperUtils;
import vc.thinker.apiv2.utils.ValidateUtil;
import vc.thinker.apiv2.utils.WeixinPayUtil;
import vc.thinker.apiv2.vo.FeedbackVO;
import vc.thinker.apiv2.vo.PayNotifyVO;
import vc.thinker.apiv2.vo.RealnameVO;
import vc.thinker.apiv2.web.BaseController;
import vc.thinker.apiv2.web.Responses;
import vc.thinker.apiv2.web.UserAuthentication;
import vc.thinker.biz.exception.ServiceException;
import vc.thinker.cabbage.common.PaymentConstants;
import vc.thinker.cabbage.money.service.MembershipCardService;
import vc.thinker.cabbage.money.service.MoneyService;
import vc.thinker.cabbage.se.FeedbackService;
import vc.thinker.cabbage.se.bo.FeedbackTypeBO;
import vc.thinker.cabbage.se.exception.CabinetNotFindException;
import vc.thinker.cabbage.se.exception.CountryInfoHasExistException;
import vc.thinker.cabbage.se.exception.CountryNotFindException;
import vc.thinker.cabbage.se.exception.UnfinishedOrdersExceptiion;
import vc.thinker.cabbage.se.model.Feedback;
import vc.thinker.cabbage.sys.PromotionConstants;
import vc.thinker.cabbage.sys.dao.CouponDao;
import vc.thinker.cabbage.sys.dao.SysSettingDao;
import vc.thinker.cabbage.sys.model.SysSetting;
import vc.thinker.cabbage.sys.service.PromotionService;
import vc.thinker.cabbage.user.CommonConstants;
import vc.thinker.cabbage.user.IdCardUtil;
import vc.thinker.cabbage.user.MemberUtil;
import vc.thinker.cabbage.user.UserSource;
import vc.thinker.cabbage.user.bo.IntegralLogBO;
import vc.thinker.cabbage.user.bo.MembershipCardBO;
import vc.thinker.cabbage.user.bo.VipPayLogBO;
import vc.thinker.cabbage.user.exception.AccountExistedException;
import vc.thinker.cabbage.user.exception.AccountNotFindException;
import vc.thinker.cabbage.user.exception.BasicCostNotPayException;
import vc.thinker.cabbage.user.exception.DepositException;
import vc.thinker.cabbage.user.exception.DepositNotPayException;
import vc.thinker.cabbage.user.exception.InvalidEmailException;
import vc.thinker.cabbage.user.exception.InviteCodeNotFindException;
import vc.thinker.cabbage.user.exception.InviteCodeUsedException;
import vc.thinker.cabbage.user.exception.MemberExistedException;
import vc.thinker.cabbage.user.exception.MemberNotBindMobileException;
import vc.thinker.cabbage.user.exception.NoOngoingOrderException;
import vc.thinker.cabbage.user.exception.PasswordErrorException;
import vc.thinker.cabbage.user.exception.PlatNotConfigDepositException;
import vc.thinker.cabbage.user.exception.UserCurrencyNotPerfectException;
import vc.thinker.cabbage.user.exception.UserHasBindedCabinetException;
import vc.thinker.cabbage.user.model.UserMoney;
import vc.thinker.cabbage.user.service.IntegralLogService;
import vc.thinker.cabbage.user.service.MemberService;
import vc.thinker.cabbage.user.service.MemberService.MyDirectPayResponse;
import vc.thinker.cabbage.user.service.UniqueRadomCodeService;
import vc.thinker.cabbage.user.service.UserCouponService;
import vc.thinker.cabbage.user.service.UserDepositLogService;
import vc.thinker.cabbage.user.vo.IntegralLogVO;
import vc.thinker.cabbage.user.vo.UserDepositLogVO;
import vc.thinker.cabbage.user.vo.VipPayLogVO;
import vc.thinker.oauth.AuthContants;
import vc.thinker.oauth.LoginTokenUtil;
import vc.thinker.oauth.VerifyCodeUtil;
import vc.thinker.pay.PayHandlerFactory;
import vc.thinker.pay.cashfree.CashfreeConfig;
import vc.thinker.pay.response.DirectPayResponse;
import vc.thinker.pay.weixin.WeixinConfig;
import vc.thinker.sys.bo.MobileBO;
import vc.thinker.sys.model.UserAccount;
import vc.thinker.sys.service.UserAccountService;
import vc.thinker.sys.utils.MsgTools;

@RestController
@RequestMapping(value = "/api/member", produces = { APPLICATION_JSON_VALUE })
@Api(value = "个人用户操作类", description = "提供与个人用户相关的api")
public class MemberController extends BaseController {
	protected Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private MemberService memberService;

	@Autowired
	protected UserAccountService userAccountService;

	@Autowired
	private SysSettingDao sysSettingDao;

	@Autowired
	private JedisPool jedisPool;

	@Autowired
	private LoginTokenUtil loginTokenUtil;

	@Autowired
	private UserDepositLogService userDepositLogService;

	@Autowired
	private WeedFSClient imageFsClient;

	@Autowired
	private Mapper mapper;

	@Autowired
	protected TokenStore tokenStore;

	@Autowired
	private UniqueRadomCodeService uniqueRadomCodeService;

	@Autowired
	private MessageSource messageSource;

	@Autowired
	private MsgTools msgTools;

	@Autowired
	private PromotionService promotionService;

	@Autowired
	private UserCouponService userCouponService;

	@Autowired
	private MembershipCardService membershipCardService;

	@Autowired
	private MoneyService moneyService;


	@Value("${pay.callback}")
	private String payCallback;

	@Value("${invite.code}")
	private String invite_code;

	@Autowired
	private CouponDao couponDao;

	@Autowired
	private SensitivewordFilter sensitivewordFilter;

	@Autowired
	private VerifyCodeUtil verifyCodeUtil;

	@Autowired
	private IntegralLogService integralLogService;

	@Autowired
	private FeedbackService feedbackService;

	@Autowired
	@Lazy(true)
	private PayHandlerFactory payHandlerFactory;

	@Value("${pay.callback}")
	private String server_url;
	
	@Value("${sms.handler.type}")
	private String smsType;
	
	@Value("${sms.handler.signature}")
	private String smsSignature;

	@PreAuthorize("hasRole('" + AuthContants.ROLE_END_USER + "')")
	@RequestMapping(value = "/unbundled_third_party", method = { RequestMethod.POST })
	@ApiOperation(value = "第三方账户解绑", notes = "第三方账户解绑")
	@ResponseBody
	public SimpleResponse unbundledThirdParty(HttpServletRequest request, @UserAuthentication User user,
			@RequestHeader(name = "api-agent", required = false) String apiAgent,
			@ApiParam(value = "第三方类型，weixin,qq,facebook", required = true) @RequestParam("third_party_type") String thirdPartyType) {
		SimpleResponse resp = new SimpleResponse();
		Long uid = Long.valueOf(user.getUsername());

		UserSource userSource = null;
		switch (thirdPartyType) {
		case "weixin":
			userSource = UserSource.WEIXIN_OPEN;
			break;
		case "qq":
			userSource = UserSource.QQ;
			break;
		case "facebook":
			userSource = UserSource.FACEBOOK;
			break;
		default:
			resp.setErrorInfo("400", "thirdPartyType is error");
			return resp;
		}
		try {
			memberService.unbundledThirdParty(uid, userSource);
		} catch (AccountNotFindException e) {
			logger.info("用户没有绑定[{}]账户", userSource.name());
		}
		return resp;
	}

	@RequestMapping(value = "/registered", method = { RequestMethod.POST })
	@ResponseBody
	@ApiOperation(value = "注册", notes = "注册", authorizations = @Authorization(value = ResourceServerMain.securitySchemaOAuth2Cc))
	public SimpleResponse registered(HttpServletRequest request,
			@RequestHeader(name = "api-agent", required = false) String apiAgent,
			@ApiParam(value = "国家编码", required = true) @RequestParam("countryCode") String countryCode,
			@ApiParam(value = "手机号码", required = true) @RequestParam("mobile") String mobile,
			@ApiParam(value = "验证码", required = false) @RequestParam("verifycode") String verifycode,
			@ApiParam(value = "密码", required = true) @RequestParam("password") String password,
			@ApiParam(value = "充电柜的别名") @RequestParam(value = "cabinetAlias", required = false) String cabinetAlias) {
		SimpleResponse resp = new SimpleResponse(messageSource, request);

		if (!ValidateUtil.validPwd(password)) {
			resp.setErrorInfo("400", "password.format.error", "密码格式不正确");
			return resp;
		}

		String code = verifyCodeUtil.getVerifyCode(jedisPool, countryCode+mobile);
		
		logger.info("手机号:[{}],verifycode:[{}], code:[{}] ",countryCode+mobile, verifycode ,code);
		
		if(StringUtils.isBlank(code)) {
			resp.setErrorInfo("406", "verifycode.expired","验证码失效");
			return resp;
		}
		if(!code.equals(verifycode)) {
			resp.setErrorInfo("406", "verifycode.error","验证码错误");
			return resp;
		}
 	 
		String ip = IPUtil.getIpAddr(request);
		try {
			MobileBO mobileBO = new MobileBO(countryCode, mobile);
			memberService.createMemeber(mobileBO, null, password, ip, apiAgent, cabinetAlias);
		} catch (MemberExistedException e) {
			resp.setErrorInfo("406", "member.existed", "您已经是平台用户");
			return resp;
		}
		return resp;
	}

	// 上传昵称和头像
	@PreAuthorize("hasRole('" + AuthContants.ROLE_END_USER + "')")
	@RequestMapping(value = "/update_binding_sysCode", method = { RequestMethod.GET })
	@ResponseBody
	@ApiOperation(value = "个人用户修改绑定的机柜", notes = "个人用户修改绑定的机柜")
	public ResponseEntity<SimpleResponse> updateBindingSysCode(HttpServletRequest request,
			@UserAuthentication User user,
			@ApiParam(value = "机柜别名", required = true) @RequestParam("cabinetAlias") String cabinetAlias) {

		SimpleResponse resp = new SimpleResponse(messageSource, request);
		resp.setSuccess(true);

		Long uid = Long.valueOf(user.getUsername());

		try {
			memberService.updateBindingSysCode(uid, cabinetAlias);
		} catch (CabinetNotFindException e) {
			resp.setSuccess(false);
			resp.setErrorInfo("406", "promoCode.notFind", "Please enter a valid promocode");
			return Responses.ok(resp);
		} catch (UserHasBindedCabinetException e) {
			resp.setSuccess(false);
			resp.setErrorInfo("406", "NOMObox.hasBinded", "Already bounded Box");
			return Responses.ok(resp);
		}

		return Responses.ok(resp);
	}

	@RequestMapping(value = "/update_password_by_mobile", method = { RequestMethod.POST })
	@ResponseBody
	@ApiOperation(value = "手机号修改密码", notes = "手机号修改密码", authorizations = @Authorization(value = ResourceServerMain.securitySchemaOAuth2Cc))
	public SimpleResponse updatePasswordByMobile(HttpServletRequest request,
			@RequestHeader(name = "api-agent", required = false) String apiAgent,
			@ApiParam(value = "国家编码", required = true) @RequestParam("countryCode") String countryCode,
			@ApiParam(value = "手机号码", required = true) @RequestParam("mobile") String mobile,
			@ApiParam(value = "验证码", required = true) @RequestParam("verifycode") String verifycode,
			@ApiParam(value = "密码", required = true) @RequestParam("password") String password) {
		SimpleResponse resp = new SimpleResponse(messageSource, request);

		if (!ValidateUtil.validPwd(password)) {
			resp.setErrorInfo("400", "password.format.error", "密码格式不正确");
			return resp;
		}

//		if (verifyCodeUtil.verifyCode(jedisPool, mobile, verifycode)) {
		try {
			MobileBO mobileBO = new MobileBO(countryCode, mobile);
			memberService.updateMemberPassword(mobileBO, password);
		} catch (AccountNotFindException e) {
			resp.setErrorInfo("406", "account.notFind", "账户不存在");
			return resp;
		}
//		}else{
//			resp.setErrorInfo("406", "verifycode.error","验证码错误");
//			return resp;
//		}
		return resp;
	}

	@PreAuthorize("hasRole('" + AuthContants.ROLE_END_USER + "')")
	@RequestMapping(value = "/update_password_by_password", method = { RequestMethod.POST })
	@ResponseBody
	@ApiOperation(value = "原密码修改密码", notes = "原密码修改密码")
	public SimpleResponse updatePasswordByPassword(HttpServletRequest request, @UserAuthentication User user,
			@ApiParam(value = "原密码", required = true) @RequestParam("oldPassword") String oldPassword,
			@ApiParam(value = "密码", required = true) @RequestParam("password") String password) {
		long uid = Long.valueOf(user.getUsername());
		SimpleResponse resp = new SimpleResponse(messageSource, request);

		if (!ValidateUtil.validPwd(password)) {
			resp.setErrorInfo("400", "password.format.error", "密码格式不正确");
		}
		try {
			memberService.updateMemberPassword(uid, password, oldPassword);
		} catch (AccountNotFindException e) {
			resp.setErrorInfo("406", "account.notFind", "账户不存在");
			return resp;
		} catch (PasswordErrorException e) {
			resp.setErrorInfo("406", "account.passordError", "原密码错误");
			return resp;
		}
		return resp;
	}

	@PreAuthorize("hasRole('" + AuthContants.ROLE_END_USER + "')")
	@RequestMapping(value = "/vip_pay_list", method = { RequestMethod.GET })
	@ApiOperation(value = "获取购买会员卡记录", notes = "获取购买会员卡记录,使用createTime 降序排列")
	public ResponseEntity<PageResponse<APIVipPayLogBO>> findVipPayList(@UserAuthentication User user,
			@ApiParam(value = "createTime小于该时间的数据", required = false) @RequestParam(value = "ltTime", required = false) Long ltTime) {
		long uid = Long.valueOf(user.getUsername());
		PageResponse<APIVipPayLogBO> resp = new PageResponse<APIVipPayLogBO>();
		resp.setSuccess(true);

		Page<VipPayLogBO> page = new Page<>();
		page.setPageNumber(1);
		page.setPageSize(10);

		VipPayLogVO vo = new VipPayLogVO();
		vo.setUid(uid);
		vo.setStatus(CommonConstants.VIP_PAY_LOG_STATUS_2);
		if (ltTime != null) {
			vo.setLtTime(new Date(ltTime));
		}
		List<VipPayLogBO> list = memberService.findVipPayList(page, vo);
		resp.init(page, MapperUtils.map(mapper, list, APIVipPayLogBO.class));
		return Responses.ok(resp);
	}

	@PreAuthorize("hasRole('" + AuthContants.ROLE_END_USER + "')")
	@RequestMapping(value = "/integral_list", method = { RequestMethod.GET })
	@ApiOperation(value = "会员积分列表", notes = "会员积分列表,使用createTime 降序排列")
	public ResponseEntity<PageResponse<APIIntegralLogBO>> findIntegralList(@UserAuthentication User user,
			@ApiParam(value = "createTime小于该时间的数据", required = false) @RequestParam(value = "ltTime", required = false) Long ltTime,
			@ApiParam(value = "积分类型,exchange 兑换", required = false) @RequestParam(value = "type", required = false) String type) {
		long uid = Long.valueOf(user.getUsername());
		PageResponse<APIIntegralLogBO> resp = new PageResponse<APIIntegralLogBO>();
		resp.setSuccess(true);

		Page<IntegralLogBO> page = new Page<>();
		page.setPageNumber(1);
		page.setPageSize(10);

		IntegralLogVO vo = new IntegralLogVO();
		vo.setUid(uid);
		vo.setLogType(type);
		if (ltTime != null) {
			vo.setLtTime(new Date(ltTime));
		}
		List<IntegralLogBO> list = integralLogService.findPageByVo(page, vo);
		resp.init(page, MapperUtils.map(mapper, list, APIIntegralLogBO.class));
		return Responses.ok(resp);
	}

	@PreAuthorize("hasRole('" + AuthContants.ROLE_END_USER + "')")
	@RequestMapping(value = "/exchange_code", method = { RequestMethod.POST })
	@ApiOperation(value = "兑换邀请码", notes = "兑换邀请码")
	public ResponseEntity<SimpleResponse> exchangeCode(@UserAuthentication User user, HttpServletRequest request,
			@ApiParam(value = "邀请码", required = false) @RequestParam(value = "inviteCode", required = false) String inviteCode) {
		long uid = Long.valueOf(user.getUsername());
		SimpleResponse resp = new SimpleResponse(messageSource, request);
		try {
			integralLogService.exchangeCode(uid, inviteCode);
		} catch (InviteCodeUsedException e) {
			resp.setErrorInfo("406", "inviteCode.used", "Invitation code is already used");
		} catch (InviteCodeNotFindException e) {
			resp.setErrorInfo("406", "inviteCode.notFind", "The invitation code does not exist");
		}
		return Responses.ok(resp);
	}

	@PreAuthorize("hasRole('" + AuthContants.ROLE_END_USER + "')")
	@RequestMapping(value = "/vip_pay_result", method = { RequestMethod.GET })
	@ApiOperation(value = "购买会员卡支付是否成功", notes = "购买会员卡支付是否成功")
	public ResponseEntity<SingleResponse<Boolean>> isCompleteVIPPay(
			@ApiParam(value = "支付的流水号") @RequestParam(value = "sn") String sn, @UserAuthentication User user) {
//		long uid = Long.valueOf(user.getUsername());
		SingleResponse<Boolean> resp = new SingleResponse<>();
		resp.setItem(memberService.isCompleteVIPPay(sn));
		return Responses.ok(resp);
	}

	@PreAuthorize("hasRole('" + AuthContants.ROLE_END_USER + "')")
	@RequestMapping(value = "/Membership_card_list", method = { RequestMethod.GET })
	@ApiOperation(value = "获取会员卡列表", notes = "获取会员卡列表")
	public ResponseEntity<ListResponse<APIMembershipCardBO>> findMembershipCardList(
			@RequestHeader(name = "api-agent", required = false) String apiAgent, HttpServletRequest request,
			@UserAuthentication User user) {
		long uid = Long.valueOf(user.getUsername());
		ListResponse<APIMembershipCardBO> resp = new ListResponse<>(messageSource, request);
		resp.setSuccess(true);

		List<MembershipCardBO> list = membershipCardService.findMembershipCardList(uid);

		resp.setItems(MapperUtils.map(mapper, list, APIMembershipCardBO.class));

		return Responses.ok(resp);
	}

	@RequestMapping(value = "/paymet_vip", method = { RequestMethod.POST })
	@PreAuthorize("hasRole('" + AuthContants.ROLE_END_USER + "')")
	@ApiOperation(value = "购买会员卡")
	@ApiResponses(value = { @ApiResponse(code = 400, message = "参数错误"), @ApiResponse(code = 406, message = "业务操作错误") })
	public ResponseEntity<SingleResponse<PayDetailsBO>> paymetVIP(@UserAuthentication User user,
			HttpServletRequest request, @RequestHeader(name = "api-agent", required = false) String apiAgent,
			@ApiParam(value = "会员卡id") @RequestParam(value = "cardId") Long cardId,
//			@ApiParam(value = "成功返回地址", required = false) @RequestParam(value="returnUrl",required=false) String returnUrl,
			@ApiParam(value = "支付方式  alipay_app,wx_app,stripe,cashfree", required = false) @RequestParam(value = "paymentMark", required = false, defaultValue = PaymentConstants.PAYMENT_MARK_CASHFREE) String paymentMark) {
		Long uid = Long.parseLong(user.getUsername());

		SingleResponse<PayDetailsBO> resp = new SingleResponse<>(messageSource, request);

		PayDetailsBO payDetails = new PayDetailsBO();

		try {

//			memberService.saveReturnUrl(uid, returnUrl, jedisPool);

			MyDirectPayResponse result = memberService.createVIPPay(uid, cardId, payCallback, paymentMark);
			DirectPayResponse payResponse = result.getDirectPayResponse();
			if (!payResponse.isSuccess()) {
				resp.setErrorInfo("406", payResponse.getMsg());
				return Responses.ok(resp);
			}

			payDetails.setSn(result.getSn());

			switch (payResponse.getChannel()) {

			case ALIPAY:
				payDetails.setAlipaPpaySignature(payResponse.getContent());
				break;

			case WEIXIN:
				if (PaymentConstants.PAYMENT_MARK_WX_JS_PAY.equals(paymentMark)) {
					logger.info("========[{}],[{}]" + JSON.toJSONString(payResponse.getPayConfig()),
							payResponse.getContent());
					payDetails.setWeiXinPaymet(WeixinPayUtil
							.makeWeixinJsPaymet((WeixinConfig) payResponse.getPayConfig(), payResponse.getContent()));
				} else {
					logger.info("========paymentMark:" + paymentMark);
					payDetails.setWeiXinPaymet(WeixinPayUtil
							.makeWeixinAppPaymet((WeixinConfig) payResponse.getPayConfig(), payResponse.getContent()));
				}
				break;
			case STRIPE:
				payDetails.setSn(payResponse.getPayOrderId());
				break;
			case CASHFREE:
				payDetails.setCashfreePaymentBO(makeCashfreePaymentBO(payResponse));
				break;
			default:
				break;
			}

			resp.setItem(payDetails);
			resp.setSuccess(true);

		} catch (DepositNotPayException e) {
			resp.setSuccess(false);
			resp.setErrorInfo("406", "member.notDeposit", "您还未交押金");
			return Responses.ok(resp);
		} catch (BasicCostNotPayException e) {
			resp.setSuccess(false);
			resp.setErrorInfo("406", "basicCost.notPay", "您还未支付基础会员费，请先去支付");
			return Responses.ok(resp);

		} catch (ServiceException e) {
			resp.setSuccess(false);
			resp.setError("406");
			resp.setErrorDescription(e.getMessage());
			return Responses.ok(resp);
		}
		return Responses.ok(resp);
	}

	@RequestMapping(value = "/paymet_basic_cost", method = { RequestMethod.POST })
	@PreAuthorize("hasRole('" + AuthContants.ROLE_END_USER + "')")
	@ApiOperation(value = "缴纳基础会员费")
	@ApiResponses(value = { @ApiResponse(code = 400, message = "参数错误"), @ApiResponse(code = 406, message = "业务操作错误") })
	public ResponseEntity<SingleResponse<PayDetailsBO>> paymetBasicCost(@UserAuthentication User user,
			HttpServletRequest request,
//			@ApiParam(value = "成功跳转地址", required = false) @RequestParam(value="returnUrl",required=false) String returnUrl,
			@ApiParam(value = "支付方式  alipay_app,wx_app,stripe,cashfree", required = false) @RequestParam(value = "paymentMark", required = false, defaultValue = PaymentConstants.PAYMENT_MARK_WX_JS_PAY) String paymentMark) {
		Long uid = Long.parseLong(user.getUsername());

		SingleResponse<PayDetailsBO> resp = new SingleResponse<>(messageSource, request);

		PayDetailsBO payDetails = new PayDetailsBO();

		try {

//			memberService.saveReturnUrl(uid, returnUrl, jedisPool);

			DirectPayResponse payResponse = memberService.createBasicCostPay(uid, payCallback, paymentMark);

			if (!payResponse.isSuccess()) {
				resp.setErrorInfo("406", payResponse.getMsg());
				return Responses.ok(resp);
			}

			switch (payResponse.getChannel()) {

			case ALIPAY:
				payDetails.setAlipaPpaySignature(payResponse.getContent());
				break;

			case WEIXIN:
				if (PaymentConstants.PAYMENT_MARK_WX_JS_PAY.equals(paymentMark)) {
					logger.info("========[{}],[{}]" + JSON.toJSONString(payResponse.getPayConfig()),
							payResponse.getContent());
					payDetails.setWeiXinPaymet(WeixinPayUtil
							.makeWeixinJsPaymet((WeixinConfig) payResponse.getPayConfig(), payResponse.getContent()));
				} else {
					logger.info("========paymentMark:" + paymentMark);
					payDetails.setWeiXinPaymet(WeixinPayUtil
							.makeWeixinAppPaymet((WeixinConfig) payResponse.getPayConfig(), payResponse.getContent()));
				}
				break;
			case STRIPE:
				payDetails.setSn(payResponse.getPayOrderId());
				break;
			case CASHFREE:
				payDetails.setCashfreePaymentBO(makeCashfreePaymentBO(payResponse));
				break;
			default:
				break;
			}

			resp.setItem(payDetails);

			resp.setSuccess(true);
		} catch (DepositNotPayException e) {
			resp.setSuccess(false);
			resp.setErrorInfo("406", "member.notDeposit", "您还未交押金");
			return Responses.ok(resp);
		} catch (ServiceException e) {
			resp.setSuccess(false);
			resp.setError("406");
			resp.setErrorDescription(e.getMessage());
			return Responses.ok(resp);
		}
		return Responses.ok(resp);
	}

	@PreAuthorize("hasRole('" + AuthContants.ROLE_END_USER + "')")
	@RequestMapping(value = "/bound_invite_code", method = { RequestMethod.POST })
	@ResponseBody
	@ApiOperation(value = "绑定邀请码")
	public ResponseEntity<SimpleResponse> boundInviteCode(HttpServletRequest request, @UserAuthentication User user,
			@ApiParam(value = "邀请码", required = true) @RequestParam("inviteCode") String inviteCode) {
		SimpleResponse resp = new SimpleResponse();
		long uid = Long.valueOf(user.getUsername());
		try {
			memberService.boundInviteCode(uid, inviteCode);
		} catch (ServiceException e) {
			resp.setErrorInfo("406", e.getMessage());
		}
		return Responses.ok(resp);
	}

	
	@RequestMapping(value = "/send_modile_verifycode", method = { RequestMethod.GET })
	@ResponseBody
	@ApiOperation(value = "手机发送短信验证码", notes = "手机发送短信验证码.<br/> App级别", authorizations = @Authorization(value = ResourceServerMain.securitySchemaOAuth2Cc))
	public ResponseEntity<SimpleResponse> sendModileVerifycode(HttpServletRequest request,
			@ApiParam(value = "国家编号", required = true) @RequestParam(value="country_code", required=true) String countryCode,
			@ApiParam(value = "手机号码", required = true) @RequestParam(value="phone_number", required=true) String phonenumber) {
		SimpleResponse resp = new SimpleResponse();

		// 发送短信
		verifyCodeUtil.sendCode(request, msgTools, jedisPool, countryCode,phonenumber, smsType,smsSignature);
		return Responses.ok(resp);
	}

	@PreAuthorize("hasRole('" + AuthContants.ROLE_END_USER + "')")
	@RequestMapping(value = "/bound_mobile", method = { RequestMethod.POST })
	@ApiOperation(value = "用户绑定手机号", notes = "用户绑定手机号")
	@ApiResponses(value = { @ApiResponse(code = 400, message = "绑定失败"), @ApiResponse(code = 406, message = "短信验证码错误"),
			@ApiResponse(code = 410, message = "请重新授权") })
	@ResponseBody
	public SingleResponse<ThirdPartyBoundModileBO> boundModile(HttpServletRequest request,
			@UserAuthentication User user, @RequestHeader("api-agent") String apiAgent,
			@ApiParam(value = "国家编码", required = true) @RequestParam("countryCode") String countryCode,
			@ApiParam(value = "手机号码", required = true) @RequestParam("mobile") String mobile,
			@ApiParam(value = "验证码", required = false) @RequestParam(value="verifycode",required = false) String verifycode,
			@ApiParam(value = "密码", required = true) @RequestParam("password") String password) {

		SingleResponse<ThirdPartyBoundModileBO> resp = new SingleResponse<>(messageSource, request);
		long uid = Long.valueOf(user.getUsername());

		if (!ValidateUtil.validPwd(password)) {
			resp.setErrorInfo("400", "password.format.error", "密码格式不正确");
			return resp;
		}

		MobileBO mobileBO = new MobileBO(countryCode, mobile);
		
		if(StringUtils.isNotBlank(verifycode)) {
			if(!verifyCodeUtil.verifyCode(jedisPool, mobile, verifycode)) {
				resp.setSuccess(false);
				resp.setError("406");
				resp.setErrorDescription("短信验证码错误");
				return resp;
			}
		}

//			if (verifyCodeUtil.verifyCode(jedisPool, mobile, verifycode)) {
		try {
			ThirdPartyBoundModileBO result = new ThirdPartyBoundModileBO();
			String ip = IPUtil.getIpAddr(request);

			UserAccount account = memberService.thirdPartyBoundModile(uid, mobileBO, password, ip, apiAgent);

			if (CommonConstants.ACCOUNT_TYPE_3.equals(account.getAccountType())
					|| CommonConstants.ACCOUNT_TYPE_4.equals(account.getAccountType())
					|| CommonConstants.ACCOUNT_TYPE_5.equals(account.getAccountType())
					|| CommonConstants.ACCOUNT_TYPE_6.equals(account.getAccountType())
					|| CommonConstants.ACCOUNT_TYPE_7.equals(account.getAccountType())
					|| CommonConstants.ACCOUNT_TYPE_8.equals(account.getAccountType())) {
				result.setIsExistModile(true);
				// 添加第三方登入令牌
				String token = UUID.randomUUID().toString();
				loginTokenUtil.setThirdPartyCode(account.getLoginName(), token);
				result.setLoginName(account.getLoginName());
				result.setToken(token);
			} else {
				result.setIsExistModile(false);
			}

			resp.setItem(result);
			return resp;
		} catch (MemberExistedException e) {
			resp.setErrorInfo("406", "account.existed", "号码已存在");
			return resp;
		} catch (ServiceException e) {
			resp.setSuccess(false);
			resp.setErrorDescription(e.getMessage());
			resp.setError(org.apache.commons.lang3.StringUtils.isNotBlank(e.getCode()) ? e.getCode() : "400");
			return resp;
		}
//			}else{
//				resp.setSuccess(false);
//				resp.setError("406");
//				resp.setErrorDescription("短信验证码错误");
//				return resp;
//			}
	}

	@PreAuthorize("hasRole('" + AuthContants.ROLE_END_USER + "')")
	@RequestMapping(value = "/bound_third_party", method = { RequestMethod.POST })
	@ApiOperation(value = "用户绑定第三方账号", notes = "用户绑定手机号")
	@ApiResponses(value = { @ApiResponse(code = 406, message = "绑定失败") })
	@ResponseBody
	public SimpleResponse boundThirdParty(HttpServletRequest request, @UserAuthentication User user,
			@RequestHeader("api-agent") String apiAgent,
			@ApiParam(value = "第三方openId", required = true) @RequestParam("openId") String openId,
			@ApiParam(value = "第三方accessToken", required = true) @RequestParam("accessToken") String accessToken,
			@ApiParam(value = "第三方类型，weixin,qq,facebook,google", required = true) @RequestParam("third_party_type") String thirdPartyType,
			@ApiParam(value = "clientId，google 时上传", required = false) @RequestParam("clientId") String clientId
			) {

		SimpleResponse resp = new SimpleResponse(messageSource, request);
		long uid = Long.valueOf(user.getUsername());
		UserSource userSource = null;
		switch (thirdPartyType) {
		case "weixin":
			userSource = UserSource.WEIXIN_OPEN;
			break;
		case "qq":
			userSource = UserSource.QQ;
		case "facebook":
			userSource = UserSource.FACEBOOK;
			break;
		case  "google":
			userSource = UserSource.GOOGLE;
			break;
		default:
			resp.setErrorInfo("400", "third_party_type 值错误");
			return resp;
		}
		try {
			memberService.boundThirdParty(uid, openId, accessToken, userSource,clientId);
		} catch (AccountExistedException e) {
			resp.setErrorInfo("406", "account.thirdParty.existed", new Object[] { thirdPartyType }, "第三方账户已存在");
			return resp;
		}
		return resp;
	}

	@RequestMapping(value = "/third_party_login", method = { RequestMethod.POST })
	@ResponseBody
	@ApiOperation(value = "第三用户登入接口", notes = "第三方用户登入1.<br/> App级别", authorizations = @Authorization(value = ResourceServerMain.securitySchemaOAuth2Cc))
	@ApiResponses(value = { @ApiResponse(code = 400, message = "登入失败") })
	public ResponseEntity<SingleResponse<String>> thirdPartyLogin(HttpServletRequest request,
			@RequestHeader(name = "api-agent", required = false) String apiAgent,
			@ApiParam(value = "第三方openId", required = true) @RequestParam("openId") String openId,
			@ApiParam(value = "第三方accessToken", required = true) @RequestParam("accessToken") String accessToken,
			@ApiParam(value = "第三方类型，weixin,qq,facebook,google", required = true) @RequestParam("third_party_type") String thirdPartyType,
			@ApiParam(value = "clientId , google 时上传", required = false) @RequestParam("clientId") String clientId
			) {

		SingleResponse<String> resp = new SingleResponse<>();
		try {
			UserSource userSource = null;
			switch (thirdPartyType) {
			case "weixin":
				userSource = UserSource.WEIXIN_OPEN;
				break;
			case "qq":
				userSource = UserSource.QQ;
			case "facebook":
				userSource = UserSource.FACEBOOK;
				break;
			case "google":
				userSource = UserSource.GOOGLE;
				break;
			default:
				throw new ServiceException("third_party_type 值错误");
			}
			String ip = IPUtil.getIpAddr(request);
			memberService.registerThirdPartyUser(openId, accessToken, userSource, ip, apiAgent,clientId);
		} catch (ServiceException e) {
			resp.setSuccess(false);
			resp.setErrorDescription(e.getMessage());
			resp.setError("400");
			return Responses.ok(resp);
		}

		// 添加第三方登入令牌
		String token = UUID.randomUUID().toString();
		loginTokenUtil.setThirdPartyCode(openId, token);

		resp.setItem(token);
		return Responses.ok(resp);
	}

	// 上传昵称和头像
	@PreAuthorize("hasRole('" + AuthContants.ROLE_END_USER + "')")
	@RequestMapping(value = "/save_user_info", method = { RequestMethod.GET })
	@ResponseBody
	@ApiOperation(value = "微信小程序上传昵称和头像")
	public ResponseEntity<SimpleResponse> saveUserInfo(HttpServletRequest request, @UserAuthentication User user,
			@ApiParam(value = "昵称", required = true) @RequestParam("nickname") String nickname,
			@ApiParam(value = "头像", required = true) @RequestParam("headImgUrl") String headImgUrl) {
		SimpleResponse resp = new SimpleResponse();
		resp.setSuccess(true);
		Long uid = Long.valueOf(user.getUsername());

		vc.thinker.cabbage.user.bo.MemberBO member = memberService.findByUid(uid);

		vc.thinker.cabbage.user.bo.MemberBO up_member = new vc.thinker.cabbage.user.bo.MemberBO();
		up_member.setUid(member.getUid());

		if (!org.apache.commons.lang3.StringUtils.isEmpty(member.getNickname())
				&& !org.apache.commons.lang3.StringUtils.isEmpty(member.getHeadImgPath())) {
			return Responses.ok(resp);
		}

		if (org.apache.commons.lang3.StringUtils.isEmpty(member.getNickname())) {
			up_member.setNickname(nickname);
		}
		if (org.apache.commons.lang3.StringUtils.isEmpty(member.getHeadImgPath())) {

			try {

				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				headImgUrl = headImgUrl.replace("https", "http");
				HttpRequest.get(headImgUrl).receive(bos);
				RequestResult result = imageFsClient.upload(bos.toByteArray(), System.currentTimeMillis() + "_WX",
						"image/jpeg");
				up_member.setHeadImgPath(result.getUrl());

			} catch (Exception e) {
				logger.info("========获取用户头像出错。。。", e);
			}
		}

		if (org.apache.commons.lang3.StringUtils.isEmpty(member.getInviteCode())) {
			up_member.setInviteCode(uniqueRadomCodeService.getCode(CommonConstants.CODE_TYPE_INVITION));
		}

		memberService.updateMemberBaseInfo(up_member);
		return Responses.ok(resp);
	}

	@PreAuthorize("hasRole('" + AuthContants.ROLE_END_USER + "')")
	@RequestMapping(value = "/modify_user_nickname", method = { RequestMethod.GET })
	@ResponseBody
	@ApiOperation(value = "客户端修改昵称")
	public ResponseEntity<SimpleResponse> modifyUserNickname(@UserAuthentication User user,
			@ApiParam(value = "昵称", required = true) @RequestParam("nickname") String nickname) {

		SimpleResponse resp = new SimpleResponse();
		resp.setSuccess(true);
		Long uid = Long.valueOf(user.getUsername());

		if (sensitivewordFilter.isContaintSensitiveWord(nickname, 1)) {
			resp.setErrorInfo("400", "昵称包含敏感字符，无法修改");
			return Responses.ok(resp);
		}

		vc.thinker.cabbage.user.bo.MemberBO up_m = new vc.thinker.cabbage.user.bo.MemberBO();

		up_m.setNickname(nickname);
		up_m.setUid(uid);

		memberService.updateMemberBaseInfo(up_m);
		return Responses.ok(resp);
	}

	@PreAuthorize("hasRole('" + AuthContants.ROLE_END_USER + "')")
	@RequestMapping(value = "/modify_user_head", method = { RequestMethod.GET })
	@ResponseBody
	@ApiOperation(value = "客户端修改头像")
	public ResponseEntity<SimpleResponse> modifyUserHead(@UserAuthentication User user,
			@ApiParam(value = "头像", required = true) @RequestParam("headImgUrl") String headImgUrl) {

		SimpleResponse resp = new SimpleResponse();
		resp.setSuccess(true);
		Long uid = Long.valueOf(user.getUsername());

		vc.thinker.cabbage.user.bo.MemberBO up_m = new vc.thinker.cabbage.user.bo.MemberBO();
		up_m.setHeadImgPath(headImgUrl);
		up_m.setUid(uid);

		memberService.updateMemberBaseInfo(up_m);
		return Responses.ok(resp);
	}

	// 实名认证
	@PreAuthorize("hasRole('" + AuthContants.ROLE_END_USER + "')")
	@RequestMapping(value = "/real_name", method = { RequestMethod.POST })
	@ResponseBody
	@ApiOperation(value = "实名认证")
	public ResponseEntity<SingleResponse<MemberBO>> realname(HttpServletRequest request, @UserAuthentication User user,
			@ApiParam @RequestBody @Valid RealnameVO vo) {

		SingleResponse<MemberBO> resp = new SingleResponse<MemberBO>();
		resp.setSuccess(true);
		Long uid = Long.valueOf(user.getUsername());

		if (!ValidateUtil.checkIdCard(vo.getIdcard())) {
			resp.setErrorInfo("400", "您的身份证格式错误");
			return Responses.ok(resp);
		}

		if (IdCardUtil.getAgeByIdCard(vo.getIdcard()) < 12) {
			resp.setErrorInfo("400", "您的年龄未满12周岁，无法通过实名认证");
			return Responses.ok(resp);
		}

		if (sensitivewordFilter.isContaintSensitiveWord(vo.getName(), 1)) {
			resp.setErrorInfo("400", "名称中有敏感字符，无法实名认证");
			return Responses.ok(resp);
		}

		memberService.realnameUpdateMember(uid, vo.getName(), vo.getIdcard(), vo.getSchoolName(), vo.getStudentId(),
				vo.getCredentialsImages());

		resp.setItem(mapper.map(memberService.findByUid(uid), MemberBO.class));
		return Responses.ok(resp);
	}

	// 我的优惠券
	@RequestMapping(value = "/listCoupon", method = { RequestMethod.POST })
	@PreAuthorize("hasRole('" + AuthContants.ROLE_END_USER + "')")
	@ApiOperation(value = "我的优惠券")
	public ResponseEntity<PageResponse<APIUserCouponBO>> findCouponList(@UserAuthentication User user) {

		PageResponse<APIUserCouponBO> resp = new PageResponse<APIUserCouponBO>();
		Long uid = Long.parseLong(user.getUsername());
		resp.setSuccess(true);

		Page<vc.thinker.cabbage.user.bo.UserCouponBO> page = new Page<>();
		page.setPageNumber(1);
		page.setPageSize(200);
		List<vc.thinker.cabbage.user.bo.UserCouponBO> list = userCouponService.findPageListByUid(uid, page);

		resp.init(page, MapperUtils.map(mapper, list, APIUserCouponBO.class));
		return Responses.ok(resp);
	}

	@PreAuthorize("hasRole('" + AuthContants.ROLE_END_USER + "')")
	@RequestMapping(value = "/user_balance", method = { RequestMethod.GET })
	@ApiOperation(value = "获取余额", notes = "获取余额")
	public ResponseEntity<SingleResponse<APIUserMoneyBO>> getMyBalance(@UserAuthentication User user) {
		SingleResponse<APIUserMoneyBO> resp = new SingleResponse<APIUserMoneyBO>();
		long uid = Long.valueOf(user.getUsername());
		UserMoney money = moneyService.findOne(uid);
		if (money == null) {
			money = new UserMoney();
			vc.thinker.cabbage.user.bo.MemberBO member = memberService.findOne(uid);
			money.setAvailableBalance(new BigDecimal(0));
			money.setCurrency(member.getCurrency());
		}
		resp.setItem(mapper.map(money, APIUserMoneyBO.class));
		return Responses.ok(resp);
	}

	// 获取用户个人信息
	@PreAuthorize("hasRole('" + AuthContants.ROLE_END_USER + "')")
	@RequestMapping(value = "/userprofile", method = { RequestMethod.GET })
	@ApiOperation(value = "获取本人信息", notes = "获取指定id的信息.<br/> 用户级别")
	public ResponseEntity<SingleResponse<vc.thinker.apiv2.bo.MemberBO>> getMyProfile(HttpServletRequest request,
			@UserAuthentication User user) {
		SingleResponse<MemberBO> resp = new SingleResponse<MemberBO>();
		long uid = Long.valueOf(user.getUsername());
		
		vc.thinker.cabbage.user.bo.MemberBO mb = memberService.findDetails(uid);

		vc.thinker.apiv2.bo.MemberBO item = mapper.map(mb, vc.thinker.apiv2.bo.MemberBO.class);

		if (mb.getIsVIP() != null && mb.getIsVIP()) {
			Locale locale = RequestContextUtils.getLocale(request);
			long second = (mb.getVipExpiresIn().getTime() - new Date().getTime()) / 1000;
			item.setVipDxpireDateDesc(MemberUtil.formatSecond(second, locale));
		}

//		String avatar = StringUtils.isNotEmpty(item.getHeadImgPath())?item.getHeadImgPath():"";
//		if(StringUtils.isEmpty(avatar)){
//			avatar = server_url + "/img/head_default.png";
//			item.setHeadImgPath(avatar);
//		}

		if (org.apache.commons.lang3.StringUtils.isEmpty(item.getCabinetAlias())) {
			item.setCabinetAlias(item.getSysCode());
		}

		if (org.apache.commons.lang3.StringUtils.isEmpty(item.getSysCode())) {
			item.setSysCode(null);
			item.setCabinetAlias(null);
		}
		item.setBalance(moneyService.getAvailableBalance(uid).setScale(2, BigDecimal.ROUND_DOWN));
		resp.setItem(item);
		return Responses.ok(resp);
	}

	// 获取押金记录
	@PreAuthorize("hasRole('" + AuthContants.ROLE_END_USER + "')")
	@RequestMapping(value = "/depositLog", method = { RequestMethod.GET })
	@ApiOperation(value = "获取押金记录", notes = "押金记录列表,使用createTime 降序排列<br/>")
	public ResponseEntity<PageResponse<APIUserDepositPayLogBo>> depositLog(@UserAuthentication User user,
			@ApiParam(value = "createTime小于该时间的数据", required = false) @RequestParam(value = "ltTime", required = false) Long ltTime) {
		long uid = Long.valueOf(user.getUsername());
		PageResponse<APIUserDepositPayLogBo> resp = new PageResponse<APIUserDepositPayLogBo>();
		resp.setSuccess(true);

		Page<vc.thinker.cabbage.user.bo.UserDepositLogBO> page = new Page<>();
		page.setPageNumber(1);
		page.setPageSize(10);
		Date ltdate = null;
		if (ltTime != null) {
			ltdate = new Date(ltTime);
		}
		UserDepositLogVO vo = new UserDepositLogVO();
		vo.setUid(uid);
		vo.setLtdate(ltdate);
		List<vc.thinker.cabbage.user.bo.UserDepositLogBO> list = userDepositLogService.findPageByVO(page, vo);
		resp.init(page, MapperUtils.map(mapper, list, APIUserDepositPayLogBo.class));
		return Responses.ok(resp);
	}

	// 退押金
	@RequestMapping(value = "/refundDeposit", method = { RequestMethod.POST })
	@PreAuthorize("hasRole('" + AuthContants.ROLE_END_USER + "')")
	@ApiOperation(value = "退押金")
	@ApiResponses(value = { @ApiResponse(code = 400, message = "参数错误"),
			@ApiResponse(code = 407, message = "用户有未结束的订单") })
	public ResponseEntity<SimpleResponse> refundDeposit(
			HttpServletRequest request, 
			@UserAuthentication User user) {
		SimpleResponse resp = new SimpleResponse(messageSource, request);
		Long uid = Long.valueOf(user.getUsername());
		
		try {
			memberService.refundDeposit(uid);
		} catch (UnfinishedOrdersExceptiion e) {
			resp.setErrorInfo("407", "order.unfinished", e.getMessage());
			return Responses.ok(resp);
		} catch (DepositException e) {
			resp.setErrorInfo("400", "member.notDeposit", e.getMessage());
			return Responses.ok(resp);
		}catch (ServiceException e) {
			resp.setErrorInfo("400",e.getMessage());
			return Responses.ok(resp);
		}
		return Responses.ok(resp);
	}

	// 充押金支付
	@RequestMapping(value = "/paymet", method = { RequestMethod.POST })
	@PreAuthorize("hasRole('" + AuthContants.ROLE_END_USER + "')")
	@ApiOperation(value = "充押金")
	@ApiResponses(value = { 
			@ApiResponse(code = 400, message = "参数错误"), 
			@ApiResponse(code = 406, message = "业务操作错误"),
			@ApiResponse(code = 408, message = "用户币种未完善") })
	public ResponseEntity<SingleResponse<PayDetailsBO>> paymet(@UserAuthentication User user,
			HttpServletRequest request,
			@ApiParam(value = "支付方式 alipay_app,wx_app,stripe,cashfree, fondy, fondy_apple_pay", required = false) 
			@RequestParam(value = "paymentMark", required = false, defaultValue = PaymentConstants.PAYMENT_MARK_CASHFREE) String paymentMark,
			@ApiParam(value = "成功跳转地址", required = false) @RequestParam(value="returnUrl",required=false) String returnUrl) {
		Long uid = Long.parseLong(user.getUsername());

		logger.info("uid:[{}], returnUrl:[{}]", uid, returnUrl);
		
		SingleResponse<PayDetailsBO> resp = new SingleResponse<PayDetailsBO>(messageSource, request);
		resp.setSuccess(true);
		
		PayDetailsBO payDetails = new PayDetailsBO();

		try {

			memberService.saveReturnUrl(uid, returnUrl, jedisPool);

			DirectPayResponse payResponse = memberService.createDepositPay(uid, payCallback, paymentMark);

			if (!payResponse.isSuccess()) {
				resp.setErrorInfo("406", payResponse.getMsg());
				return Responses.ok(resp);
			}

			switch (payResponse.getChannel()) {

			case ALIPAY:
				payDetails.setAlipaPpaySignature(payResponse.getContent());
				break;
			case WEIXIN:
				if (PaymentConstants.PAYMENT_MARK_WX_JS_PAY.equals(paymentMark)) {
					logger.info("========" + paymentMark);
					payDetails.setWeiXinPaymet(WeixinPayUtil
							.makeWeixinJsPaymet((WeixinConfig) payResponse.getPayConfig(), payResponse.getContent()));
				} else {
					logger.info("========paymentMark:" + paymentMark);
					payDetails.setWeiXinPaymet(WeixinPayUtil
							.makeWeixinAppPaymet((WeixinConfig) payResponse.getPayConfig(), payResponse.getContent()));
				}
				break;
			case STRIPE:
				payDetails.setSn(payResponse.getPayOrderId());
				break;
			case CASHFREE:
				payDetails.setCashfreePaymentBO(makeCashfreePaymentBO(payResponse));
				break;
			case FONDY:
				payDetails.setFondyPaymetBO(new FondyPaymetBO(payResponse.getContent()));
				break;
//			case FONDYAPPLEPAY:
//				payDetails.setApplePaymentBO(makeApplePaymentBO(payResponse));
//				break;
			default:
				break;
			}
			resp.setItem(payDetails);
			resp.setSuccess(true);

		} catch (PlatNotConfigDepositException e) {
			resp.setSuccess(false);
			resp.setErrorInfo("406", "platform.noCofigDeposit", "No deposit is set for the platform");
			return Responses.ok(resp); 
		}catch (MemberNotBindMobileException e) {
			resp.setSuccess(false);
			resp.setErrorInfo("412", "user.notBoundMobile", "User Not bound mobile");
			return Responses.ok(resp); 
		}catch (ServiceException e) {
			resp.setSuccess(false);
			resp.setErrorInfo("406", e.getMessage());
			return Responses.ok(resp);
		} 
		return Responses.ok(resp);
	}

	private CashfreePaymentBO makeCashfreePaymentBO(DirectPayResponse payResponse) {
		CashfreePaymentBO item = new CashfreePaymentBO();
		item.setToken(payResponse.getContent());
		item.setOrderId(payResponse.getOrderId());
		item.setOrderCurrency(payResponse.getOrderCurrency());
		item.setOrderAmount(payResponse.getOrderAmount());
		item.setNotifyUrl(payResponse.getNotifyUrl());
		item.setCustomerPhone(payResponse.getCustomerPhone());
		item.setCustomerEmail("anoopvootkuri95@gmail.com");
		CashfreeConfig payConfig = (CashfreeConfig) payResponse.getPayConfig();
		item.setAppId(payConfig.getAppId());
		return item;
	}
	
	private ApplePaymentBO makeApplePaymentBO(DirectPayResponse payResponse) {
		ApplePaymentBO item = new ApplePaymentBO();
		item.setOrderId(payResponse.getOrderId());
		item.setCurrency(payResponse.getOrderCurrency());
		item.setAmount(payResponse.getOrderAmount());
		item.setServerCallBackUrl(payResponse.getNotifyUrl());
		return item;
	}

	@RequestMapping(value = "/feedback_type_list", method = { RequestMethod.GET })
	@PreAuthorize("hasRole('" + AuthContants.ROLE_END_USER + "')")
	@ApiOperation(value = "问题反馈类型列表")
	public ResponseEntity<ListResponse<FeedbackTypeBO>> findFeedbackTypeList(
			@ApiParam(value = "反馈类型分类，1 一般问题 2 行程中 3 已完成用户", required = true) @RequestParam(value = "type") String type,
			@UserAuthentication User user, HttpServletRequest request) {
		ListResponse<FeedbackTypeBO> resp = new ListResponse<>();
		
		logger.info("findFeedbackTypeList:[{}]",request.getLocale().getLanguage());
		
		List<vc.thinker.cabbage.se.bo.FeedbackTypeBO> list = feedbackService.findFeedbackTypeTypeByType(type, request.getLocale().getLanguage());
		if (!list.isEmpty()) {
			resp.setItems(MapperUtils.map(mapper, list, FeedbackTypeBO.class));
		}
 		return Responses.ok(resp);
	}

	@RequestMapping(value = "/feedback", method = { RequestMethod.POST })
	@PreAuthorize("hasRole('" + AuthContants.ROLE_END_USER + "')")
	@ApiOperation(value = "问题反馈")
	@ApiResponses(value = { @ApiResponse(code = 406, message = "业务操作错误") })
	public ResponseEntity<SimpleResponse> feedback(
			HttpServletRequest request,
			@RequestHeader(name = "api-agent", required = false) String apiAgent,
			@ApiParam @RequestBody @Valid FeedbackVO vo, @UserAuthentication User user) {
		SimpleResponse resp = new SimpleResponse(messageSource, request);
		Long uid = Long.valueOf(user.getUsername());
		try {
			Feedback feedback = mapper.map(vo, Feedback.class);
			feedback.setUid(uid);
			feedback.setClientSource(apiAgent);
			feedbackService.create(feedback);
		} catch (CabinetNotFindException e) {
			resp.setErrorInfo("406", "order.cabinetNotFind", e.getMessage());
		} catch (ServiceException e) {
			resp.setErrorInfo("406", e.getMessage());
		}
		return Responses.ok(resp);
	}

	@RequestMapping(value = "/get_deposit", method = { RequestMethod.GET })
	@PreAuthorize("hasRole('" + AuthContants.ROLE_END_USER + "')")
	@ApiOperation(value = "获取需要交的押金数额)")
	@ApiResponses(value = { @ApiResponse(code = 408, message = "用户币种未完善") })
	public ResponseEntity<SingleResponse<APIDepositBO>> get_deposit(@UserAuthentication User user,
			HttpServletRequest request) {
		SingleResponse<APIDepositBO> resp = new SingleResponse<>(messageSource, request);
//		Long uid = Long.valueOf(user.getUsername());

		try {
			APIDepositBO deposit = new APIDepositBO();
			SysSetting findOne = sysSettingDao.findOne();
			BigDecimal val = memberService.queryDetpsit(findOne.getPlayformDefaultCurrency());
			deposit.setDeposit(val);
			deposit.setCurrency(findOne.getPlayformDefaultCurrency());
			resp.setItem(deposit);
		} catch (PlatNotConfigDepositException e) {
			resp.setSuccess(false);
			resp.setErrorInfo("406", "platform.noCofigDeposit", "No deposit is set for the platform");
			return Responses.ok(resp);
		} catch (ServiceException e) {
			resp.setSuccess(false);
			resp.setErrorInfo("406", e.getMessage());
		}
		return Responses.ok(resp);
	}

	@RequestMapping(value = "/get_basic_cost", method = { RequestMethod.GET })
	@PreAuthorize("hasRole('" + AuthContants.ROLE_END_USER + "')")
	@ApiOperation(value = "获取要缴纳的基础会员费用)")
	@ApiResponses(value = { @ApiResponse(code = 408, message = "用户币种未完善") })
	public ResponseEntity<SingleResponse<APIBasicCostBO>> getBasicCost(@UserAuthentication User user,
			HttpServletRequest request) {

		SingleResponse<APIBasicCostBO> resp = new SingleResponse<APIBasicCostBO>(messageSource, request);

		Long uid = Long.valueOf(user.getUsername());

		try {
			APIBasicCostBO deposit = new APIBasicCostBO();

			vc.thinker.cabbage.user.bo.MemberBO member = memberService.findByUid(uid);

			BigDecimal val = memberService.findSysBasicCost(uid);
			deposit.setCost(val);
			deposit.setCurrency(member.getCurrency());
			resp.setItem(deposit);
		} catch (UserCurrencyNotPerfectException e) {
			resp.setSuccess(false);
			resp.setErrorInfo("408", "user.currenty.notFind", "The user's currency information is not perfect");
			return Responses.ok(resp);
		} catch (ServiceException e) {
			resp.setSuccess(false);
			resp.setErrorInfo("406", e.getMessage());
		}

		return Responses.ok(resp);
	}

	@RequestMapping(value = "/query_invite_amount", method = { RequestMethod.GET })
	@PreAuthorize("hasRole('" + AuthContants.ROLE_END_USER + "')")
	@ApiOperation(value = "查询邀请好友优惠券金额", notes = "查询邀请好友优惠券金额")
	@ApiResponses(value = { @ApiResponse(code = 400, message = "活动已结束") })
	public ResponseEntity<SingleResponse<BigDecimal>> queryInviteAmount() {

		SingleResponse<BigDecimal> resp = new SingleResponse<BigDecimal>();

		List<vc.thinker.cabbage.sys.bo.PromotionBO> pro_list = promotionService
				.findHaveInHand(PromotionConstants.PROMOTION_TYPE_INVITE_CODE, null);
		if (null != pro_list && pro_list.size() > 0) {
			vc.thinker.cabbage.sys.bo.CouponBO coupon = couponDao.findOne(pro_list.get(0).getCouponId());
			if (null != coupon.getAmount()) {
				resp.setItem(coupon.getAmount());
				resp.setSuccess(true);
			} else {
				resp.setSuccess(false);
				resp.setErrorInfo("401", "活动金额配置有误");
			}
		} else {
			resp.setSuccess(false);
			resp.setErrorInfo("400", "活动已结束");
		}

		return Responses.ok(resp);
	}

	// 购买充电宝
	@PreAuthorize("hasRole('" + AuthContants.ROLE_END_USER + "')")
	@RequestMapping(value = "/paymet_pb", method = { RequestMethod.GET })
	@ResponseBody
	@ApiOperation(value = "购买充电宝")
	public ResponseEntity<SimpleResponse> paymetPb(HttpServletRequest request,
			@UserAuthentication User user
//			@ApiParam(value = "成功回调地址", required = false) @RequestParam(value="returnUrl",required=false) String returnUrl,
//			@ApiParam(value = "支付方式  alipay_app,wx_app,wx_js,stripe,cashfree", required = true) 
//	@RequestParam(value = "paymentMark", required = true, defaultValue = PaymentConstants.PAYMENT_MARK_WX_JS_PAY) String paymentMark
	) {

		Long uid = Long.valueOf(user.getUsername());

		SimpleResponse resp = new SimpleResponse(messageSource, request);
		resp.setSuccess(true);
		
//		SingleResponse<PayDetailsBO> resp = new SingleResponse<>(messageSource, request);
//		PayDetailsBO payDetails = new PayDetailsBO();

		try {
			memberService.createPbBuyPay(uid);
		} catch (NoOngoingOrderException e) {
			resp.setSuccess(false);
			resp.setErrorInfo("406", "order.ongoing.notFind", "没有进行中的订单");
			return Responses.ok(resp);
		} catch (ServiceException e) {
			resp.setSuccess(false);
			resp.setError("406");
			resp.setErrorDescription(e.getMessage());
			return Responses.ok(resp);
		}
		return Responses.ok(resp);
	}

	@PreAuthorize("hasRole('" + AuthContants.ROLE_END_USER + "')")
	@RequestMapping(value = "/third_perfect_info", method = { RequestMethod.GET })
	@ResponseBody
	@ApiOperation(value = "第三方用户完善国家信息和机柜信息")
	public ResponseEntity<SimpleResponse> thirdPartyPerfectInfo(HttpServletRequest request,
			@UserAuthentication User user,
			@ApiParam(value = "国家代码 中国 86,马来西亚 60,新加坡 65", required = true) @RequestParam("countryCode") String countryCode,
			@ApiParam(value = "机柜别名", required = false) @RequestParam(value = "cabinetAlias", required = false) String cabinetAlias ) {

		SimpleResponse resp = new SimpleResponse(messageSource, request);

		resp.setSuccess(true);
		Long uid = Long.valueOf(user.getUsername());

		try {
			memberService.thirdPartyPerfectInfo(uid, countryCode, cabinetAlias);
		} catch (CabinetNotFindException e) {
			resp.setSuccess(false);
			resp.setErrorInfo("406", "promoCode.notFind", "The promo code does not exist");
			return Responses.ok(resp);
		} catch (CountryNotFindException e) {
			resp.setSuccess(false);
			resp.setErrorInfo("406", "countryCode.notFind", "The National code not exist");
			return Responses.ok(resp);
		} catch (CountryInfoHasExistException e) {
			resp.setSuccess(false);
			resp.setErrorInfo("406", "countryCode.hasExits", "The user's national information has been perfected");
			return Responses.ok(resp);
		}

		return Responses.ok(resp);
	}

	/**
	 * 校验客户端的版本号
	 * 
	 * @param clientVersion
	 * @return
	 */
	public Boolean checkClientVersion(String clientVersion) {

		logger.info("@@@@clientVersion:" + clientVersion);

		if (org.apache.commons.lang3.StringUtils.isEmpty(clientVersion)) {
			return true;
		}

		String client = clientVersion.split("/")[0];

		if (!CommonConstants.CLIENT_TYPE_IPHONE.equals(client)) {
			return true;
		}

		clientVersion = clientVersion.substring(clientVersion.lastIndexOf("/") + 2);

		Double clientBig = Double.parseDouble(clientVersion.substring(0, clientVersion.lastIndexOf(".")));
		Double clientSmall = Double.parseDouble(clientVersion.substring(clientVersion.lastIndexOf(".") + 1));

		Double compateBig = Double.parseDouble("1.4");
		Double compateSma = Double.parseDouble("2");

		if (clientBig.compareTo(compateBig) < 0) {
			return false;
		} else if (clientBig.compareTo(compateBig) == 0) {
			if (clientSmall.compareTo(compateSma) <= 0) {
				return false;
			}
		}

		return true;
	}

	
	@PreAuthorize("hasRole('" + AuthContants.ROLE_END_USER + "')")
	@RequestMapping(value = "/bound_email", method = { RequestMethod.POST })
	@ResponseBody
	@ApiOperation(value = "绑定邮箱")
	public ResponseEntity<SimpleResponse> boundEmail(HttpServletRequest request, @UserAuthentication User user,
			@ApiParam(value = "邮箱", required = true) @RequestParam("email") String email) {
		SimpleResponse resp = new SimpleResponse(messageSource, request);
		long uid = Long.valueOf(user.getUsername());
		try {
			memberService.boundEmail(uid, email);
		}  catch (InvalidEmailException e) {
			resp.setSuccess(false);
			resp.setErrorInfo("406", "invalid.email", "Invalid Email");
		}catch (ServiceException e) {
			resp.setSuccess(false);
			resp.setErrorInfo("406", e.getMessage());
		}
		return Responses.ok(resp);
	}
	
	
	// 废弃的接口, 
	@PreAuthorize("hasRole('" + AuthContants.ROLE_END_USER + "')")
	@RequestMapping(value = "/payNotify", method = { RequestMethod.POST })
	@ResponseBody
	@ApiOperation(value = "支付结果通知")
	public ResponseEntity<SimpleResponse> payNotify(@UserAuthentication User user,
			@ApiParam @RequestBody @Valid PayNotifyVO vo) {

		SimpleResponse resp = new SimpleResponse();
		resp.setSuccess(true);

//		try {
//			
//			logger.info("收到app支付回调,订单号:[{}],referenceId:[{}]",vo.getOrderId(), vo.getReferenceId());
//			
//			CashfreePayHandler payHandler = payHandlerFactory.getPayHandler(PayChannel.CASHFREE);
//			CashfreeNotify payNotify = mapper.map(vo, CashfreeNotify.class);
//			payNotify.setConfigMark(PaymentConstants.PAYMENT_MARK_CASHFREE);
//			
//			if (!(payHandler.verifyNotify(payNotify))) {
//				logger.info("签名错误");
//				return Responses.ok(resp);
//			}
//
//			if (!"SUCCESS".equals(payNotify.getTxStatus())) {
//				logger.info("交易状态为:[{}]", payNotify.getTxStatus());
//				return Responses.ok(resp);
//			}
//
//			String payType = payNotify.getOrderId().split("-")[0];
//			String orderId = payNotify.getOrderId();
//
//			String referenceId = payNotify.getReferenceId();
//			String orderAmount = payNotify.getOrderAmount();
//
//			switch (payType) {
//			case CommonConstants.PAY_TYPE_DEPOSIT:
//				memberService.completeDepositPay(orderId, referenceId, new BigDecimal(orderAmount));
//				break;
//			case CommonConstants.PAY_TYPE_VIP:
//				memberService.completeVIPPay(orderId, referenceId, new BigDecimal(orderAmount));
//				break;
//			case CommonConstants.PAY_TYPE_BALANCE:
//				moneyService.rechargeMoney(orderId, referenceId, new BigDecimal(orderAmount));
//				break;
//			case CommonConstants.PAY_TYPE_BASIC_COST:
//				memberService.completeBasicCostPay(orderId, referenceId, new BigDecimal(orderAmount));
//				break;
//			case CommonConstants.PAY_TYPE_PB_BUY:
//				memberService.completePbPay(orderId, referenceId, new BigDecimal(orderAmount));
//				break;
//			default:
//				logger.info("无效类型:[{}]", payType);
//				break;
//			}
//			logger.info("订单处理完成:[{}]", vo.getOrderId());
			return Responses.ok(resp);
//		} catch (PayException e) {
//			logger.info("完成支付异常:[{}]", e.getMessage());
//			return Responses.ok(resp);
//		}catch (ServiceException e) {
//			logger.info("完成支付异常:[{}]", e.getMessage());
//			return Responses.ok(resp);
//		}
	}
	
}