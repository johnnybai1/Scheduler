public class SJF extends AbstractStrategy implements Strategy {

    public SJF() {
        super("Shortest Job First", 1, new BurstComparator());
    }



}
