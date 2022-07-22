import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

///--------------------------------------------------------
// Fil: PrimeDesertPara.java
// Implements Sequential and Parallel Prime Desert finding
// written by: Eric Jul, University of Oslo, 2018
//
// Cloned from:
// Fil: EratosthenesSil.java
// implements bit-array (Boolean) for prime numbers
// written by: Arne Maus , Univ of Oslo, 2013, 2015
//
//
//--------------------------------------------------------
/**
 * Implements the bitArray of length 'bitLen' [0..bitLen ]
 * 1 - true (is prime number)
 * 0 - false
 * can be used up to 2 G Bits (integer range)
 */
public class PrimeDesertPara {
  byte[] bitArr;
  int bitLen;
  final int[] bitMask = {
      1,
      2,
      4,
      8,
      16,
      32,
      64,
      128
  };
  final int[] bitMask2 = {
      255 - 1,
      255 - 2,
      255 - 4,
      255 - 8,
      255 - 16,
      255 - 32,
      255 - 64,
      255 - 128
  };
  CyclicBarrier readyToGo,
      allDone;

  PrimeDesertPara(int max) {
    bitLen = max;
    bitArr = new byte[(bitLen / 16) + 1];
    setAllPrime();
    generatePrimesByEratosthenes();
  }

  public static void main(String args[]) {
    PrimeDesertPara sil;
    if (args.length != 2) {
      System.out.println("use: >java PrimeDesertPara <Max> <threadCount>");
      System.exit(0);
    }
    int num = Integer.parseInt(args[0]);
    int threadCount = Integer.parseInt(args[1]);
    if (!((num >= 5) && (threadCount >= 2))) {
      System.out.println("Bad parameters: Max must be at least 5 and threadCount at least 2");
      System.exit(0);
    }
    ;
    // Here we generate the sil sequentially as we assumme that you already have a
    // parallel
    // version of the sil generation from Oblig 3
    long silTime = System.nanoTime();
    sil = new PrimeDesertPara(num);
    silTime = System.nanoTime() - silTime;
    System.out.println("Sil sequential generation time: " + (silTime / 1000000.0) + " ms");
    // now do the Prime Desert calculations
    sil.doit(sil, num, threadCount);
  } // end main()

