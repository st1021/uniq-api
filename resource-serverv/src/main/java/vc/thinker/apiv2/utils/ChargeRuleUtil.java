package vc.thinker.apiv2.utils;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Locale;

public class ChargeRuleUtil {
	/**
	 * 秒格式化输出
	 * @param second
	 * @return
	 */
	public static String formatDateMinute(Integer minute,Locale locale) {
		//设置12小时一天
       long day1=minute/(24*60);  
       long hour1=minute%(24*60)/60;  
       long minute1=minute%60;
       StringBuilder result= new StringBuilder("");
       if(day1 > 0){
    	   if(day1>1){
    		   result.append(day1);
    	   }
    	   
    	   if(locale.getLanguage().startsWith("zh")) {
    		   result.append("天");   
    	   }else {
    		   result.append("Day");   
    	   }
    	   
       }
       if(hour1 > 0){
    	   result.append(hour1);
    	   if(locale.getLanguage().startsWith("zh")) {
    		   result.append("小时");   
    	   }else {
    		   result.append("h");   
    	   }
    	 
       }
       if(minute1 > 0){
    	   result.append(minute1);
    	   if(locale.getLanguage().startsWith("zh")) {
    		   result.append("分钟");   
    	   }else {
    		   result.append("min");   
    	   }
       }
       return result.toString();
	}
	
	
	/**
	 * 价格格式化
	 * @param price
	 * @return
	 */
	public static String formatPrice(String currency,BigDecimal price){
		DecimalFormat df2 = new DecimalFormat("#.##"); 
		return currency+" "+df2.format(price.doubleValue());
	}
}
