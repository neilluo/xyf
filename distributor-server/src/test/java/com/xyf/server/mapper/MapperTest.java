package com.xyf.server.mapper;

import com.xyf.server.domain.DistributionTask;
import com.xyf.server.domain.PlatformAccount;
import com.xyf.server.domain.VideoMeta;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Mapper 单元测试 - 使用 H2 内存数据库
 */
@SpringBootTest
@ActiveProfiles("test")
class MapperTest {

    @Autowired
    private VideoMetaMapper videoMetaMapper;

    @Autowired
    private DistributionTaskMapper distributionTaskMapper;

    @Autowired
    private PlatformAccountMapper platformAccountMapper;

    @Test
    void testVideoMetaCrud() {
        // 创建
        VideoMeta video = new VideoMeta();
        video.setUserId(1L);
        video.setTitle("红烧肉家常做法");
        video.setDescription("今天教大家做一道经典红烧肉");
        video.setTags(List.of("美食", "红烧肉", "家常菜"));
        video.setOssBucket("video-dist-sg");
        video.setOssKey("videos/2024/test-video.mp4");
        video.setOssRegion("ap-southeast-1");
        video.setFileSize(232000000L);
        video.setFileFormat("mp4");

        Map<String, Object> extInfo = new HashMap<>();
        extInfo.put("traceId", "0a1b2c3d4e1719821234560001ab123");
        extInfo.put("source", "cli-upload");
        video.setExtInfo(extInfo);

        int rows = videoMetaMapper.insert(video);
        assertEquals(1, rows);
        assertNotNull(video.getId());

        // 查询
        VideoMeta found = videoMetaMapper.selectById(video.getId());
        assertNotNull(found);
        assertEquals("红烧肉家常做法", found.getTitle());
        assertNotNull(found.getExtInfo());
        assertEquals("0a1b2c3d4e1719821234560001ab123", found.getExtInfo().get("traceId"));
    }

    @Test
    void testDistributionTaskClaimTask() {
        // 先创建 platform_account
        PlatformAccount account = new PlatformAccount();
        account.setUserId(1L);
        account.setPlatform("YOUTUBE");
        account.setAccountName("test-channel");
        account.setStatus("ACTIVE");
        Map<String, Object> accExtInfo = new HashMap<>();
        accExtInfo.put("traceId", "0a1b2c3d4e1719821234560001ab124");
        account.setExtInfo(accExtInfo);
        platformAccountMapper.insert(account);

        // 创建任务
        DistributionTask task = new DistributionTask();
        task.setUserId(1L);
        task.setVideoId(1L);
        task.setPlatform("YOUTUBE");
        task.setAccountId(account.getId());
        task.setStatus("PENDING");
        task.setRetryCount(0);
        task.setMaxRetry(3);
        task.setScheduledAt(LocalDateTime.now());

        Map<String, Object> extInfo = new HashMap<>();
        extInfo.put("traceId", "0a1b2c3d4e1719821234560001ab125");
        task.setExtInfo(extInfo);

        distributionTaskMapper.insert(task);

        // 乐观锁拾取
        int claimed = distributionTaskMapper.claimTask(task.getId());
        assertEquals(1, claimed);

        // 重复拾取应失败
        int claimedAgain = distributionTaskMapper.claimTask(task.getId());
        assertEquals(0, claimedAgain);

        // 验证状态已更新
        DistributionTask updated = distributionTaskMapper.selectById(task.getId());
        assertEquals("UPLOADING", updated.getStatus());
    }

    @Test
    void testPlatformAccountUniqueConstraint() {
        PlatformAccount account1 = new PlatformAccount();
        account1.setUserId(1L);
        account1.setPlatform("TIKTOK");
        account1.setAccountName("unique-test-account");
        account1.setStatus("ACTIVE");
        Map<String, Object> extInfo = new HashMap<>();
        extInfo.put("traceId", "0a1b2c3d4e1719821234560001ab126");
        account1.setExtInfo(extInfo);

        int rows = platformAccountMapper.insert(account1);
        assertEquals(1, rows);
        assertNotNull(account1.getId());

        // 查询
        PlatformAccount found = platformAccountMapper.selectById(account1.getId());
        assertEquals("TIKTOK", found.getPlatform());
        assertEquals("unique-test-account", found.getAccountName());
    }
}
