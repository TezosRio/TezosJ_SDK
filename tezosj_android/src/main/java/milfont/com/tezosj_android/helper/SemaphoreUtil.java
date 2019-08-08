package milfont.com.tezosj_android.helper;

import java.util.concurrent.Semaphore;

public class SemaphoreUtil {

    public interface SemaphoreCallback {
        void execute(final Semaphore semaphore);
    }

    public static void executeSemaphoreCallback(SemaphoreCallback callback) {
        final Semaphore semaphore = new Semaphore(0);
        callback.execute(semaphore);
        semaphore.acquireUninterruptibly();
    }

}

