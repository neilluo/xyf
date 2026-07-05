package com.xyf.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xyf.server.domain.VideoMeta;
import org.apache.ibatis.annotations.Mapper;

/**
 * 视频元数据 Mapper
 */
@Mapper
public interface VideoMetaMapper extends BaseMapper<VideoMeta> {
}
