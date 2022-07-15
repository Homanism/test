Question 3.2
import java.util. * ;
import java.util.concurrent. * ;
import java.util.concurrent.locks. * ;
///--------------------------------------------------------
// Fil: TwinPrimePairPara.java
// Implements Sequential and Parallel Prime pair finding
// written by: Eric Jul, University of Oslo, 2019
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
public class TwinPrimePairPara {
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
  TwinPrimePairPara(int max) {
    bitLen = max;
    bitArr = new byte[(bitLen / 16) + 1];
    setAllPrime();
    generatePrimesByEratosthenes();
  }
  public static void main(String args[]) {
    TwinPrimePairPara sil;
    if (args.length != 2) {
      System.out.println("use: >java TwinPrimePairPara <Max>
<threadCount>");
      System.exit(0);
    }
    int num = Integer.parseInt(args[0]);
    int threadCount = Integer.parseInt(args[1]);
    if (! ((num >= 5) && (threadCount >= 2))) {
      System.out.println("Bad parameters: Max must be at least 5 and
threadCount at least 2");
      System.exit(0);
    };
    // Here we generate the sil sequentially as we assumme that you already
    have a parallel
    // version of the sil generation from Oblig 3
    long silTime = System.nanoTime();
    sil = new TwinPrimePairPara(num);
    silTime = System.nanoTime() - silTime;
    System.out.println("Sil sequential generation time: " + (silTime / 1000000.0) + " ms");
    // now do the Prime pair calculations
    sil.doit(sil, num, threadCount);
  } // end main()
  void doit(TwinPrimePairPara sil, int num, int threadCount) {
    ArrayList < int[] > pairs = new ArrayList < int[] > ();
    readyToGo = new CyclicBarrier(threadCount + 1); // includes main() thread
    allDone = new CyclicBarrier(threadCount + 1);
    // Sequential version of TwinPrimePair
    long seqTime = System.nanoTime(); // Start sequential timing
    int[] pair = new int[2];
    int nextSP,
    nextEP,
    lastP;
    lastP = sil.lastPrime();
    nextSP = 3;
    nextEP = sil.nextPrime(nextSP);
    while (nextEP <= lastP) {
      if ((nextEP - nextSP) == 2) {
        pair = new int[2];
        pair[0] = nextSP;
        pair[1] = nextEP;
        pairs.add(pair);
      }
      nextSP = nextEP;
      nextEP = sil.nextPrime(nextSP);
    };
    seqTime = System.nanoTime() - seqTime;
    System.out.println("Twin Prime Pairs Sequential time " + (seqTime / 1000000.0) + " ms\n");
    // sil.printpairs(pairs);
    // Parallel version
    long paraTime = System.nanoTime();
    // Create a pair list for each thread
    ArrayList < ArrayList < int[] >> pairLists = new
    ArrayList < ArrayList < int[] >> ();
    for (int i = 0; i < threadCount; i++) {
      pairLists.add(new ArrayList < int[] > ());
    }
    // start threads
    int largestPrime = sil.lastPrime();
    int chunkSize = largestPrime / threadCount;
    int lastThread = threadCount - 1;
    int from = 2;
    for (int i = 0; i < threadCount; i++) {
      int fromPrime = sil.nextPrime(from);
      int uptoPrime;
      if (i < lastThread) {
        uptoPrime = sil.nextPrime(from + chunkSize);
      } else uptoPrime = largestPrime;
      // System.out.println("Starting thread "+i+" from "+fromPrime+" upto
      "+uptoPrime);
new Thread(new Para(i, threadCount, sil, fromPrime, uptoPrime,
pairLists.get(i))).start();
from = from + chunkSize;
}
try {
readyToGo.await(); // await all threads ready to execute
} catch (Exception e) {return;}
// Now the threads are doing their thing
try {
allDone.await(); // await all worker threads DONE
} catch (Exception e) {return;}
// Combine results
ArrayList<int[]> currentList;
ArrayList<int[]> combinedList = new ArrayList<int[]>();
for (int i = 0; i < threadCount; i++) {
currentList = pairLists.get(i);
// System.out.println("
      pair List " + i + "
      number of pairs: " +
currentList.size());
for (int j = 0; j < currentList.size(); j++) {
combinedList.add(currentList.get(j));
}
}
paraTime = System.nanoTime() - paraTime;
System.out.println("
      Prime pair Parallel time " + (paraTime/1000000.0) +
"
      ms\nSpeedup "+ seqTime*1.0/paraTime);
/*
sil.printpairs(combinedList);
for (int i = 0; i < threadCount; i++) {
System.out.println("
      pair List " + i);
sil.printpairs(pairLists.get(i));
}
*/
}
void printpairs(ArrayList<int[]> pairs) {
for (int i = 0; i < pairs.size(); i++) {
System.out.println(""+i+": ["+pairs.get(i)[0]+", "+pairs.get(i)[1]+"]");
}
System.out.println("------------------");
}
void setAllPrime() {
for (int i = 0; i < bitArr.length; i++) {
bitArr[i] = -1 ; // alt ( byte)255;
}
}
void setNotPrime(int i) {
bitArr[i/16] &= bitMask2[(i%16)>>1];
}
boolean isPrime (int i) {
if (i == 2 ) return true;
if ((i&1) == 0) return false;
else return (bitArr[i>>4] & bitMask[(i&15)>>1]) != 0;
}
ArrayList<Long> factorize (long num) {
ArrayList <Long> fakt = new ArrayList <Long>();
int maks = (int) Math.sqrt(num*1.0) +1;
int pCand =2;
while (num > 1 & pCand < maks) {
while ( num % pCand == 0){
fakt.add((long) pCand);
num /= pCand;
}
pCand = nextPrime(pCand);
// maks = (int) Math.sqrt(num*1.0) +1;
}
if (pCand>=maks) fakt.add(num);
return fakt;
} // end factorize
int nextPrime(int i) {
// returns next prime number after number 'i'
int k;
if (i < 2) return 2;
if (i == 2) return 3;
if ((i&1)==0) k =i+1; // if i is even, start at i+1
else k = i+2; // next possible prime
while (!isPrime(k)) k+=2;
return k;
} // end nextPrime
int lastPrime() {
int j = ((bitLen>>1)<<1) -1;
while (! isPrime(j) ) j-=2;
return j;
} // end lastPrime
long largestLongFactorizedSafe () {
long l;
int i,j = ((bitLen>>1)<<1) -1;
while (! isPrime(j) ) j -= 2;
i = j-2;
while (! isPrime(i) ) i -= 2;
return (long)i*(long)j;
} // end largestLongFactorizedSafe
void printAllPrimes(){
for ( int i = 2; i <= bitLen; i++)
if (isPrime(i)) System.out.println(""+i);
} // end printAllPrimes
int numberOfPrimesLess(int n){
int num = 2; // we know 2 and 3 are primes
int p ;
for (p=3 ; p < n;p = nextPrime(p) ){
num++;
}
return num;
} // end numberOfPrimesLess
void generatePrimesByEratosthenes() {
int m = 3, m2=6,mm =9; // next prime
setNotPrime(1); // 1 is not a prime
while ( mm < bitLen) {
m2 = m+m;
for ( int k = mm; k < bitLen; k +=m2){
setNotPrime(k);
}
m = nextPrime(m);
mm= m*m;
}
} // end generatePrimesByEratosthenes
class Para implements Runnable {
int ind, from, upto, threadCount;
TwinPrimePairPara sil;
ArrayList<int[]> mypairs;
Para(int in, int c, TwinPrimePairPara sil, int from, int upto,
ArrayList<int[]> mypairs) {
ind = in;
threadCount = c;
this.sil = sil;
this.from = from;
this.upto = upto;
this.mypairs = mypairs;
} // konstruktor
public void run() { // Her er det som kjores i parallell:
try {
readyToGo.await(); // await all threads ready to execute
} catch (Exception e) {return;}
int[] pair;
int nextSP, nextEP, lastP;
lastP = upto;
nextSP = from;
nextEP = sil.nextPrime(nextSP);
while (nextEP <= lastP) {
if ((nextEP-nextSP) == 2) {
pair = new int[2];
pair[0] = nextSP;
pair[1] = nextEP;
mypairs.add(pair);
}
nextSP = nextEP;
nextEP = sil.nextPrime(nextSP);
};
// Done
try {
allDone.await(); // await all threads done
} catch (Exception e) {return;}
} // end run
} // end class Para
} // end class TwinPrimePair"