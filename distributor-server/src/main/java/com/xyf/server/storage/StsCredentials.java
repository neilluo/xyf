package com.xyf.server.storage;

/**
 * STS 临时凭证 DTO
 */
public class StsCredentials {

    private String accessKeyId;
    private String accessKeySecret;
    private String securityToken;
    private String expiration;
    private String bucket;
    private String region;
    private String endpoint;
    private String uploadPrefix;

    public StsCredentials() {}

    public StsCredentials(String accessKeyId, String accessKeySecret, String securityToken,
                          String expiration, String bucket, String region, String endpoint, String uploadPrefix) {
        this.accessKeyId = accessKeyId;
        this.accessKeySecret = accessKeySecret;
        this.securityToken = securityToken;
        this.expiration = expiration;
        this.bucket = bucket;
        this.region = region;
        this.endpoint = endpoint;
        this.uploadPrefix = uploadPrefix;
    }

    public String getAccessKeyId() { return accessKeyId; }
    public void setAccessKeyId(String accessKeyId) { this.accessKeyId = accessKeyId; }

    public String getAccessKeySecret() { return accessKeySecret; }
    public void setAccessKeySecret(String accessKeySecret) { this.accessKeySecret = accessKeySecret; }

    public String getSecurityToken() { return securityToken; }
    public void setSecurityToken(String securityToken) { this.securityToken = securityToken; }

    public String getExpiration() { return expiration; }
    public void setExpiration(String expiration) { this.expiration = expiration; }

    public String getBucket() { return bucket; }
    public void setBucket(String bucket) { this.bucket = bucket; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }

    public String getUploadPrefix() { return uploadPrefix; }
    public void setUploadPrefix(String uploadPrefix) { this.uploadPrefix = uploadPrefix; }
}
