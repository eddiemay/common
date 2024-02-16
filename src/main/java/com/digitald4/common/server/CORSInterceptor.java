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

  private static final ImmutableSet<String> ALLOWED_ORIGINS = ImmutableSet.of(
      "http://localhost:3000", "http://localhost:5500", "http://localhost:63342",
      "http://127.0.0.1:3000", "http://127.0.0.1:5500", "http://127.0.0.1:63342");
  private static final String ALLOWED_METHODS = "GET, OPTIONS, HEAD, PUT, POST, PATCH, DELETE";

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {}

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
      throws IOException, ServletException {
    HttpServletRequest httpServletRequest = (HttpServletRequest) request;

    String requestOrigin = httpServletRequest.getHeader("Origin");
    if (ALLOWED_ORIGINS.contains(requestOrigin)) {
      HttpServletResponse httpServletResponse = (HttpServletResponse) response;
      // Authorize the origin, all headers, and all methods
      httpServletResponse.addHeader("Access-Control-Allow-Origin", requestOrigin);
      httpServletResponse.addHeader("Access-Control-Allow-Headers", "*");
      httpServletResponse.addHeader("Access-Control-Allow-Methods", ALLOWED_METHODS);

      // CORS handshake (pre-flight request)
      if (httpServletRequest.getMethod().equals("OPTIONS")) {
        httpServletResponse.setStatus(HttpServletResponse.SC_ACCEPTED);
        return;
      }
    }
    // pass the request along the filter chain
    filterChain.doFilter(request, response);
  }

  @Override
  public void destroy() {}
}