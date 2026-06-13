package com.rapidocurier.paquetesservice.infrastructure.adapter.out.feign.client;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class FeignAuthInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes servletAttrs) {
            HttpServletRequest request = servletAttrs.getRequest();
            String userId = request.getHeader("X-User-Id");
            String roles = request.getHeader("X-User-Roles");
            if (userId != null) {
                template.header("X-User-Id", userId);
            }
            if (roles != null) {
                template.header("X-User-Roles", roles);
            }
        }
    }
}
