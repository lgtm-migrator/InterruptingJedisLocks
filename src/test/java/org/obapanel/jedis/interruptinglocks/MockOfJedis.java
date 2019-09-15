package org.obapanel.jedis.interruptinglocks;

import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Response;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.params.SetParams;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import static org.mockito.ArgumentMatchers.*;

/**
 * Mock of jedis methods used by the lock
 */
public class MockOfJedis {

    private static final Logger log = LoggerFactory.getLogger(MockOfJedis.class);

    public static final String CLIENT_RESPONSE_OK = "OK";
    public static final String CLIENT_RESPONSE_KO = "KO";


    // Zero to prevent some integration test
    // One to one pass
    // More to more passes
    public static final int INTEGRATION_TEST_CYCLES = 5;

    static boolean integrationTestEnabled(){
        return INTEGRATION_TEST_CYCLES > 0;
    }


    static boolean checkLock(IJedisLock jedisLock){
        log.info("interruptingLock.isLocked() " + jedisLock.isLocked() + " for thread " + Thread.currentThread().getName());
        if (jedisLock.isLocked()) {
            log.debug("LOCKED");
            return true;
        } else {
            IllegalStateException ise =  new IllegalStateException("LOCK NOT ADQUIRED isLocked " + jedisLock.isLocked());
            log.error("ERROR LOCK NOT ADQUIRED e {} ", ise.getMessage(), ise);
            throw ise;
        }
    }

    static boolean checkLock(java.util.concurrent.locks.Lock lock){
        if (lock instanceof  org.obapanel.jedis.interruptinglocks.Lock) {
            org.obapanel.jedis.interruptinglocks.Lock jedisLock = (org.obapanel.jedis.interruptinglocks.Lock) lock;
            log.info("interruptingLock.isLocked() " + jedisLock.isLocked() + " for thread " + Thread.currentThread().getName());
            if (jedisLock.isLocked()) {
                log.debug("LOCKED");
                return true;
            } else {
                IllegalStateException ise =  new IllegalStateException("LOCK NOT ADQUIRED isLocked " + jedisLock.isLocked());
                log.error("ERROR LOCK NOT ADQUIRED e {} ", ise.getMessage(), ise);
                throw ise;
            }
        } else {
            return true;
        }
    }

    private Jedis jedis;
    private Transaction transaction;
    private Map<String, String> data = Collections.synchronizedMap(new HashMap<>());
    private Timer timer;

    public MockOfJedis() {
        timer = new Timer();
        jedis = Mockito.mock(Jedis.class);
        transaction = Mockito.mock(Transaction.class);
        Mockito.when(jedis.set(anyString(), anyString(), any(SetParams.class))).thenAnswer(ioc -> {
            String key = ioc.getArgument(0);
            String value = ioc.getArgument(1);
            SetParams setParams = ioc.getArgument(2);
            return mockSet(key, value, setParams);

        });
        Mockito.when(jedis.get(anyString())).thenAnswer(ioc -> {
            String key = ioc.getArgument(0);
            return mockGet(key);
        });
        Mockito.when(jedis.eval(anyString(),any(List.class), any(List.class))).thenAnswer(ioc -> {
            String script = ioc.getArgument(0);
            List<String> keys = ioc.getArgument(1);
            List<String> values = ioc.getArgument(2);
            return mockEval(script, keys, values);
        });
        Mockito.when(jedis.multi()).thenReturn(transaction);
        Mockito.when(transaction.set(anyString(), anyString(), any(SetParams.class))).thenAnswer(ioc -> {
            String key = ioc.getArgument(0);
            String value = ioc.getArgument(1);
            SetParams setParams = ioc.getArgument(2);
            String s = mockSet(key, value, setParams);
            return createMockResponse(s);
        });
        Mockito.when(transaction.get(anyString())).thenAnswer(ioc -> {
            String key = ioc.getArgument(0);
            Object o = mockGet(key);
            return createMockResponse(o);
        });

    }

    private synchronized Object mockGet(String key) {
        return data.get(key);
    }

    private synchronized Object mockEval(String script, List<String> keys, List<String> values) {
        Object response = null;
        if (script.equalsIgnoreCase(JedisLock.UNLOCK_LUA_SCRIPT)) {
            if (values.get(0).equalsIgnoreCase(data.get(keys.get(0)))){
                String removed = data.remove(keys.get(0));
                response = removed != null ? 1 : 0;
            }
        }
        return response;
    }

    private synchronized String mockSet(final String key, String value, SetParams setParams) {
        boolean insert = true;
        if (isSetParamsNX(setParams)) {
            insert = !data.containsKey(key);
        }
        if (insert) {
            data.put(key, value);
            Long expireTime = getExpireTimePX(setParams);
            if (expireTime != null){
                timer.schedule(wrapTTL(() -> { data.remove(key);}),expireTime);
            }
            return  CLIENT_RESPONSE_OK;
        } else {
            return  CLIENT_RESPONSE_KO;
        }
    }

    private static <T> Response<T> createMockResponse(T data) {
        Response<T> response = Mockito.mock(Response.class);
        Mockito.when(response.get()).thenReturn(data);
        return  response;
    }

    public Jedis getJedis(){
        return jedis;
    }

    public synchronized void clearData(){
        data.clear();
    }


    public synchronized Map<String,String> getCurrentData() {
        return new HashMap<>(data);
    }

    boolean isSetParamsNX(SetParams setParams) {
        boolean result = false;
        for(byte[] b: setParams.getByteParams()){
            String s = new String(b);
            if ("nx".equalsIgnoreCase(s)){
                result = true;
            }
        }
        return result;
    }

    Long getExpireTimePX(SetParams setParams) {
        return (Long) setParams.getParam("px");
    }


    public static String getJedisLockValue(JedisLock jedisLock) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method privateMethod = JedisLock.class.getDeclaredMethod("getValue", null);
        privateMethod.setAccessible(true);
        String returnValue = (String) privateMethod.invoke(jedisLock, null);
        return returnValue;
    }

    private static TimerTask wrapTTL(Runnable r) {
        return new TTL(r);
    }

    // TimerTaskLamda
    private static class TTL extends TimerTask {

        private Runnable runnable;

        public TTL(Runnable runnable) {
            this.runnable = runnable;
        }

        @Override
        public void run() {
            runnable.run();
        }
    }



}
