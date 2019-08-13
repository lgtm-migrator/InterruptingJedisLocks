package org.obapanel.jedis.interruptinglocks;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertFalse;


public class JedisLocksOnCriticalZoneTestWithConnectionPool {


    private static final Logger log = LoggerFactory.getLogger(JedisLocksOnCriticalZoneTestWithConnectionPool.class);


    private AtomicBoolean intoCriticalZone = new AtomicBoolean(false);
    private AtomicBoolean errorInCriticalZone = new AtomicBoolean(false);
    private JedisPool jedisPool;
    private String lockName;
    private List<JedisLock> lockList = new ArrayList<>();


    @Before
    public void before() {
        jedisPool = JedisTestFactory.createJedisPool();
        lockName = "lock:" + this.getClass().getName() + ":" + System.currentTimeMillis();
    }

    @After
    public void after() {
        jedisPool.close();
    }

    @Test
    public void testIfInterruptedFor5SecondsLock() throws InterruptedException {
        Thread t1 = new Thread(() -> accesLockOfCriticalZone(1));
        t1.setName("prueba_t1");
        Thread t2 = new Thread(() -> accesLockOfCriticalZone(7));
        t2.setName("prueba_t2");
        Thread t3 = new Thread(() -> accesLockOfCriticalZone(3));
        t3.setName("prueba_t3");
        t1.start();
        t2.start();
        t3.start();
        Thread.sleep(TimeUnit.SECONDS.toMillis(5));
        t1.join();
        t2.join();
        t3.join();
        assertFalse(errorInCriticalZone.get());
        assertFalse(lockList.stream().anyMatch(il ->  il != null && il.isLocked() ));


        }

    private void accesLockOfCriticalZone(int sleepTime) {
        Jedis jedis = jedisPool.getResource();
        JedisLock jedisLock = new JedisLock(jedis,lockName);
        lockList.add(jedisLock);
        jedisLock.lock();
        accessCriticalZone(sleepTime);
        jedisLock.unlock();
        jedis.quit();
    }

    private void accessCriticalZone(int sleepTime){
        if (intoCriticalZone.get()) {
            errorInCriticalZone.set(true);
            throw new IllegalStateException("Other thread is here");
        }
        intoCriticalZone.set(true);
        try {
            Thread.sleep(TimeUnit.SECONDS.toMillis(sleepTime));
        } catch (InterruptedException e) {

        }
        intoCriticalZone.set(false);
    }
}
