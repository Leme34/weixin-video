package com.lee.controller;

import com.lee.VO.HttpJSONResult;
import com.lee.pojo.Users;
import com.lee.pojo.vo.UsersVO;
import com.lee.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

@Api(value = "用户相关业务的接口", tags = {"用户相关业务的controller"})
@RestController
@RequestMapping("/user")
public class UserController extends BasicController {

    @Autowired
    private UserService userService;

    @ApiOperation(value = "用户上传头像", notes = "用户上传头像的接口")
    @ApiImplicitParam(name = "userId", value = "用户id", required = true,
            dataType = "String", paramType = "query")
    @PostMapping("/uploadFace")
    public HttpJSONResult uploadFace(String userId,
                                     @RequestParam("file") MultipartFile[] files) throws Exception {
        String originalFilename = "";
        String relativePath = "";
        //用户id为空直接返回失败
        if (StringUtils.isEmpty(userId)) {
            return HttpJSONResult.errorMsg("用户id不能为空...");
        }

        //若有文件上传
        if (files != null && files.length > 0) {

            //1、拼接用户上传的文件完整路径
            originalFilename = files[0].getOriginalFilename();//上传的文件名
            relativePath = "/" + userId + "/face"; // 此用户头像文件存放的文件夹路径
            // FILE_SPACE + /180425CFA4RB6T0H/face + / + originalFilename
            String filePath = FILE_SPACE + relativePath + "/" + originalFilename;//得到文件完整路径

            //2、若(用户id命名的)父文件夹不存在则需要创建
            File file = new File(filePath);
            if (file.getParentFile() == null || !file.getParentFile().isDirectory()) {
                file.getParentFile().mkdirs();
            }

            //3、若有文件，则上传(复制)文件
            FileOutputStream os = null;
            InputStream is = null;
            try {
                byte[] buf = new byte[1024];
                int len = 0;
                os = new FileOutputStream(filePath);
                is = files[0].getInputStream();
                //读多少写多少
                while ((len = is.read(buf)) != -1) {
                    os.write(buf, 0, len);
                }
            } catch (Exception e) {
                e.printStackTrace();
                return HttpJSONResult.errorMsg("上传出错...");
            } finally {
                if (os != null) {
                    os.flush();
                    os.close();
                }
                if (is != null) {
                    is.close();
                }
            }
        } else {  //上传文件为空
            return HttpJSONResult.errorMsg("上传出错...");
        }
        //更新数据库中的face_image记录的图片相对路径 : /180425CFA4RB6T0H/xxx.jpg
        Users user = new Users();
        user.setId(userId);
        //set新的头像文件相对路径(/180425CFA4RB6T0H/face/xxx.jpg),更新到数据库中
        user.setFaceImage(relativePath + "/" + originalFilename);
        userService.updateUserInfo(user);

        //把头像文件相对路径返回前端
        return HttpJSONResult.ok(relativePath + "/" + originalFilename);
    }


    @ApiOperation(value="查询用户信息", notes="查询用户信息的接口")
    @ApiImplicitParam(name="userId", value="用户id", required=true,
            dataType="String", paramType="query")
    @PostMapping("/query")
    public HttpJSONResult query(String userId, String fanId) throws Exception {

        //用户id为空直接返回失败
        if (StringUtils.isEmpty(userId)) {
            return HttpJSONResult.errorMsg("用户id不能为空...");
        }

        UsersVO usersVO = new UsersVO();
        Users user = userService.queryUserInfo(userId);
        BeanUtils.copyProperties(user,usersVO);

        return HttpJSONResult.ok(usersVO);
    }


}
