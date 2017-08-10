import java.util.ArrayList;
import java.util.Iterator;
import java.util.PriorityQueue;

public class SimpleMLFQ extends AbstractStrategy implements Strategy {

    private int levels; // how many levels in our MLFQ?
    private AbstractStrategy[] strategies; // what are the strategies?
    private int[] quantums; // quantums of each RR
    private int[] demoteQuantums; // how much CPU process can use before it is demoted
    private int maxWait; // how long a process must wait before being promoted
    private boolean resetOnBlock; // do we reset process's CPU time when it enters I/O?

    private int[] currentWaitTime; // how long each process has waited
    private int[] currentRunTime; // how long each process has ran

    /**
     * Default SimpleMLFQ uses three round robin queues
     * Lowest level obeys a RR with a quantum of 5s
     * Middle level obeys a RR with a quantum of 5s, and max runtime of 20s
     * Highest level obeys a RR with a quantum of 5s, and max runtime of 15s
     * If a process has not run for 100s, it will be promoted a level
     * If a process enters I/O, we do not reset its runtime
     */

    public SimpleMLFQ(int numProcesses) {
        this(new int[numProcesses], new int[numProcesses]);
    }

    public SimpleMLFQ(int[] currentWaitTime, int[] currentRunTime) {
        this(3, new int[] {5, 5, 5}, new int[] {-1, 20, 15},
                100, false, currentWaitTime, currentRunTime);
    }

    /**
     * @param levels: how many levels in your MLFQ
     * @param quantums: specify the quantum time of each round robin queue
     * @param demoteQuantums: specify demote quantums {lowest queue (must be -1), ... highest queue}
     * @param maxWait: length before a process is promoted to the top
     * @param resetOnBlock: false to prevent "cheating", otherwise can be true
     */
    public SimpleMLFQ(int levels, int[] quantums, int[] demoteQuantums,
                int maxWait, boolean resetOnBlock, int[] currentWaitTime, int[] currentRunTime) {
        super("Multi-level Feedback Queue", levels);
        this.levels = levels;
        strategies = new AbstractStrategy[levels];
        for (int i = 0; i < levels; i++) {
            strategies[i] = new RoundRobin(quantums[i]);
        }
        this.quantums = quantums;
        this.demoteQuantums = demoteQuantums;
        this.maxWait = maxWait;
        this.resetOnBlock = resetOnBlock;
        this.currentWaitTime = currentWaitTime;
        this.currentRunTime = currentRunTime;
    }

    @Override
    public void arrived(Process process) {
//        int p = process.getPriority();
        process.setPriority(levels-1); // highest priority
        strategies[levels-1].arrived(process); // enter at the highest
    }

    @Override
    public void ready(Process process, int time) {
        int p = process.getPriority();
        strategies[p].ready(process, time);
    }

    @Override
    public void blocked(Scheduler scheduler) {
        Process CPU = scheduler.getCPU();
        int p = CPU.getPriority();
        strategies[p].blocked(scheduler);
        if (resetOnBlock) {
            CPU.resetCurrentRuntime();
        }
    }

    @Override
    public void preempt(Scheduler scheduler) {
        Process CPU = scheduler.getCPU();
        int p = CPU.getPriority();
        strategies[p].preempt(scheduler);
    }

    @Override
    public boolean preemptCondition(Scheduler scheduler) {
        Process CPU = scheduler.getCPU();
        int p = CPU.getPriority();
        // Check if any processes are in the higher queues
        for (int i = levels - 1; i > p; i--) {
            PriorityQueue<Process> curr = strategies[i].getReadyQueue();
            if (!curr.isEmpty()) {
                return true;
            }
        }
        // See if we've exceeded this level's quantum
        return strategies[p].preemptCondition(scheduler);
    }

    @Override
    public Process next() {
        for (int i = levels - 1; i >= 0; i--) {
            if (strategies[i].getReadyQueue().peek() != null) {
                return strategies[i].getReadyQueue().poll();
            }
        }
        return null;
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
        for (int i = 0; i < levels; i++) {
            Iterator<Process> itr = strategies[i].getReadyQueue().iterator();
            while (itr.hasNext()) {
                Process curr = itr.next();
                curr.incrementWaitingTime();
                int pid = curr.getPID();
                currentWaitTime[pid]++;
                if (currentWaitTime[pid] >= maxWait &&
                        curr.getPriority() < levels - 1) { // if our process waited long enough
                    // promote
                    currentWaitTime[pid] = 0; // reset its wait time
                    promoteToTop(curr); // set it to top
                    curr.setReadyTime(time);
                    strategies[curr.getPriority()].getReadyQueue().add(curr); // add to appropriate queue
                    itr.remove(); // remove from current queue
                }
            }
        }
    }

    public int getMaxWait() {
        return maxWait;
    }

    public AbstractStrategy[] getStrategies() {
        return strategies;
    }

    public int[] getDemoteQuantums() {
        return demoteQuantums;
    }

    public int[] getQuantums() {
        return quantums;
    }

    public int[] getCurrentRunTime() {
        return currentRunTime;
    }

    public void incrementProcessRunTime(Process process) {
        currentRunTime[process.getPID()]++;
    }

    public void resetProcessRunTime(Process process) {
        currentRunTime[process.getPID()] = 0;
    }

    public int getProcessRunTime(Process process) {
        return currentRunTime[process.getPID()];
    }

    public void resetWaitTime(Process process) {
        currentWaitTime[process.getPID()] = 0;
    }

    public void promoteToTop(Process process) {
        process.setPriority(levels - 1);
    }

    @Override
    public PriorityQueue<Process> getReadyQueue() {
        for (int i = strategies.length - 1; i >= 0; i--) {
            if (strategies[i].getReadyQueue() != null && !strategies[i].getReadyQueue().isEmpty()) {
                return strategies[i].getReadyQueue();
            }
        }
        return null;
    }

    public ArrayList<PriorityQueue<Process>> getReadyQueues() {
        ArrayList<PriorityQueue<Process>> result = new ArrayList<>();
        for (int i = 0; i < strategies.length; i++) {
            PriorityQueue<Process> rdy = strategies[i].getReadyQueue();
            PriorityQueue<Process> add = new PriorityQueue<Process>(rdy.comparator());
            Iterator<Process> itr = rdy.iterator();
            while (itr.hasNext()) {
                add.add(itr.next());
            }
            result.add(add);
        }
        return result;
    }

    @Override
    public String getName() {
        return super.getName();
    }

    @Override
    public String readyQueueToString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < levels; i++) {
            sb.append("Ready Queue L");
            sb.append(i);
            sb.append(" (Q=");
            sb.append(quantums[i]);
            sb.append("): ");
            if (!strategies[i].getReadyQueue().isEmpty()) {
                Iterator<Process> itr = strategies[i].getReadyQueue().iterator();
                while (itr.hasNext()) {
                    sb.append("P");
                    sb.append(itr.next().getPID());
                    sb.append(" ");
                }
                sb.append("\n");
            }
            else {
                sb.append("{ NONE }\n");
            }
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }
}
