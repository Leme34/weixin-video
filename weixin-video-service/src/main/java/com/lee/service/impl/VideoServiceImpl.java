package com.lee.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.lee.VO.PagedResult;
import com.lee.mapper.*;
import com.lee.pojo.Comments;
import com.lee.pojo.SearchRecords;
import com.lee.pojo.Videos;
import com.lee.pojo.vo.CommentsVO;
import com.lee.pojo.vo.VideosVO;
import com.lee.service.VideoService;
import com.lee.utils.TimeAgoUtils;
import org.n3r.idworker.Sid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

import java.util.Date;
import java.util.List;

@Service
public class VideoServiceImpl implements VideoService {

    @Autowired
    private VideosMapper videosMapper;
    @Autowired
    private VideosMapperCustom videosMapperCustom;
    @Autowired
    private Sid sid;
    @Autowired
    private SearchRecordsMapper searchRecordsMapper;
    @Autowired
    private CommentsMapper commentsMapper;
    @Autowired
    private CommentsMapperCustom commentsMapperCustom;

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public String saveVideo(Videos video) {
        String id = sid.nextShort();
        video.setId(id);
        videosMapper.insertSelective(video);
        return id;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public PagedResult getAllVideo(Videos video, Integer isSaveRecord, int page, int pageSize) {
        //若是搜索视频的请求,则保存热搜词
        String desc = video.getVideoDesc();
        if (isSaveRecord != null && isSaveRecord == 1) {
            SearchRecords record = new SearchRecords();
            String recordId = sid.nextShort();
            record.setId(recordId);
            record.setContent(desc);
            searchRecordsMapper.insert(record);
        }
        //分页查询
        PageHelper.startPage(page, pageSize);
        List<VideosVO> videosVOList = videosMapperCustom.queryAllVideos(desc);
        PageInfo<VideosVO> pageInfo = new PageInfo<>(videosVOList);
        //封装自定义PagedResult对象返回前端
        PagedResult pagedResult = new PagedResult();
        pagedResult.setPage(page);
        pagedResult.setTotal(pageInfo.getPages());
        pagedResult.setRows(videosVOList);
        pagedResult.setRecords(pageInfo.getTotal());
        return pagedResult;
    }


    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public List<String> getHotwords() {
        return searchRecordsMapper.getHotwords();
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public void saveComment(Comments comment) {
        //生成id和时间
        String id = sid.nextShort();
        comment.setId(id);
        comment.setCreateTime(new Date());
        commentsMapper.insert(comment);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public PagedResult getAllComments(String videoId, Integer page, Integer pageSize) {
        //分页
        PageHelper.startPage(page,pageSize);
        //关联查询此视频评论人和被评论人信息的commentsVOList
        List<CommentsVO> commentsVOList = commentsMapperCustom.queryComments(videoId);
        PageInfo<CommentsVO> pageInfo = new PageInfo<>(commentsVOList);

        //根据创建时间生成每个评论的timeAgoStr字段(几秒/分钟前)
        for (CommentsVO c : commentsVOList) {
            String timeAgo = TimeAgoUtils.format(c.getCreateTime());
            c.setTimeAgoStr(timeAgo);
        }

        //封装PagedResult对象
        PagedResult pagedResult = new PagedResult();
        pagedResult.setPage(page);  //当前页数
        pagedResult.setTotal(pageInfo.getPages());  //总页数
        pagedResult.setRecords(pageInfo.getTotal());  //总记录数
        pagedResult.setRows(commentsVOList);

        return pagedResult;
    }
}
