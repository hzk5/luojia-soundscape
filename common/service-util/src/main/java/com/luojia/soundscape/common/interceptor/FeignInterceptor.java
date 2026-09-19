package com.luojia.soundscape.common.interceptor;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 只有上游系统通过OpenFeign接口调用下游系统会被自动拦截
 */

@Component
public class FeignInterceptor implements RequestInterceptor {

    public void apply(RequestTemplate requestTemplate){
        //  获取请求对象
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        //异步编排 与 MQ消费者端 为 null
        if(null != requestAttributes) {
            ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes)requestAttributes;
            //从上游系统获取token
            HttpServletRequest request = servletRequestAttributes.getRequest();
            String token = request.getHeader("token");
            //设置到OpenFeign请求 请求头中
            requestTemplate.header("token", token);
        }
    }
}
