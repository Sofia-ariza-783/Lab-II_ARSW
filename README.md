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

---