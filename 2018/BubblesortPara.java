import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import java.util.concurrent.atomic.*;

///--------------------------------------------------------
// Fil: BubblesortPara.java
// Implements Sequential and Parallel BubbleSort
// written by: Eric Jul, University of Oslo, 2018
//
//--------------------------------------------------------
public class BubblesortPara {
  CyclicBarrier readyToGo,
      allDone;
  Semaphore lockEntryRegion;
  Semaphore[] sems;
  AtomicInteger passId;
  int[] arr;
  int segmentLength;

  BubblesortPara(int max) {
  } // end constructor BubblesortPara

  public void fillArr(int[] arr) {
    int seed = 123;
    int n = arr.length;
    Random r = new Random(seed);
    for (int i = 0; i < n; i++) {
      arr[i] = r.nextInt(n);
    }
  } // end fillArr

  public void bubblesort(int[] arr) {
    int n = arr.length;
    int temp;
    for (int i = 0; i < n; i++) {
      for (int j = 1; j < (n - i); j++) {
        if (arr[j - 1] > arr[j]) {
          // swap elements
          temp = arr[j - 1];
          arr[j - 1] = arr[j];
          arr[j] = temp;
        }
      }
    }
  }

  public static void main(String args[]) throws InterruptedException {
    BubblesortPara b;
    final int min = 1000;
    if (args.length != 2) {
      System.out.println("use: >java BubblesortPara <Max> <threadCount>");
      System.exit(0);
    }
    int num = Integer.parseInt(args[0]);
    int threadCount = Integer.parseInt(args[1]);
    if (!((num >= min) && (threadCount >= 2))) {
      System.out.println("Bad parameters: Max must be at least " + min + " and threadCount at least 2");
      System.exit(0);
    }
    ;
    b = new BubblesortPara(num);
    b.doit(b, num, threadCount);
  } // end main()

  void doit(BubblesortPara b, int num, int threadCount) throws InterruptedException {
    arr = new int[num];
    // Sequential version of Bubblesort
    b.fillArr(arr);
    long seqTime = System.nanoTime(); // Start sequential timing
    b.bubblesort(arr);
    seqTime = System.nanoTime() - seqTime;
    System.out.println("Bubblesort Sequential time " + (seqTime / 1000000.0) + " ms\n");
    // Parallel version
    b.fillArr(arr); // reset the array to original content.
    long paraTime = System.nanoTime();
    readyToGo = new CyclicBarrier(threadCount + 1); // includes main() thread
    allDone = new CyclicBarrier(threadCount + 1);
    lockEntryRegion = new Semaphore(1, true);
    // Divide the array into threadCount*10 segments
    // Create a Semaphore to protect each segment
    int segmentCount = threadCount * 10;
    segmentLength = num / segmentCount;
    // System.out.println("Segment length: "+segmentLength+" segment count:
    // "+segmentCount);
    sems = new Semaphore[segmentCount];
    passId = new AtomicInteger(num);
    for (int k = 0; k < segmentCount; k++)
      sems[k] = new Semaphore(1, true);
    // fill array.
    b.fillArr(arr);
    // start threads
    int lastThread = threadCount - 1;
    for (int i = 0; i < threadCount; i++) {
      // System.out.println("Starting thread "+i);
      new Thread(new Para(i, b, num)).start();
    }
    try {
      readyToGo.await(); // await all threads ready to execute
    } catch (Exception e) {
      return;
    }
    // Now the threads are doing their thing
    try {
      allDone.await(); // await all worker threads DONE
    } catch (Exception e) {
      return;
    }
    // Combine results
    paraTime = System.nanoTime() - paraTime;
    System.out
        .println("Bubblesort Parallel time " + (paraTime / 1000000.0) + " ms\nSpeedup " + seqTime * 1.0 / paraTime);
  }

  class Para implements Runnable {
    int ind;
    BubblesortPara b;
    int myPassId;
    int num,
        currentSeg,
        myLastSeg,
        temp;

    Para(int in, BubblesortPara b, int num) {
      ind = in;
      this.b = b;
      this.num = num;
    } // konstruktor

    public void run() { // Her er det som kjores i parallell:
      try {
        readyToGo.await(); // await all threads ready to execute
      } catch (Exception e) {
        return;
      }
      // ************************ Thread code for parallel part
      // *************************
      int currentSeg;
      // System.out.println("T"+ind);
      while (passId.get() > 1) {
        // enter entry region and grab next available pass
        try {
          lockEntryRegion.acquire();
        } catch (Exception e) {
          return;
        }
        myPassId = passId.getAndDecrement();
        // System.out.println("T"+ind+" got myPassId: "+myPassId);
        if (myPassId <= 1) {
          // others have done the work, so quit
          lockEntryRegion.release();
          break;
        }
        currentSeg = 0;
        try {
          sems[currentSeg].acquire();
        } catch (Exception e) {
          return;
        }
        // System.out.println("Seg length "+segmentLength);
        lockEntryRegion.release();
        // end of entry region - protected by lockEntryRegion
        // Now repeatedly bubble thru the segments
        myLastSeg = (myPassId - 1) / segmentLength;
        for (int s = 0; s <= myLastSeg; s++) {
          int j;
          // for each segment do the bubbling
          int segEnd = (s + 1) * segmentLength - 1;
          if (segEnd >= myPassId)
            segEnd = myPassId - 1;
          // System.out.println("T"+ind+" starting seg "+s+" segEnd: "+segEnd+" myLastSeg
          // "+myLastSeg);
          for (j = s * segmentLength + 1; j <= segEnd; j++) {
            if (arr[j - 1] > arr[j]) {
              // swap elements
              temp = arr[j - 1];
              arr[j - 1] = arr[j];
              arr[j] = temp;
            }
          }
          if (s < myLastSeg) {
            // Must handle overlap with next segment at boundary, so must lock both segments
            try {
              sems[s + 1].acquire();
            } catch (Exception e) {
              return;
            }
            if (arr[j - 1] > arr[j]) {
              // swap elements
              temp = arr[j - 1];
              arr[j - 1] = arr[j];
              arr[j] = temp;
            }
          }
          // done with this segment so release lock
          // System.out.println("T"+ind+" releasing seg "+s);
          sems[s].release();
        } // for each segment
      }
      // ************************ Thread specific code done
      // *****************************
      try {
        allDone.await(); // await all threads done
      } catch (Exception e) {
        return;
      }
    } // end run
  } // end class Para
} // end class BubblesortPara
