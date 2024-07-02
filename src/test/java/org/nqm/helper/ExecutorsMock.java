package org.nqm.helper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class ExecutorsMock {

  private static MockedStatic<Executors> mock;

  private static void mockVirtualThread(ExecutorService svc) {
    if (mock == null || mock.isClosed()) {
      mock = Mockito.mockStatic(Executors.class);
    }
    mock.when(Executors::newVirtualThreadPerTaskExecutor).thenReturn(svc);
  }

  /**
   * This mock enables all virtual threads to run on the main thread
   */
  public static void mockVirtualThreadCallable(ExecutorService exe) {
    mockVirtualThread(exe);
    doAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        return CompletableFuture.completedFuture(((Callable<?>) invocation.getArguments()[0]).call());
      }
    }).when(exe).submit((Callable<?>) any());
  }

  public static void mockVirtualThreadCallableThrowException(ExecutorService exe, Throwable e) {
    mockVirtualThread(exe);
    doAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        throw (e);
      }
    }).when(exe).submit((Callable<?>) any());
  }

  /**
   * This mock enables all virtual threads to run on the main thread
   */
  public static void mockVirtualThreadRunnable(ExecutorService exe) {
    mockVirtualThread(exe);
    doAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        var runnable = (Runnable) invocation.getArguments()[0];
        runnable.run();
        return null;
      }
    }).when(exe).submit((Runnable) any());
  }

  public static void close() {
    if (mock != null && !mock.isClosed()) {
      mock.close();
    }
  }
}
