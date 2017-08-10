public class SJRF extends AbstractStrategy implements Strategy {

    public SJRF() {
        super("Shortest Job Remaining First", 1, new BurstComparator());
    }

    @Override
    public boolean preemptCondition(Scheduler scheduler) {
        Process CPU = scheduler.getCPU();
        if (!getReadyQueue().isEmpty()) {
            return getReadyQueue().peek().getCurrentBurst() < CPU.getCurrentBurst();
        }
        return false;
    }



}
