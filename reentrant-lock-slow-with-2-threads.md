# Why does non-fair ReentrantLock fail to scale with two threads as expected.

While investigating how the various Java synchronization primitives perform I noticed that the performance of non-fair RentrantLock & Semaphore with 2 threads, was much worse than with 3 or more threads.

The full output of the Test program for macOS and Ubuntu is shown later. A summary of the results are as below:

- macOS Big Sur 11.0.1 non fair ReentrantLock
  - 2 threads takes 554 milliseconds versus the average for 3 or more threads of 172 milliseconds.
  - **3 times slower.**
- Ubuntu 20.04.1 non-fair ReentrantLock
  - 2 threads takes 575 milliseconds versus the average for 3 or more threads of 289 milliseconds.
  - **2 times slower.**

Focusing on the full result of non-fair RentrantLock on macOS:

```
Threads ReentrantLock
======= =============
      1           114
      2           554
      3           178
      4           175
      5           175
      6           173
      7           171
      8           168
      9           170
     10           169
     12           170
     24           173
```

It can be seen that the time for 3 threads or more, is pretty consistent (averaging 172 milliseconds).

**I would have expected that the time for 2 threads would by very similar, if not quicker.**

- This is concerning as 2 threads is common in producer/consumer scenarios.

Any help explaining the behaviour of the 2 thread case would be much appreciated.

### Test program respository on GitHub.

I have made the test program, with a maven build, available on GitHub:

```
git clone https://github.com/collymy/sync-test.git
```
The code can be built with a version of Java 11 or later:

```
cd sync-test
mvn package
```

and run with:

```
java -Xms1g -XX:+UnlockExperimentalVMOptions -XX:+UseEpsilonGC \
-cp target/classes org.nsm.test.SyncTest
```

The vm arguments -XX:+UnlockExperimentalVMOptions -XX:+UseEpsilonGC start the JVM with the Epsilon grabage collector (available from Java 11) which is the no grabage collection option. The -Xms1g starts the JVM with 1 Giga Byte of memory for the heap.

The program can be run with one of the following arguments for profiling:

1. lock - just run the ReentrantLock tests
2. 2 - just run the 2 thread ReentrantLock test
3. 3 - just run the 3 thread ReentrantLock test

As an example, to run the 3 thread test:

```
java -Xms1g -XX:+UnlockExperimentalVMOptions -XX:+UseEpsilonGC \
-cp target/classes org.nsm.test.SyncTest 3
```
#### Profiles

Inluded in the repository are JVM profiles of the 2 thread and 3 thread ReentrantLock cases:

- sync-test-2-profile.png
- sync-test-3-profile.png

The profiles look very similar, and the biggest difference I can see is that the 3 thread case has 2 additional C1 compiler threads. Perhaps suggesting that Hotspot is not optimizing the 2 thread case for some reason.

Both RentrantLock and Sempahore are based on java.util.concurrent.locks.AbstractQueuedSynchronizer, so it is no surprise that they both show the same performance behaviour for the 2 thread case.

## macOS Big Sur 11.0.1 results

### uname -a
```
Darwin 20.1.0 Darwin Kernel Version 20.1.0: Sat Oct 31 00:07:11 PDT 2020; root:xnu-7195.50.7~2/RELEASE_X86_64 x86_64
```

### macOS system report
```
Model Name:	iMac
Model Identifier:	iMac20,1
Processor Name:	10-Core Intel Core i9
Processor Speed:	3.6 GHz
Number of Processors:	1
Total Number of Cores:	10
L2 Cache (per Core):	256 KB
L3 Cache:	20 MB
Hyper-Threading Technology:	Enabled
Memory:	32 GB
```

