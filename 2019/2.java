import java.util.concurrent.*;

class CyclicBarrierJoinP {
    static CyclicBarrier cb;

public static void done() {
XXXX
}

public static void main(String[] args) {
int numberofthreads = 10;
Thread[] t = new Thread[numberofthreads];
cb = XXXX;
for (int j = 0; j < numberofthreads; j++) {
(t[j] = new Thread( new ExThread() )).start();
}
// try {
// for (int k = 0; k < numberofthreads; k++) t[k].join();
// } catch (Exception e) { return; }
XXXX;
}

    static class ExThread implements Runnable {
        public void run() {
            try {
                TimeUnit.SECONDS.sleep(10);
            } catch (Exception e) {
                return;
            }
            ;
            done();
        }
    }
}