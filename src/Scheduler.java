import javax.swing.*;
import java.util.*;
import java.io.*;

public class Scheduler {

    // A snapshot of everything at each time unit
    private static ArrayList<Scheduler> timeline = new ArrayList<>();

    private static ArrayList<SchedulerState> history = new ArrayList<>();

    // What the blocked processes list looks at the current time
    private ArrayList<Process> blockedList;
    private ArrayList<String> readyQueueString;


    private Process CPU; // Active process
    private AbstractStrategy strategy; // Which algorithm?
    private Process[] table; // Process table
    private int numProcesses;

    private int timer;
    private int contextSwitch;
    private int idle;

    private static ArrayList<String> DQRRstats = new ArrayList<>();

    public Scheduler(AbstractStrategy strategy, Process[] table) {
        CPU = null;
        this.strategy = strategy;
        this.table = table;
        numProcesses = table.length;
        idle = -1;
        // to print later
        blockedList = new ArrayList<>();
        readyQueueString = new ArrayList<>();
        contextSwitch = -1;
    }

    public static Scheduler copyScheduler(Scheduler copy) {
        Scheduler result = new Scheduler(AbstractStrategy.copyAbstractStrategy(copy.strategy), copy.table);
        if (copy.CPU == null) {
            result.CPU = null;
        } else result.CPU = new Process(copy.CPU);
        result.table = new Process[copy.table.length];
        for (int i = 0; i < copy.table.length; i++) {
            result.table[i] = new Process(copy.table[i]);
        }
        result.contextSwitch = copy.contextSwitch;
        result.idle = copy.idle;
        result.timer = copy.timer;
        ArrayList<Process> blocked = Strategy.getBlockedProcesses();
        for (int i = 0; i < blocked.size(); i++) {
            result.blockedList.add(blocked.get(i));
        }
        for (int i = 0; i < copy.readyQueueString.size(); i++) {
            result.readyQueueString.add(copy.readyQueueString.get(i));
        }
        return result;
    }

    /**
     * work() is called every iteration or "time step". It does the following:
     * 1. Handle arriving processes (if any)
     * 2. Progress blocked processes
     * 3. Progress ready processes
     * 4. If the CPU is occupied, make progress on the CPU
     * 5. If the CPU is IDLE, assign the next process (if any)
     * 6. Save the state of scheduler into a list.
     */
    public void work() {
        System.out.println("TIMER: " + timer);
        // Check for arriving processes
        for (int i = 0; i < numProcesses; i++) {
            if (table[i].getArrivalTime() == timer) {
                strategy.arrived(table[i]);
                table[i].setReadyTime(timer);
            }
        }
        // Make progress on blocked processes: see if any are ready
        strategy.progressBlocked(timer);
        // Make progress on ready processes: promote/demote, update stats
        strategy.progressReady(timer);
        // Make progress on CPU: should it be preempted?
        if (CPU != null) {
            progressCPU(); // make progress on the CPU
        }
        // If our CPU is free
        if (CPU == null) {
            Process next = strategy.next();
            if (next != null) {
                incrementContextSwitch();
                CPU = next;
                if (strategy instanceof SimpleMLFQ) {
                    ((SimpleMLFQ) strategy).resetWaitTime(CPU);
                }
                // Since progressReady() incremented it
                CPU.decrementWaitingTime();
                if (CPU.getFirstTimeOnCPU() == 0) {
                    CPU.setFirstTimeOnCPU(timer);
                }
                System.out.println("Running Process" + CPU.getPID());
            } else incrementIdle(); // If we fail to assign a process to the CPU
        }
        record();
        incrementTimer();
    }

    public void progressCPU() {
        CPU.decrementBurst(); // compute
        CPU.incrementCurrentRuntime(); // increment how long it ran for
        if (strategy instanceof SimpleMLFQ) {
            // we must do additional things for SimpleMLFQ
            ((SimpleMLFQ) strategy).incrementProcessRunTime(CPU);
            if (((SimpleMLFQ) strategy).getProcessRunTime(CPU) >=
                    ((SimpleMLFQ) strategy).getDemoteQuantums()[CPU.getPriority()]
                    && CPU.getPriority() > 0) {
                ((SimpleMLFQ) strategy).resetProcessRunTime(CPU);
                CPU.decrementPriority();
            }
        }
        // Did the CPU finish?
        if (CPU.isFinished()) {
            complete();
        }
        // If not, does it need to enter IO?
        else if (CPU.finishedCurrentBurst()) {
            strategy.blocked(this);
        }
        // If also no, did it get preempted?
        else if (strategy.preemptCondition(this) && !strategy.getReadyQueue().isEmpty()) {
            strategy.preempt(this);
        }
    }

