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
import com.xyf.server.config.DynamicConfigService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
public class OssStorageService {

    private static final Logger log = LoggerFactory.getLogger(OssStorageService.class);

    private final DynamicConfigService configService;
    private OSS ossClient;

    public OssStorageService(DynamicConfigService configService) {
        this.configService = configService;
    }

    @PostConstruct
    public void init() {
        String accessKeyId = configService.get("OSS", "access_key_id");
        String endpoint = configService.get("OSS", "endpoint");
        if (endpoint != null && !endpoint.isBlank() && accessKeyId != null && !accessKeyId.isBlank()) {
            String internalEndpoint = configService.get("OSS", "internal_endpoint");
            String effectiveEndpoint = (internalEndpoint != null && !internalEndpoint.isBlank())
                    ? internalEndpoint : endpoint;
            this.ossClient = new OSSClientBuilder().build(
                    effectiveEndpoint,
                    accessKeyId,
                    configService.get("OSS", "access_key_secret")
            );
            log.info("OSS client initialized, bucket={}, region={}",
                    configService.get("OSS", "bucket"), configService.get("OSS", "region"));
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
            String region = configService.get("OSS", "region", "ap-southeast-1");
            String accessKeyId = configService.get("OSS", "access_key_id");
            String accessKeySecret = configService.get("OSS", "access_key_secret");

            DefaultProfile profile = DefaultProfile.getProfile(region, accessKeyId, accessKeySecret);
            DefaultAcsClient stsClient = new DefaultAcsClient(profile);

            AssumeRoleRequest request = new AssumeRoleRequest();
            request.setRoleArn(configService.get("OSS", "sts_role_arn"));
            request.setRoleSessionName(sessionName);
            String duration = configService.get("OSS", "sts_duration_seconds", "3600");
            request.setDurationSeconds(Long.parseLong(duration));

            String bucket = configService.get("OSS", "bucket");
            String uploadPrefix = configService.get("OSS", "upload_prefix", "videos/");
            String policy = String.format("""
                {
                  "Version": "1",
                  "Statement": [{
                    "Effect": "Allow",
                    "Action": ["oss:PutObject"],
                    "Resource": ["acs:oss:*:*:%s/%s*"]
                  }]
                }
                """, bucket, uploadPrefix);
            request.setPolicy(policy);

            AssumeRoleResponse response = stsClient.getAcsResponse(request);
            AssumeRoleResponse.Credentials credentials = response.getCredentials();

            return new StsCredentials(
                    credentials.getAccessKeyId(),
                    credentials.getAccessKeySecret(),
                    credentials.getSecurityToken(),
                    credentials.getExpiration(),
                    bucket,
                    region,
                    configService.get("OSS", "endpoint"),
                    uploadPrefix
            );
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OSS_STS_TOKEN_FAILED,
                    "Failed to generate STS token: " + e.getMessage(), e);
        }
    }

    public InputStream getObjectStream(String ossKey) {
        ensureClientInitialized();
        OSSObject object = ossClient.getObject(configService.get("OSS", "bucket"), ossKey);
        return object.getObjectContent();
    }

    public boolean doesObjectExist(String ossKey) {
        ensureClientInitialized();
        return ossClient.doesObjectExist(configService.get("OSS", "bucket"), ossKey);
    }

    public long getObjectSize(String ossKey) {
        ensureClientInitialized();
        return ossClient.getObjectMetadata(configService.get("OSS", "bucket"), ossKey).getContentLength();
    }

    public void deleteObject(String ossKey) {
        ensureClientInitialized();
        String bucket = configService.get("OSS", "bucket");
        ossClient.deleteObject(bucket, ossKey);
        log.info("OSS object deleted: bucket={}, key={}", bucket, ossKey);
    }

    private void ensureClientInitialized() {
        if (ossClient == null) {
            // 懒初始化：@PostConstruct 时 DynamicConfigService 可能还没加载 DB 配置
            // 首次实际调用时重试初始化
            init();
        }
        if (ossClient == null) {
            throw new BusinessException(ErrorCode.OSS_CLIENT_NOT_INITIALIZED, "OSS client not initialized");
        }
    }
}
