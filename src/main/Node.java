import java.util.ArrayList;

public class Node {
    private int label;
    private int quantity;
    private int dual;
    private boolean isHead;
    private boolean isTail;
    private ArrayList<Arc> arcList;

    Node(int label) {
        this.label = label;
        this.quantity = 0;
        this.dual = 0;
        this.isHead = false;
        this.isTail = false;
        this.arcList = new ArrayList<>();
    }

    public int getLabel() {
        return label;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getDual() {
        return dual;
    }

    public void setDual(int dual) {
        this.dual = dual;
    }

    public boolean isHead() {
        return isHead;
    }

    public void setIsHead(boolean head) {
        isHead = head;
    }

    public boolean isTail() {
        return isTail;
    }

    public void setIsTail(boolean tail) {
        isTail = tail;
    }

    public ArrayList<Arc> getArcList() {
        return this.arcList;
    }

    @Override
    public String toString() {
        return Integer.toString(label);
    }
}
