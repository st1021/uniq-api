package vc.thinker.apiv2.bo;


import java.util.Date;

import io.swagger.annotations.ApiModelProperty;
import vc.thinker.cabbage.se.model.Feedback;
/**
 * 
 * BO 用于返回数据
 *
 */
public class APIFeedbackBO extends Feedback{

	@ApiModelProperty("订单编号")
    private String orderCode;

    @ApiModelProperty("类型 1：首页，2：行程中，3：已完成 ")
    private String feedType;

    @ApiModelProperty("问题表述")
    private String feedDesc;

    /** 用户上送的照片 **/
    private String imgUrl1;

    /** 用户上送的照片 **/
    private String imgUrl2;

    /** 用户上送的照片 **/
    private String imgUrl3;

    /** 用户上送的照片 **/
    private String imgUrl4;

    @ApiModelProperty("0:已处理，1：未处理，2：无需处理")
    private Integer status;

    /** 创建时间 **/
    private Date createTime;

    /** 修改时间 **/
    private Date updateTime;

    /** 修改人 **/
    private String updateBy;

    /** 备注 **/
    private String remark;

    /** 处理方式 **/
    private String dealType;

    /** 票券id **/
    private Long ticketId;

    /** 问题类型id **/
    private Long typeId;

    @ApiModelProperty(" 充电柜编码")
    private String cabinetCode;

    @ApiModelProperty("充电宝编码")
    private String pbCode;

    /** 系统编号 **/
    private String sysCode;

    /** 客户端源 **/
    private String clientSource;

	@Override
	public String getOrderCode() {
		return orderCode;
	}

	@Override
	public void setOrderCode(String orderCode) {
		this.orderCode = orderCode;
	}

	@Override
	public String getFeedType() {
		return feedType;
	}

	@Override
	public void setFeedType(String feedType) {
		this.feedType = feedType;
	}

	@Override
	public String getFeedDesc() {
		return feedDesc;
	}

	@Override
	public void setFeedDesc(String feedDesc) {
		this.feedDesc = feedDesc;
	}

	@Override
	public String getImgUrl1() {
		return imgUrl1;
	}

	@Override
	public void setImgUrl1(String imgUrl1) {
		this.imgUrl1 = imgUrl1;
	}

	@Override
	public String getImgUrl2() {
		return imgUrl2;
	}

	@Override
	public void setImgUrl2(String imgUrl2) {
		this.imgUrl2 = imgUrl2;
	}

	@Override
	public String getImgUrl3() {
		return imgUrl3;
	}

	@Override
	public void setImgUrl3(String imgUrl3) {
		this.imgUrl3 = imgUrl3;
	}

	@Override
	public String getImgUrl4() {
		return imgUrl4;
	}

	@Override
	public void setImgUrl4(String imgUrl4) {
		this.imgUrl4 = imgUrl4;
	}

	@Override
	public Integer getStatus() {
		return status;
	}

	@Override
	public void setStatus(Integer status) {
		this.status = status;
	}

	@Override
	public Date getCreateTime() {
		return createTime;
	}

	@Override
	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

	@Override
	public Date getUpdateTime() {
		return updateTime;
	}

	@Override
	public void setUpdateTime(Date updateTime) {
		this.updateTime = updateTime;
	}

	@Override
	public String getUpdateBy() {
		return updateBy;
	}

	@Override
	public void setUpdateBy(String updateBy) {
		this.updateBy = updateBy;
	}

	@Override
	public String getRemark() {
		return remark;
	}

	@Override
	public void setRemark(String remark) {
		this.remark = remark;
	}

	@Override
	public String getDealType() {
		return dealType;
	}

	@Override
	public void setDealType(String dealType) {
		this.dealType = dealType;
	}

	@Override
	public Long getTicketId() {
		return ticketId;
	}

	@Override
	public void setTicketId(Long ticketId) {
		this.ticketId = ticketId;
	}

	@Override
	public Long getTypeId() {
		return typeId;
	}

	@Override
	public void setTypeId(Long typeId) {
		this.typeId = typeId;
	}

	@Override
	public String getCabinetCode() {
		return cabinetCode;
	}

	@Override
	public void setCabinetCode(String cabinetCode) {
		this.cabinetCode = cabinetCode;
	}

	@Override
	public String getPbCode() {
		return pbCode;
	}

	@Override
	public void setPbCode(String pbCode) {
		this.pbCode = pbCode;
	}

	@Override
	public String getSysCode() {
		return sysCode;
	}

	@Override
	public void setSysCode(String sysCode) {
		this.sysCode = sysCode;
	}

	@Override
	public String getClientSource() {
		return clientSource;
	}

	@Override
	public void setClientSource(String clientSource) {
		this.clientSource = clientSource;
	}
}