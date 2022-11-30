import Jama.*;
import java.io.*;
import java.util.*;

public class NetworkSimplex {
    ArrayList<Node> nodeList = new ArrayList<>(); // an arraylist of the nodes in the network
    ArrayList<Arc> arcList = new ArrayList<>(); // an arraylist of the directed arcs in the network
    ArrayList<Arc> completeArcList = new ArrayList<>(); // an arraylist of all undirected arcs in the network
    ArrayList<Node> nodeCycleList = new ArrayList<>(); // an arraylist of the nodes in the current cycle

    // constructor to create a network object with the specified number of nodes
    private NetworkSimplex(int totalNodes) {
        for (int i = 0; i < totalNodes; i++) {
            nodeList.add(new Node(i + 1));
        }
    }

    // helper method that gets node from the node list
    private Node getNode(int i){
        return nodeList.get(i - 1);
    }

    // helper method that converts node the node list
    private Node getNode(Node n){
        return getNode(n.getLabel());
    }

    // helper method that gets the arc between two nodes
    private Arc getArc(Node tail, Node head) {
        for (Arc arc : tail.getArcList()) {
            Node u = arc.getTail();
            Node v = arc.getHead();
            if ((tail == u && head == v) || (tail == v && head == u)) {
                return arc;
            }
        }
        return null;
    }

    // creates a network based on the .txt file
    private static NetworkSimplex createNetwork(File file) throws FileNotFoundException {
        Scanner scan = new Scanner(file);

        int totalNodes = scan.nextInt(); // first line represents the total number of nodes
        scan.nextLine();

        NetworkSimplex network = new NetworkSimplex(totalNodes); // creates a network object

        for (int i = 1; i <= totalNodes; i++) { // adding the arcs to the network
            String arc = scan.nextLine();
            String[] splitLine = arc.split(",");
            int quantityLabel = Integer.parseInt(splitLine[0]); // first integer corresponds to the quantity
            Node tail = network.nodeList.get(i - 1); //
            tail.setQuantity(quantityLabel);
            for (int j = 1; j < splitLine.length; j+=2) { // for the rest of the line
                int headLabel = Integer.parseInt(splitLine[j]); // first integer corresponds to connecting node (head)
                Node head = network.nodeList.get(headLabel - 1);
                int cost = Integer.parseInt(splitLine[j+1]); // second integer corresponds to the cost of the arc between tail and head
                network.insertArc(tail, head, cost); // inserts the resulting arc from the info
            }
        }
        network.printNetwork(); // visually prints out the network
        System.out.println('\n');
        return network;
    }

    // inserts arcs with the specified tail, head, and cost
    private void insertArc(Node tail, Node head, int cost) {
        Arc arc = new Arc(tail, head, cost); // creates a new arc object
        tail.getArcList().add(arc); // adds the arc to the corresponding arc list of the origin node
        arcList.add(arc); // adds the arc to the overall arc list
    }

