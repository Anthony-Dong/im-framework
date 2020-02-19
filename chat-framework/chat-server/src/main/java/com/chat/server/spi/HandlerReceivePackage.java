package com.chat.server.spi;

import com.chat.core.exception.HandlerException;
import com.chat.core.model.NPack;
import com.chat.core.model.URL;
import com.chat.core.model.netty.Request;
import com.chat.core.spi.SPIUtil;
import com.chat.server.handler.ChatServerContext;
import com.chat.server.handler.ServerReadChatEventHandler;
import com.chat.server.spi.defaulthandler.DefaultHandlerChainBuilder;
import com.chat.server.spi.filter.DefaultFilter;
import com.chat.server.spi.filter.Filter;
import com.chat.server.spi.handler.HandlerChainBuilder;
import com.chat.server.spi.handler.RequestHandlerProcess;
import io.netty.channel.ChannelHandlerContext;

import java.util.Collections;
import java.util.Objects;

import static com.chat.core.model.UrlConstants.ID_KEY;
import static com.chat.core.model.UrlConstants.MSG_PROTOCOL;

/**
 * 服务器读处理
 * {@link ServerReadChatEventHandler}
 *
 * @date:2019/12/25 8:43
 * @author: <a href='mailto:fanhaodong516@qq.com'>Anthony</a>
 */
public final class HandlerReceivePackage {

    /**
     * 过滤器
     */
    private final Filter filter;

    /**
     * 保存数据包
     */
    private final RequestHandlerProcess process;

    /**
     * context
     */
    private final ChatServerContext context;

    private final String host;

    private final int port;

    private final short version;

    /**
     * 构造方法 , SPI 加载
     */
    public HandlerReceivePackage(ChatServerContext context) {
        this.context = context;
        this.host = context.getAddress().getHostName();
        this.port = context.getAddress().getPort();
        this.version = context.getVersion();
        this.filter = SPIUtil.loadFirstInstanceOrDefault(Filter.class, DefaultFilter.class);
        HandlerChainBuilder builder = SPIUtil.loadFirstInstanceOrDefault(HandlerChainBuilder.class, DefaultHandlerChainBuilder.class);
        this.process = Objects.requireNonNull(builder).build();
    }

    /**
     * 处理器  : 过滤器 和 执行器
     *
     * @param pack 数据包
     * @throws HandlerException 可能处理异常, 抛出
     */
    public void handlerNPack(NPack pack) throws HandlerException {
        ChannelHandlerContext channelContext = null;
        Request request = null;
        try {
            request = buildRequest(pack);
            if (this.filter.doFilter(request)) {
                return;
            }
            // 获取他关联的 context
            channelContext = this.context.getContext(pack.getAddress());
            // 然后清空pack的引用,释放他.
            pack.release();
            // 从第一个去处理 , 处理空异常
            Objects.requireNonNull(process.getFirst()).handler(request, channelContext);
        } finally {
            if (request != null) {
                request.release();
            }
        }
    }


    private Request buildRequest(NPack pack) {
        URL url = URL.valueOfByDecode(pack.getRouter());
        byte[] body = pack.getBody();
        if (body == null || body.length == 0) {
            return new Request(url, null, pack.getTimestamp(), this.host, this.port, this.version);
        }
        return new Request(url, pack.getBody(), pack.getTimestamp(), this.host, this.port, this.version);
    }
}
