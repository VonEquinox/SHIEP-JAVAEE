package com.hotel.gateway;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 登录校验过滤器。
 * token 由 user-service 登录时签发，格式为 Base64("userId:role")；
 * 校验通过后把 X-User-Id / X-User-Role 传给下游服务，下游直接取用。
 * 客户端自带的 X-User-* 头一律剥离，防止伪造身份。
 */
@Component
public class AuthFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(h -> {
                    h.remove("X-User-Id");
                    h.remove("X-User-Role");
                })
                .build();

        if (isWhitelisted(request)) {
            return chain.filter(exchange.mutate().request(request).build());
        }

        String token = request.getHeaders().getFirst("Authorization");
        try {
            String[] parts = new String(Base64.getDecoder().decode(token), StandardCharsets.UTF_8).split(":");
            // HTTP 头只允许 ASCII，中文角色名转成 ADMIN / USER 下发
            request = request.mutate()
                    .header("X-User-Id", parts[0])
                    .header("X-User-Role", "管理员".equals(parts[1]) ? "ADMIN" : "USER")
                    .build();
            return chain.filter(exchange.mutate().request(request).build());
        } catch (Exception e) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    /** 无需登录：注册、登录，以及 GET 方式的酒店浏览、评价浏览 */
    private boolean isWhitelisted(ServerHttpRequest request) {
        String path = request.getURI().getPath();
        if (path.equals("/api/user/register") || path.equals("/api/user/login")) {
            return true;
        }
        return HttpMethod.GET.equals(request.getMethod())
                && (path.startsWith("/api/hotel") || path.startsWith("/api/review/hotel"));
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
