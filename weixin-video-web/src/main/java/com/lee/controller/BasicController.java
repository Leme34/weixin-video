package com.lee.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RestController;


/**
 * 定义一些配置文件注入的自定义属性,其他Controller通过继承此类使用
 */
@RestController
public class BasicController {

    /**
     * @Value配置文件注入值的属性不能使用static或final修饰
     */

    //redis中分布式session的key
    @Value("${USER_REDIS_SESSION}")
    public String USER_REDIS_SESSION;

    //redis中分布式session的过期时间
    @Value("${USER_REDIS_SESSION_EXPIRE}")
    public Long USER_REDIS_SESSION_EXPIRE;

    // 上传的文件保存在本地的根目录
    @Value("${FILE_SPACE}")
    public String FILE_SPACE;

    // ffmpeg所在目录
    @Value("${FFMPEG_EXE}")
    public String FFMPEG_EXE;

    // 每页分页的记录数
    @Value("${PAGE_SIZE}")
    public Integer PAGE_SIZE;

}