    /**
     * What happens when the process running finishes?
     * 1. CPU is freed (CPU = null)
     * 2. Mark the process as completed
     * 3. Save the time it completes
     */
    public void complete() {
        CPU.setFinishTime(timer);
        System.out.println("Process" + CPU.getPID() + " completed!");
        freeCPU();
    }

    public boolean terminate() {
        for (int i = 0; i < numProcesses; i++) {
            if (table[i].getFinishTime() == 0) {
                return false;
            }
        }
        return true;
    }

    public void record() {
        readyQueueString.add(strategy.readyQueueToString());
        history.add(new SchedulerState(this));
    }

    public Process getCPU() {
        return CPU;
    }

    public int getNumProcesses() {
        return numProcesses;
    }

    public int getTimer() {
        return timer;
    }

    public int getIdle() {
        return idle;
    }

    public int getContextSwitch() {
        return contextSwitch;
    }

    public void freeCPU() {
        CPU = null;
    }

    public void incrementTimer() {
        timer++;
    }

    public void incrementIdle() {
        idle++;
    }

    public void incrementContextSwitch() {
        contextSwitch++;
    }

    public static ArrayList<Scheduler> getTimeline() {
        return timeline;
    }

    public static ArrayList<SchedulerState> getHistory() {
        return history;
    }

    public Process[] getTable() {
        return table;
    }

    public AbstractStrategy getStrategy() {
        return strategy;
    }

    public ArrayList<Process> getBlockedList() {
        return blockedList;
    }

    /********************************
     * COMPUTE PERFORMANCE AVERAGES *
     ********************************/

    public double averageWait() {
        double sum = 0;
        for (int i = 0; i < table.length; i++) {
            sum += table[i].getWaitingTime();
        }
        return sum / table.length;
    }

    public double averageTurnaround() {
        double sum = 0;
        for (int i = 0; i < table.length; i++) {
            sum += table[i].getTurnaroundTime();
        }
        return sum / table.length;
    }

    public double averageResponse() {
        double sum = 0;
        for (int i = 0; i < table.length; i++) {
            sum += table[i].getResponseTime();
        }
        return sum / table.length;
    }

    public double utilization() {
        return (timer - idle - 1) / (timer - 1.0);
    }

    public float throughput() {
        float numProcess = table.length;
        return numProcess / (timer - 1);
    }

    public void displayPerformanceMeasurements() {
        System.out.println("Scheduling Algorithm: " + strategy.getName());
        System.out.println("Number of Processes: " + getNumProcesses());
        System.out.printf("CPU Utilization: %.2f\n", utilization() * 100);
        System.out.printf("Throughput: %.4f\n", throughput());
        System.out.printf("Average Wait Time: %.2f\n", averageWait());
        System.out.printf("Average Turnaround Time: %.2f\n", averageTurnaround());
        System.out.printf("Average Response Time: %.2f\n", averageResponse());
        System.out.println("Number of Context Switches: " + contextSwitch);
        System.out.println("===================================");
    }

    /****************************
     * DISPLAY DETAILED HISTORY *
     ****************************/

    public static void displayTimeline() {
        for (Scheduler curr : timeline) {
            System.out.println("Time = " + curr.timer);
            System.out.println("PID\t\tArrival\t\tPriority\t\tBursts [CPU IO CPU ... CPU]");
            for (int i = 0; i < curr.getNumProcesses(); i++) {
                System.out.println(curr.table[i].getPID() + "\t\t" + curr.table[i].getArrivalTime() + "\t\t\t" +
                        curr.table[i].getPriority() + "\t\t\t\t" + curr.table[i].burstToString());
            }
            if (curr.CPU != null) {
                System.out.println("CPU: P" + curr.CPU.getPID());
            } else {
                System.out.println("CPU: IDLE");
            }

            System.out.println(curr.readyQueueString.get(curr.timer));

            StringBuilder sb = new StringBuilder();
            sb.append("Blocked Processes: ");
            if (curr.blockedList.size() == 0) {
                sb.append("{ NONE }");
            } else {
                for (int i = 0; i < curr.blockedList.size(); i++) {
                    sb.append("P");
                    sb.append(curr.blockedList.get(i).getPID());
                    sb.append(" ");
                }
            }
            sb.append("\nNumber of context switches: " + curr.getContextSwitch());
            System.out.println(sb.toString());
            if (!DQRRstats.isEmpty()) {
                System.out.println(DQRRstats.get(curr.timer));
            }
            System.out.println("===================================");
        }
    }

