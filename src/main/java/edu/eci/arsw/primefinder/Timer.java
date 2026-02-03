package edu.eci.arsw.primefinder;

public class Timer extends Thread{
    private final long duration;

    public Timer(int duration) {
        this.duration = duration;
    }
    public void run() {
        try {
            sleep(duration);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            Control.pause();
        }

    }
}
