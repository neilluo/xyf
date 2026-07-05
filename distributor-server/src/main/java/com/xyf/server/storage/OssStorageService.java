package com.xyf.server.storage;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.OSSObject;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.auth.sts.AssumeRoleRequest;
import com.aliyuncs.auth.sts.AssumeRoleResponse;
import com.aliyuncs.profile.DefaultProfile;
import com.xyf.server.common.BusinessException;
import com.xyf.server.common.constants.ErrorCode;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.InputStream;

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
            throw new BusinessException(ErrorCode.OSS_STS_TOKEN_FAILED,
                    "Failed to generate STS token: " + e.getMessage(), e);
        }
    }

    public InputStream getObjectStream(String ossKey) {
        ensureClientInitialized();
        OSSObject object = ossClient.getObject(ossProperties.getBucket(), ossKey);
        return object.getObjectContent();
    }

    public boolean doesObjectExist(String ossKey) {
        ensureClientInitialized();
        return ossClient.doesObjectExist(ossProperties.getBucket(), ossKey);
    }

    public long getObjectSize(String ossKey) {
        ensureClientInitialized();
        return ossClient.getObjectMetadata(ossProperties.getBucket(), ossKey).getContentLength();
    }

    private void ensureClientInitialized() {
        if (ossClient == null) {
            throw new BusinessException(ErrorCode.OSS_CLIENT_NOT_INITIALIZED, "OSS client not initialized");
        }
    }
}
