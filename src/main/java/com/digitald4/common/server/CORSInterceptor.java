package com.digitald4.common.server;

import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebFilter(asyncSupported = true, urlPatterns = {"/*"})
public class CORSInterceptor implements Filter {

  private static final ImmutableSet<String> allowedOrigins = ImmutableSet.of(
      "http://localhost:3000", "http://localhost:5500", "http://localhost:63342",
      "http://127.0.0.1:3000", "http://127.0.0.1:5500", "http://127.0.0.1:63342");

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {}

  @Override
  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
      FilterChain filterChain) throws IOException, ServletException {
    HttpServletRequest request = (HttpServletRequest) servletRequest;

    String requestOrigin = request.getHeader("Origin");
    if (isAllowedOrigin(requestOrigin)) {
      // Authorize the origin, all headers, and all methods
      ((HttpServletResponse) servletResponse).addHeader("Access-Control-Allow-Origin",
          requestOrigin);
      ((HttpServletResponse) servletResponse).addHeader("Access-Control-Allow-Headers", "*");
      ((HttpServletResponse) servletResponse).addHeader("Access-Control-Allow-Methods",
          "GET, OPTIONS, HEAD, PUT, POST, DELETE");

      HttpServletResponse resp = (HttpServletResponse) servletResponse;

      // CORS handshake (pre-flight request)
      if (request.getMethod().equals("OPTIONS")) {
        resp.setStatus(HttpServletResponse.SC_ACCEPTED);
        return;
      }
    }
    // pass the request along the filter chain
    filterChain.doFilter(request, servletResponse);
  }

  @Override
  public void destroy() {}

  private boolean isAllowedOrigin(String origin) {
    return allowedOrigins.contains(origin);
  }
}