package org.obapanel.jedis.interruptinglocks.functional;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.obapanel.jedis.interruptinglocks.JedisLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;

import static org.junit.Assert.assertFalse;
import static org.obapanel.jedis.interruptinglocks.functional.JedisTestFactory.*;


public class FunctionalLocksOnCriticalZoneTest {


    private static final Logger log = LoggerFactory.getLogger(FunctionalLocksOnCriticalZoneTest.class);


    private AtomicBoolean intoCriticalZone = new AtomicBoolean(false);
    private AtomicBoolean errorInCriticalZone = new AtomicBoolean(false);
    private AtomicBoolean otherError = new AtomicBoolean(false);
    private String lockName;
    private List<Lock> lockList = new ArrayList<>();


    @Before
    public void before() {
        org.junit.Assume.assumeTrue(functionalTestEnabled());
        lockName = "lock:" + this.getClass().getName() + ":" + System.currentTimeMillis();
    }

    @After
    public void after() {

    }

    @Test
    public void testIfInterruptedFor5SecondsLock() throws InterruptedException {
        for(int i = 0; i < FUNCTIONAL_TEST_CYCLES; i++) {
            intoCriticalZone.set(false);
            errorInCriticalZone.set(false);
            otherError.set(false);
            log.info("_\n");
            log.info("i {}", i);
            Thread t1 = new Thread(() -> accesLockOfCriticalZone(1));
            t1.setName("prueba_t1");
            Thread t2 = new Thread(() -> accesLockOfCriticalZone(7));
            t2.setName("prueba_t2");
            Thread t3 = new Thread(() -> accesLockOfCriticalZone(3));
            t3.setName("prueba_t3");
            List<Thread> threadList = Arrays.asList(t1,t2,t3);
            Collections.shuffle(threadList);
            threadList.forEach(Thread::start);
            t1.join();
            t2.join();
            t3.join();
            assertFalse(errorInCriticalZone.get());
            assertFalse(otherError.get());
            assertFalse(lockList.stream().anyMatch(il -> il != null && ((org.obapanel.jedis.interruptinglocks.Lock) il).isLocked()));
        }
    }

    private void accesLockOfCriticalZone(int sleepTime) {
        try {
            Jedis jedis = JedisTestFactory.createJedisClient();
            Lock lock = new JedisLock(jedis, lockName).asConcurrentLock();
            lockList.add(lock);
            lock.lock();
            checkLock(lock);
            accessCriticalZone(sleepTime);
            lock.unlock();
            jedis.quit();
        }catch (Exception e) {
            log.error("Other error ", e);
            otherError.set(true);
        }
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
