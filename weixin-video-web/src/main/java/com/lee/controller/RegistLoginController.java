package com.lee.controller;

import com.lee.VO.HttpJSONResult;
import com.lee.pojo.Users;
import com.lee.pojo.vo.UsersVO;
import com.lee.service.UserService;
import com.lee.utils.MD5Utils;
import com.lee.utils.RedisOperator;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Api(value = "用户注册登录的接口", tags = {"注册和登录的controller"})
@RestController
public class RegistLoginController extends BasicController {

    @Autowired
    private UserService userService;

    @Autowired
    public RedisOperator redisOperator;

    /**
     * 用户注册
     */
    @ApiOperation(value = "用户注册", notes = "用户注册的接口")
    @PostMapping("/regist")
    public HttpJSONResult regist(@RequestBody Users user) throws Exception {
        //1、验证用户名密码是否为空
        if (StringUtils.isEmpty(user.getUsername()) || StringUtils.isEmpty(user.getPassword())) {
            return HttpJSONResult.errorMsg("用户名和密码不能为空");
        }
        //2、判断用户名是否已存在
        if (userService.queryUsernameIsExist(user.getUsername())) {
            return HttpJSONResult.errorMsg("用户名已经存在，请换一个再试");
        }
        // 3. 保存用户注册信息
        user.setNickname(user.getUsername());  //用户名作为昵称
        user.setPassword(MD5Utils.getMD5Str(user.getPassword())); //编码密码
        user.setFansCounts(0);
        user.setReceiveLikeCounts(0);
        user.setFollowCounts(0);
        //生成id保存到数据库
        userService.saveUser(user);
        //4. 复制属性到vo对象中,返回前端
        UsersVO usersVO = new UsersVO();
        BeanUtils.copyProperties(user, usersVO);
        return HttpJSONResult.ok(usersVO);
    }

    /**
     * 用户登录
     */
    @ApiOperation(value = "用户登录", notes = "用户登录的接口")
    @PostMapping("/login")
    public HttpJSONResult login(@RequestBody Users user) throws Exception {  //此处接收的对象中只有用户名和密码
        //1、验证用户名密码是否为空
        if (StringUtils.isEmpty(user.getUsername()) || StringUtils.isEmpty(user.getPassword())) {
            System.out.println("用户名和密码不能为空");
            return HttpJSONResult.errorMsg("用户名和密码不能为空");
        }
        //把用户密码编码后去数据库比对
        String encodedPwd = MD5Utils.getMD5Str(user.getPassword());
        Users result = userService.queryUserForLogin(user.getUsername(), encodedPwd);
        //密码错误
        if (result == null) {
            return HttpJSONResult.errorMsg("用户名或密码错误, 请重试...");
        }
        //登录成功,设置userId
        user.setId(result.getId());
        //登录成功,保存分布式session到redis,得到带有生成的userToken的UsersVO对象
        UsersVO usersVO = setUserRedisSessionToken(user); //在方法内拷贝了属性到dto对象


        return HttpJSONResult.ok(usersVO);
    }

    /**
     * 用户登出(注销)
     */
    @ApiOperation(value = "用户注销",notes = "用户注销的接口")
    @ApiImplicitParam(name = "userId", value = "用户id", required = true,
                        dataType = "String", paramType = "query")
    @PostMapping("/logout")
    public HttpJSONResult logout(String userId){
        //删除redis中分布式session的key ( userToken )
        redisOperator.del(USER_REDIS_SESSION + ":" + userId);
        return HttpJSONResult.ok();
    }

    /**
     * 保存分布式session到redis,返回带有生成的userToken ( USER_REDIS_SESSION+userId ) 的UsersVO对象
     */
    public UsersVO setUserRedisSessionToken(Users user) {
        UsersVO usersVO = new UsersVO();
        //生成userToken
        String userToken = UUID.randomUUID().toString();
        //保存到redis中,key = USER_REDIS_SESSION : userId ,value = UUID
        redisOperator.set(USER_REDIS_SESSION + ":" + user.getId(), userToken, USER_REDIS_SESSION_EXPIRE);
        //赋值user的属性到vo对象中
        BeanUtils.copyProperties(user, usersVO);
        //加上userToken,作为redis中分布式session的key
        usersVO.setUserToken(userToken);
        return usersVO;
    }


}
