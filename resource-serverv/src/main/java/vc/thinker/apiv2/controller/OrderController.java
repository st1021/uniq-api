package vc.thinker.apiv2.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import javax.servlet.http.HttpServletRequest;
import org.dozer.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.RequestContextUtils;
import com.sinco.data.core.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import vc.thinker.apiv2.bo.APIFeedbackBO;
import vc.thinker.apiv2.bo.APIOrderBO;
import vc.thinker.apiv2.bo.APIUserCouponBO;
import vc.thinker.apiv2.bo.CashfreePaymentBO;
import vc.thinker.apiv2.bo.FondyPaymetBO;
import vc.thinker.apiv2.bo.PayDetailsBO;
import vc.thinker.apiv2.http.response.ListResponse;
import vc.thinker.apiv2.http.response.PageResponse;
import vc.thinker.apiv2.http.response.SingleResponse;
import vc.thinker.apiv2.utils.ChargeRuleUtil;
import vc.thinker.apiv2.utils.MapperUtils;
import vc.thinker.apiv2.utils.WeixinPayUtil;
import vc.thinker.apiv2.web.BaseController;
import vc.thinker.apiv2.web.Responses;
import vc.thinker.apiv2.web.UserAuthentication;
import vc.thinker.cabbage.common.PaymentConstants;
import vc.thinker.cabbage.common.SysQrCodeUtil;
import vc.thinker.cabbage.money.exception.BalanceNotEnoughException;
import vc.thinker.cabbage.se.CabinetChargeRuleService;
import vc.thinker.cabbage.se.CabinetService;
import vc.thinker.cabbage.se.FeedbackService;
import vc.thinker.cabbage.se.OrderService;
import vc.thinker.cabbage.se.bo.CabinetBO;
import vc.thinker.cabbage.se.bo.CabinetChargeRuleBO;
import vc.thinker.cabbage.se.bo.FeedbackBO;
import vc.thinker.cabbage.se.bo.OrderBO;
import vc.thinker.cabbage.se.exception.BatteryIsNotEnoughExceptiion;
import vc.thinker.cabbage.se.exception.CabinetIsOfflineException;
import vc.thinker.cabbage.se.exception.CabinetNotFindException;
import vc.thinker.cabbage.se.exception.CabinetStatusNotFindException;
import vc.thinker.cabbage.se.exception.OrderHavePaidException;
import vc.thinker.cabbage.se.exception.OrderNotFindException;
import vc.thinker.cabbage.se.exception.UnfinishedOrdersExceptiion;
import vc.thinker.cabbage.se.model.Order;
import vc.thinker.cabbage.user.bo.UserCouponBO;
import vc.thinker.cabbage.user.exception.DepositException;
import vc.thinker.cabbage.user.exception.SellerLockedException;
import vc.thinker.cabbage.user.service.UserCouponService;
import vc.thinker.oauth.AuthContants;
import vc.thinker.pay.cashfree.CashfreeConfig;
import vc.thinker.pay.response.DirectPayResponse;
import vc.thinker.pay.weixin.WeixinConfig;

@RestController
@RequestMapping(value = "/api/order/", produces = { APPLICATION_JSON_VALUE })
@Api(value = "个人用户订单", description = "个人用户订单")
public class OrderController extends BaseController {
	protected Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private OrderService orderService;

	@Autowired
	private Mapper mapper;

	@Autowired
	private UserCouponService userCouponService;

	@Autowired
	private FeedbackService feedbackService;

	@Autowired
	private CabinetService cabinetService;

	@Autowired
	private CabinetChargeRuleService cabinetChargeRuleService;

	@Autowired
	private MessageSource messageSource;

	@Value("${pay.callback}")
	private String payCallback;

