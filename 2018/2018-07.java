import java.util.concurrent.*;

class Problem {
    // felles data og metoder A
    static int num = 3;
    CyclicBarrier b = new CyclicBarrier(num);

    public static void main(String[] args) {
        Problem p = new Problem();
        p.utfoer(num + 1); // num+1 == 4
        System.out.println(" Main TERMINATED");
    } // end main

    void utfoer(int antT) {
        Thread[] t = new Thread[antT];
        for (int i = 0; i < antT; i++)
            (t[i] = new Thread(new Arbeider(i))).start();
        try {
            for (int i = 0; i < antT; i++)
                t[i].join();
        } catch (Exception e) {
        }
    } // end utfoer

    class Arbeider implements Runnable {
        // lokale data og metoder B
        int ind;

        void sync() {
            try {
                b.await();
            } catch (Exception e) {
                return;
            }
        }

        public Arbeider(int in) {
            ind = in;
        };

        public void run() {
            // kalles naar traaden er startet
            if (ind == 1)
                try {
                    TimeUnit.SECONDS.sleep(2);
                } catch (Exception e) {
                    return;
                }
            ;
            sync();
            System.out.println("A" + ind);
            sync();
            System.out.println("B" + ind);
        } // end run
    } // end indre klasse Arbeider
} // end class Problem