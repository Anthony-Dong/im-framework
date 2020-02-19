package com.chat.server.spi.defaulthandler;

import com.chat.server.rpc.RpcMap;

/**
 * @date:2020/2/18 20:34
 * @author: <a href='mailto:fanhaodong516@qq.com'>Anthony</a>
 */
public class RpcMapBuilder {

    static final RpcMap map = new RpcMap();

    public static void addService(Class<?> service, Object proxy) {
        map.addService(service, proxy);
    }
}