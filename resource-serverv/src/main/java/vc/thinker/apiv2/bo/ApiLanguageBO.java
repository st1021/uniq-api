package vc.thinker.apiv2.bo;

import java.util.Date;

import io.swagger.annotations.ApiModelProperty;

/**
 *
 * @author ZhangGaoXiang
 * @time Jan 7, 20205:47:56 PM
 */
public class ApiLanguageBO {

	 /** 英文名称 **/
	@ApiModelProperty("语言名称")
    private String languageName;

    /** 默认语言 **/
	@ApiModelProperty("语言")
    private String language;

    /** 语言描述 **/
	@ApiModelProperty("语言描述")
    private String languageDesc;

    /** 创建时间 **/
    private Date createTime;

	public String getLanguageName() {
		return languageName;
	}

	public void setLanguageName(String languageName) {
		this.languageName = languageName;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public String getLanguageDesc() {
		return languageDesc;
	}

	public void setLanguageDesc(String languageDesc) {
		this.languageDesc = languageDesc;
	}

	public Date getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}
    
}
