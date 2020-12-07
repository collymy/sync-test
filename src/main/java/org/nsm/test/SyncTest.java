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
