package edu.eci.arsw.snake.core.engine;

import edu.eci.arsw.snake.core.GameState;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public final class GameClock implements AutoCloseable {

  private final ScheduledExecutorService scheduler =
          Executors.newSingleThreadScheduledExecutor();

  private final ExecutorService gameLogicExecutor;
  private final long periodMillis;

  private final List<Runnable> gameLogicTasks = new ArrayList<>();
  private final List<Runnable> uiTasks = new ArrayList<>();

  private final AtomicReference<GameState> state =
          new AtomicReference<>(GameState.STOPPED);

  public GameClock(long periodMillis, ExecutorService gameLogicExecutor) {
    if (periodMillis <= 0) throw new IllegalArgumentException("periodMillis must be > 0");
    this.periodMillis = periodMillis;
    this.gameLogicExecutor = (gameLogicExecutor != null)
            ? gameLogicExecutor
            : Executors.newVirtualThreadPerTaskExecutor();
  }

  public GameClock(long periodMillis) {
    this(periodMillis, null);
  }

  public void addGameLogicTask(Runnable task) { gameLogicTasks.add(task); }
  public void addUITask(Runnable task)        { uiTasks.add(task); }

  public void start() {
    if (!state.compareAndSet(GameState.STOPPED, GameState.RUNNING)) return;

    scheduler.scheduleAtFixedRate(() -> {
      if (state.get() == GameState.RUNNING) tick();
    }, 0, periodMillis, TimeUnit.MILLISECONDS);
  }

  private void tick() {
    try {
      List<Callable<Void>> calls = new ArrayList<>(gameLogicTasks.size());
      for (Runnable task : gameLogicTasks) {
        calls.add(wrapAsCallable(task, "logic"));
      }

      List<Future<Void>> futures = gameLogicExecutor.invokeAll(calls);

      for (Future<Void> f : futures) {
        try {
          f.get();
        } catch (ExecutionException e) {
          System.err.println("Error: " + e.getCause());
        }
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      System.err.println("Tick interrupted");
      return;
    }

    runAll(uiTasks, "UI");
  }

  private static Callable<Void> wrapAsCallable(Runnable task, String tipo) {
    return () -> {
      try {
        task.run();
      } catch (Exception e) {
        System.err.println("Error : " + e.getMessage());
      }
      return null;
    };
  }

  private static void runAll(List<Runnable> tasks, String tipo) {
    for (Runnable task : tasks) {
      try {
        task.run();
      } catch (Exception e) {
        System.err.println("Error: " + e.getMessage());
      }
    }
  }

  public void pause()  {state.set(GameState.PAUSED);}
  public void resume() { state.set(GameState.RUNNING); }
  public void stop()   { state.set(GameState.STOPPED); }

  @Override
  public void close() {
    stop();
    shutdownAndAwait(scheduler, 1, TimeUnit.SECONDS);
    shutdownAndAwait(gameLogicExecutor, 1, TimeUnit.SECONDS);
  }

  private static void shutdownAndAwait(ExecutorService ex, long time, TimeUnit unit) {
    ex.shutdown();
    try {
      if (!ex.awaitTermination(time, unit)) ex.shutdownNow();
    } catch (InterruptedException e) {
      ex.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }
}