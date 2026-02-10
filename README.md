## ╰┈➤ -【🐍】 | Laboratorio II: Snake Race ┆⤿⌗

---

Nombres:
- Sofia Nicolle Ariza Goenaga
---

# 🥇 Parte I
### 📋 Actividades
1. Toma el programa **[Prime Finder](./src/main/java/edu/eci/arsw/primefinder/Main.java).**
2. Modifícalo para que cada t milisegundos:
   * Se pausen todos los hilos trabajadores.
   * Se muestre cuántos números primos se han encontrado.
   * El programa esperé ENTER para reanudar.
   
La sincronización debe usar synchronized, wait(), notify() / notifyAll() sobre el mismo monitor (sin busy-waiting).
Entrega en el reporte de laboratorio las observaciones y/o comentarios explicando tu diseño de sincronización (qué lock, qué condición, cómo evitas lost wakeups).

**Ejecución con Maven:**

```bash
mvn compile exec:java -Dexec.mainClass="edu.eci.arsw.primefinder.Main"
```

# 🥈 Parte II
### 1) Análisis de concurrencia
* Explica cómo el código usa hilos para dar autonomía a cada serpiente.
* Identifica y documenta en el reporte de laboratorio:
   * Posibles condiciones de carrera.
   * Colecciones o estructuras no seguras en contexto concurrente.
   * Ocurrencias de espera activa (busy-wait) o de sincronización innecesaria.
---
### 2) Correcciones mínimas y regiones críticas
* Elimina esperas activas reemplazándolas por señales / estados o mecanismos de la librería de concurrencia.
* Protege solo las regiones críticas estrictamente necesarias (evita bloqueos amplios).
* Justifica en el reporte de laboratorio cada cambio: cuál era el riesgo y cómo lo resuelves.

---
### 3) Control de ejecución seguro (UI)
 *  Implementa la UI con Iniciar / Pausar / Reanudar (ya existe el botón Action y el reloj GameClock).
 *  Al Pausar, muestra de forma consistente (sin tearing):
   *  La serpiente viva más larga.
   *  La peor serpiente (la que primero murió).
 *  Considera que la suspensión no es instantánea; coordina para que el estado mostrado no quede “a medias”.

---
### 4) Robustez bajo carga
   Ejecuta con N alto (-Dsnakes=20 o más) y/o aumenta la velocidad.
   El juego no debe romperse: sin ConcurrentModificationException, sin lecturas inconsistentes, sin deadlocks.
   Si habilitas teleports y turbo, verifica que las reglas no introduzcan carreras.
   
---

# ➤ °.⭑【🎯】 Reporte de laboratorio ┆⤿⌗ 

## 1) Análisis de concurrencia 

La clase que orquesta toda la lógica del juego y, a su vez, la interfaz gráfica (GUI) es **[SnakeApp.java](src/main/java/edu/eci/arsw/snake/ui/legacy/SnakeApp.java)**, la cual se encarga, mediante un ejecutor, de crear un SnakeRunner y su serpiente asociada, en hilos independientes.

Dentro de cada **[SnakeRunner.java](src/main/java/edu/eci/arsw/snake/concurrency/SnakeRunner.java)** se inicia un ciclo que funciona indefinidamente hasta que se interrumpe la ejecución normal de los hilos. En dicho ciclo se ejecuta la lógica del movimiento de la serpiente bajo diferentes condiciones, tales como:

* Si choca, gira en una dirección aleatoria.
* Existe una probabilidad de que gire aleatoriamente en cada hilo.
* Si se come un turbo, la penalización de sleep que recibe se reduce a la mitad a la mitad (40 ms).

**[SnakeRunner.java](src/main/java/edu/eci/arsw/snake/concurrency/SnakeRunner.java)** se encarga de coordinar el tablero (board) y sus entidades para que interactúen con la serpiente, cada una en un hilo individual. Además, controla la mayoría de los accesos concurrentes mediante métodos marcados como synchronized.

El dibujo de la serpiente está controlado por **[GameClock.java](src/main/java/edu/eci/arsw/snake/core/engine/GameClock.java)**, un hilo que cada cierto tiempo ejecuta el llamado a la función que repinta la GUI. Es asi que cada 60ms actualiza la posicion de la serpiente en el tablero que ve el usuario de acuerdo con la información de las serpientes y el tablero que esten en el tick actual.

### 【🚥】 Posibles comportamientos inesperados 〮

El código actualmente presenta varios errores potenciales:

