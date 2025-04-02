package com.sky.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sky.properties.WeChatProperties;
import com.wechat.pay.contrib.apache.httpclient.WechatPayHttpClientBuilder;
import com.wechat.pay.contrib.apache.httpclient.util.PemUtil;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigDecimal;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.ArrayList;
import java.util.Base64;

/**
 * 微信支付工具类
 */
@Component
public class WeChatPayUtil {

    // 微信支付下单接口地址
    public static final String JSAPI = "https://api.mch.weixin.qq.com/v3/pay/transactions/jsapi";

    // 申请退款接口地址
    public static final String REFUNDS = "https://api.mch.weixin.qq.com/v3/refund/domestic/refunds";

    @Autowired
    private WeChatProperties weChatProperties;

    /**
     * 获取调用微信接口的客户端工具对象
     *
     * @return CloseableHttpClient
     */
    private CloseableHttpClient getClient() {
        try {
            // 加载商户私钥
            PrivateKey merchantPrivateKey = PemUtil.loadPrivateKey(
                    new FileInputStream(new File(weChatProperties.getPrivateKeyFilePath()))
            );

            // 构建 HttpClient
            WechatPayHttpClientBuilder builder = WechatPayHttpClientBuilder.create()
                    .withMerchant(weChatProperties.getMchid(), weChatProperties.getMchSerialNo(), merchantPrivateKey);

            return builder.build();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize WeChat Pay client", e);
        }
    }

    /**
     * 发送 POST 请求
     *
     * @param url  接口地址
     * @param body 请求体
     * @return 响应结果
     */
    private String post(String url, String body) throws Exception {
        try (CloseableHttpClient httpClient = getClient()) {
            HttpPost httpPost = new HttpPost(url);
            httpPost.addHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.toString());
            httpPost.addHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString());
            httpPost.setEntity(new StringEntity(body, "UTF-8"));

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                return EntityUtils.toString(response.getEntity());
            }
        }
    }

    /**
     * jsapi 下单
     *
     * @param orderNum    商户订单号
     * @param total       总金额（单位：元）
     * @param description 商品描述
     * @param openid      用户 openid
     * @return 微信支付返回的结果
     */
    private String jsapi(String orderNum, BigDecimal total, String description, String openid) throws Exception {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("appid", weChatProperties.getAppid());
        jsonObject.put("mchid", weChatProperties.getMchid());
        jsonObject.put("description", description);
        jsonObject.put("out_trade_no", orderNum);
        jsonObject.put("notify_url", weChatProperties.getNotifyUrl());

        JSONObject amount = new JSONObject();
        amount.put("total", total.multiply(new BigDecimal(100)).setScale(0, BigDecimal.ROUND_HALF_UP).intValue());
        amount.put("currency", "CNY");

        jsonObject.put("amount", amount);

        JSONObject payer = new JSONObject();
        payer.put("openid", openid);

        jsonObject.put("payer", payer);

        return post(JSAPI, jsonObject.toJSONString());
    }

    /**
     * 小程序支付
     *
     * @param orderNum    商户订单号
     * @param total       金额（单位：元）
     * @param description 商品描述
     * @param openid      用户 openid
     * @return 支付所需数据
     */
    public JSONObject pay(String orderNum, BigDecimal total, String description, String openid) throws Exception {
        // 统一下单
        String result = jsapi(orderNum, total, description, openid);
        JSONObject response = JSON.parseObject(result);

        // 检查是否成功获取 prepay_id
        String prepayId = response.getString("prepay_id");
        if (prepayId == null) {
            throw new RuntimeException("Failed to get prepay_id: " + response);
        }

        // 构造支付签名
        String timeStamp = String.valueOf(System.currentTimeMillis() / 1000);
        String nonceStr = RandomStringUtils.randomAlphanumeric(32);
        ArrayList<String> list = new ArrayList<>();
        list.add(weChatProperties.getAppid());
        list.add(timeStamp);
        list.add(nonceStr);
        list.add("prepay_id=" + prepayId);

        StringBuilder signMessage = new StringBuilder();
        for (String s : list) {
            signMessage.append(s).append("\n");
        }

        Signature signature = Signature.getInstance("SHA256withRSA");
        PrivateKey privateKey = PemUtil.loadPrivateKey(
                new FileInputStream(new File(weChatProperties.getPrivateKeyFilePath()))
        );
        signature.initSign(privateKey);
        signature.update(signMessage.toString().getBytes());
        String paySign = Base64.getEncoder().encodeToString(signature.sign());

        // 返回小程序支付所需的参数
        JSONObject resultJson = new JSONObject();
        resultJson.put("timeStamp", timeStamp);
        resultJson.put("nonceStr", nonceStr);
        resultJson.put("package", "prepay_id=" + prepayId);
        resultJson.put("signType", "RSA");
        resultJson.put("paySign", paySign);

        return resultJson;
    }

    /**
     * 申请退款
     *
     * @param outTradeNo   商户订单号
     * @param outRefundNo  商户退款单号
     * @param refund       退款金额（单位：元）
     * @param total        原订单金额（单位：元）
     * @return 退款结果
     */
    public String refund(String outTradeNo, String outRefundNo, BigDecimal refund, BigDecimal total) throws Exception {
        JSONObject requestBody = new JSONObject();
        requestBody.put("out_trade_no", outTradeNo);
        requestBody.put("out_refund_no", outRefundNo);

        JSONObject amount = new JSONObject();
        amount.put("refund", refund.multiply(new BigDecimal(100)).setScale(0, BigDecimal.ROUND_HALF_UP).intValue());
        amount.put("total", total.multiply(new BigDecimal(100)).setScale(0, BigDecimal.ROUND_HALF_UP).intValue());
        amount.put("currency", "CNY");

        requestBody.put("amount", amount);
        requestBody.put("notify_url", weChatProperties.getRefundNotifyUrl());

        return post(REFUNDS, requestBody.toJSONString());
    }
}