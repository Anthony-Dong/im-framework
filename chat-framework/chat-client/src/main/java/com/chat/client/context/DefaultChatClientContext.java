package com.chat.client.context;

import com.alibaba.fastjson.JSON;
import com.chat.client.future.NpackFuture;
import com.chat.client.hander.ChatClientContext;
import com.chat.core.exception.TimeOutException;
import com.chat.core.model.NPack;
import com.chat.core.model.RouterBuilder;
import com.chat.core.model.netty.ArgsUtil;
import com.chat.core.model.netty.Response;
import com.chat.core.netty.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.function.Consumer;

/**
 * @date:2020/2/17 10:55
 * @author: <a href='mailto:fanhaodong516@qq.com'>Anthony</a>
 */
public class DefaultChatClientContext extends ChatClientContext implements RpcContext, MsgContext {

    /**
     * 本来想做缓存的,发现麻烦了
     */
    private static final Logger logger = LoggerFactory.getLogger(DefaultChatClientContext.class);

    /**
     * 如果消息正确响应了, 但是超时了, 那么直接进入次接口, 记录日志, 用户也可以自己实现
     */
    private Consumer<Response> fallback = response -> logger.error("Fall Back : {}.", response);

    /**
     * 客户端接收到信息
     */
    @Override
    protected void onReading(Response response) {
        NpackFuture.received(response, fallback);
    }

    /**
     * 发送日志,不需要返回消息,server在path路径上了. 其他信息在body里
     */
    public void sendLog(String server, String format, Object... args) {
        String logs = String.format(format, args);
        String router = RouterBuilder.buildLog(server);
        NPack pack = new NPack(router, logs.getBytes());
        context.writeAndFlush(pack);
    }

    /**
     * 发送
     */
    @Override
    public Response sendMessageBySync(String msg, String sender, String receiver) throws TimeOutException {
        return sendMessageBySync(msg, sender, receiver, Constants.DEFAULT_TIMEOUT);
    }

    /**
     * 发送消息
     */
    @Override
    public void sendMessage(String msg, String sender, String receiver) {
        String router = RouterBuilder.buildMessage(sender, receiver);
        NPack pack = new NPack(router, msg.getBytes());
        context.writeAndFlush(pack);
    }

    /**
     * 回调模式 . 出现超时再处理
     */
    @Override
    public void senderMessageWithThrowable(String msg, String sender, String receiver, Consumer<TimeOutException> consumer) {
        try {
            sendMessageBySync(msg, sender, receiver);
        } catch (TimeOutException e) {
            consumer.accept(e);
        }
    }

    /**
     * 发送消息
     *
     * @param timeout 超时时间
     */
    public Response sendMessageBySync(String msg, String sender, String receiver, long timeout) throws TimeOutException {
        // 为了更加精准 , 我们使用了 纳秒作为ID , 时间统一
        int id = NpackFuture.getCount();
        // 1.实例化router
        String router = RouterBuilder.buildMessageWithACK(sender, receiver, id, timeout);
        NPack pack = new NPack(router, msg.getBytes());
        NpackFuture future = new NpackFuture(id, timeout, pack);
        context.writeAndFlush(pack);
        return future.get();
    }


    /**
     * RPC,必须制定超时时间,构造器
     *
     * @throws TimeOutException
     */
    @Override
    public Object invoke(Class<?> clazz, Method method, long timeout, Object... args) throws TimeOutException {
        if (clazz.isInterface()) {
            // clazz -> name
            String path = clazz.getName();

            // name  -> echo.java.util.Map.java.util.List ,比如echo
            String name = ArgsUtil.getMethodName(method);
            // 机器唯一ID
            int id = NpackFuture.getCount();
            // 路由信息
            String router = RouterBuilder.buildRPC(path, name, id, timeout);

            // 参数信息, 转成json
            String json = ArgsUtil.convertArgs(args);
            NPack pack;
            long start = System.currentTimeMillis();
            if (json == null) {
                pack = new NPack(router, start);
            } else {
                pack = new NPack(router, json.getBytes(), start);
            }
            // 1.先实例化对象
            NpackFuture future = new NpackFuture(id, timeout, pack);
            // 2.然后发送 , 1,2顺序不能改变
            context.writeAndFlush(pack);
            // 3.然后再去阻塞的get结果
            byte[] result = future.get().getResult();
            if (result == null || result.length == 0) {
                // 即无结果,返回void
                return null;
            }
            Class<?> returnType = method.getReturnType();
            return JSON.parseObject(result, returnType);
        } else {
            throw new RuntimeException(String.format("代理%s对象应该是一个接口对象.", clazz));
        }
    }


    public Consumer<Response> getFallback() {
        return fallback;
    }

    public void setFallback(Consumer<Response> fallback) {
        this.fallback = fallback;
    }

    @Override
    public void onBootstrap() {
        // 啥也不做
    }

    @Override
    public void onShutdown() {
        // 啥也不做
    }
}