	@Value("${lbs.baidu.ak}")
	private String baiduAK;

	
	// 支付
	@RequestMapping(value = "/paymet", method = { RequestMethod.POST })
	@PreAuthorize("hasRole('" + AuthContants.ROLE_END_USER + "')")
	@ApiOperation(value = "支付")
	@ApiResponses(value = { @ApiResponse(code = 400, message = "参数错误"), @ApiResponse(code = 406, message = "业务操作错误"),
			@ApiResponse(code = 408, message = "订单已完成支付"), @ApiResponse(code = 409, message = "余额不足") })
	public ResponseEntity<SingleResponse<PayDetailsBO>> paymet(
			HttpServletRequest request,
			@UserAuthentication User user,
			@RequestHeader(name="Accept-Language",required=true) String language,
			@ApiParam(value = "订单编号", required = true) @RequestParam(value = "orderCode", required = true) String orderCode,
			@ApiParam(value = "优惠卷Id", required = false) @RequestParam(value = "cid", required = false) Long cid,
			@ApiParam(value = "支付方式  alipay_app,wx_app,stripe,balance", required = false) @RequestParam(value = "paymentMark", required = false) String paymentMark) {

		logger.info("paymet language:[{}]", language);
		SingleResponse<PayDetailsBO> resp = new SingleResponse<>(messageSource, request);
		PayDetailsBO payDetails = new PayDetailsBO();

		try {
			DirectPayResponse payResponse = orderService.pay(orderCode, cid, payCallback, paymentMark);

			if (null != payResponse && null != payResponse.getChannel()) {

				switch (payResponse.getChannel()) {

				case ALIPAY:
					payDetails.setAlipaPpaySignature(payResponse.getContent());
					break;
				case WEIXIN:
					if (PaymentConstants.PAYMENT_MARK_WX_JS_PAY.equals(paymentMark)) {

						payDetails.setWeiXinPaymet(WeixinPayUtil.makeWeixinJsPaymet(
								(WeixinConfig) payResponse.getPayConfig(), payResponse.getContent()));
					} else {

						payDetails.setWeiXinPaymet(WeixinPayUtil.makeWeixinAppPaymet(
								(WeixinConfig) payResponse.getPayConfig(), payResponse.getContent()));
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
				default:
					break;
				}

				resp.setItem(payDetails);
			}
			resp.setSuccess(true);

		} catch (OrderNotFindException e) {
			resp.setErrorInfo("406", "order.notFind", e.getMessage());
			return Responses.ok(resp);
		} catch (OrderHavePaidException e) {
			resp.setErrorInfo("408", "order.havePaid", e.getMessage());
			return Responses.ok(resp);
		} catch (BalanceNotEnoughException e) {
			resp.setErrorInfo("409", "member.balance.NotEnough", e.getMessage());
			return Responses.ok(resp);
		}
		return Responses.ok(resp);
	}

	// 我的订单
	@RequestMapping(value = "/list", method = { RequestMethod.POST })
	@PreAuthorize("hasRole('" + AuthContants.ROLE_END_USER + "')")
	@ApiOperation(value = "订单列表")
	public ResponseEntity<PageResponse<APIOrderBO>> findOrderList(HttpServletRequest request,
			@ApiParam(value = "结束时间的排序", required = false) @RequestParam(value = "ltTime", required = false) Long ltTime,
			@UserAuthentication User user) {

		PageResponse<APIOrderBO> resp = new PageResponse<APIOrderBO>();
		Long uid = Long.parseLong(user.getUsername());
		resp.setSuccess(true);

		Page<OrderBO> page = new Page<>();
		page.setPageNumber(1);
		page.setPageSize(15);
		Date ltFinishTime = null;
		if (ltTime != null) {
			ltFinishTime = new Date(ltTime);
		}
		List<OrderBO> list = orderService.findPageListByUid(page, uid, ltFinishTime);

		resp.init(page, MapperUtils.map(mapper, list, APIOrderBO.class));
		return Responses.ok(resp);
	}

	@RequestMapping(value = "/create", method = { RequestMethod.POST })
	@PreAuthorize("hasRole('" + AuthContants.ROLE_END_USER + "')")
	@ApiOperation(value = "创建订单")
	@ApiResponses(value = { @ApiResponse(code = 406, message = "业务操作错误"),
			@ApiResponse(code = 407, message = "用户有未结束订单"), @ApiResponse(code = 409, message = "用户还未交押金") })
	public ResponseEntity<SingleResponse<APIOrderBO>> createOrder(HttpServletRequest request,
			@UserAuthentication User user, @RequestHeader(name = "api-agent", required = false) String apiAgent,
			@ApiParam(value = "二维码", required = true) @RequestParam("qrCode") String qrCode,
			@ApiParam(value = "电池类型", required = true) @RequestParam("batteryType") String batteryType) {
		Long uid = Long.parseLong(user.getUsername());
		SingleResponse<APIOrderBO> resp = new SingleResponse<>(messageSource, request);
		resp.setSuccess(true);

		String sysCode = SysQrCodeUtil.getSysCode(qrCode);

		// 创建行程
		try {
			Order order = orderService.createOrder(uid, sysCode, batteryType, apiAgent);
			resp.setItem(mapper.map(order, APIOrderBO.class));
		} catch (CabinetNotFindException e) {
			resp.setErrorInfo("406", "order.cabinetNotFind", e.getMessage());
			return Responses.ok(resp);
		} catch (SellerLockedException e) {
			resp.setErrorInfo("406", "seller.locked", e.getMessage());
			return Responses.ok(resp);
		} catch (UnfinishedOrdersExceptiion e) {
			resp.setErrorInfo("407", "order.unfinished", e.getMessage());
			return Responses.ok(resp);
		} catch (CabinetStatusNotFindException e) {
			resp.setErrorInfo("406", "order.cabinetStatusNotFind", e.getMessage());
			return Responses.ok(resp);
		} catch (BatteryIsNotEnoughExceptiion e) {
			resp.setErrorInfo("406", "battery.isNotEnough", e.getMessage());
			return Responses.ok(resp);
		} catch (DepositException e) {
			resp.setErrorInfo("409", "member.notDeposit", e.getMessage());
			return Responses.ok(resp);
		} catch (CabinetIsOfflineException e) {
			resp.setErrorInfo("406", "cabinet.notOnline", e.getMessage());
			return Responses.ok(resp);
		}
		return Responses.ok(resp);
	}

	@RequestMapping(value = "/doing_info", method = { RequestMethod.GET })
	@PreAuthorize("hasRole('" + AuthContants.ROLE_END_USER + "')")
	@ApiOperation(value = "进行中订单信息")
	public ResponseEntity<SingleResponse<APIOrderBO>> findDoingInfo(
			HttpServletRequest request,
			@RequestHeader(name="Accept-Language",required=true) String language,
			@UserAuthentication User user) {
		
		SingleResponse<APIOrderBO> resp = new SingleResponse<>();
		Long uid = Long.parseLong(user.getUsername());
		List<OrderBO> list = orderService.findUserNotEndOrder(uid);

		if (!list.isEmpty()) {

			OrderBO order = list.get(0);

			APIOrderBO item = mapper.map(order, APIOrderBO.class);

			if (item != null) {
				List<FeedbackBO> feedbacks = feedbackService.findDoingByOrderCode(item.getOrderCode());
				item.setDoingFeedbacks(MapperUtils.map(mapper, feedbacks, APIFeedbackBO.class));

				CabinetBO cabinet = cabinetService.findOne(order.getBorrowCabinetId());
				CabinetChargeRuleBO cabinetChargeRule = cabinetChargeRuleService.findOne(cabinet.getRuleId());
				Locale locale = RequestContextUtils.getLocale(request);
				String freeUseTime = ChargeRuleUtil.formatDateMinute(cabinetChargeRule.getFreeUseTime(), locale);
				String unitMinute = ChargeRuleUtil.formatDateMinute(cabinetChargeRule.getUnitMinute(), locale);
				String unitPrice = ChargeRuleUtil.formatPrice(cabinetChargeRule.getCurrency(),
						cabinetChargeRule.getUnitPrice());
				String chargeRuleDesc = "";
				if ("".equals(freeUseTime)) {
					chargeRuleDesc = messageSource.getMessage("cabinet.chargeRule.noFree",
							new Object[] { unitMinute, unitPrice }, locale);
				} else {
					chargeRuleDesc = messageSource.getMessage("cabinet.chargeRule",
							new Object[] { freeUseTime, unitMinute, unitPrice }, locale);
				}
				item.setChargeRuleDesc(chargeRuleDesc);
			}
			resp.setItem(item);
		}
		return Responses.ok(resp);
	}

	@RequestMapping(value = "/not_pay", method = { RequestMethod.GET })
	@PreAuthorize("hasRole('" + AuthContants.ROLE_END_USER + "')")
	@ApiOperation(value = "未支付的订单")
	public ResponseEntity<SingleResponse<APIOrderBO>> findNotPay(HttpServletRequest request,
			@UserAuthentication User user) {
		SingleResponse<APIOrderBO> resp = new SingleResponse<>();

		Long uid = Long.parseLong(user.getUsername());
		OrderBO order = orderService.findNotPay(uid);
		if (order != null) {
			resp.setItem(mapper.map(order, APIOrderBO.class));

			UserCouponBO coupon = userCouponService.findFitCoupon(uid, order.getPrice(), order.getCurrency(), null);
			if (coupon != null) {
				resp.getItem().setFitCoupon(mapper.map(coupon, APIUserCouponBO.class));
			}
//			vc.thinker.cabbage.user.bo.MemberBO member = memberService.findByUid(uid);

//			Locale locale = RequestContextUtils.getLocale(request);
			BigDecimal ratePrice = order.getPrice();
//			if (!member.getCurrency().equals(order.getCurrency())) {
//				ExchangeRateBO exchangeRate = exchangeRateService.findOne(order.getCurrency(), member.getCurrency());
//				String exchangeRateDesc = messageSource.getMessage("order.exchangeRateDesc", new Object[] {
//						order.getCurrency() + " 1.00", member.getCurrency() + " " + exchangeRate.getExchangeRate() },
//						locale);
//				resp.getItem().setExchangeRateDesc(exchangeRateDesc);
//				ratePrice = order.getPrice().multiply(exchangeRate.getExchangeRate()).setScale(2,
//						BigDecimal.ROUND_DOWN);
//			}
			resp.getItem().setRatePrice(ratePrice);
		}

		return Responses.ok(resp);
	}

	@RequestMapping(value = "/profile", method = { RequestMethod.GET })
	@PreAuthorize("hasRole('" + AuthContants.ROLE_END_USER + "')")
	@ApiOperation(value = "订单详情")
	public ResponseEntity<SingleResponse<APIOrderBO>> profile(
			@ApiParam(value = "订单编码", required = true) @RequestParam("orderCode") String orderCode,
			@UserAuthentication User user) {
		SingleResponse<APIOrderBO> resp = new SingleResponse<>();

//		Long uid=Long.parseLong(user.getUsername());
		OrderBO order = orderService.findDetailed(orderCode);

		if (order != null) {
			APIOrderBO item = mapper.map(order, APIOrderBO.class);
			resp.setItem(item);
		}

		return Responses.ok(resp);
	}

	// 我的优惠券
	@RequestMapping(value = "/coupon_list", method = { RequestMethod.POST })
	@PreAuthorize("hasRole('" + AuthContants.ROLE_END_USER + "')")
	@ApiOperation(value = "未支付订单优惠券列表")
	public ResponseEntity<ListResponse<APIUserCouponBO>> findCouponList(@UserAuthentication User user) {
		ListResponse<APIUserCouponBO> resp = new ListResponse<APIUserCouponBO>();
		Long uid = Long.parseLong(user.getUsername());
		OrderBO order = orderService.findNotPay(uid);
		if (order == null) {
			resp.setErrorInfo("406", "没有找到未支付订单");
			return Responses.ok(resp);
		}
		List<UserCouponBO> couponList = userCouponService.findByOrder(order);
		resp.setItems(MapperUtils.map(mapper, couponList, APIUserCouponBO.class));

		return Responses.ok(resp);
	}

	private CashfreePaymentBO makeCashfreePaymentBO(DirectPayResponse payResponse) {

		CashfreePaymentBO item = new CashfreePaymentBO();
		item.setToken(payResponse.getContent());
		item.setOrderCurrency(payResponse.getOrderCurrency());
		item.setOrderId(payResponse.getOrderId());
		item.setOrderAmount(payResponse.getOrderAmount());
		item.setNotifyUrl(payResponse.getNotifyUrl());
		item.setCustomerPhone(payResponse.getCustomerPhone());
		item.setCustomerEmail("anoopvootkuri95@gmail.com");
		CashfreeConfig payConfig = (CashfreeConfig) payResponse.getPayConfig();
		item.setAppId(payConfig.getAppId());
		return item;
	}
}
