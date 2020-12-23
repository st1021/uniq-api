package vc.thinker.oauth;

import java.util.Random;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sinco.common.utils.IPUtil;
import com.sinco.messager.sms.TencentYunMessage;
import com.sinco.messager.sms.TencentYunMessageHandler;
import com.sinco.messager.sms.turbo.TurboMessage;
import com.sinco.messager.sms.turbo.TurboMessageHandler;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import vc.thinker.cabbage.common.SmsHandlerType;
import vc.thinker.cabbage.user.CommonConstants;
import vc.thinker.sys.utils.MsgTools;

public class VerifyCodeUtil {

	public static String MOBILE_VERFIY_CODE_KEY = "MOBILE_VERFIY_CODE_";

	protected Logger logger = LoggerFactory.getLogger(getClass());

	private Boolean isTest = false;

	public VerifyCodeUtil() {
		isTest = false;
	}

	public VerifyCodeUtil(Boolean isTest) {
		this.isTest = isTest;
	}

	public boolean sendCode(HttpServletRequest request,MsgTools msgTools,JedisPool pool, String countryCode,
			String mobile,String smsType, String signature) {
		
		Jedis jedis = null;
		try {
			jedis = pool.getResource();
			
			String code = getSixString();
			
			if(StringUtils.isNotBlank(smsType)) {
				switch (smsType) {
				case SmsHandlerType.TYEP_TRUBO:
					TurboMessageHandler smsHandler = (TurboMessageHandler) msgTools.getSmsHandler();
					TurboMessage message = new TurboMessage();
					message.setSignature(signature);
					message.setMessage("Код доступа:" + code);
					smsHandler.sendMessage(countryCode+mobile, message);
					break;
				case SmsHandlerType.TYEP_TENCENT:
					logger.info("手机号:[{}]", countryCode+mobile);
					logger.info("验证码:[{}]", code);
					TencentYunMessageHandler handler = (TencentYunMessageHandler) msgTools.getSmsHandler();
					String [] params = new String [] {code};
					TencentYunMessage yunMessage = new TencentYunMessage(signature, 521406, countryCode, params);
					handler.sendMessage(mobile, yunMessage);
					break;
				default:
					break;
				}
			}else {
				msgTools.sendTemplateSms(CommonConstants.SMS_VALIDATE_CODE, mobile,
						IPUtil.getIpAddr(request), "{code:\""+code+"\"}",signature);
			}

			jedis.set(MOBILE_VERFIY_CODE_KEY + countryCode+mobile, String.valueOf(code));
			jedis.expire(MOBILE_VERFIY_CODE_KEY + countryCode+mobile, 60 * 5);
		} finally {
			if (jedis != null)
				jedis.close();
		}
		return true;
	}

	public String getSixString() {
		Random rNo = new Random();
		int code = rNo.nextInt((999999 - 100000) + 1) + 100000;
		if (isTest) {
			code = 123456;
		}
		return String.valueOf(code);
	}

	public boolean verifyCode(JedisPool pool, String phonenumber, String idcode) {
		Jedis jedis = null;
		try {
			jedis = pool.getResource();
			String key = MOBILE_VERFIY_CODE_KEY + phonenumber;
			String code = jedis.get(key);
			logger.info("phonenumber:[{}],idcode:[{}], Code:[{}]", phonenumber, idcode, code);
			return idcode.equals(code);
		} finally {
			if (jedis != null)
				jedis.close();
		}
	}
	
	public String getVerifyCode(JedisPool pool, String phonenumber) {
		Jedis jedis = null;
		try {
			jedis = pool.getResource();
			String key = MOBILE_VERFIY_CODE_KEY + phonenumber;
			String code = jedis.get(key);
			return code;
		} finally {
			if (jedis != null)
				jedis.close();
		}
	}

	public String getCode(JedisPool pool, String phonenumber) {
		Jedis jedis = null;
		try {
			jedis = pool.getResource();
			String key = MOBILE_VERFIY_CODE_KEY + phonenumber;
			return jedis.get(key);
		} finally {
			if (jedis != null)
				jedis.close();
		}
	}

	public void delCode(JedisPool pool, String phonenumber) {
		Jedis jedis = null;
		try {
			jedis = pool.getResource();
			String key = MOBILE_VERFIY_CODE_KEY + phonenumber;
			jedis.del(key);
		} finally {
			if (jedis != null)
				jedis.close();
		}
	}
}
