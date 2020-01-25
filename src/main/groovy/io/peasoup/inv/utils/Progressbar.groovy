package io.peasoup.inv.utils

import io.peasoup.inv.Logger

import java.util.concurrent.atomic.AtomicInteger

class Progressbar {

    private final static Integer LOOP_MS = 500
    private final static Integer CHAR_WIDTH = 20
    private final static char CHAR_EMPTY = ' '
    private final static char CHAR_REACHED_SYMBOL = '='
    private final static char CHAR_NOT_REACHED_SYMBOL = '.'

    private final String title
    private final Boolean eol

    private AtomicInteger index = new AtomicInteger(0)
    private AtomicInteger upperBound = new AtomicInteger(0)
    private Boolean running = false

    Progressbar(String title, boolean eol=false) {
        this(title, 0, eol)
    }

    Progressbar(String title, int limit, boolean eol=false) {
        assert title, 'Title is required'
        assert limit > -1, 'Limit must be a positive value'

        this.title = title
        this.eol = eol
        this.upperBound = new AtomicInteger(limit)
    }

    synchronized void start(Closure body) {
        assert body

        if (upperBound.get() < 1)
            return

        if (running)
            return

        running = true

        final myself = this
        def executeThread = new Thread({
            body.delegate = myself
            try {
                body()
            } catch(Exception ex) {
                Logger.error(ex)
                upperBound.set(0)
            }
        }, "progressbar-execute")

        def writingThread = new Thread({
            write()
        }, "progressbar-write")


        writingThread.start()
        executeThread.start()

        executeThread.join()
        writingThread.join()

        running = false
    }

    void limit(int amount=1) {
        upperBound.addAndGet(amount)
    }

    void step(int amount=1) {
        index.addAndGet(amount)
    }

    boolean isRunning() {
        return running
    }

    private void write() {
        final OutputStream out = System.out

        while(index < upperBound) {
            int index = index.get()
            int upperBound = upperBound.get()

            float stepSize = upperBound / CHAR_WIDTH
            int reachStep = Math.floor(index / stepSize)
            int percentage = Math.floor(index / upperBound * 100)

            report(out, reachStep, index, upperBound, percentage)
            sleep(LOOP_MS)
        }

        report(out, CHAR_WIDTH, index.get(), upperBound.get())
        out.print(System.lineSeparator())
    }

    private void report(
            PrintStream out,
            Integer reachStep,
            Integer step,
            Integer limit,
            Integer percentage=100) {
        out.print('\r' + title + CHAR_EMPTY + '[')

        for (int i=0; i<reachStep; i++) {
            out.print(CHAR_REACHED_SYMBOL)
        }
        for (int i=0; i<CHAR_WIDTH-reachStep; i++) {
            out.print(CHAR_NOT_REACHED_SYMBOL)
        }
        out.print(']' + CHAR_EMPTY)
        out.printf('%3s', percentage)
        out.print('%' + CHAR_EMPTY)
        out.print('(' + step + '/' + limit + ')')

        if (eol)
            out.print(System.lineSeparator())
    }
}
