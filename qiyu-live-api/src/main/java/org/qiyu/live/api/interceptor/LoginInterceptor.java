package org.qiyu.live.api.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.dubbo.config.annotation.DubboReference;
import org.qiyu.live.api.annotation.LoginRequired;
import org.qiyu.live.api.context.LoginUserContext;
import org.qiyu.live.api.vo.WebResponseVO;
import org.qiyu.live.user.interfac.IUserPhoneRpc;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    public static final String LOGIN_USER_ID_ATTR = "loginUserId";
    private static final String TOKEN_COOKIE_NAME = "qiyu_token";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @DubboReference(check = false)
    private IUserPhoneRpc userPhoneRpc;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        String token = extractToken(request);
        Long userId = null;
        if (token != null && !token.isBlank()) {
            userId = userPhoneRpc.getUserIdByToken(token);
            if (userId != null) {
                request.setAttribute(LOGIN_USER_ID_ATTR, userId);
                LoginUserContext.setUserId(userId);
            }
        }

        if (requiresLogin(handlerMethod) && userId == null) {
            writeUnauthorized(response);
            return false;
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        LoginUserContext.clear();
    }

    private boolean requiresLogin(HandlerMethod handlerMethod) {
        return handlerMethod.hasMethodAnnotation(LoginRequired.class)
                || handlerMethod.getBeanType().isAnnotationPresent(LoginRequired.class);
    }

    private String extractToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring("Bearer ".length()).trim();
        }

        String token = request.getHeader("token");
        if (token != null && !token.isBlank()) {
            return token.trim();
        }

        String qiyuTokenHeader = request.getHeader("qiyu_token");
        if (qiyuTokenHeader != null && !qiyuTokenHeader.isBlank()) {
            return qiyuTokenHeader.trim();
        }

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (TOKEN_COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        String tokenParam = request.getParameter("token");
        if (tokenParam != null && !tokenParam.isBlank()) {
            return tokenParam.trim();
        }
        return null;
    }

    private void writeUnauthorized(HttpServletResponse response) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(WebResponseVO.fail("login required")));
    }
}
