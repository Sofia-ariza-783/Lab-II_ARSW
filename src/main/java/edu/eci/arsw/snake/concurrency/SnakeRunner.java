package edu.eci.arsw.snake.concurrency;

import edu.eci.arsw.snake.core.Board;
import edu.eci.arsw.snake.core.Direction;
import edu.eci.arsw.snake.core.Snake;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public final class SnakeRunner implements Runnable {
  private final Snake snake;
  private final Board board;
  private final int baseSleepMs = 80;
  private final int turboSleepMs = 40;
  private AtomicInteger turboTicks = new AtomicInteger(0);

  public SnakeRunner(Snake snake, Board board) {
    this.snake = snake;
    this.board = board;
  }

  @Override
  public void run() {
    maybeTurn();
    var res = board.step(snake);
    if (res == Board.MoveResult.HIT_OBSTACLE) {
      randomTurn();
    } else if (res == Board.MoveResult.ATE_TURBO) {
      turboTicks.set(0); ;
    }
    if (turboTicks.get() > 0) turboTicks.decrementAndGet();


  }

  public boolean isTurbo() {
    return turboTicks.get() > 0;
  }

  private void maybeTurn() {
    double p = (turboTicks.get() > 0) ? 0.05 : 0.10;
    if (ThreadLocalRandom.current().nextDouble() < p) randomTurn();
  }

  private void randomTurn() {
    var dirs = Direction.values();
    snake.turn(dirs[ThreadLocalRandom.current().nextInt(dirs.length)]);
  }
}
