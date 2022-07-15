import java.util.concurrent.*;

class Problem {
    // felles data og metoder A
    static int num = 2;
    static int extra = 2;
    static CyclicBarrier b;

    public static void main(String[] args) {
        Problem p = new Problem();
        num = Integer.parseInt(args[0]);
        extra = Integer.parseInt(args[1]);
        b = new CyclicBarrier(num);
        p.utfoer(num + extra); // extra threads
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
            sync();
            System.out.println("A" + ind);
            sync();
            System.out.println("B" + ind);
        } // end run
    } // end indre klasse Arbeider
} // end class Problem