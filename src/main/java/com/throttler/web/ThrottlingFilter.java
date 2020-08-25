package com.throttler.web;

import com.throttler.ThrottlingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

@Component
@Order(1)
public class ThrottlingFilter implements Filter {
    private static final int ERROR_CODE = 429;
    private static final String ERROR_MESSAGE = "Too many requests";
    @Autowired
    private ThrottlingService throttlingService;
    @Value("tokenParamName") private String tokenParamName;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        if(throttlingService.isRequestAllowed(Optional.ofNullable(request.getParameter(tokenParamName))))
            chain.doFilter(servletRequest, servletResponse);
        else{
            ((HttpServletResponse) servletResponse).sendError(ERROR_CODE, ERROR_MESSAGE);
        }
    }
}
