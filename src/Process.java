public class Process {

    private int PID; // Process ID
    private int arrivalTime;
    private int priority;
    private int[] bursts; // CPU/IO burst times

    private int tracker; // to keep track of which burst we are on
    private int currentRuntime; // to decide when this process' priority
                                // needs to be reconsidered
    private int readyTime; // to keep track of when this process entered ready state

    // to track overall stats of a Strategy
    private int finishTime;
    private int waitingTime;
    private int firstTimeOnCPU;

    public Process(int PID, int arrivalTime, int priority, int[] bursts) {
        this.PID = PID;
        this.arrivalTime = arrivalTime;
        this.priority = priority;
        this.bursts = bursts;
        this.readyTime = arrivalTime;
    }

    public Process(Process copy) {
        PID = copy.PID;
        arrivalTime = copy.arrivalTime;
        priority = copy.priority;
        bursts = new int[copy.bursts.length];
        for (int i = 0; i < bursts.length; i++) {
            bursts[i] = copy.bursts[i];
        }
        readyTime = copy.readyTime;
        tracker = copy.tracker;

        currentRuntime = copy.currentRuntime;
        finishTime = copy.finishTime;
        waitingTime = copy.waitingTime;
        firstTimeOnCPU = copy.firstTimeOnCPU;
    }

    /***********
     *Modifiers*
     ***********/

    public void incrementTracker() {
        tracker++;
    }

    public void decrementBurst() {
        if (bursts[tracker]>0) bursts[tracker]--;
    }

    public void incrementPriority() {
        priority++;
    }

    public void decrementPriority() {
        priority--;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public void incrementWaitingTime() {
        waitingTime++;
    }

    public void decrementWaitingTime() {
        waitingTime--;
    }

    public void setFirstTimeOnCPU(int time) {
        firstTimeOnCPU = time;
    }

    public void setFinishTime(int time) {
        finishTime = time;
    }

    public void incrementCurrentRuntime() {
        currentRuntime++;
    }

    public void resetCurrentRuntime() {
        currentRuntime = 0;
    }

    public void setReadyTime(int time) {
        readyTime = time;
    }


    /*********
     *Getters*
     *********/
    public int getPID() {
        return PID;
    }

    public int getArrivalTime() {
        return arrivalTime;
    }

    public int getPriority() {
        return priority;
    }

    public int[] getBursts() {
        return bursts;
    }

    public int getTracker() {
        return tracker;
    }

    public int getCurrentRuntime() {
        return currentRuntime;
    }

    public int getCurrentBurst() {
        return bursts[tracker];
    }

    public boolean onFinalBurst() {
        return tracker == bursts.length - 1;
    }

    public boolean finishedCurrentBurst() {
        return getCurrentBurst() == 0;
    }

    public boolean isFinished() {
        return onFinalBurst() && getCurrentBurst() == 0;
    }

    public int getReadyTime() {
        return readyTime;
    }

    /****************
     *Stats Tracking*
     ****************/

    public int getFinishTime() {
        return finishTime;
    }

    public int getWaitingTime() {
        return waitingTime;
    }

    public int getFirstTimeOnCPU() {
        return firstTimeOnCPU;
    }

    public int getResponseTime() {
        return firstTimeOnCPU - arrivalTime;
    }

    public int getTurnaroundTime() {
        return finishTime - arrivalTime;
    }


    /**********
     * Prints *
     **********/
    public String burstToString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[ ");
        for (int i = 0; i < bursts.length; i++) {
            sb.append(bursts[i]);
            sb.append(" ");
        }
        sb.append("]");
        return sb.toString();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[Process");
        sb.append(PID);
        sb.append(" ");
        sb.append(arrivalTime);
        sb.append(" ( ");
        for (int i = 0; i < bursts.length; i++) {
            sb.append(bursts[i]);
            sb.append(" ");
        }
        sb.append(") ");
        sb.append(tracker);
        sb.append("]");
        return sb.toString();
    }

}