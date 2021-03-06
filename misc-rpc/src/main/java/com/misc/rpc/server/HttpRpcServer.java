package com.misc.rpc.server;

import com.misc.core.netty.NettyServer;
import com.misc.core.proto.http.HttpCodecProvider;
import com.misc.rpc.core.RpcRequest;
import com.misc.rpc.core.RpcResponse;
import com.misc.rpc.proto.HttpServerConvertHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;

/**
 * http
 *
 * @date: 2020-05-16
 * @author: <a href='mailto:fanhaodong516@qq.com'>Anthony</a>
 */
public class HttpRpcServer extends NettyServer.Builder<FullHttpRequest, FullHttpResponse, RpcRequest, RpcResponse> {

    private RpcServerConfig rpcServerConfig;

    private HttpRpcServer(RpcServerConfig rpcServerConfig) {
        this.rpcServerConfig = rpcServerConfig;
    }


    protected void init() {
        super.setNettyCodecProvider(new HttpCodecProvider());
        super.setNettyConvertHandler(new HttpServerConvertHandler(rpcServerConfig));
        super.setNettyEventListener(new RpcServerHandler());
    }


    public static void run(RpcServerConfig rpcServerConfig) throws Throwable {
        new HttpRpcServer(rpcServerConfig).build().start().sync();
    }

}
