package com.amy.gateway.handler;

import org.springframework.cloud.gateway.support.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import com.amy.common.core.utils.ServletUtils;
import reactor.core.publisher.Mono;

/**
 * 网关统一异常处理
 *
 * @author amy
 */
@Order(-1)
@Configuration
public class GatewayExceptionHandler implements WebExceptionHandler
{
    private static final Logger log = LoggerFactory.getLogger(GatewayExceptionHandler.class);

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex)
    {
        ServerHttpResponse response = exchange.getResponse();

        if (exchange.getResponse().isCommitted())
        {
            return Mono.error(ex);
        }

        HttpStatus status;
        String msg;

        if (ex instanceof NotFoundException)
        {
            status = HttpStatus.NOT_FOUND;
            msg = "服务未找到";
        }
        else if (ex instanceof ResponseStatusException)
        {
            // 沿用原始异常携带的 HTTP 状态（如 503 服务不可用），非标准码兜底为 500
            ResponseStatusException responseStatusException = (ResponseStatusException) ex;
            HttpStatus resolved = HttpStatus.resolve(responseStatusException.getStatusCode().value());
            status = resolved != null ? resolved : HttpStatus.INTERNAL_SERVER_ERROR;
            msg = responseStatusException.getMessage();
        }
        else
        {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            msg = "内部服务器错误";
        }

        log.error("[网关异常处理]请求路径:{},异常信息:{}", exchange.getRequest().getPath(), ex.getMessage());

        return ServletUtils.webFluxResponseWriter(response, status, msg);
    }
}
