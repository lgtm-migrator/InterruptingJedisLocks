package org.obapanel.jedis.common.test;


import org.obapanel.jedis.semaphore.MockOfJedis;

import java.util.TimerTask;


public class TTL extends TimerTask {


    public static TimerTask wrapTTL(Runnable r) {
        return new TTL(r);
    }


    private final Runnable runnable;

    public TTL(Runnable runnable) {
        this.runnable = runnable;
    }

    @Override
    public void run() {
        runnable.run();
    }
}