    public void displayHistory() {
        for (SchedulerState curr : history) {
            System.out.println("Time = " + curr.getTimer());
            System.out.println("PID\t\tArrival\t\tPriority\t\tBursts [CPU IO CPU ... CPU]");
            for (int i = 0; i < curr.getTable().length; i++) {
                System.out.println(curr.table[i].getPID() + "\t\t" + curr.table[i].getArrivalTime() + "\t\t" +
                        curr.table[i].getPriority() + "\t\t\t" + curr.table[i].burstToString());
            }
            if (curr.CPU != null) {
                System.out.println("CPU: P" + curr.CPU.getPID());
            } else {
                System.out.println("CPU: IDLE");
            }
            StringBuilder sb = new StringBuilder();
            if (strategy instanceof SimpleMLFQ) {

            }
            ArrayList<PriorityQueue<Process>> readyQueues = curr.getReadyQueue();
            System.out.println("READY QUEUES SIZE: " + readyQueues.size());
            for (int i = 0; i < readyQueues.size(); i++) {
                sb.append("Ready Queue " + i + ": ");
                PriorityQueue<Process> currReadyQueue = readyQueues.get(i);
                if (!currReadyQueue.isEmpty()) {
                    Iterator<Process> itr = currReadyQueue.iterator();
                    while (itr.hasNext()) {
                        sb.append("P");
                        sb.append(itr.next().getPID());
                        sb.append(" ");
                    }
                } else sb.append("{ NONE }");
                sb.append("\n");
            }

            sb.append("Blocked Processes: ");
            if (curr.blockedList.size() == 0) {
                sb.append("{ NONE }");
            } else {
                for (int i = 0; i < curr.blockedList.size(); i++) {
                    sb.append("P");
                    sb.append(curr.blockedList.get(i).getPID());
                    sb.append(":");
                    sb.append(curr.blockedList.get(i).getCurrentBurst());
                    sb.append(" ");
                }
            }
            sb.append("\nNumber of context switches: " + curr.getContextSwitch());
            System.out.println(sb.toString());
            if (!DQRRstats.isEmpty()) {
                System.out.println(DQRRstats.get(curr.timer));
            }
            System.out.println("===================================");
        }
    }

    public class SchedulerState {

        private Process[] table;
        private Process CPU;
        private ArrayList<PriorityQueue<Process>> readyQueue;
        private ArrayList<Process> blockedList;
        private int[] quantums;

        private int timer;
        private int contextSwitch;
        private int idle;


        /**
         * Captures the state of the scheduler at any given time. This is almost
         * like a copy constructor, but is capable of better representing
         * a snapshot.
         * @param scheduler
         */
        public SchedulerState(Scheduler scheduler) {
            table = new Process[scheduler.table.length];
            for (int i = 0; i < table.length; i++) {
                table[i] = new Process(scheduler.table[i]);
            }
            blockedList = new ArrayList<>();
            ArrayList<Process> blocked = Strategy.getBlockedProcesses();
            for (int i = 0; i < blocked.size(); i++) {
                blockedList.add(new Process(blocked.get(i)));
            }

            readyQueue = new ArrayList<>();
            if (scheduler.CPU != null) {
                this.CPU = new Process(scheduler.CPU);
            }
            else this.CPU = null;

            quantums = new int[scheduler.getStrategy().getNumStrategies()];

            if (scheduler.strategy instanceof SimpleMLFQ) {
                SimpleMLFQ strat = (SimpleMLFQ) scheduler.strategy;
                quantums = strat.getQuantums();
                ArrayList<PriorityQueue<Process>> queues = strat.getReadyQueues();
                for (int i = 0; i < queues.size(); i++) {
                    PriorityQueue<Process> queue = queues.get(i);
                    readyQueue.add(i, new PriorityQueue<>(queue.comparator()));
                    Iterator<Process> itr = queue.iterator();
                    while (itr.hasNext()) {
                        readyQueue.get(i).add(new Process(itr.next()));
                    }
                }
            }
            else {
                if (scheduler.strategy instanceof RoundRobin ) {
                    quantums[0] = ((RoundRobin) scheduler.strategy).getQuantum();
                }
                if (scheduler.strategy instanceof DQRR) {
                    quantums[0] = ((DQRR) scheduler.strategy).getQuantum();
                }
                PriorityQueue<Process> queue = scheduler.getStrategy().getReadyQueue();
                readyQueue.add(new PriorityQueue<>(queue.comparator()));
                Iterator<Process> itr = queue.iterator();
                while (itr.hasNext()) {
                    readyQueue.get(0).add(new Process(itr.next()));
                }
            }
            timer = scheduler.timer;
            contextSwitch = scheduler.contextSwitch;
            idle = scheduler.idle;
        }

