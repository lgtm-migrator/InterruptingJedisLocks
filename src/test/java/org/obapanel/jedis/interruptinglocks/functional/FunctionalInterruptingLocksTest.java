package org.obapanel.jedis.interruptinglocks.functional;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.obapanel.jedis.common.test.JedisTestFactory;
import org.obapanel.jedis.interruptinglocks.InterruptingJedisJedisLockBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisPool;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.obapanel.jedis.interruptinglocks.functional.JedisTestFactoryLocks.checkLock;


public class FunctionalInterruptingLocksTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(FunctionalInterruptingLocksTest.class);

    private final JedisTestFactory jtfTest = JedisTestFactory.get();

    private String lockName;
    private JedisPool jedisPool;


    @Before
    public void before() {
        org.junit.Assume.assumeTrue(jtfTest.functionalTestEnabled());
        if (!jtfTest.functionalTestEnabled()) return;
        jedisPool = jtfTest.createJedisPool();
        lockName = "lock:" + this.getClass().getName() + ":" + System.currentTimeMillis();
    }

    @After
    public void after() {
        if (!jtfTest.functionalTestEnabled()) return;
        if (jedisPool != null) jedisPool.close();
    }

    @Test
    public void testIfInterruptedFor5SecondsLock() {
        for(int i = 0; i < jtfTest.getFunctionalTestCycles(); i++) {
            LOGGER.info("_\n");
            LOGGER.info("i {}", i);
            boolean wasInterruptedFor3Seconds = wasInterrupted(3);
            LOGGER.info("i {} wasInterruptedFor3Seconds {}", i, wasInterruptedFor3Seconds);
            boolean wasInterruptedFor7Seconds = wasInterrupted(7);
            LOGGER.info("i {} wasInterruptedFor7Seconds {}", i, wasInterruptedFor7Seconds);
            boolean wasInterruptedFor1Seconds = wasInterrupted(1);
            LOGGER.info("i {} wasInterruptedFor1Seconds {}", i, wasInterruptedFor1Seconds);
            boolean wasInterruptedFor9Seconds = wasInterrupted(9);
            LOGGER.info("i {} wasInterruptedFor9Seconds {}", i, wasInterruptedFor9Seconds);
            assertFalse(wasInterruptedFor3Seconds);
            assertTrue(wasInterruptedFor7Seconds);
            assertFalse(wasInterruptedFor1Seconds);
            assertTrue(wasInterruptedFor9Seconds);
        }

    }


    private boolean wasInterrupted(int sleepSeconds){
        boolean wasInterrupted = false;
        boolean wasLocked = false;
        InterruptingJedisJedisLockBase interruptingJedisLock = new InterruptingJedisJedisLockBase(jedisPool, lockName, 5, TimeUnit.SECONDS);
        interruptingJedisLock.lock();
        wasLocked = checkLock(interruptingJedisLock);
        try {
            Thread.sleep(TimeUnit.SECONDS.toMillis(sleepSeconds));
        } catch (InterruptedException e) {
            wasInterrupted = true;
        }
        interruptingJedisLock.unlock();
        LOGGER.info("thread wasLocked " + wasLocked + " wasInterrupted " + wasInterrupted + " thread " + Thread.currentThread().getName());
        return wasInterrupted && wasLocked;
    }
}