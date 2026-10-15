package com.example.shortlink.config;


import com.example.shortlink.pojo.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import tools.jackson.databind.ObjectMapper;


@Component
public class ApiKeyInterceptor implements HandlerInterceptor {

    @Value("${administratorApiKey.api-key}")
    private String validApiKey;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) throws Exception{

        //如果是GET请求直接允许
        if("GET".equalsIgnoreCase(request.getMethod())) return true;


        //获取请求体中的APIKey
        String apiKey = request.getHeader("X-API-Key");
        if(apiKey!=null && apiKey.equals(validApiKey)) return true;

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                objectMapper.writeValueAsString(Result.error("未授权：缺少或错误的 API Key"))
        );
        return false;
    }
}
