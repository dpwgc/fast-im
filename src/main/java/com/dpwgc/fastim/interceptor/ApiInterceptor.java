package com.dpwgc.fastim.interceptor;

import com.alibaba.fastjson.JSONObject;
import com.dpwgc.fastim.util.LoginUtil;
import com.dpwgc.fastim.util.RedisUtil;
import com.dpwgc.fastim.util.ResultUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;

/**
 * 接口访问拦截器
 */
public class ApiInterceptor implements HandlerInterceptor {

    @Autowired
    private LoginUtil loginUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String userId = request.getHeader("userId");
        String token = request.getHeader("token");
        //校验登录状态
        return loginUtil.loginCheck(userId,token);
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {

    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {

    }

    /**
     * 身份验证出错
     * @param response 响应信息
     * @throws IOException
     */
    private void returnErrorResponse(HttpServletResponse response) throws IOException {
        ResultUtil<Object> resultUtil = new ResultUtil<>();
        resultUtil.setCode(440);
        resultUtil.setMsg("请登录后访问");
        OutputStream out = null;
        try {
            response.setCharacterEncoding("utf-8");
            response.setContentType("application/x-www-form-urlencoded");
            out = response.getOutputStream();
            out.write(JSONObject.toJSONString(resultUtil).getBytes("utf-8"));
            out.flush();
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }
}
