package ign.middleman.controllers;

import com.google.inject.Singleton;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

@Singleton
public class SendServerHeader implements Filter {

    String host;

    public void init(FilterConfig filterConfig) throws ServletException {
        try {
            host = thisHost();
        } catch (UnknownHostException e) {
            host = "localhost";
        }
    }

    String thisHost() throws UnknownHostException {
        return InetAddress.getLocalHost().getHostName();
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        try {
            if (response instanceof HttpServletResponse) {
                ((HttpServletResponse) response).setHeader("X-Via-Server", host);
                ((HttpServletResponse) response).setHeader("Via", "Middleman/1.0");
            }
        } finally {
            chain.doFilter(request, response);
        }
    }

    public void destroy() {
    }
}
