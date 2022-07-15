Question 3.1
import java.util. * ;
///--------------------------------------------------------
//
// Original File: EratosthenesSil.java
// implements bit-array (Boolean) for prime numbers
// written by: Arne Maus , Univ of Oslo, 2013, 2015
// Derived for Twin Primes, Eric Jul, Univ. of Oslo 2019
//
//--------------------------------------------------------
/**
* Implements the bitArray of length 'bitLen' [0..bitLen ]
* 1 - true (is prime number)
* 0 - false
* can be used up to 2 G Bits (integer range)
*/
public class TwinPrimePair {
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
  TwinPrimePair(int max) {
    bitLen = max;
    bitArr = new byte[(bitLen / 16) + 1];
    setAllPrime();
    generatePrimesByEratosthenes();
  } // end konstruktor TwinPrimePair
  public static void main(String args[]) {
    ArrayList < int[] > pairs = new ArrayList < int[] > ();
    TwinPrimePair sil;
    int num = Integer.parseInt(args[0]);
    int[] pair;
    sil = new TwinPrimePair(num);
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
    for (int i = 0; i < pairs.size(); i++) {
      System.out.println(" " + i + ": [" + pairs.get(i)[0] + ",
" + pairs.get(i)[1] + "]");
    }
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
    if (i == 2) return true;
    if ((i & 1) == 0) return false;
    else return (bitArr[i >> 4] & bitMask[(i & 15) >> 1]) != 0;
  }
  ArrayList < Long > factorize(long num) {
    ArrayList < Long > fakt = new ArrayList < Long > ();
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
    if (pCand >= maks) fakt.add(num);
    return fakt;
  } // end factorize
  int nextPrime(int i) {
    // returns next prime number after number 'i'
    int k;
    if ((i & 1) == 0) k = i + 1; // if i is even, start at i+1
    else k = i + 2; // next possible prime
    while (!isPrime(k)) k += 2;
    return k;
  } // end nextPrime
  int lastPrime() {
    int j = ((bitLen >> 1) << 1) - 1;
    while (!isPrime(j)) j -= 2;
    return j;
  } // end lastPrime
  long largestLongFactorizedSafe() {
    long l;
    int i,
    j = ((bitLen >> 1) << 1) - 1;
    while (!isPrime(j)) j -= 2;
    i = j - 2;
    while (!isPrime(i)) i -= 2;
    return (long) i * (long) j;
  } // end largestLongFactorizedSafe
  void printAllPrimes() {
    for (int i = 2; i <= bitLen; i++)
    if (isPrime(i)) System.out.println(" " + i);
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
} // end class Twin Primepair