* En el caso de las serpientes, uno de sus atributos, el body, es accedido concurrentemente por diferentes hilos a través de métodos que están sincronizados entre sí, pero no se utiliza una clase Thread-Safe. Esto puede provocar estados inconsistentes y lecturas sucias.
* No se trata de un problema de condición de carrera, pero el GameClock, que debería controlar las pausas y el ritmo del juego, no es escuchado realmente por la lógica interna que administra el tablero y las serpientes; solo controla la GUI. Como resultado, se generan errores visuales y la impresión de que las cosas se teletransportan o cambian repentinamente de estado.
* La dirección en la que se mueve la serpiente también puede ser inconsistente, pues no existe un método que bloquee o sincronice la aplicación del movimiento. Esto ocasiona solapamientos entre el cálculo de los movimientos aleatorios y la entrada del usuario, lo que a veces da la sensación de que se pierde el movimiento.
* En snakeApp también se está utilizando concurrentemente una estructura no segura, el arraylist de snakes, a pesar de que solo se recorre podría causar problemas con pequeños cambios en el código.
* En el tablero se hacen copias constantes que se envian a la GUI siendo un proceso pesado que genera muchos objetos basura.

## 2) Correcciones mínimas y regiones crítica

Se centralizó la lógica del juego para que [GameClock.java](src/main/java/edu/eci/arsw/snake/core/engine/GameClock.java) controlara los ticks de todo el sistema. Para ello se utilizó una mezcla del scheduler que ya existía y el executor, encargado de administrar los diferentes hilos y ejecutar sus métodos run cada vez que el scheduler lo indicaba. Para evitar problemas al dibujar, al final de cada tick se emplea una barrera implícita y se actualiza la interfaz, evitando inconsistencias.

Antes de corregir este error, el GameClock y la GUI controlaban los estados del juego (pausado, corriendo o detenido) sin informar a la lógica, la cual se ejecutaba de manera independiente e infinita. Esto generaba inconsistencias visuales al pausar y cambios bruscos al reanudar. Ahora esos problemas se solucionaron, pues el GameClock centraliza la ejecución de los ticks y las serpientes ya no corren infinitamente por su cuenta, sino que siguen el ritmo del scheduler que administra el juego, sin alterar su funcionamiento actual.

Adicionalmente, se sincronizaron los métodos que accedían concurrentemente al body para protegerlo, junto con la función turn, que ocasionaba solapamientos entre entradas y podía producir movimientos no válidos que ignoraban las reglas establecidas en el juego.

Para corregir el error de las copias constantes cada vez que la GUI realizaba un repaint, se modificaron los métodos de acceso a cada uno de los elementos del tablero (mice, obstáculos, turbo, etc.) para que no generaran una copia en cada solicitud, sino que compartieran una copia accesible por todos los hilos, la cual se actualiza después de cada interacción de una snake con el tablero. Esto se implementó mediante un record y una variable volatile que se actualiza en cada momento clave (cuando alguna snake interactúa con los elementos del tablero o cuando se inicializa el tablero).

---

### 3) Control de ejecución seguro (UI)

[Screen Recording 2026-02-09 212251.mp4](docs/Screen%20Recording%202026-02-09%20212251.mp4)

Dado que todo se esta manejando con una barrera y la GUI no se actualiza hasta que todos los calculos se terminen de hacer y el tick es una funcion que se tiene que terminar de ejecutar completa, el flujo actual asegura que no queden estados a medias o retrasados.

---

### 4) Robustez bajo carga

* 20 serpientes - velocidad normal
[Screen Recording 2026-02-09 214442.mp4](docs/Screen%20Recording%202026-02-09%20214442.mp4)

* 2 serpientes - velocidad x2 (30ms)
[Screen Recording 2026-02-09 214528.mp4](docs/Screen%20Recording%202026-02-09%20214528.mp4)

* 20 serpientes - velocidad x2 (30ms)
[Screen Recording 2026-02-09 214958.mp4](docs/Screen%20Recording%202026-02-09%20214958.mp4)

Incluso al ejecutar el juego durante varios minutos, este no presenta deadlocks ni problemas de acceso concurrente, ya que la mayoría de las sincronizaciones se redujeron y no se agregaron bloqueos directos. En su lugar, todos los hilos se ejecutan en paralelo, orquestados por el GameClock.

El rendimiento general de la solución es bueno, con un consumo reducido de recursos: alrededor de 1.2 GB de memoria y aproximadamente 0.2 % de procesador.


---

### Ejecutar

```bash

mvn clean verify

mvn -q -DskipTests compile exec:java -Dexec.mainClass="edu.eci.arsw.snake.app.Main" -Dsnakes=20

```