public class Priority extends AbstractStrategy implements Strategy {

    public Priority() {
        super("Priority Scheduling", 1, new PriorityComparator());
    }

    @Override
    public boolean preemptCondition(Scheduler scheduler) {
        Process CPU = scheduler.getCPU();
        if (!getReadyQueue().isEmpty()) {
            return getReadyQueue().peek().getPriority() > CPU.getPriority();
        }
        return false;
    }

}
