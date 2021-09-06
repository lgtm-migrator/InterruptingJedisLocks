package org.obapanel.jedis.countdownlatch.functional;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.obapanel.jedis.countdownlatch.JedisAdvancedCountDownLatch;
import org.obapanel.jedis.countdownlatch.JedisCountDownLatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.obapanel.jedis.countdownlatch.functional.JedisTestFactory.createJedisClient;
import static org.obapanel.jedis.countdownlatch.functional.JedisTestFactory.createJedisPool;
import static org.obapanel.jedis.countdownlatch.functional.JedisTestFactory.functionalTestEnabled;


public class FunctionalJedisAdvancedCountDownLatchTest {

    private static final Logger LOG = LoggerFactory.getLogger(FunctionalJedisAdvancedCountDownLatchTest.class);

    private String countDownLatch;
    private Jedis jedis;
    private JedisPool jedisPool;

    @Before
    public void before() {
        org.junit.Assume.assumeTrue(functionalTestEnabled());
        if (!functionalTestEnabled()) return;
        countDownLatch = "countDownLatch:" + this.getClass().getName() + ":" + System.currentTimeMillis();
        jedis = createJedisClient();
        jedisPool = createJedisPool();
    }

    @After
    public void after() {
        if (!functionalTestEnabled()) return;
        if (jedis != null) jedis.close();
        if (jedisPool != null) jedisPool.close();
    }



    @Test
    public void waitTest(){
        final AtomicBoolean awaitDone = new AtomicBoolean(false);
        final Thread t1 = new Thread(() -> {
            try {
                JedisAdvancedCountDownLatch jedisCountDownLatch1 = new JedisAdvancedCountDownLatch(jedisPool, countDownLatch,1);
                jedisCountDownLatch1.await();
                awaitDone.set(true);
            } catch (Exception e) {
                LOG.error("Error in thread 1", e);
            }
        });
        t1.setName("T1");
        t1.setDaemon(true);
        final Thread t2 = new Thread(() -> {
            try {
                JedisAdvancedCountDownLatch jedisCountDownLatch2 = new JedisAdvancedCountDownLatch(jedisPool, countDownLatch,1);
                Thread.sleep(500);
                jedisCountDownLatch2.countDown();
            } catch (Exception e) {
                LOG.error("Error in thread 2", e);
            }
        });
        t2.setName("T2");
        t2.setDaemon(true);
        t1.start();
        t2.start();
        try {
            t2.join(1000);
            t1.join(1000);
            Thread.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertTrue(awaitDone.get());
        assertTrue( 0L ==  new JedisCountDownLatch(jedisPool, countDownLatch,1).getCount());
    }

    @Test
    public void dontWaitTest(){
        final AtomicBoolean awaitDone = new AtomicBoolean(false);
        final Thread t1 = new Thread(() -> {
            try {
                JedisAdvancedCountDownLatch jedisCountDownLatch1 = new JedisAdvancedCountDownLatch(jedisPool, countDownLatch,1);
                jedisCountDownLatch1.await();
                awaitDone.set(true);
            } catch (Exception e) {
                LOG.error("Error in thread 1", e);
            }
        });
        t1.setName("T1");
        t1.setDaemon(true);
        final Thread t2 = new Thread(() -> {
            try {
                JedisAdvancedCountDownLatch jedisCountDownLatch2 = new JedisAdvancedCountDownLatch(jedisPool, countDownLatch,1);
                Thread.sleep(2500);
                jedisCountDownLatch2.countDown();
            } catch (Exception e) {
                LOG.error("Error in thread 2", e);
            }
        });
        t2.setName("T2");
        t2.setDaemon(true);
        t1.start();
        t2.start();
        try {
            t2.join(200);
            t1.join(200);
            Thread.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertFalse(awaitDone.get());
        assertTrue( 1L ==  new JedisCountDownLatch(jedisPool, countDownLatch,1).getCount());
    }


    @Test(expected = IllegalArgumentException.class)
    public void badInit(){
        new JedisCountDownLatch(jedisPool, countDownLatch,-1);
    }



}
