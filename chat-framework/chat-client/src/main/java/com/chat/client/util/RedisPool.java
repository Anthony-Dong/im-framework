package com.chat.client.util;


import com.chat.core.exception.JedisException;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;

import java.util.concurrent.ArrayBlockingQueue;

/**
 *  由于 redis' 的部分原因 , 需要将 redis 的依赖导一下 , 依赖于这个包的
 *
 * @date:2019/11/11 12:01
 * @author: <a href='mailto:fanhaodong516@qq.com'>Anthony</a>
 */

public class RedisPool {

    private ThreadLocal<Jedis> threadLocal;

    private RedisConnectionQueue queue;

    public RedisPool(Integer capacity, HostAndPort hostAndPort) {
        queue = new RedisConnectionQueue(capacity, hostAndPort);
        this.threadLocal = new ThreadLocal<>();
    }


    public Jedis get() throws Exception {
        if (null == threadLocal.get()) {
            threadLocal.set(queue.takeConnection());
        }
        return threadLocal.get();
    }


    public void remove(Jedis jedis) throws Exception {
        if (null != threadLocal.get()) {
            threadLocal.remove();
            queue.putConnection(jedis);
        }
    }

    public class RedisConnectionQueue {

        private Integer capacity;

        private Boolean useFlag;

        private ArrayBlockingQueue<Jedis> queue;

        private HostAndPort hostAndPort;


        public RedisConnectionQueue(Integer capacity, HostAndPort hostAndPort) {
            this.capacity = capacity;
            this.hostAndPort = hostAndPort;
            this.queue = new ArrayBlockingQueue<>(capacity);
            for (Integer integer = 0; integer < capacity; integer++) {
                this.queue.offer(new Jedis(hostAndPort));
            }
        }

        public Jedis takeConnection() throws Exception{
            Jedis connection = null;
            try {
                connection = queue.take();
                return connection;
            } catch (InterruptedException e) {
                throw new JedisException("take 失败");
            }
        }

        public void putConnection(Jedis connection)  throws Exception{
            try {
                queue.put(connection);
            } catch (InterruptedException e) {
                throw new JedisException("put失败");
            }
        }
    }
}
