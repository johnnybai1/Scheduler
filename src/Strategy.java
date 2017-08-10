import java.util.ArrayList;
import java.util.Comparator;
import java.util.PriorityQueue;

/**
 * Every Strategy must define these methods. Strategies will maintain its own ready queue
 */
public interface Strategy {

    // If using multiple Strategies, they all share the same list of blocked processes
    ArrayList<Process> blockedProcesses = new ArrayList<>();

    /**
     * What happens when a process arrives?
     * 1. Add it to the ready queue
     */
    void arrived(Process process);

    /**
     * What happens when a process becomes ready (unblocked)?
     * 1. Increment its tracker
     * 2. Remove it from the blocked list
     * 3. Add it to the ready list
     */
    void ready(Process process, int time);

    /**
     * What happens when a process becomes blocked? Similar to preempt.
     * 1. Increment its tracker
     * 2. CPU is freed (CPU = null)
     * 3. The process is added to the blocked list
     * 4. (optional) Reset the process's running time (for use in priority adjustment)
     */
    void blocked(Scheduler scheduler);

    /**
     * What does the scheduler's state look like for the CPU to be preempted?
     */
    boolean preemptCondition(Scheduler scheduler);

    /**
     * What happens when the CPU is preempted?
     * 1. CPU is freed (CPU = null)
     * 2. The process returns to the ready queue
     * 3. (optional) Reset the process's running time (for use in priority adjustment)
     */
    void preempt(Scheduler scheduler);


    /**
     * What is the next process that runs? Depends on the strategy's implementation
     * of the Comparator for the priority queue
     */
    Process next();

    /**
     * Make progress on the blocked processes
     */
    void progressBlocked(int time);

    /**
     * Make progress on the ready processes
     */
    void progressReady(int time);


    PriorityQueue<Process> getReadyQueue();

    static ArrayList<Process> getBlockedProcesses() {
        return blockedProcesses;
    }

    class NullComparator implements Comparator<Process> {
        @Override
        public int compare(Process o1, Process o2) {
            if (o1.getReadyTime() < o2.getReadyTime()) {
                return -1;
            }
            if (o1.getReadyTime() > o2.getReadyTime()) {
                return 1;
            }
            return 0;
        }
    }

    class PriorityComparator implements Comparator<Process> {

        @Override
        public int compare(Process o1, Process o2) {
            if (o1.getPriority() > o2.getPriority()) {
                return 1;
            }
            if (o1.getPriority() < o2.getPriority()) {
                return -1;
            }
            if (o1.getPriority() == o2.getPriority()) {
                if (o1.getReadyTime() < o2.getReadyTime()) {
                    return -1;
                }
                if (o1.getReadyTime() > o2.getReadyTime()) {
                    return 1;
                }
            }
            return 0;
        }
    }

    class BurstComparator implements Comparator<Process> {

        @Override
        public int compare(Process o1, Process o2) {
            if (o1.getCurrentBurst() > o2.getCurrentBurst()) {
                return 1;
            }
            if (o1.getCurrentBurst() < o2.getCurrentBurst()) {
                return -1;
            }
            return 0;
        }
    }

}
