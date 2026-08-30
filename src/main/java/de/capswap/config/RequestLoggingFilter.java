package de.capswap.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;

// loggt REST Calls und Threads
@Component
@Slf4j
public class RequestLoggingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) //servlet nimmt http requests und responses entgegen, filtert sie und leitet sie weiter
            throws IOException, ServletException {
            
        if (request instanceof HttpServletRequest req) {
            String uri = req.getRequestURI();
            if (uri.startsWith("/api/")) { //nur REST-API-Calls loggen
                log.info("REST-Verarbeitung: {} {} (Thread={})", 
                         req.getMethod(), uri, Thread.currentThread().getName());
            }
        }
        
        chain.doFilter(request, response);
    }
}
