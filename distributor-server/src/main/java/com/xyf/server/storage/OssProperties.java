package com.xyf.server.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * OSS 配置属性
 */
@Component
@ConfigurationProperties(prefix = "oss")
public class OssProperties {

    private String endpoint;
    private String internalEndpoint;
    private String bucket;
    private String region = "ap-southeast-1";
    private String accessKeyId;
    private String accessKeySecret;

    /** STS 配置 */
    private String stsEndpoint = "sts.ap-southeast-1.aliyuncs.com";
    private String stsRoleArn;
    private int stsDurationSeconds = 3600;

    /** 上传前缀 */
    private String uploadPrefix = "videos/";

    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }

    public String getInternalEndpoint() { return internalEndpoint; }
    public void setInternalEndpoint(String internalEndpoint) { this.internalEndpoint = internalEndpoint; }

    public String getBucket() { return bucket; }
    public void setBucket(String bucket) { this.bucket = bucket; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getAccessKeyId() { return accessKeyId; }
    public void setAccessKeyId(String accessKeyId) { this.accessKeyId = accessKeyId; }

    public String getAccessKeySecret() { return accessKeySecret; }
    public void setAccessKeySecret(String accessKeySecret) { this.accessKeySecret = accessKeySecret; }

    public String getStsEndpoint() { return stsEndpoint; }
    public void setStsEndpoint(String stsEndpoint) { this.stsEndpoint = stsEndpoint; }

    public String getStsRoleArn() { return stsRoleArn; }
    public void setStsRoleArn(String stsRoleArn) { this.stsRoleArn = stsRoleArn; }

    public int getStsDurationSeconds() { return stsDurationSeconds; }
    public void setStsDurationSeconds(int stsDurationSeconds) { this.stsDurationSeconds = stsDurationSeconds; }

    public String getUploadPrefix() { return uploadPrefix; }
    public void setUploadPrefix(String uploadPrefix) { this.uploadPrefix = uploadPrefix; }
}