  void doit(PrimeDesertPara sil, int num, int threadCount) {
    ArrayList<int[]> deserts = new ArrayList<int[]>();
    readyToGo = new CyclicBarrier(threadCount + 1); // includes main() thread
    allDone = new CyclicBarrier(threadCount + 1);
    // Sequential version of PrimeDesert
    long seqTime = System.nanoTime(); // Start sequential timing
    int[] desert = new int[2];
    desert[0] = 2;
    desert[1] = 3;
    deserts.add(desert);
    int desertLength = (3 - 2);
    int nextSP,
        nextEP,
        lastP;
    lastP = sil.lastPrime();
    nextSP = 3;
    nextEP = sil.nextPrime(nextSP);
    while (nextEP <= lastP) {
      if ((nextEP - nextSP) > desertLength) {
        desert = new int[2];
        desert[0] = nextSP;
        desert[1] = nextEP;
        deserts.add(desert);
        desertLength = nextEP - nextSP;
      }
      nextSP = nextEP;
      nextEP = sil.nextPrime(nextSP);
    }
    ;
    seqTime = System.nanoTime() - seqTime;
    System.out.println("Prime Desert Sequential time " + (seqTime / 1000000.0) + " ms\n");
    sil.printDeserts(deserts);
    // Parallel version
    long paraTime = System.nanoTime();
    // Create a desert list for each thread
    ArrayList<ArrayList<int[]>> desertLists = new ArrayList<ArrayList<int[]>>();
    for (int i = 0; i < threadCount; i++) {
      desertLists.add(new ArrayList<int[]>());
    }
    // start threads
    int largestPrime = sil.lastPrime();
    int chunkSize = largestPrime / threadCount;
    int lastThread = threadCount - 1;
    for (int i = 0; i < threadCount; i++) {
      int from = i * chunkSize;
      int fromPrime = sil.nextPrime(from);
      int upto;
      if (i < lastThread) {
        upto = sil.nextPrime(from + chunkSize);
      } else
        upto = largestPrime;
      System.out.println("Starting thread " + i + " from " + fromPrime + " upto " + upto);
      new Thread(new Para(i, threadCount, sil, fromPrime, upto, desertLists.get(i))).start();
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
    int currentLength = 0;
    int len;
    ArrayList<int[]> currentList;
    ArrayList<int[]> combinedList = new ArrayList<int[]>();
    for (int i = 0; i < threadCount; i++) {
      // System.out.println("Desert List " + i);
      currentList = desertLists.get(i);
      // sil.printDeserts(currentList);
      for (int j = 0; j < currentList.size(); j++) {
        len = currentList.get(j)[1] - currentList.get(j)[0];
        if (len > currentLength) {
          currentLength = len;
          combinedList.add(currentList.get(j));
        }
      }
    }
    paraTime = System.nanoTime() - paraTime;
    System.out
        .println("Prime Desert Parallel time " + (paraTime / 1000000.0) + " ms\nSpeedup " + seqTime * 1.0 / paraTime);
    sil.printDeserts(combinedList);
    for (int i = 0; i < threadCount; i++) {
      System.out.println("Desert List " + i);
      sil.printDeserts(desertLists.get(i));
    }
  }

  void printDeserts(ArrayList<int[]> deserts) {
    for (int i = 0; i < deserts.size(); i++) {
      int len = deserts.get(i)[1] - deserts.get(i)[0];
      System.out.println(" " + i + ": [" + deserts.get(i)[0] + ", " + deserts.get(i)[1] + "] length: " + len);
    }
    System.out.println("------------------");
  }

  void setAllPrime() {
    for (int i = 0; i < bitArr.length; i++) {
      bitArr[i] = -1; // alt ( byte)255;
    }
  }

  void setNotPrime(int i) {
    bitArr[i / 16] &= bitMask2[(i % 16) >> 1];
  }

  boolean isPrime(int i) {
    if (i == 2)
      return true;
    if ((i & 1) == 0)
      return false;
    else
      return (bitArr[i >> 4] & bitMask[(i & 15) >> 1]) != 0;
  }

  ArrayList<Long> factorize(long num) {
    ArrayList<Long> fakt = new ArrayList<Long>();
    int maks = (int) Math.sqrt(num * 1.0) + 1;
    int pCand = 2;
    while (num > 1 & pCand < maks) {
      while (num % pCand == 0) {
        fakt.add((long) pCand);
        num /= pCand;
      }
      pCand = nextPrime(pCand);
      // maks = (int) Math.sqrt(num*1.0) +1;
    }
    if (pCand >= maks)
      fakt.add(num);
    return fakt;
  } // end factorize

  int nextPrime(int i) {
    // returns next prime number after number 'i'
    int k;
    if (i < 2)
      return 2;
    if (i == 2)
      return 3;
    if ((i & 1) == 0)
      k = i + 1; // if i is even, start at i+1
    else
      k = i + 2; // next possible prime
    while (!isPrime(k))
      k += 2;
    return k;
  } // end nextPrime

  int lastPrime() {
    int j = ((bitLen >> 1) << 1) - 1;
    while (!isPrime(j))
      j -= 2;
    return j;
  } // end lastPrime

  long largestLongFactorizedSafe() {
    long l;
    int i,
        j = ((bitLen >> 1) << 1) - 1;
    while (!isPrime(j))
      j -= 2;
    i = j - 2;
    while (!isPrime(i))
      i -= 2;
    return (long) i * (long) j;
  } // end largestLongFactorizedSafe

  void printAllPrimes() {
    for (int i = 2; i <= bitLen; i++)
      if (isPrime(i))
        System.out.println(" " + i);
  } // end printAllPrimes

  int numberOfPrimesLess(int n) {
    int num = 2; // we know 2 and 3 are primes
    int p;
    for (p = 3; p < n; p = nextPrime(p)) {
      num++;
    }
    return num;
  } // end numberOfPrimesLess

  void generatePrimesByEratosthenes() {
    int m = 3,
        m2 = 6,
        mm = 9; // next prime
    setNotPrime(1); // 1 is not a prime
    while (mm < bitLen) {
      m2 = m + m;
      for (int k = mm; k < bitLen; k += m2) {
        setNotPrime(k);
      }
      m = nextPrime(m);
      mm = m * m;
    }
  } // end generatePrimesByEratosthenes

  class Para implements Runnable {
    int ind,
        from,
        upto,
        threadCount;
    PrimeDesertPara sil;
    ArrayList<int[]> myDeserts;

    Para(int in, int c, PrimeDesertPara sil, int from, int upto, ArrayList<int[]> myDeserts) {
      ind = in;
      threadCount = c;
      this.sil = sil;
      this.from = from;
      this.upto = upto;
      this.myDeserts = myDeserts;
    } // konstruktor

    public void run() { // Her er det som kjores i parallell:
      try {
        readyToGo.await(); // await all threads ready to execute
      } catch (Exception e) {
        return;
      }
      int[] desert = new int[2];
      desert[0] = from;
      desert[1] = sil.nextPrime(from);
      int desertLength = desert[1] - desert[0];
      myDeserts.add(desert);
      int nextSP,
          nextEP,
          lastP;
      lastP = upto;
      nextSP = desert[1];
      nextEP = sil.nextPrime(nextSP);
      while (nextEP <= lastP) {
        if ((nextEP - nextSP) > desertLength) {
          desert = new int[2];
          desert[0] = nextSP;
          desert[1] = nextEP;
          myDeserts.add(desert);
          desertLength = nextEP - nextSP;
        }
        nextSP = nextEP;
        if (ind < threadCount) {
          nextEP = sil.nextPrime(nextSP);
        } else if (nextEP < lastP) {
          nextEP = sil.nextPrime(nextSP);
        } else
          nextEP = lastP + 1;
      }
      ;
      // Done
      try {
        allDone.await(); // await all threads done
      } catch (Exception e) {
        return;
      }
    } // end run
  } // end class Para
} // end class PrimeDesert