   // generates a matrix based on the network
    private Matrix generateMatrix() {
        int rowDelete = 0; // corresponds to the row to be removed
        double[][] incidenceMatrix = new double[nodeList.size()][arcList.size()];
        double[][] associatedMatrix = new double[nodeList.size() - 1][arcList.size()];

        for (int i = 0; i < nodeList.size(); i++) {
            for (int j = 0; j < arcList.size(); j++) {
                if (arcList.get(j).getTail() == nodeList.get(i)) {
                    incidenceMatrix[i][j] = 1; // entry labelled 1 if node i is the tail of the arc
                } else if (arcList.get(j).getHead() == nodeList.get(i)) {
                    incidenceMatrix[i][j] = -1; // entry labelled -1 if node i is the head of the arc
                } else {
                    incidenceMatrix[i][j] = 0; // entry labelled 0 is node i is not connected to the arc
                }
            }
            for (int k = 0; k < arcList.size(); k++) {
                if (incidenceMatrix[i][k] == 1) { // repeats until it finds a row that solely contains -1's with 0's
                    rowDelete = i + 1; // this means that it is the "last node" in the sequence
                    break;
                }
            }
        }

        System.out.println("Incidence Matrix:");
        printMatrix(incidenceMatrix); // prints out the resulting incidence matrix

        for (int i = 0; i < nodeList.size(); i++) {
            if (i == rowDelete) { // will skip adding the "last node" in the incidence matrix
                continue;
            }
            for (int j = 0; j < arcList.size(); j++) {
                if (arcList.get(j).getTail() == nodeList.get(i)) {
                    associatedMatrix[i][j] = 1; // entry labelled 1 if node i is the tail of the arc
                } else if (arcList.get(j).getHead() == nodeList.get(i)) {
                    associatedMatrix[i][j] = -1; // entry labelled -1 if node i is the head of the arc
                } else {
                    associatedMatrix[i][j] = 0; // entry labelled 0 is node i is not connected to the arc
                }
            }
        }

        System.out.println("Associated Matrix:");
        printMatrix(associatedMatrix); // prints out the resulting associated matrix

        return Matrix.constructWithCopy(associatedMatrix); // constructs a matrix based on the associated matrix
    }

    // finds an initial BFS based on the matrix
    private void findInitialBFS(Matrix matrix) {
        Matrix basis = new Matrix(nodeList.size() - 1, nodeList.size() - 1); // basis is a square matrix
        LUDecomposition decomposition = matrix.transpose().lu(); // uses LU decomposition to find basis
        Matrix transposedU = decomposition.getU();
        Matrix u = transposedU.transpose();
        int columnCounter = 0; // corresponds to the current column of the basis being found

        for (int i = 0; i < nodeList.size() - 2; i++) {
            for (int j = 0; j < arcList.size(); j++) {
                Matrix uColumn = u.getMatrix(0, nodeList.size() - 2, i, i); // column in upper triangular matrix
                Matrix matrixColumn = matrix.getMatrix(0, nodeList.size() - 2, j, j); // column in associated matrix
                boolean isSame = true;

                for (int k = 0; k < nodeList.size() - 1; k++) { // checks if the two columns are the same, if not, iterate to next column
                    if (uColumn.get(k, 0) != matrixColumn.get(k, 0)) {
                        isSame = false;
                        break;
                    }
                }

                if (isSame) { // if the two columns are the same, add it to the index of the basis of the corresponding column counter
                    basis.setMatrix(0, nodeList.size() - 2, columnCounter, columnCounter, matrixColumn);
                    arcList.get(j).setLabel(1); // set the arc as "used" in the network
                    columnCounter++; // iterate to next column of basis being found
                    i++;
                }
            }
        }
        System.out.println("Basis:");
        printMatrix(basis.getArray()); // prints out the initial basis

    }

    // updates the initial flow of the arcs
    private void initialFLow() {
        ArrayList<Arc> usedArcs = new ArrayList<>(); // an arraylist that contains the used arcs in the network

        for (Arc arc : arcList) { // for each arc in the arc list
            if (arc.getLabel() == 1) { // if the arc is labelled as "used"
                usedArcs.add(arc); // add the arc to the used arcs list
            }
        }

        for (int i = 0; i < usedArcs.size() - 1; i++) {
            if (i == 0) { // if we are looking at the first arc
                int flow = usedArcs.get(i).getTail().getQuantity(); // the flow is simply the quantity of the root node
                usedArcs.get(i).setFlow(flow); // set the flow
            }
            int flow = usedArcs.get(i).getFlow() + usedArcs.get(i + 1).getTail().getQuantity(); // otherwise, flow is
                                                                                                // equal to the
                                                                                                // flow of the previous
                                                                                                // connecting arc + the
                                                                                                // quantity of current tail
            usedArcs.get(i + 1).setFlow(flow); // set the flow
        }
    }

