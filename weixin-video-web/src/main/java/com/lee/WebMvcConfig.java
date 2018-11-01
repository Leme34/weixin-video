package com.lee;

import com.lee.interceptor.LoginInterceptor;
import com.lee.utils.ZKCuratorClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * 配置通过tomcat访问服务器本地的文件路径
 */
@Configuration
public class WebMvcConfig extends WebMvcConfigurerAdapter {

    @Value("${FILE_SPACE}")
    private String FILE_SPACE;

    /**
     * 配置静态资源访问
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")   //访问所有资源
                .addResourceLocations("classpath:/META-INF/resource/")  //配置访问swagger的静态资源
                .addResourceLocations("file:" + FILE_SPACE + "/");   //资源路径,必须要加末尾的"/"
    }

    /**
     * 注入拦截器
     */
    @Bean
    public LoginInterceptor loginInterceptor() {
        return new LoginInterceptor();
    }

    @Autowired
    private LoginInterceptor loginInterceptor;

    /**
     * 拦截未登录的用户
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        registry.addInterceptor(loginInterceptor).addPathPatterns("/user/**")
                .addPathPatterns("/video/upload", "/video/uploadCover",
                        "/video/userLike", "/video/userUnLike",
                        "/video/saveComment")
                .addPathPatterns("/bgm/**")
                .excludePathPatterns("/user/queryPublisher");

        super.addInterceptors(registry);
    }

    /**
     * 注入zk工具类
     * 调用初始化方法init:启动客户端,监听zk节点的事件(实现后台上传bgm同步到此服务器中)
     */
    @Bean(initMethod="init")
    public ZKCuratorClient zkCuratorClient() {
        return new ZKCuratorClient();
    }

}
