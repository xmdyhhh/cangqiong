//package com.sky.properties;
//
//import lombok.Data;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.boot.context.properties.ConfigurationProperties;
//import org.springframework.stereotype.Component;
//
//@Component
//@ConfigurationProperties(prefix = "sky.wechat")
//@Data
//public class WeChatProperties {
//
//    private String appid; //小程序的appid
//    private String secret; //小程序的秘钥
//    private String mchid; //商户号
//    private String mchSerialNo; //商户API证书的证书序列号
//    private String privateKeyFilePath; //商户私钥文件
//    private String apiV3Key; //证书解密的密钥
//    private String weChatPayCertFilePath; //平台证书
//    private String notifyUrl; //支付成功的回调地址
//    private String refundNotifyUrl; //退款成功的回调地址
//
//}
package com.sky.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "wechat")
public class WeChatProperties {

    private String privateKeyFilePath;
    private String weChatPayCertFilePath;
    private String mchid;
    private String mchSerialNo;
    private String appid;
    private String notifyUrl;
    private String refundNotifyUrl;

    // Getters and Setters

    public String getPrivateKeyFilePath() {
        return privateKeyFilePath;
    }

    public void setPrivateKeyFilePath(String privateKeyFilePath) {
        this.privateKeyFilePath = privateKeyFilePath;
    }

    public String getWeChatPayCertFilePath() {
        return weChatPayCertFilePath;
    }

    public void setWeChatPayCertFilePath(String weChatPayCertFilePath) {
        this.weChatPayCertFilePath = weChatPayCertFilePath;
    }

    public String getMchid() {
        return mchid;
    }

    public void setMchid(String mchid) {
        this.mchid = mchid;
    }

    public String getMchSerialNo() {
        return mchSerialNo;
    }

    public void setMchSerialNo(String mchSerialNo) {
        this.mchSerialNo = mchSerialNo;
    }

    public String getAppid() {
        return appid;
    }

    public void setAppid(String appid) {
        this.appid = appid;
    }

    public String getNotifyUrl() {
        return notifyUrl;
    }

    public void setNotifyUrl(String notifyUrl) {
        this.notifyUrl = notifyUrl;
    }

    public String getRefundNotifyUrl() {
        return refundNotifyUrl;
    }

    public void setRefundNotifyUrl(String refundNotifyUrl) {
        this.refundNotifyUrl = refundNotifyUrl;
    }
}