    // updates the flow of the arcs
    private void updateFLow(int oldFlow) {
        ArrayList<Arc> usedArcs = new ArrayList<>(); // an arraylist that contains the used arcs in the network

        for (Arc arc : arcList) { // for each arc in the arc list
            if (arc.getLabel() == 1) { // if the arc is labelled as "used"
                usedArcs.add(arc); // add the arc to the used arcs list
            }
        }

        for (int i = 0; i < usedArcs.size() - 1; i++) {
            if (i == 0) { // if we are looking at the first arc
                int flow = usedArcs.get(i).getTail().getQuantity(); // the flow is simply the quantity of the root node
                usedArcs.get(i).setFlow(flow); // set the flow
                continue; // continue onto the other arcs
            }
            Node u = getNode(nodeCycleList.get(i));
            Node v = getNode(nodeCycleList.get(i + 1));
            Arc cycleArc = getArc(u, v);
            for (Arc arc : usedArcs) {
                if (arc == cycleArc) {
                    int flow = arc.getFlow() - oldFlow;
                    usedArcs.get(i).setFlow(flow);
                    break;
                }
            }
        }
    }

    // calculates the duals of the nodes
    private void calculateDuals() {
        nodeList.get(0).setDual(0);

        for (Arc arc : arcList) {
            if (arc.getLabel() == 1) {
                int dualHead = getNode(arc.getTail()).getDual() - arc.getCost();
                getNode(arc.getHead()).setDual(dualHead);
            }
        }
    }

    // calculates the reduced costs of the unused arcs
    private void calculateReducedCost() {
        int smallestCost = 0;

        for (Arc arc : arcList) {
            if (arc.getLabel() == 0) {
                int reducedCost = arc.getCost() - arc.getTail().getDual() + arc.getHead().getDual();
                arc.setReducedCost(reducedCost);
                if (smallestCost > arc.getReducedCost()) {
                    smallestCost = arc.getReducedCost();
                }
            }
        }
    }

    // checks if the current set of arcs is optimal
    private boolean isOptimal() {
        boolean isOptimal = true;

        for (Arc arc : arcList) {
            if (arc.getLabel() == 1 && arc.getReducedCost() < 0) {
                isOptimal = false;
                break;
            }
        }
        return isOptimal;
    }

    // gets the entering arc based on the reduced cost
    private Arc getEnteringArc() {
        Arc enteringArc = null;
        int smallestReducedCost = 0;

        for (Arc arc : arcList) {
            if (arc.getLabel() == 0 && arc.getReducedCost() < 0) {
                if (smallestReducedCost > arc.getReducedCost()) {
                    smallestReducedCost = arc.getReducedCost();
                    enteringArc = arc;
                }
            }
        }
        return enteringArc;
    }

    // calculates the minimum cost of the optimal solution
    private int getMinimumCost() {
        ArrayList<Arc> optimalSolution = new ArrayList<>();
        int minimumCost = 0;

        for (Arc arc : arcList) {
            if (arc.getLabel() == 1) {
                optimalSolution.add(arc);
            }
        }

        for (Arc arc : optimalSolution) {
            minimumCost += arc.getCost();
        }

        return minimumCost;
    }

    // gets the neighboring nodes of the specified node
    private ArrayList<Node> getNeighbors(Node node) {
        ArrayList<Node> neighborsList = new ArrayList<>();
        for (Arc newEdge : getNode(node).getArcList()) {
            if (newEdge.getLabel() == 1 || newEdge.getLabel() == 2) {
                neighborsList.add(newEdge.getOpposite(getNode(node)));
            }
        }
        return neighborsList;
    }

