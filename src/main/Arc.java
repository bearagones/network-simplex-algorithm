public class Arc {
    private Node tail;
    private Node head;
    private int flow;
    private int cost;
    private int reducedCost;
    private int label;

    Arc(Node tail, Node head, int cost) {
        this.tail = tail;
        this.head = head;
        this.flow = -1;
        this.cost = cost;
        this.reducedCost = -1;
        this.label = 0;
    }

    public Node getHead() {
        return head;
    }

    public Node getTail() {
        return tail;
    }

    public int getFlow() {
        return flow;
    }

    public void setFlow(int flow) {
        this.flow = flow;
    }

    public int getCost() {
        return cost;
    }

    public int getReducedCost() {
        return reducedCost;
    }

    public void setReducedCost(int reducedCost) {
        this.reducedCost = reducedCost;
    }

    public int getLabel() {
        return label;
    }

    public void setLabel(int label) {
        this.label = label;
    }

    Node getOpposite(Node n) {
        if (n.getLabel() == head.getLabel()) {
            return tail;
        } else if (n.getLabel() == tail.getLabel()) {
            return head;
        } else {
            return null;
        }
    }

    @Override
    public String toString() {
        return "(" + tail + "," + head + ")";
    }
}
