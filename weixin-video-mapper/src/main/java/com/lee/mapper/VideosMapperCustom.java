package com.lee.mapper;

import com.lee.pojo.Videos;
import com.lee.pojo.vo.VideosVO;
import com.lee.utils.MyMapper;

import java.util.List;

/**
 * 自定义关联查询videos表与users表
 * 用于展示主页的小视频列表
 */
public interface VideosMapperCustom extends MyMapper<Videos> {

    /**
     * 根据视频描述关联查询视频列表
     */
    List<VideosVO> queryAllVideos(String videoDesc);
}