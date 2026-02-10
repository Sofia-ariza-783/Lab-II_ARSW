package edu.eci.arsw.snake.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public final class Board {
  private final int width;
  private final int height;

  private final Set<Position> mice = new HashSet<>();
  private final Set<Position> obstacles = new HashSet<>();
  private final Set<Position> turbo = new HashSet<>();
  private final Map<Position, Position> teleports = new HashMap<>();

  public enum MoveResult { MOVED, ATE_MOUSE, HIT_OBSTACLE, ATE_TURBO, TELEPORTED }
  public static record Snapshot(
          Set<Position> mice,
          Set<Position> obstacles,
          Set<Position> turbo,
          Map<Position, Position> teleports
  ) { }
  private volatile Snapshot snapshot;

  public Board(int width, int height) {
    if (width <= 0 || height <= 0) throw new IllegalArgumentException("Board dimensions must be positive");
    this.width = width;
    this.height = height;
    for (int i=0;i<6;i++) mice.add(randomEmpty());
    for (int i=0;i<4;i++) obstacles.add(randomEmpty());
    for (int i=0;i<3;i++) turbo.add(randomEmpty());
    createTeleportPairs(2);

    refreshSnapshot();
  }

  public int width() { return width; }
  public int height() { return height; }

  public Set<Position> mice() { return snapshot.mice();}
  public Set<Position> obstacles() { return snapshot.obstacles(); }
  public Set<Position> turbo() { return snapshot.turbo(); }
  public Map<Position, Position> teleports() { return snapshot.teleports(); }

  public synchronized MoveResult step(Snake snake) {
    Objects.requireNonNull(snake, "snake");

    var head = snake.head();
    var dir = snake.direction();

    Position next = new Position(head.x() + dir.dx, head.y() + dir.dy).wrap(width, height);

    if (obstacles.contains(next)) return MoveResult.HIT_OBSTACLE;

    boolean teleported = false;
    if (teleports.containsKey(next)) {
      next = teleports.get(next);
      teleported = true;
    }

    boolean changed = false;

    boolean ateMouse = mice.remove(next);
    boolean ateTurbo = turbo.remove(next);

    if (ateMouse || ateTurbo) changed = true;

    snake.advance(next, ateMouse);

    if (ateMouse) {
      mice.add(randomEmpty());
      obstacles.add(randomEmpty());
      if (ThreadLocalRandom.current().nextDouble() < 0.2) {
        turbo.add(randomEmpty());
      }
      changed = true;
    }

    if (changed) {
      refreshSnapshot();
    }

    if (ateTurbo) return MoveResult.ATE_TURBO;
    if (ateMouse) return MoveResult.ATE_MOUSE;
    if (teleported) return MoveResult.TELEPORTED;
    return MoveResult.MOVED;
  }

  private void createTeleportPairs(int pairs) {
    for (int i=0;i<pairs;i++) {
      Position a = randomEmpty();
      Position b = randomEmpty();
      teleports.put(a, b);
      teleports.put(b, a);
    }
  }

  public Snapshot snapshot() {
    return snapshot;
  }

  private void refreshSnapshot() {
    snapshot = new Snapshot(
            Set.copyOf(mice),
            Set.copyOf(obstacles),
            Set.copyOf(turbo),
            Map.copyOf(teleports)
    );
  }

  private Position randomEmpty() {
    var rnd = ThreadLocalRandom.current();
    Position p;
    int guard = 0;
    do {
      p = new Position(rnd.nextInt(width), rnd.nextInt(height));
      guard++;
      if (guard > width*height*2) break;
    } while (mice.contains(p) || obstacles.contains(p) || turbo.contains(p) || teleports.containsKey(p));
    return p;
  }
}
