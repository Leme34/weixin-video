package com.lee.controller;

import com.lee.VO.HttpJSONResult;
import com.lee.VO.PagedResult;
import com.lee.enums.VideoStatusEnum;
import com.lee.pojo.Bgm;
import com.lee.pojo.Comments;
import com.lee.pojo.Users;
import com.lee.pojo.Videos;
import com.lee.service.BgmService;
import com.lee.service.VideoService;
import com.lee.utils.FetchVideoCover;
import com.lee.utils.MergeVideoMp3;
import io.swagger.annotations.*;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.UUID;


@RestController
@Api(value = "视频相关业务的接口", tags = {"视频相关业务的controller"})
@RequestMapping("/video")
public class VideoController extends BasicController {

    @Autowired
    private VideoService videoService;
    @Autowired
    private BgmService bgmService;

    @ApiOperation(value = "上传视频", notes = "上传视频的接口")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "userId", value = "用户id", required = true,
                    dataType = "String", paramType = "form"),
            @ApiImplicitParam(name = "bgmId", value = "背景音乐id", required = false,
                    dataType = "String", paramType = "form"),
            @ApiImplicitParam(name = "videoSeconds", value = "背景音乐播放长度", required = true,
                    dataType = "String", paramType = "form"),
            @ApiImplicitParam(name = "videoWidth", value = "视频宽度", required = true,
                    dataType = "String", paramType = "form"),
            @ApiImplicitParam(name = "videoHeight", value = "视频高度", required = true,
                    dataType = "String", paramType = "form"),
            @ApiImplicitParam(name = "desc", value = "视频描述", required = false,
                    dataType = "String", paramType = "form")
    })
    @PostMapping(value = "/upload", headers = "content-type=multipart/form-data")
    public HttpJSONResult upload(String userId,
                                 String bgmId, double videoSeconds,
                                 int videoWidth, int videoHeight,
                                 String desc,
                                 @ApiParam(value = "短视频", required = true)  //此注释使swagger测试接口处可以上传文件
                                         MultipartFile file) throws Exception {
        String originalFilename = "";
        String uploadFilePath = "";  //保存在数据库的路径: /userId/video/xxx.mp4
        String relativePath = "";
        String finalVideoPath = "";
        //用户id为空直接返回失败
        if (StringUtils.isEmpty(userId)) {
            return HttpJSONResult.errorMsg("用户id不能为空...");
        }
        //若有文件上传
        if (file != null) {
            //1、拼接用户上传的文件完整路径
            originalFilename = file.getOriginalFilename();  //上传的文件名
            relativePath = "/" + userId + "/video";   //存放此用户视频文件的文件夹相对路径
            uploadFilePath = relativePath + "/" + originalFilename;  //保存在数据库的路径: /userId/video/xxx.mp4
            // FILE_SPACE + /180425CFA4RB6T0H/video + / + originalFilename
            finalVideoPath = FILE_SPACE + uploadFilePath;//得到文件本地完整路径
            //2、若(用户id命名的)父文件夹不存在则需要创建
            File targetFile = new File(finalVideoPath);
            if (targetFile.getParentFile() == null || !targetFile.getParentFile().isDirectory()) {
                targetFile.getParentFile().mkdirs();
            }
            //3、上传(复制)文件到本地
//            if (copyFile(file, finalVideoPath)) return HttpJSONResult.errorMsg("上传出错...");
            FileOutputStream os = null;
            InputStream is = file.getInputStream();
            try {
                os = new FileOutputStream(finalVideoPath);
                IOUtils.copy(is, os);
            } catch (Exception e) {
                e.printStackTrace();
                return HttpJSONResult.errorMsg("上传出错...");
            } finally {
                if (os != null) {
                    os.close();
                }
                if (is != null) {
                    is.close();
                }
            }
        } else {  //上传文件为空
            return HttpJSONResult.errorMsg("上传出错...");
        }

        // 判断bgmId是否为空，如果不为空，那就查询bgm的信息，并且合并视频，生产新的视频
        if (!StringUtils.isEmpty(bgmId)) {
            //查出此bgm的存放路径
            Bgm bgm = bgmService.queryBgmById(bgmId);
            String bgmPath = FILE_SPACE + bgm.getPath();
            //暂存原视频路径
            String sourceVideoPath = finalVideoPath;
            //更新数据库保存的文件路径
            uploadFilePath = relativePath + "/" + UUID.randomUUID().toString() + ".mp4";
            //替换为新视频的路径
            finalVideoPath = FILE_SPACE + uploadFilePath;
            //调用ffmpeg合并音视频工具类
            MergeVideoMp3 tool = new MergeVideoMp3(FFMPEG_EXE);
            tool.convertor(sourceVideoPath, bgmPath, videoSeconds, finalVideoPath);
        }

        //使用ffmpeg对上传的视频截取并输出封面图
        FetchVideoCover fetchVideoCover = new FetchVideoCover(FFMPEG_EXE);
        String coverPath = relativePath + "/" + UUID.randomUUID().toString() + ".jpg";
        fetchVideoCover.getCover(finalVideoPath, FILE_SPACE + coverPath);

        //保存视频信息到videos表
        Videos video = new Videos();
        video.setAudioId(bgmId);
        video.setUserId(userId);
        video.setVideoSeconds((float) videoSeconds);
        video.setVideoHeight(videoHeight);
        video.setVideoWidth(videoWidth);
        video.setVideoDesc(desc);
        video.setVideoPath(uploadFilePath);  //保存在数据库的相对路径: /180425CFA4RB6T0H/video + / + xxx.mp4
        video.setCoverPath(coverPath);  //set封面图片路径
        video.setStatus(VideoStatusEnum.SUCCESS.value); // 状态默认为：发布成功
        video.setCreateTime(new Date());

        String videoId = videoService.saveVideo(video);
        //保存视频,返回video的id用于上传视频成功后上传封面图片(微信小程序原因不能多文件上传)
        return HttpJSONResult.ok(videoId);
    }


    /**
     * 分页展示首页视频列表、搜索查询视频列表
     * video : 需要查询的video对象
     * isSaveRecord：1 - 需要保存热搜词,只有在搜索视频的时候才需要
     * 0 - 不需要保存热搜词 ，或者为空的时候
     */
    @PostMapping(value = "/showAll")
    public HttpJSONResult showAll(@RequestBody Videos video,
                                  @RequestParam(value = "isSaveRecord", required = false, defaultValue = "0") Integer isSaveRecord,
                                  @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
                                  @RequestParam(value = "pageSize", required = false, defaultValue = "5") Integer pageSize) throws Exception {
        PagedResult result = videoService.getAllVideo(video, isSaveRecord, page, pageSize);
        return HttpJSONResult.ok(result);
    }

    /**
     * 查询所有最热搜索词
     */
    @PostMapping(value = "/hot")
    public HttpJSONResult hot() throws Exception {
        return HttpJSONResult.ok(videoService.getHotwords());
    }

    /**
     * 保存用户留言
     */
    @PostMapping("/saveComment")
    public HttpJSONResult saveComment(@RequestBody Comments comment, String fatherCommentId, String toUserId) throws Exception {
        //补上前端传入的父评论id和被评论人id
        comment.setFatherCommentId(fatherCommentId);
        comment.setToUserId(toUserId);
        videoService.saveComment(comment);
        return HttpJSONResult.ok();
    }


    /**
     * 分页查询此视频的所有留言
     */
    @PostMapping("/getVideoComments")
    public HttpJSONResult getVideoComments(String videoId,
                                           @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
                                           @RequestParam(value = "pageSize", required = false, defaultValue = "5") Integer pageSize) {
        //若videoId为空
        if (StringUtils.isEmpty(videoId)) {
            return HttpJSONResult.ok();
        }
        PagedResult result = videoService.getAllComments(videoId, page, pageSize);
        return HttpJSONResult.ok(result);
    }


    /**
     * 由于小程序端上传不了封面图路径所以只能在上边handler从上传的视频中截图保存
     */
