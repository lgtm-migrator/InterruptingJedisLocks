package org.obapanel.jedis.countdownlatch.functional;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.obapanel.jedis.common.test.JedisTestFactory;
import org.obapanel.jedis.countdownlatch.JedisCountDownLatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;



public class FunctionalJedisCountDownLatchTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(FunctionalJedisCountDownLatchTest.class);

    private final JedisTestFactory jtfTest = JedisTestFactory.get();

    private String countDownLatch;
    private Jedis jedis;
    private JedisPool jedisPool;

    @Before
    public void before() {
        org.junit.Assume.assumeTrue(jtfTest.functionalTestEnabled());
        if (!jtfTest.functionalTestEnabled()) return;
        countDownLatch = "countDownLatch:" + this.getClass().getName() + ":" + System.currentTimeMillis();
        jedis = jtfTest.createJedisClient();
        jedisPool = jtfTest.createJedisPool();
    }

    @After
    public void after() {
        if (!jtfTest.functionalTestEnabled()) return;
        if (jedis != null) jedis.close();
        if (jedisPool != null) jedisPool.close();
    }



    @Test
    public void waitTest(){
        final AtomicBoolean awaitDone = new AtomicBoolean(false);
        final Thread t1 = new Thread(() -> {
            try {
                JedisCountDownLatch jedisCountDownLatch1 = new JedisCountDownLatch(jedisPool, countDownLatch,1).
                        withWaitingTimeMilis(100);
                jedisCountDownLatch1.await();
                awaitDone.set(true);
            } catch (InterruptedException e) {
                LOGGER.error("Error in thread 1", e);
            }
        });
        t1.setName("T1");
        t1.setDaemon(true);
        final Thread t2 = new Thread(() -> {
            try {
                JedisCountDownLatch jedisCountDownLatch2 = new JedisCountDownLatch(jedisPool, countDownLatch,1).
                        withWaitingTimeMilis(100);
                Thread.sleep(500);
                jedisCountDownLatch2.countDown();
            } catch (Exception e) {
                LOGGER.error("Error in thread 2", e);
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
                JedisCountDownLatch jedisCountDownLatch1 = new JedisCountDownLatch(jedisPool, countDownLatch,1).
                        withWaitingTimeMilis(100);
                jedisCountDownLatch1.await();
                awaitDone.set(true);
            } catch (InterruptedException e) {
                LOGGER.error("Error in thread 1", e);
            }
        });
        t1.setName("T1");
        t1.setDaemon(true);
        final Thread t2 = new Thread(() -> {
            try {
                JedisCountDownLatch jedisCountDownLatch2 = new JedisCountDownLatch(jedisPool, countDownLatch,1).
                        withWaitingTimeMilis(100);
                Thread.sleep(2500);
                jedisCountDownLatch2.countDown();
            } catch (Exception e) {
                LOGGER.error("Error in thread 2", e);
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

    @Test
    public void waitTestForAWhile(){
        final AtomicBoolean awaitDone = new AtomicBoolean(false);
        final AtomicBoolean awaitZero = new AtomicBoolean(false);
        final Thread t1 = new Thread(() -> {
            try {
                JedisCountDownLatch jedisCountDownLatch1 = new JedisCountDownLatch(jedisPool, countDownLatch,1).
                        withWaitingTimeMilis(100);
                boolean reachedZero = jedisCountDownLatch1.await(1000, TimeUnit.MILLISECONDS);
                awaitZero.set(reachedZero);
                awaitDone.set(true);
            } catch (InterruptedException e) {
                LOGGER.error("Error in thread 1", e);
            }
        });
        t1.setName("T1");
        t1.setDaemon(true);
        final Thread t2 = new Thread(() -> {
            try {
                JedisCountDownLatch jedisCountDownLatch2 = new JedisCountDownLatch(jedisPool, countDownLatch,1).
                        withWaitingTimeMilis(100);
                Thread.sleep(500);
                jedisCountDownLatch2.countDown();
            } catch (Exception e) {
                LOGGER.error("Error in thread 2", e);
            }
        });
        t2.setName("T2");
        t2.setDaemon(true);
        t1.start();
        t2.start();
        try {
            t2.join(1200);
            t1.join(1200);
            Thread.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertTrue(awaitDone.get());
        assertTrue(awaitZero.get());
        assertTrue( 0L ==  new JedisCountDownLatch(jedisPool, countDownLatch,1).getCount());
    }

    @Test
    public void dontwaitTestForAWhile(){
        final AtomicBoolean awaitDone = new AtomicBoolean(false);
        final AtomicBoolean awaitZero = new AtomicBoolean(false);
        final Thread t1 = new Thread(() -> {
            try {
                JedisCountDownLatch jedisCountDownLatch1 = new JedisCountDownLatch(jedisPool, countDownLatch,1).
                        withWaitingTimeMilis(100);
                LOGGER.debug("Thread1 - Created jedisCountDownLatch1");
                boolean reachedZero = jedisCountDownLatch1.await(500, TimeUnit.MILLISECONDS);
                LOGGER.debug("Thread1 - reachedZero jedisCountDownLatch1 reachedZero {}", reachedZero);
                awaitZero.set(reachedZero);
                awaitDone.set(true);
            } catch (InterruptedException e) {
                LOGGER.error("Error in thread 1", e);
            }
        });
        t1.setName("T1");
        t1.setDaemon(true);
        final Thread t2 = new Thread(() -> {
            try {
                JedisCountDownLatch jedisCountDownLatch2 = new JedisCountDownLatch(jedisPool, countDownLatch,1).
                        withWaitingTimeMilis(100);
                LOGGER.debug("Thread2 - Created jedisCountDownLatch2");
                Thread.sleep(2000);
                LOGGER.debug("Thread2 - countdown before");
                jedisCountDownLatch2.countDown();
                LOGGER.debug("Thread2 - countdown after");
            } catch (Exception e) {
                LOGGER.error("Error in thread 2", e);
            }
        });
        t2.setName("T2");
        t2.setDaemon(true);
        t1.start();
        t2.start();
        try {
            t2.join(2100);
            t1.join(1200);
            Thread.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertTrue(awaitDone.get());
        assertFalse(awaitZero.get());
        assertTrue( 0L ==  new JedisCountDownLatch(jedisPool, countDownLatch,1).getCount());
    }

    @Test
    public void dontwaitTestForALongWhile(){
        final AtomicBoolean awaitDone = new AtomicBoolean(false);
        final AtomicBoolean awaitZero = new AtomicBoolean(false);
        final Thread t1 = new Thread(() -> {
            try {
                JedisCountDownLatch jedisCountDownLatch1 = new JedisCountDownLatch(jedisPool, countDownLatch,1).
                        withWaitingTimeMilis(100);
                LOGGER.debug("Thread1 - Created jedisCountDownLatch1");
                boolean reachedZero = jedisCountDownLatch1.await(1500, TimeUnit.MILLISECONDS);
                LOGGER.debug("Thread1 - reachedZero jedisCountDownLatch1 reachedZero {}", reachedZero);
                awaitZero.set(reachedZero);
                awaitDone.set(true);
            } catch (InterruptedException e) {
                LOGGER.error("Error in thread 1", e);
            }
        });
        t1.setName("T1");
        t1.setDaemon(true);
        final Thread t2 = new Thread(() -> {
            try {
                JedisCountDownLatch jedisCountDownLatch2 = new JedisCountDownLatch(jedisPool, countDownLatch,1).
                        withWaitingTimeMilis(100);
                LOGGER.debug("Thread2 - Created jedisCountDownLatch2");
                Thread.sleep(2500);
                LOGGER.debug("Thread2 - countdown before");
                jedisCountDownLatch2.countDown();
                LOGGER.debug("Thread2 - countdown after");
            } catch (Exception e) {
                LOGGER.error("Error in thread 2", e);
            }
        });
        t2.setName("T2");
        t2.setDaemon(true);
        t1.start();
        t2.start();
        try {
            t2.join(500);
            t1.join(500);
            Thread.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertFalse(awaitDone.get());
        assertFalse(awaitZero.get());
        assertTrue( 1L ==  new JedisCountDownLatch(jedisPool, countDownLatch,1).getCount());
    }

    @Test(expected = IllegalArgumentException.class)
    public void badInit(){
        new JedisCountDownLatch(jedisPool, countDownLatch,-1);
    }



}