### java -Xms1g -XX:+UnlockExperimentalVMOptions -XX:+UseEpsilonGC -cp target/classes org.nsm.test.SyncTest
```
SyncTest v1.0.14

Mac OS X (x86_64) version: 10.16
OpenJDK Runtime Environment: 13.0.1+9
OpenJDK 64-Bit Server VM: build 13.0.1+9, mixed mode, sharing
Java VM args: -Xms1g -XX:+UnlockExperimentalVMOptions -XX:+UseEpsilonGC

JVM warm up: 4 loops of count 10,000,000
Test count: 10,000,000

Warming up the Jvm JIT compiler and caches: completed

All values are in MILLISECONDS

Threads        Atomic  Synchronized ReentrantLock     Semaphore
======= ============= ============= ============= =============
      1            53            71           114           140
      2           105           290           554           848
      3           127           276           178           239
      4           147           512           175           261
      5           142           639           175           240
      6           174           634           173           259
      7           152           631           171           243
      8           150           616           168           255
      9           147           766           170           241
     10           149           762           169           254
     12           146           675           170           254
     24           118           603           173           251

Test complete
```
## Ubuntu 20.04.1 results

### uname -a
```
Linux 5.4.0-56-generic #62-Ubuntu SMP Mon Nov 23 19:20:19 UTC 2020 x86_64 x86_64 x86_64 GNU/Linux
```

### lscpu
```
Architecture:                    x86_64
CPU op-mode(s):                  32-bit, 64-bit
Byte Order:                      Little Endian
Address sizes:                   46 bits physical, 48 bits virtual
CPU(s):                          16
On-line CPU(s) list:             0-15
Thread(s) per core:              2
Core(s) per socket:              8
Socket(s):                       1
NUMA node(s):                    1
Vendor ID:                       GenuineIntel
CPU family:                      6
Model:                           85
Model name:                      Intel(R) Core(TM) i7-7820X CPU @ 3.60GHz
Stepping:                        4
CPU MHz:                         1436.298
CPU max MHz:                     4500.0000
CPU min MHz:                     1200.0000
BogoMIPS:                        7200.00
Virtualisation:                  VT-x
L1d cache:                       256 KiB
L1i cache:                       256 KiB
L2 cache:                        8 MiB
L3 cache:                        11 MiB
NUMA node0 CPU(s):               0-15
```

### java -Xms1g -XX:+UnlockExperimentalVMOptions -XX:+UseEpsilonGC -cp target/classes org.nsm.test.SyncTest
```
SyncTest v1.0.14

Linux (amd64) version: 5.4.0-56-generic
Java(TM) SE Runtime Environment: 13.0.1+9
Java HotSpot(TM) 64-Bit Server VM: build 13.0.1+9, mixed mode, sharing
Java VM args: -Xms1g -XX:+UnlockExperimentalVMOptions -XX:+UseEpsilonGC

JVM warm up: 4 loops of count 10,000,000
Test count: 10,000,000

Warming up the Jvm JIT compiler and caches: completed

All values are in MILLISECONDS

Threads        Atomic  Synchronized ReentrantLock     Semaphore
======= ============= ============= ============= =============
      1            58           164           132           149
      2           117           541           575           607
      3           128           551           292           403
      4           134         1,062           290           388
      5           139         1,551           298           404
      6           136         1,866           282           397
      7           137         1,927           289           396
      8           136         1,904           289           402
      9           135         1,810           289           399
     10           133         1,664           284           392
     12           133         1,478           290           393
     24            87         1,482           283           401

Test complete
```

## Test program

