package lab02.jpf;

import gov.nasa.jpf.vm.Verify;

import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;

public class Assignment1 {

    public static void main(String[] args) throws InterruptedException {
        SynchronizedStack synchronizedStack = new SynchronizedStack();
        ConcurrentHashMap<String, Long> filesToLines = new ConcurrentHashMap<>();
        filesToLines.put("testPath", 0L);
        ConcurrentHashMap<Interval, Integer> intervalToNumFiles = new ConcurrentHashMap<>();
        ModelObserver modelObserver = new ModelObserver();
        Verify.beginAtomic();
        Thread thread1 = new Thread(new LineCounterWorker(synchronizedStack, filesToLines, intervalToNumFiles, modelObserver));
        Thread thread2 = new Thread(new LineCounterWorker(synchronizedStack, filesToLines, intervalToNumFiles, modelObserver));
        // Start the threads
        thread1.start();
        thread2.start();
        Verify.endAtomic();
        thread1.join();
        thread2.join();
        assert synchronizedStack.isEmpty();
        assert filesToLines.size() == 1;
    }

    static class SynchronizedStack {
        private final Stack<String> stack;

        public SynchronizedStack() {
            this.stack = new Stack<>();
        }

        public synchronized String pop() {
            return this.stack.pop();
        }

        public synchronized boolean isEmpty() {
            return this.stack.isEmpty();
        }
    }

    static class Interval {
        private final long lowerBound;
        private final long upperBound;

        public Interval(long lowerBound, long upperBound) {
            this.lowerBound = lowerBound;
            this.upperBound = upperBound;
        }

        public boolean contains(long numLines) {
            return numLines >= this.lowerBound && numLines <= this.upperBound;
        }
    }

    static class ModelObserver {
        public void modelChanged() {
            System.out.println("Model changed, refresh GUI");
        }
    }

    static class LineCounterWorker implements Runnable {
        private final SynchronizedStack synchronizedStack;
        // A ConcurrentHashMap is used to share the results of the worker threads computation
        private final ConcurrentHashMap<String, Long> filesToLines;
        private final ConcurrentHashMap<Interval, Integer> intervalToNumFiles;
        private final ModelObserver modelObserver;

        public LineCounterWorker(SynchronizedStack synchronizedStack,
                                 ConcurrentHashMap<String, Long> filesToLines,
                                 ConcurrentHashMap<Interval, Integer> intervalToNumFiles,
                                 ModelObserver modelObserver) {
            this.synchronizedStack = synchronizedStack;
            this.filesToLines = filesToLines;
            this.intervalToNumFiles = intervalToNumFiles;
            this.modelObserver = modelObserver;
        }

        @Override
        public void run() {
        /*
            synchronized (this.synchronizedStack) {
                while (!this.synchronizedStack.isEmpty()) {
                    String fileToCount = this.synchronizedStack.pop();
                    System.out.println("Counting lines in file: " + fileToCount);
                    long numLines = 0;
                    synchronized (this.filesToLines) {
                        synchronized (this.intervalToNumFiles) {
                            this.filesToLines.put(fileToCount, numLines);
                            Interval interval = this.intervalToNumFiles
                                    .keySet()
                                    .stream()
                                    .filter(i -> i.contains(numLines))
                                    .findFirst()
                                    .orElseThrow(() -> new RuntimeException("No interval found for the number of lines"));
                            this.intervalToNumFiles.put(interval, this.intervalToNumFiles.get(interval) + 1);
                            this.modelObserver.modelChanged();
                        }
                    }
                }
            }
*/
        while (!Thread.currentThread().isInterrupted()) {
            synchronized (this.synchronizedStack) {
                boolean stackEmpty = this.synchronizedStack.isEmpty();
                if (stackEmpty) break;
            }
            String fileToCount = this.synchronizedStack.pop();
            System.out.println("Counted lines in file: " + fileToCount);
            long numLines = 0;
            synchronized (this.filesToLines) {
                synchronized (this.intervalToNumFiles) {
                    this.filesToLines.put(fileToCount, numLines);
                    Interval interval = this.intervalToNumFiles
                            .keySet()
                            .stream()
                            .filter(i -> i.contains(numLines))
                            .findFirst()
                            .orElseThrow(() -> new RuntimeException("No interval found for the number of lines"));
                    this.intervalToNumFiles.put(interval, this.intervalToNumFiles.get(interval) + 1);
                    this.modelObserver.modelChanged();
                }
            }
        }
        }
    }
}