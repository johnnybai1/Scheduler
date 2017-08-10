public class RoundRobin extends AbstractStrategy implements Strategy {

    private int quantum;

    public RoundRobin() {
        this(5);
    }

    /**
     * @param quantum: how long of a quantum is this round robin assigned?
     */
    public RoundRobin(int quantum) {
        super("Round Robin");
        this.quantum = quantum;
    }

    public int getQuantum() {
        return quantum;
    }

    @Override
    public boolean preemptCondition(Scheduler scheduler) {
        Process CPU = scheduler.getCPU();
        return (CPU.getCurrentRuntime() >= quantum && !getReadyQueue().isEmpty());
    }
}