        public Process[] getTable() {
            return table;
        }

        public Process getCPU() {
            return CPU;
        }

        public ArrayList<PriorityQueue<Process>> getReadyQueue() {
            return readyQueue;
        }

        public ArrayList<Process> getBlockedList() {
            return blockedList;
        }

        public int getTimer() {
            return timer;
        }

        public int getIdleTime() {
            return idle;
        }

        public int getContextSwitch() {
            return contextSwitch;
        }

        public int[] getQuantums() {
            return quantums;
        }

    }

    /**
     * The method to run the scheduler.
     * @param scheduler
     */
    public static void runScheduler(Scheduler scheduler) {
        while (!scheduler.terminate()) {
            scheduler.work();
            if (scheduler.strategy instanceof DQRR) {
                DQRRstats.add(((DQRR) scheduler.strategy).displayStats());
            }
        }
        scheduler.displayHistory();
        scheduler.displayPerformanceMeasurements();
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JFrame frame = new SchedulerAnimation(scheduler);
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.pack();
                frame.setLocationRelativeTo(null); // center the application window
                frame.setVisible(true);            // show it
            }
        });
    }

    /**
     * Usage: java Scheduler "path_to_process_table.txt" algorithm
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        /*
        To automatically import data from a text file. The file format should be
        as follows:
        The first line is an integer, defining the number of processes
        Each line after begins with process ID, and should be in order from 0 to
        number of processes - 1
        The subsequent lines are eight integers separated by a space
        PID arrivalTime Priority CPUBurst0 IOBurst0 CPUBurst1 IOBurst1 CPUBurst2
         */
        String line;
        int pid, arrival,priority;
        int[] burst;
        //input filename as parameter
        BufferedReader br = new BufferedReader (new FileReader(args[0]));
        //first line is number of processes
        int number_of_process = Integer.parseInt(br.readLine());
        Process[] table = new Process[number_of_process];
        int icount = 0;
        while ((line = br.readLine() ) != null && icount < number_of_process){
            StringTokenizer st = new StringTokenizer(line);
            pid = Integer.parseInt(st.nextToken());
            arrival = Integer.parseInt(st.nextToken());
            priority = Integer.parseInt(st.nextToken());
            burst = new int[5]; // To support 5 bursts (can change to any odd value)
            // Make sure the text file being imported has the correct number of bursts
            for (int i = 0; i<burst.length; i++){
                burst[i] = Integer.parseInt(st.nextToken());
            }
            Process process = new Process (pid, arrival,priority,burst);
            table[icount] = process;
            icount++;
        }
        Scheduler scheduler = null;
        switch (args[1]) {
            case "FCFS": // first-come, first-served
                scheduler = new Scheduler(new FCFS(), table);
                break;
            case "P": // simple priority queue
                scheduler = new Scheduler(new Priority(), table);
                break;
            case "SJF": // shortest job first
                scheduler = new Scheduler(new SJF(), table);
                break;
            case "SJRF": // shortest job remaining first
                scheduler = new Scheduler(new SJRF(), table);
                break;
            case "RR": // round robin
                if (args.length == 3) {
                    scheduler = new Scheduler(new RoundRobin(Integer.parseInt(args[2])), table);
                }
                else scheduler = new Scheduler(new RoundRobin(), table);
                break;
            case "DQRR": // dynamic queue round robin
                scheduler = new Scheduler(new DQRR(), table);
                break;
            case "MLFQ": // multi level feedback queue
                scheduler = new Scheduler(new SimpleMLFQ(table.length), table);
                break;
        }
        runScheduler(scheduler);
    }

}