//    @ApiOperation(value = "上传视频封面", notes = "上传视频封面的接口")
//    @ApiImplicitParams({
//            @ApiImplicitParam(name = "userId", value = "用户id", required = true,
//                    dataType = "String", paramType = "form"),
//            @ApiImplicitParam(name = "videoId", value = "视频主键id", required = true,
//                    dataType = "String", paramType = "form")
//    })
//    @PostMapping(value = "/uploadCover", headers = "content-type=multipart/form-data")
//    public HttpJSONResult uploadCover(String userId,
//                                      String videoId,
//                                      @ApiParam(value = "视频封面", required = true)
//                                      MultipartFile file) throws Exception {
//        String originalFilename = "";
//        String relativePath = "";
//        //用户id为空直接返回失败
//        if (StringUtils.isEmpty(userId)||StringUtils.isEmpty(videoId)) {
//            return HttpJSONResult.errorMsg("用户id和视频id不能为空...");
//        }
//        //若有文件上传
//        if (file != null) {
//            //1、拼接用户上传的文件完整路径
//            originalFilename = file.getOriginalFilename();//上传的文件名
//            relativePath = "/" + userId + "/video"; // 此用户视频封面文件存放的文件夹路径
//            // FILE_SPACE + /180425CFA4RB6T0H/video + / + originalFilename
//            String coverName = originalFilename.split("\\.")[0]; //取视频文件名前缀作为封面图片名字
//            String filePath = FILE_SPACE + relativePath + "/" + coverName + ".jpg";//得到文件完整路径
//            //2、若(用户id命名的)父文件夹不存在则需要创建
//            File targetFile = new File(filePath);
//            if (targetFile.getParentFile() == null || !targetFile.getParentFile().isDirectory()) {
//                targetFile.getParentFile().mkdirs();
//            }
//            //3、若有文件，则上传(复制)文件
//            if (copyFile(file, filePath)) return HttpJSONResult.errorMsg("上传出错...");
//        } else {  //上传文件为空
//            return HttpJSONResult.errorMsg("上传出错...");
//        }
//        return HttpJSONResult.ok(videoId);
//    }


    //使用Apache的commons工具类
//    private boolean copyFile(MultipartFile sourceFile, String targetFilePath) throws IOException {
//        FileOutputStream os = null;
//        InputStream is = null;
//        try {
//            byte[] buf = new byte[1024];
//            int len = 0;
//            os = new FileOutputStream(targetFilePath);
//            is = sourceFile.getInputStream();
//            //读多少写多少
//            while ((len = is.read(buf)) != -1) {
//                os.write(buf, 0, len);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            return true;
//        } finally {
//            if (os != null) {
//                os.flush();
//                os.close();
//            }
//            if (is != null) {
//                is.close();
//            }
//        }
//        return false;
//    }

}