    // checks if a cycle is created
    private ArrayList<Node> searchCycle(Node tail, Node head) {
        ArrayList<Node> nodePath = new ArrayList<>();
        boolean[] marked = new boolean[nodeList.size()];
        Node[] parent = new Node[nodeList.size()];

        nodeCycleList.clear();

        marked[tail.getLabel() - 1] = true;
        ArrayList<Node> markedNodes = new ArrayList<>();
        markedNodes.add(tail);
        parent[tail.getLabel() - 1] = tail;

        while (!markedNodes.isEmpty()) {
            Node markedNode = markedNodes.remove(0);
            for (Node neighbor : getNeighbors(markedNode)) {
                if (!marked[neighbor.getLabel() - 1]) {
                    marked[neighbor.getLabel() - 1] = true;
                    markedNodes.add(neighbor);
                    parent[neighbor.getLabel() - 1] = markedNode;
                }
            }
        }

        if (parent[head.getLabel() - 1] == null) {
            return null;
        }
        Node currently = head;
        nodePath.add(0, head);
        nodeCycleList.add(0, head);
        while (!currently.equals(tail)) {
            currently = parent[currently.getLabel() - 1];
            nodePath.add(0, currently);
            nodeCycleList.add(0, currently);
        }
        return nodePath;
    }

    // exchanges the exiting arc with the exiting arc
    private void exchange(Arc enteringArc) {
        Arc exitingArc = null;
        int smallestFlow = 100;

        for (Arc arc : arcList) {
            Arc oppositeArc = new Arc(arc.getHead(), arc.getTail(), 0);
            oppositeArc.getTail().getArcList().add(arc);
            completeArcList.add(oppositeArc);
            completeArcList.add(arc);
            if (arc.getLabel() == 1) {
                oppositeArc.setLabel(2);
            }
        }

        ArrayList<Node> nodePath = searchCycle(enteringArc.getTail(), enteringArc.getHead());
        if (nodePath != null) {
            for (int i = 0; i < nodePath.size() - 1; i++) {
                Node u = getNode(nodePath.get(i));
                Node v = getNode(nodePath.get(i + 1));
                Arc arc = getArc(u, v);
                if (arc.getFlow() < smallestFlow && arc.getLabel() == 1) {
                    smallestFlow = arc.getFlow();
                    exitingArc = arc;
                }
            }
        }

        enteringArc.setLabel(1);
        enteringArc.setFlow(exitingArc.getFlow());
        exitingArc.setLabel(0);
    }

    // method that runs the entire algorithm
    private static void algorithm(NetworkSimplex network) {

        Matrix matrix = network.generateMatrix();
        network.findInitialBFS(matrix);
        network.initialFLow();
        while (!network.isOptimal()) {
            network.calculateDuals();
            network.calculateReducedCost();
            Arc enteringArc = network.getEnteringArc();
            network.exchange(enteringArc);
            network.updateFLow(enteringArc.getFlow());
        }

        int minimumCost = network.getMinimumCost();
        System.out.println("The minimum cost is: " + minimumCost);
        for (Arc arc : network.arcList) {
            if (arc.getLabel() == 1) {
                System.out.println(arc);
            }
        }
    }

    // prints the initial network in the form of an adjacency list data structure
    private void printNetwork() {
        System.out.println("Adjacency List: ");
        for (int i = 1; i <= nodeList.size(); i++) {
            System.out.print(nodeList.get(i - 1).getLabel() + " => [");
            Node currentNode = nodeList.get(i - 1);
            for (Arc arc : currentNode.getArcList()) {
                System.out.print("(" + i + "," + arc.getOpposite(currentNode) + ")");
            }
            System.out.print("]\n");
        }
    }

    // prints out a matrix
    private void printMatrix(double[][] matrix) {
        System.out.println(Arrays.deepToString(matrix)
                .replace("], ", "]\n")
                .replace(", ", "  ")
                .replace("[[", "[")
                .replace("]]", "]") + '\n');
    }

    // main method
    public static void main(String[] args) throws FileNotFoundException {
        File file = new File(args[0]);
        NetworkSimplex network = createNetwork(file);
        algorithm(network);
    }
}