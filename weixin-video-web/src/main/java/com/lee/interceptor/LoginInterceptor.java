package com.lee.interceptor;

import com.google.gson.Gson;
import com.lee.VO.HttpJSONResult;
import com.lee.utils.RedisOperator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 对用户操作是否已登录的拦截器
 */
public class LoginInterceptor implements HandlerInterceptor {

    @Value("${USER_REDIS_SESSION}")
    private String USER_REDIS_SESSION;

    @Autowired
    private RedisOperator redisOperator;

    /**
     * 拦截请求，在controller调用之前
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //获取请求头信息(USER_REDIS_SESSION+":"+userId作为redis中分布式session的key ,userToken也是session的value)
        String userId = request.getHeader("headerUserId");
        String userToken = request.getHeader("headerUserToken");
        System.out.println("userId="+userId);
        System.out.println("userToken="+userToken);
        //若header信息不为空
        if (!StringUtils.isEmpty(userId) && !StringUtils.isEmpty(userToken)) {
            //查询redis中是否存在此session
            String session_userToken = redisOperator.get(USER_REDIS_SESSION + ":" + userId);
            System.out.println("session_userToken="+session_userToken);
            //若session不存在
            if (StringUtils.isEmpty(userToken)) {
                System.out.println("session不存在,当前请求url："+request.getRequestURI()+"  请先登录...");
                //返回错误码并输出错误信息
                returnErrorResponse(response, HttpJSONResult.errorTokenMsg("请先登录..."));
                return false;
            } else {  //session存在
                //若session内容已改变则是被挤下线
                if (!session_userToken.equals(userToken)) {
                    System.out.println("账号被挤出...");
                    returnErrorResponse(response, HttpJSONResult.errorTokenMsg("账号被挤出..."));
                    return false;
                }
                //否则已登录状态
                return true;
            }
        } else {  //header信息为空,未登录
            System.out.println("header信息为空,当前请求url："+request.getRequestURI()+"  请先登录...");
            returnErrorResponse(response, HttpJSONResult.errorTokenMsg("请先登录..."));
            return false;
        }
    }


    /**
     * 在拦截器只能通过输出流写数据
     */

    public void returnErrorResponse(HttpServletResponse response, HttpJSONResult result) throws Exception {
        //输出utf-8的json数据,这样前端才能转化为json对象
        response.setCharacterEncoding("utf-8");
        response.setContentType("text/json");
        ServletOutputStream out = response.getOutputStream();
        Gson gson = new Gson();
        try {
            out.write(gson.toJson(result).getBytes("utf-8"));
            out.flush();
        } finally {
            if (out != null) {
                out.close();
            }
        }

    }


    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {

    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {

    }
}