```Java
package org.nsm.test;

import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class SyncTest {

  private static class Link {
    Link next;
    String name;
    int counter;

    Link(String name) {
      this.name = name;
      next = null;
      counter = -1;
    }

    @Override public String toString() {
      return String.format("%s(%d)", name, counter);
    }
  }

  private static class DeQue {
    Link head;
    Link tail;

    DeQue() {
      clear();
    }

    void clear() {
      head = null;
      tail = null;
    }

    void add(Link link) {
      if (head == null) {
        head = link;
        tail = link;
      }
      else tail.next = link;
    }

    Link remove() {
      Link h = head;
      if (h != null) {
        head = h.next;
        h.next = null;
      }
      return h;
    }
  }

  private enum TestType {ATOMIC, SYNCHRONIZED, LOCK, SEM}
  private static boolean JustLock = false;
  private static int JustThreadCount = -1;

  private static final int TestCount = 10000000;
  private static final int JvmWarmLoops = 4;
  private static final int JvmWarmUpCount = 10000000;

  private static final AtomicInteger atomicCounter = new AtomicInteger(0);

  private static final Object sync = new Object();
  private static final DeQue syncQ = new DeQue();
  private static final Link syncLink = new Link("sync");
  private static int syncCounter;

  private static final Lock lock = new ReentrantLock(false);
  private static final DeQue lockQ = new DeQue();
  private static final Link lockLink = new Link("lock");
  private static int lockCounter;

  private static final Semaphore sem = new Semaphore(1);
  private static final DeQue semQ = new DeQue();
  private static final Link semLink = new Link("sem");
  private static int semCounter;

  public static void main(String[] args) throws Exception {
    System.out.print("SyncTest v1.0.14\n\n");

    if (args.length == 1) {
      switch (args[0]) {
        case "lock":
          JustLock = true;
          break;
        case "2":
          JustLock = true;
          JustThreadCount = 2;
          break;
        case "3":
          JustLock = true;
          JustThreadCount = 3;
          break;
        default:
          System.err.printf("unknown argument: %s\n", args[0]);
          System.exit(1);
      }
    }

    System.out.printf(
      "%s (%s) version: %s\n",
      System.getProperty("os.name"), System.getProperty("os.arch"), System.getProperty("os.version")
    );
    System.out.printf(
      "%s: %s\n",
      System.getProperty("java.runtime.name"), System.getProperty("java.runtime.version")
    );
    System.out.printf(
      "%s: build %s, %s\n",
      System.getProperty("java.vm.name"), System.getProperty("java.vm.version"), System.getProperty("java.vm.info")
    );
    List<String> allArgs = ManagementFactory.getRuntimeMXBean().getInputArguments();
    List<String> vmArgs = allArgs.stream().filter(arg -> arg.startsWith("-X")).collect(Collectors.toList());
    System.out.print("Java VM args:");
    for (String arg : vmArgs) {
      System.out.printf(" %s", arg);
    }
    System.out.print("\n\n");

    System.out.printf("JVM warm up: %d loops of count %,d\n", JvmWarmLoops, JvmWarmUpCount);
    System.out.printf("Test count: %,d\n\n", TestCount);

    System.out.print("Warming up the Jvm JIT compiler and caches:  ");
    for (int i = JvmWarmLoops; i >= 1; i--) {
      System.out.printf("\b%d", i);
      if (!JustLock) runIndividualTest(TestType.ATOMIC, 3, JvmWarmUpCount);
      if (!JustLock) runIndividualTest(TestType.SYNCHRONIZED, 3, JvmWarmUpCount);
      runIndividualTest(TestType.LOCK, 3, JvmWarmUpCount);
      if (!JustLock) runIndividualTest(TestType.SEM, 3, JvmWarmUpCount);
    }
    System.out.print("\bcompleted\n\n");

    System.out.print("All values are in MILLISECONDS\n\n");
    if (JustLock) {
      System.out.print("Threads ReentrantLock\n");
      System.out.print("======= =============\n");
    }
    else {
      System.out.print("Threads        Atomic  Synchronized ReentrantLock     Semaphore\n");
      System.out.print("======= ============= ============= ============= =============\n");
    }
    System.gc();

    if (JustThreadCount == 2) runAllTests(2);
    else if (JustThreadCount == 3) runAllTests(3);
    else {
      runAllTests(1);
      runAllTests(2);
      runAllTests(3);
      runAllTests(4);
      runAllTests(5);
      runAllTests(6);
      runAllTests(7);
      runAllTests(8);
      runAllTests(9);
      runAllTests(10);
      runAllTests(12);
      runAllTests(24);
    }

    System.out.print("\nTest complete\n");
  }

  private static void runAllTests(int threadCount) throws Exception {
    if (JustLock) {
      long lockElapsed = runIndividualTest(TestType.LOCK, threadCount, TestCount);
      System.out.printf(
          "%7d %,13d\n",
          threadCount, lockElapsed
      );
    }
    else {
      long atomicElapsed = runIndividualTest(TestType.ATOMIC, threadCount, TestCount);
      long synchronizedElapsed = runIndividualTest(TestType.SYNCHRONIZED, threadCount, TestCount);
      long lockElapsed = runIndividualTest(TestType.LOCK, threadCount, TestCount);
      long semElapsed = runIndividualTest(TestType.SEM, threadCount, TestCount);
      System.out.printf(
        "%7d %,13d %,13d %,13d %,13d\n",
        threadCount, atomicElapsed, synchronizedElapsed, lockElapsed, semElapsed
      );
    }
  }

  private static long runIndividualTest(final TestType testType, int threadCount, final int endValue) throws Exception {
    final CyclicBarrier testsStarted = new CyclicBarrier(threadCount + 1);
    final CountDownLatch testsComplete = new CountDownLatch(threadCount);
    for (int i = 0; i < threadCount; i++) {
      startTestThread(testType, testsStarted, testsComplete, endValue);
    }
    return waitForTests(testsStarted, testsComplete);
  }

  private static void startTestThread(
    final TestType testType,
    final CyclicBarrier testsStarted,
    final CountDownLatch testsComplete,
    final int endValue
  ) {
    Thread t = new Thread(() -> {
      try {
        testsStarted.await();
        switch (testType) {
          case ATOMIC:
            runAtomicTest(endValue);
            break;
          case SYNCHRONIZED:
            runSynchronizedTest(endValue);
            break;
          case LOCK:
            runLockTest(endValue);
            break;
          case SEM:
            runSemTest(endValue);
            break;
          default:
            throw new IllegalStateException("unexpected testType: " + testType);
        }
      }
      catch (Throwable t1) {
        t1.printStackTrace();
        System.exit(1);
      }
      testsComplete.countDown();
    });
    t.start();
  }

  private static long waitForTests(CyclicBarrier testsStarted, CountDownLatch testsComplete) throws Exception {
    testsStarted.await();
    long startTime = System.currentTimeMillis();
    testsComplete.await();
    long endTime = System.currentTimeMillis();
    reset();
    return endTime - startTime;
  }

  private static void reset() {
    atomicCounter.set(0);

    synchronized (sync) {
      syncCounter = 0;
      syncQ.clear();
    }

    lock.lock();
    lockCounter = 0;
    lockQ.clear();
    lock.unlock();

    sem.acquireUninterruptibly();
    semCounter = 0;
    semQ.clear();
    sem.release();
  }

  private static void runAtomicTest(long endValue) {
    boolean run = true;
    while (run) {
      run = atomicCounter.incrementAndGet() < endValue;
    }
  }

  private static void runSynchronizedTest(long endValue) {
    boolean run = true;
    while (run) {
      synchronized (sync) {
        if ((syncCounter % 2) == 0) {
          syncLink.counter = syncCounter;
          syncQ.add(syncLink);
        }
        else {
          if (syncQ.remove() == null) {
            throw new IllegalStateException("syncQ not balanced");
          }
        }
        syncCounter += 1;
        run = syncCounter < endValue;
      }
    }
  }

  private static void runLockTest(long endValue) {
    boolean run = true;
    while (run) {
      lock.lock();
      if ((lockCounter % 2) == 0) {
        lockLink.counter = lockCounter;
        lockQ.add(lockLink);
      }
      else {
        if (lockQ.remove() == null) {
          throw new IllegalStateException("lockQ not balanced");
        }
      }
      lockCounter += 1;
      run = lockCounter < endValue;
      lock.unlock();
    }
  }

  private static void runSemTest(long endValue) {
    boolean run = true;
    while (run) {
      sem.acquireUninterruptibly();
      if ((semCounter % 2) == 0) {
        semLink.counter = semCounter;
        semQ.add(semLink);
      }
      else {
        if (semQ.remove() == null) {
          throw new IllegalStateException("semQ not balanced");
        }
      }
      semCounter += 1;
      run = semCounter < endValue;
      sem.release();
    }
  }

}
```