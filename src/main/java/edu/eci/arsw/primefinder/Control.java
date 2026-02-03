/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.eci.arsw.primefinder;

import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 */
public class Control extends Thread {

    private final static int NTHREADS = 3;
    private final static int MAXVALUE = 30000000;
    private final static int TMILISECONDS = 1000;
    private final int NDATA = MAXVALUE / NTHREADS;
    private PrimeFinderThread pft[];
    private Timer timer;
    private static AtomicInteger countDownNums = new AtomicInteger(MAXVALUE);
    private static AtomicBoolean paused = new AtomicBoolean(false);

    private Control() {
        super();
        this.pft = new  PrimeFinderThread[NTHREADS];

        int i;
        for(i = 0;i < NTHREADS - 1; i++) {
            PrimeFinderThread elem = new PrimeFinderThread(i*NDATA, (i+1)*NDATA);
            pft[i] = elem;
        }
        pft[i] = new PrimeFinderThread(i*NDATA, MAXVALUE + 1);
    }

    public static Control newControl() {
        return new Control();
    }

    @Override
    public void run() {
        for (int i = 0; i < NTHREADS; i++) {
            pft[i].start();
        }

        Scanner scanner = new Scanner(System.in);
        while (countDownNums.get() >= 0){
            timer = new Timer(TMILISECONDS);
            timer.start();

            try {
                sleep(TMILISECONDS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            System.out.println( "Se han encontrado : "+ getAllPrimes() + " primos");
            System.out.print("Press Enter to resume the threads");
            scanner.nextLine();
            resumes();
        }
        System.out.println(getAllPrimes());

    }

    public static void pause(){
        paused.set(true);
    }

    public int getAllPrimes(){
        int primes = 0;
        for(int i = 0;i < NTHREADS;i++ ) {
            primes = primes + pft[i].getPrimes().size();
        }

        return primes;
    }

    public static boolean isPaused(){
        return paused.get();
    }

    public void resumes(){
        paused.set(false);
        for (PrimeFinderThread thread : pft){
            synchronized(thread) {
                thread.notifyAll();
            }
        }
    }

    public static void countDown(){
        countDownNums.decrementAndGet();
    }
    
}
