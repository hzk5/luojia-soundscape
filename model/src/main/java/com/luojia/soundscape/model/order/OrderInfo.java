package com.luojia.soundscape.model.order;

import com.luojia.soundscape.model.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "OrderInfo")
@TableName("order_info")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderInfo extends BaseEntity {

	private static final long serialVersionUID = 1L;

	@Schema(description = "用户ID")
	@TableField("user_id")
	private Long userId;

	@Schema(description = "订单标题")
	@TableField("order_title")
	private String orderTitle;

	@Schema(description = "订单号")
	@TableField("order_no")
	private String orderNo;

	@Schema(description = "订单状态：0901-未支付 0902-已支付 0903-已取消")
	@TableField("order_status")
	private String orderStatus;

	@Schema(description = "订单原始金额")
	@TableField("original_amount")
	private BigDecimal originalAmount;

	@Schema(description = "减免总金额")
	@TableField("derate_amount")
	private BigDecimal derateAmount;

	@Schema(description = "订单总价")
	@TableField("order_amount")
	private BigDecimal orderAmount;

	@Schema(description = "付款项目类型: 1001-专辑 1002-声音 1003-vip会员")
	@TableField("item_type")
	private String itemType;

	@Schema(description = "支付方式：1101-微信 1102-支付宝 1103-账户余额")
	@TableField("pay_way")
	private String payWay;


	@Schema(description = "订单明细列表")
	@TableField(exist = false)
	private List<OrderDetail> orderDetailList;

	@Schema(description = "订单减免明细列表")
	@TableField(exist = false)
	private List<OrderDerate> orderDerateList;

	//json框架转换 通过属性get方法，去掉get首字母小写，获取属性名称  get方法返回值就是属性值
	public String getOrderStatusName() {
		if("0901".equals(orderStatus)){
			return "未支付";
		} else if ("0902".equals(orderStatus)) {
			return "已支付";
		} else if ("0903".equals(orderStatus)) {
			return "已取消";
		}
		return null;
	}

	public String getPayWayName() {
		if ("1101".equals(payWay)) {
			return "微信";
		} else if ("1102".equals(payWay)) {
			return "支付宝";
		} else if ("1103".equals(payWay)) {
			return "余额";
		}
		return "";
	}
}
