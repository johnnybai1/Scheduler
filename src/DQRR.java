import java.util.Iterator;
import java.util.PriorityQueue;

public class DQRR extends AbstractStrategy implements Strategy {

    /**
     * DQRR is initially 0, our progression automatically changes these
     */
    private int quantum;
    private int sumBurst;
    private int avgBurst;

    public DQRR() {
        super("Dynamic Quantum Round Robin");
    }

    @Override
    public void arrived(Process process) {
        super.arrived(process); // Add to queue and announce
        sumBurst = sumBurst();
        avgBurst = avgBurst();
        quantum = avgBurst;
    }


    @Override
    public void ready(Process process, int time) {
        super.ready(process, time);
        sumBurst = sumBurst();
        avgBurst = avgBurst();
        // When a process comes to the ready queue, update our quantum
        quantum = avgBurst();
    }

    @Override
    public void blocked(Scheduler scheduler) {
        super.blocked(scheduler);
        sumBurst = sumBurst();
        avgBurst = avgBurst();
        quantum = avgBurst();
    }

    @Override
    public void preempt(Scheduler scheduler) {
        super.preempt(scheduler);
        sumBurst = sumBurst();
        avgBurst = avgBurst();
        quantum = avgBurst();
    }

    @Override
    public boolean preemptCondition(Scheduler scheduler) {
        Process CPU = scheduler.getCPU();
        return (CPU.getCurrentRuntime() >= quantum && !getReadyQueue().isEmpty());
    }

    @Override
    public Process next() {
        PriorityQueue<Process> ready = getReadyQueue();
        Process next = ready.poll();
        if (next != null) {
            if (ready.isEmpty()) {
                quantum = next.getCurrentBurst();
                sumBurst = 0;
                avgBurst = 0;
            }
            else {
                sumBurst = sumBurst();
                avgBurst = avgBurst();
                quantum = avgBurst;
            }
            return next;
        }
        return null;
    }

    public int sumBurst() {
        int sum = 0;
        PriorityQueue<Process> ready = getReadyQueue();
        Iterator<Process> itr = ready.iterator();
        while (itr.hasNext()) {
            sum += itr.next().getCurrentBurst();
        }
        return sum;
    }

    public int avgBurst() {
        int numProcessesWaiting = getReadyQueue().size();
        if (numProcessesWaiting > 0) {
            return sumBurst / numProcessesWaiting;
        }
        return avgBurst;
        // return Max(avgBurst, minimum);
    }

    public int getQuantum() {
        return quantum;
    }

    public String displayStats() {
        StringBuilder sb = new StringBuilder();
        sb.append("Current properties: [SUM=" + sumBurst + " AVG=" + avgBurst + " QTM=" + quantum + "]");
        System.out.println(sb.toString());
        return sb.toString();
    }



}
