import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;

public abstract class AbstractStrategy implements Strategy {

    private PriorityQueue<Process> readyQueue;
    private String name;
    private int numStrategies;

    public AbstractStrategy(String name) {
        this(name, 1, new NullComparator());
    }

    public AbstractStrategy(String name, int numStrategies) {
        this(name, numStrategies, new NullComparator());
    }

    public AbstractStrategy(String name, int numStrategies, Comparator<Process> comparator) {
        this.name = name;
        this.numStrategies = numStrategies;
        readyQueue = new PriorityQueue<>(comparator);
    }

    public static AbstractStrategy copyAbstractStrategy(AbstractStrategy copy) {
        AbstractStrategy result = new AbstractStrategy(copy.name){};
        result.readyQueue = new PriorityQueue<>(copy.readyQueue.comparator());
        // Copy readyQueue
        Iterator<Process> itr = copy.readyQueue.iterator();
        while (itr.hasNext()) {
            result.readyQueue.add(new Process(itr.next()));
        }
        return result;
    }

    @Override
    public void arrived(Process process) {
        readyQueue.add(process);
        System.out.println("Process" + process.getPID() + " has arrived!");
    }

    @Override
    public void ready(Process process, int time) {
        process.incrementTracker();
        blockedProcesses.remove(process);
        process.setReadyTime(time);
        readyQueue.add(process);
        System.out.println("Process" + process.getPID() + " is ready!");
    }

    @Override
    public void blocked(Scheduler scheduler) {
        Process CPU = scheduler.getCPU();
        CPU.resetCurrentRuntime();
        CPU.incrementTracker();
        blockedProcesses.add(CPU);
        System.out.println("Process" + CPU.getPID() + " blocked!");
        scheduler.freeCPU();
    }

    @Override
    public void preempt(Scheduler scheduler) {
        Process CPU = scheduler.getCPU();
        CPU.resetCurrentRuntime();
        CPU.setReadyTime(scheduler.getTimer());
        readyQueue.add(CPU);
        System.out.println("Process" + CPU.getPID() + " preempted!");
        scheduler.freeCPU();
    }

    @Override
    public boolean preemptCondition(Scheduler scheduler) {
        return false;
    }

    @Override
    public Process next() {
        return readyQueue.poll();
    }

    @Override
    public void progressBlocked(int time) {
        Iterator<Process> itr = blockedProcesses.iterator();
        while (itr.hasNext()) {
            Process curr = itr.next();
            curr.decrementBurst(); // Perform I/O
            if (curr.getCurrentBurst() == 0) {
                // If we finished I/O
                itr.remove(); //
                ready(curr, time); // move to ready
            }
        }
    }

    @Override
    public void progressReady(int time) {
        Iterator<Process> itr = readyQueue.iterator();
        while (itr.hasNext()) {
            itr.next().incrementWaitingTime();
        }
    }

    @Override
    public PriorityQueue<Process> getReadyQueue() {
        return readyQueue;
    }


    public int getNumStrategies() {
        return numStrategies;
    }

    public String getName() {
        return name;
    }

    public String readyQueueToString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Ready Queue: ");
        if (readyQueue.isEmpty()) {
            sb.append("{ NONE }");
            return sb.toString();
        }
        else {
            Iterator<Process> itr = readyQueue.iterator();
            while (itr.hasNext()) {
                sb.append("P");
                sb.append(itr.next().getPID());
                sb.append(" ");
            }
            return sb.toString();
        }
    }

}
