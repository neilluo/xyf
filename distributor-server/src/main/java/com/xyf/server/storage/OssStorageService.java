package com.xyf.server.storage;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.OSSObject;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.auth.sts.AssumeRoleRequest;
import com.aliyuncs.auth.sts.AssumeRoleResponse;
import com.aliyuncs.profile.DefaultProfile;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.InputStream;

/**
 * OSS 存储服务
 * <p>
 * 职责：
 * 1. 生成 STS 临时凭证（给 CLI 上传用）
 * 2. 通过内网读取 OSS 对象（给服务端分发任务用）
 */
@Service
public class OssStorageService {

    private static final Logger log = LoggerFactory.getLogger(OssStorageService.class);

    private final OssProperties ossProperties;
    private OSS ossClient;

    public OssStorageService(OssProperties ossProperties) {
        this.ossProperties = ossProperties;
    }

    @PostConstruct
    public void init() {
        String accessKeyId = ossProperties.getAccessKeyId();
        if (ossProperties.getEndpoint() != null && accessKeyId != null && !accessKeyId.isBlank()) {
            // 使用内网 endpoint 读取（服务端在新加坡 ECS，走内网）
            String endpoint = ossProperties.getInternalEndpoint() != null
                    ? ossProperties.getInternalEndpoint()
                    : ossProperties.getEndpoint();
            this.ossClient = new OSSClientBuilder().build(
                    endpoint,
                    accessKeyId,
                    ossProperties.getAccessKeySecret()
            );
            log.info("OSS client initialized, bucket={}, region={}", ossProperties.getBucket(), ossProperties.getRegion());
        } else {
            log.warn("OSS not configured (missing credentials), storage operations will be unavailable");
        }
    }

    @PreDestroy
    public void destroy() {
        if (ossClient != null) {
            ossClient.shutdown();
        }
    }

    /**
     * 生成 STS 临时凭证，仅允许 PutObject 到指定前缀
     */
    public StsCredentials generateStsToken(String sessionName) {
        try {
            DefaultProfile profile = DefaultProfile.getProfile(
                    ossProperties.getRegion(),
                    ossProperties.getAccessKeyId(),
                    ossProperties.getAccessKeySecret()
            );
            DefaultAcsClient stsClient = new DefaultAcsClient(profile);

            AssumeRoleRequest request = new AssumeRoleRequest();
            request.setRoleArn(ossProperties.getStsRoleArn());
            request.setRoleSessionName(sessionName);
            request.setDurationSeconds((long) ossProperties.getStsDurationSeconds());

            // 限制 Policy：仅允许 PutObject 到指定前缀
            String policy = String.format("""
                {
                  "Version": "1",
                  "Statement": [{
                    "Effect": "Allow",
                    "Action": ["oss:PutObject"],
                    "Resource": ["acs:oss:*:*:%s/%s*"]
                  }]
                }
                """, ossProperties.getBucket(), ossProperties.getUploadPrefix());
            request.setPolicy(policy);

            AssumeRoleResponse response = stsClient.getAcsResponse(request);
            AssumeRoleResponse.Credentials credentials = response.getCredentials();

            return new StsCredentials(
                    credentials.getAccessKeyId(),
                    credentials.getAccessKeySecret(),
                    credentials.getSecurityToken(),
                    credentials.getExpiration(),
                    ossProperties.getBucket(),
                    ossProperties.getRegion(),
                    ossProperties.getEndpoint(),
                    ossProperties.getUploadPrefix()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate STS token: " + e.getMessage(), e);
        }
    }

    /**
     * 流式读取 OSS 对象（内网访问）
     *
     * @param ossKey 对象 Key
     * @return InputStream（调用者负责关闭）
     */
    public InputStream getObjectStream(String ossKey) {
        if (ossClient == null) {
            throw new IllegalStateException("OSS client not initialized");
        }
        OSSObject object = ossClient.getObject(ossProperties.getBucket(), ossKey);
        return object.getObjectContent();
    }

    /**
     * 检查对象是否存在
     */
    public boolean doesObjectExist(String ossKey) {
        if (ossClient == null) {
            throw new IllegalStateException("OSS client not initialized");
        }
        return ossClient.doesObjectExist(ossProperties.getBucket(), ossKey);
    }

    /**
     * 获取对象大小
     */
    public long getObjectSize(String ossKey) {
        if (ossClient == null) {
            throw new IllegalStateException("OSS client not initialized");
        }
        return ossClient.getObjectMetadata(ossProperties.getBucket(), ossKey).getContentLength();
    }
}
