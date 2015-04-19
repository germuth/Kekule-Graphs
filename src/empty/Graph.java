package empty;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.Set;

/**
 * Graph
 * 
 * This represents an undirected, simple, (connected? not yet) graph.
 * 
 * In Kekule theory, certain nodes of the graphs are called ports and represent connections outside
 * the graph. The graph keeps track of the number of total nodes, the number of ports, and which
 * nodse are ports. The Edges of this graph are stored in an adjacency matrix, although each Edge is
 * converted to BitVector later in the program.
 * 
 * The Number of Nodes must never exceed 32 since the set of nodes is represented in BitVectors, and
 * the BitVector for a set of 32 size would overflow the integer. cP <= cN <= 32 Above is CORRECT
 * 
 * TODO this classed could be re-worked to conform with the rest of the program better
 */
public class Graph {
	private String name;
	private int numPorts;
	private int numNodes;
	/**
	 * Cell containing all the edges of this graph in bitvector form
	 */
	private Cell edgeCell;

	// take in graph from input
	public Graph(String name, int nP, int nC, Set<String> edges) {

		if (nC >= 31) {
			System.err.println("31 Nodes Reached");
			return;
		}
		Set<BitVector> bvEdges = new HashSet<BitVector>();

		for (String x : edges) {
			Scanner edgeScanner = new Scanner(x);
			int firstNode = edgeScanner.nextInt();
			int secondNode = edgeScanner.nextInt();

			// turn each node number into bit vector (<<)
			// the add together to get bit vector of two nodes
			// each node in the edge
			bvEdges.add(new BitVector((1 << firstNode) + (1 << secondNode)));
			edgeScanner.close();

		}

		this.name = name;
		this.numNodes = nC;
		this.numPorts = nP;
		this.edgeCell = new Cell(bvEdges, numPorts);
	}

	// TODO 31 nodes shit
	public Graph(String name, int nP, int nC, Cell edges) {
		this.numPorts = nP;
		this.numNodes = nC;
		if (this.numNodes >= 31) {
			System.err.println("31 Nodes Reached");
		}
		this.edgeCell = edges;
		this.name = name;
	}

	public Graph(Graph g) {
		this.name = g.name;
		this.numNodes = g.numNodes;
		if (this.numNodes >= 31) {
			System.err.println("31 Nodes Reached");
		}
		this.numPorts = g.numPorts;
		this.edgeCell = new Cell(g.edgeCell);
	}

	/**
	 * TODO is this required? Returns whether the graphs have identical edges
	 */
	@Override
	public boolean equals(Object obj) {
		Graph another = (Graph) obj;
		this.edgeCell.sortBySize();
		another.edgeCell.sortBySize();
		if (this.edgeCell.equals(another.edgeCell)) {
			return true;
		}
		return false;
	}

	public void addEdge(BitVector bv) {
		this.edgeCell.add(bv);
	}

	//TODO should return boolean
	public void removeEdge(BitVector bv) {
		// if doesn't contain edge
		if (!this.edgeCell.contains(bv)) {
			return;
		}
		int ports = this.edgeCell.getNumPorts();

		BitVector[] edges = new BitVector[this.edgeCell.size() - 1];
		int k = 0;

		for (int i = 0; i < this.edgeCell.size(); i++) {
			BitVector currentEdge = this.edgeCell.getPA()[i];
			if (!currentEdge.equals(bv)) {
				edges[k] = currentEdge;
				k++;
			}
		}
		this.edgeCell = new Cell(edges);
		this.edgeCell.setNumPorts(ports);
	}
	
	public boolean hasEdge(BitVector bv){
		return this.getEdgeCell().contains(bv);
	}

	
	/**
	 * Bad edge is one of the following: 
	 * 		(1) already contained in this graph 
	 * 		(2) is an invalid edge (doesn't reach two nodes) 
	 * 		(3) increase the degree on a node too high
	 */
	public boolean isBadEdge(BitVector edge) {
		// if edge goes to itself..
		if (edge.getNumber() - edge.firstBit() == 0) {
			return true;
		}
		// if we already have that edge
		if (this.edgeCell.contains(edge)) {
			return true;
		}
		// if it makes degree too high
		Graph temp = new Graph(this);
		temp.addEdge(edge);
		if (temp.hasInvalidDegree()) {
			return true;
		}
		return false;
	}

	/**
	 * Checks every node in the graph and determines its degree. 
	 * 		Any node with a degree greater than 3 is deemed unrealistic 
	 * 		Any port with a degree greater than 2 is deemed unrealistic
	 */
	public boolean hasInvalidDegree() {
		if (this.getHighestDegree() > 3 || this.getHighestPortDegree() > 2) {
			return true;
		}
		return false;
	}

	/**
	 * Checks every cycle in the graph and determines its size If any circle is not size 5 or 6, the
	 * cycle is not a realistic size Should size 7 cycles be included?
	 */
	public boolean hasInvalidCycles(ArrayList<ArrayList<BitVector>> allCycles) {
		for (ArrayList<BitVector> cycle : allCycles) {
			if (cycle.size() != 5 && cycle.size() != 6) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Checks the graph for a bad cycle connectivity. Bad Cycle connectivity is when it is generally
	 * not realistic to carbon chemistry. For example 
	 * 	- no two cycles can share more than one edge 
	 * 	- a cycle of length 5 can only share an edge with one other cycle of length 5 
	 * 		- TODO: this rule should be changed slightly
	 * 		- it can share more with cycles of other sizes
	 */
	private boolean hasBadCycleConnectivity(ArrayList<ArrayList<BitVector>> allCycles) {
		// iterate through all pairs of cycles
		// if any two have intersection greater than 2
		// this graph indeed has bad cycles
		for (int i = 0; i < allCycles.size(); i++) {
			ArrayList<BitVector> cycle1 = allCycles.get(i);
			for (int j = i + 1; j < allCycles.size(); j++) {
				ArrayList<BitVector> cycle2 = new ArrayList<BitVector>(allCycles.get(j));

				// cycle 2 becomes intersection
				cycle2.retainAll(cycle1);

				if (cycle2.size() > 2) {
					return true;
				}
			}
		}

		// check for cycles of length 5
		for (ArrayList<BitVector> cycle1 : allCycles) {
			if (cycle1.size() == 5) {
				int partners = 0;
				for (ArrayList<BitVector> cycle2 : allCycles) {
					if (!cycle1.equals(cycle2) && cycle2.size() == 5) {
						partners++;
					}
				}
				if (partners > 1) {
					return true;
				}
			}
		}
		return false;
	}

	
	/**
	 * Tries to fix all cycles whose size are not 5 or 6 This can often be done by adding or
	 * removing two nodes, without affecting the graph's cell
	 */
	public boolean tryToFixCycleSize(ArrayList<ArrayList<BitVector>> allCycles) {
		for(int i = 0; i < allCycles.size(); i++){
			ArrayList<BitVector> cycle = allCycles.get(i);
			if(cycle.size() < 5){
				if(!widenCycle(cycle, allCycles)){
					return false;
				}
			}
			if(cycle.size() > 6){
				if(!shortenCycle(cycle, allCycles)){
					return false;
				}
				i--; //cycle require multiple shortenings
			}
		}
		return true;
	}

	/**
	 * Uses the 'flooding' algorithm to find all cycles in a graph. Excludes so called 'super
	 * cycles' which contain all the nodes of two smaller cycles.
	 * 
	 * start with packet at every node at every opportunity, send packet to all your neighbors
	 * except pipe you already sent it down if at any point you reach node you started at, add to
	 * cycles if at any point, packet length exceeds maximum, remove packet should find all cycles
	 * below max length then parse through and eliminate super cycles if union of two cycles equals
	 * any other cycles, remove that cycle return list if cycles
	 * 
	 * TODO http://efficientbits.blogspot.ca/2012/12/scaling-up-faster-ring-detection-in-cdk.html
	 */
	public ArrayList<ArrayList<BitVector>> getAllCycles() {
		ArrayList<ArrayList<BitVector>> allCycles = new ArrayList<ArrayList<BitVector>>();

		LinkedList<ArrayList<BitVector>> openList = new LinkedList<ArrayList<BitVector>>();

		// can detect all cycles by only starting packets at nodes with degree 3
		for (int i = 1; i <= this.getLastNode().getNumber(); i *= 2) {
			BitVector currentNode = new BitVector(i);
			if (this.getDegree(currentNode) == 3) {
				ArrayList<BitVector> packet = new ArrayList<BitVector>();
				packet.add(currentNode);
				openList.add(packet);
			}
		}
		// if no nodes with degree of 3, then either the graph contains no cycles, or
		// the graph is one giant cycle, so we add random node
		if (openList.isEmpty()) {
			BitVector currentNode = new BitVector(1);
			ArrayList<BitVector> packet = new ArrayList<BitVector>();
			packet.add(currentNode);
			openList.add(packet);
		}
		while (!openList.isEmpty()) {

			ArrayList<BitVector> packet = openList.pop();
			BitVector parent = packet.get(packet.size() - 1);

			// if packet.end == packet.front
			// we have a cycle
			// ensure cycle is at least length 3
			if (packet.get(0).equals(packet.get(packet.size() - 1)) && packet.size() > 2) {
				packet.remove(packet.size() - 1);
				allCycles.add(packet);
				continue;
			}

			// search all neighbors
			ArrayList<BitVector> neighbours = this.getAllNeighbours(parent);
			for (BitVector buddy : neighbours) {

				// don't send packet back to the neighbor that sent it to you
				if (packet.size() >= 2 && buddy.equals(packet.get(packet.size() - 2))) {
					continue;
				}

				// prevents cycles within cycles
				// 5 -> 6 -> 7 -> 8 -> 6 -> 5 is not valid cycle
				// shouldn't be allowed to add node we already have in list,
				// unless that node is the starting node and we have found a legitimate cycle
				// if we already contain that node, and that node isn't the starting point,
				// don't go this way
				if (packet.contains(buddy) && !packet.get(0).equals(buddy)) {
					continue;
				}

				// send packet everywhere else
				// as long as packet is below max length
				if (packet.size() < 12) {
					ArrayList<BitVector> newPacket = new ArrayList<BitVector>(packet);
					newPacket.add(buddy);
					openList.add(newPacket);
				}

			}
		}

		// if list empty
		if (allCycles.isEmpty()) {
			return allCycles;
		}

		// delete all duplicates
		for (int i = 0; i < allCycles.size(); i++) {
			ArrayList<BitVector> cycle1 = allCycles.get(i);
			if (cycle1 == null) {
				continue;
			}
			for (int j = i + 1; j < allCycles.size(); j++) {
				ArrayList<BitVector> cycle2 = allCycles.get(j);
				if (cycle2 == null) {
					continue;
				}

				if (cycle1.size() == cycle2.size()) {
					if (cycle1.containsAll(cycle2)) {
						allCycles.set(j, null);
						continue;
					}
				}
			}
		}

		// remove super cycles
		allCycles = removeSuperCycles(allCycles);

		return allCycles;
	}

	/**
	 * Parses through a list of cycles, and removes super cycles. Super cycles are larger cycles
	 * that are made up of two combined cycles
	 */
	public ArrayList<ArrayList<BitVector>> removeSuperCycles(
			ArrayList<ArrayList<BitVector>> allCycles) {
		// for each cycle
		for (int i = 0; i < allCycles.size(); i++) {
			if (allCycles.get(i) == null) {
				continue;
			}
			ArrayList<BitVector> currentCycle = allCycles.get(i);

			ArrayList<String> currEdges = makeEdgesString(currentCycle);

			// if this one of the other cycles is a subset of this cycle, then
			// this cycle is not chord-less, and should be removed

			for (int j = 0; j < allCycles.size(); j++) {
				if (allCycles.get(j) == null || i == j) {
					continue;
				}
				ArrayList<BitVector> cycle2 = allCycles.get(j);

				//check if cycle2 is subset of currentCycle
//				if (isSubSet(cycle2, currentCycle)) {
				if(currentCycle.containsAll(cycle2)){
					allCycles.set(i, null);
					continue;
				}

			}

			// If the other currently used cycles include every edge in this cycle so far
			// then do not include this cycle
			ArrayList<String> totalEdgesUsed = new ArrayList<String>();
			for (int j = 0; j < i; j++) {
				ArrayList<BitVector> cycle2 = allCycles.get(j);
				if (cycle2 == null) {
					continue;
				}
				ArrayList<String> edges2 = makeEdgesString(cycle2);
				totalEdgesUsed.addAll(edges2);
			}

			if (totalEdgesUsed.containsAll(currEdges)) {
				allCycles.set(i, null);
			}
		}

		allCycles = Utils.removeNulls(allCycles);

		return allCycles;
	}

	public static ArrayList<String> makeEdgesString(ArrayList<BitVector> cycle) {
		ArrayList<String> edges = new ArrayList<String>();
		for (int i = 1; i < cycle.size(); i++) {
			String first = cycle.get(i - 1).toString();
			String second = cycle.get(i).toString();
			if (first.compareTo(second) < 0) {
				edges.add(first + "-" + second);
			} else {
				edges.add(second + "-" + first);
			}
		}

		String first = cycle.get(cycle.size() - 1).toString();
		String second = cycle.get(0).toString();
		if (first.compareTo(second) == -1) {
			edges.add(first + "-" + second);
		} else {
			edges.add(second + "-" + first);
		}
		return edges;
	}

	// TODO make a lot of methods private

	/**
	 * Returns a list of BitVector nodes. Each node is a neighbour of the inputted node
	 */
	public ArrayList<BitVector> getAllNeighbours(BitVector bv) {
		ArrayList<BitVector> allNeighbours = new ArrayList<BitVector>();

		for (int i = 0; i < this.getEdgeCell().size(); i++) {
			BitVector current = this.getEdgeCell().getPA()[i];

			if (current.contains(bv.getNumber())) {
				allNeighbours.add(new BitVector(current.getNumber() - bv.getNumber()));
			}
		}

		return allNeighbours;
	}

	public boolean isPort(BitVector node){
		return node.getNumber() <= this.getLastPort().getNumber();
	}
	
	public int getHighestDegree() {
		int max = -1;
		int lastNode = 1 << (this.numNodes - 1);
		// cycle through all nodes
		for (int node = 1; node <= lastNode; node *= 2) {

			int degree = 0;
			Cell edges = this.getEdgeCell();
			// cycle through edges and count occurrences of that node
			for (int i = 0; i < edges.size(); i++) {
				BitVector edge = edges.getPA()[i];
				if (edge.contains(node)) {
					degree++;
				}
			}

			if (degree > max) {
				max = degree;
			}
		}

		return max;
	}
	public BitVector getHighestDegreeNode(){
		return getHighestDegreeNodeInternal(this.numNodes);
	}
	
	public BitVector getHighestDegreePort(){
		return getHighestDegreeNodeInternal(this.numPorts);
	}
	
	//TODO not elegant solution with this and getHighestDegree :(
	private BitVector getHighestDegreeNodeInternal(int nodes){
		int max = -1;
		int high = -1;
		int lastNode = 1 << (nodes - 1);
		// cycle through all nodes
		for (int node = 1; node <= lastNode; node *= 2) {

			int degree = 0;
			Cell edges = this.getEdgeCell();
			// cycle through edges and count occurrences of that node
			for (int i = 0; i < edges.size(); i++) {
				BitVector edge = edges.getPA()[i];
				if (edge.contains(node)) {
					degree++;
				}
			}

			if (degree > max) {
				max = degree;
				high = node;
			}
		}

		return new BitVector(high);
	}

	public int getDegree(BitVector bv) {
		int node = bv.getNumber();

		int degree = 0;
		Cell edges = this.getEdgeCell();
		// cycle through edges and count occurences of that node
		for (int i = 0; i < edges.size(); i++) {
			BitVector edge = edges.getPA()[i];
			if (edge.contains(node)) {
				degree++;
			}
		}
		return degree;
	}

	/**
	 * Find all nodes with specified degree.
	 * if degree == -1, you will get all nodes
	 */
	public ArrayList<BitVector> getAllNodes(int degree) {
		ArrayList<BitVector> degree1List = new ArrayList<BitVector>();

		int lastNode = 1 << (this.numNodes - 1);
		// cycle through all nodes
		for (int node = 1; node <= lastNode; node *= 2) {

			int currDegree = 0;
			Cell edges = this.getEdgeCell();
			// cycle through edges and count occurences of that node
			for (int i = 0; i < edges.size(); i++) {
				BitVector edge = edges.getPA()[i];
				if (edge.contains(node)) {
					currDegree++;
				}
			}
			if (degree == -1 || degree == currDegree) {
				degree1List.add(new BitVector(node));
			}
		}

		return degree1List;
	}

	/**
	 * Extends every port of this graph out one node, and replaces its former position with an
	 * internal vertex. The interval vertex is bonded to everything the port was bonded to. The port
	 * is bonded to exclusively the internal vertex.
	 * 
	 * This procedure can often be used to alleviate the degree on your ports, while maintaining the
	 * graph's cell. However, this procedure is not guaranteed to preserve the cell.
	 * 
	 * If the extended graph has a different cell, null is returned. Graph must be port-connected
	 */
	public Graph extendPorts() {
		Cell cell = GraphtoCell.makeCell(this);
		cell.normalize();

		Graph extended = extendPortsNoCell();

		extended.getEdgeCell().sortBySize();
		Cell newCell = GraphtoCell.makeCell(extended);
		if (newCell.size() != 0) {
			newCell.normalize();
		}
		if (!newCell.equals(cell)) {
			// extended = new Graph(this);
			return null;
		}
		extended.setName(extended.getName() + "Extended");
		return extended;
	}

	/**
	 * Same as extendPorts, except returns regardless of any cell change
	 */
	public Graph extendPortsNoCell() {
		// extended graph
		Graph extended = new Graph(this);

		int lastNode = extended.getLastNode().getNumber();
		// ports always 0 1 2 3 4
		for (int port = 1; port <= (1 << (extended.numPorts - 1)); port *= 2) {

			// add new node to replace port
			int newNode = lastNode * 2;
			lastNode = newNode;
			extended.numNodes++;

			// find all edges which connect to extended port
			for (int i = 0; i < extended.edgeCell.size(); i++) {
				BitVector edge = extended.edgeCell.getPA()[i];
				if (edge.contains(port)) {

					int otherNode = edge.getNumber() - port;

					// delete edges from port to other
					extended.removeEdge(edge);
					// add edge from port to other
					extended.addEdge(new BitVector(otherNode + newNode));
				}
			}

			extended.addEdge(new BitVector(newNode + port));
		}

		extended.getEdgeCell().sortBySize();
		extended.setName(extended.getName() + "Extended");
		return extended;
	}

	/**
	 * Attempts to connect all ports of the graph
	 * 
	 * Connects graph by adding two interval vertices. One of the vertices is connected to a node on
	 * each half of the graph. The other internal vertex is attached only to the other vertex. This
	 * means they must always share a double bond, meaning the bonds added to connect the graphs can
	 * never be used for a double bond and the cell is always the same (probably).
	 * 
	 * Returns whether it was successful in connecting the graph. Or true in the case that the graph
	 * was never port-disjoint in the first place
	 */
	public boolean tryToPortConnect() {
		if (isPortConnected()) {
			return true;
		}

		int before = this.countNodes();
		ArrayList<BitVector> nodes = this.getAllNodes(-1);
		Set<BitVector> allNodes = new HashSet<BitVector>();
		for (BitVector b : nodes) {
			allNodes.add(b);
		}

		int lastNode = 1 << (this.numNodes - 1);
		// cycle through all pairs of nodes
		for (int first = 1; first <= lastNode; first *= 2) {
			for (int second = first * 2; second <= lastNode; second *= 2) {
				int newNode1 = lastNode * 2;
				int newNode2 = newNode1 * 2;

				//need to make sure we don't exceed degree limitations
				BitVector firstNode = new BitVector(first);
				BitVector secondNode = new BitVector(second);
				if((isPort(firstNode) && getDegree(firstNode) >= 2)
						|| getDegree(firstNode) >= 3) {
					continue;
				} 
				if((isPort(secondNode) && getDegree(secondNode) >= 2)
						|| getDegree(secondNode) >= 3) {
					continue;
				}

				Graph copy = new Graph(this);
				if (copy.addTwoNodes()) {
					copy.addEdge(new BitVector(newNode1 + newNode2));
					copy.addEdge(new BitVector(first + newNode1));
					copy.addEdge(new BitVector(second + newNode1));

					// if there are more nodes accessible now than before
					if (before < copy.countNodes() - 2) {
						this.addTwoNodes();
						this.addEdge(new BitVector(newNode1 + newNode2));
						this.addEdge(new BitVector(first + newNode1));
						this.addEdge(new BitVector(second + newNode1));
						return tryToPortConnect();
					}
				}
			}
		}
		return false;
	}

	/**
	 * TODO: Graph must be port-connected 
	 * Searches graph for disjoint (non-port) nodes and removes them from the graph. 
	 * This means shifting the node numbers for any others nodes in some cases.
	 * For example, graph with 12 nodes, we delete disjoint node #7. Nodes #8,9,10, and 11 are all
	 * reduced by 1, to #7,8,9 and 10 to get a final graph with 11 nodes numbered 0-10.
	 */
	public void removeExtraNodes() {
		// find nodes which are not connected
		ArrayList<BitVector> disjoint = this.findUnreachableNodes();
		// sort so we remove the highest number nodes first
		Collections.sort(disjoint, new Comparator<BitVector>() {
			@Override
			public int compare(BitVector o1, BitVector o2) {
				return new Integer(o2.getNumber()).compareTo(o1.getNumber());
			}
		});
		for (BitVector bv : disjoint) {
			int nodeNum = bv.firstNode();
			if (nodeNum >= this.numPorts) {
				// node is not a port, it can be removed

				// start by removing all its edges
				ArrayList<BitVector> neighbours = this.getAllNeighbours(bv);
				for (BitVector neigh : neighbours) {
					this.removeEdge(new BitVector(bv.getNumber() + neigh.getNumber()));
				}

				// rename all nodes which are greater in number than it
				BitVector[] myEdges = this.edgeCell.getPA();
				for (int i = 0; i < myEdges.length; i++) {
					BitVector edge = myEdges[i];
					int node1 = edge.firstNode();
					edge = edge.remove(1 << node1);
					int node2 = edge.firstNode();
					edge = edge.remove(1 << node2); // edge now empty

					if (node1 > nodeNum) {
						node1--;
					}
					if (node2 > nodeNum) {
						node2--;
					}
					int val1 = 1 << node1;
					int val2 = 1 << node2;
					myEdges[i] = new BitVector(val1 + val2);
				}

				this.numNodes--;
			} else {
				throw new IllegalArgumentException("Graph was not Port-Connected");
			}
		}
	}

	/**
	 * This method attempts to shorten all cycles of length 7 or greater. Some cycles may be unable
	 * to be shortened
	 * 
	 * This method calls itself everytime a cycle is found and fixed, which restarts and tries to
	 * fix all cycles again.
	 */
	private boolean shortenCycle(ArrayList<BitVector> theCycle,
			ArrayList<ArrayList<BitVector>> allCycles) {
		// grab all pairs of nodes in cycle
		for (int i = 0; i < theCycle.size(); i++) {
			BitVector node1 = theCycle.get(i);
			for (int j = i + 1; j < theCycle.size(); j++) {
				BitVector node2 = theCycle.get(j);

				// make sure these nodes have edge between them
				BitVector edge = new BitVector(node1.getNumber() + node2.getNumber());
				if (this.hasEdge(edge) 
						&& this.getDegree(node1) == 2 && this.getDegree(node2) == 2) {
					// make sure they aren't ports
					if (!isPort(node1) && !isPort(node2)) {
						// we can remove node1 and node 2
						// get nodes on other sides of node1 and node2
						// get left
						ArrayList<BitVector> n = this.getAllNeighbours(node1);
						n.remove(node2);
						BitVector left = n.get(0);
						ArrayList<BitVector> m = this.getAllNeighbours(node2);
						m.remove(node1);
						BitVector right = m.get(0);

						// remove node1 and node2, connect left and right
						if (left != null && right != null) {
							removeEdge(new BitVector(left.getNumber() + node1.getNumber()));
							removeEdge(new BitVector(right.getNumber() + node2.getNumber()));
							addEdge(new BitVector(left.getNumber() + right.getNumber()));

							// remove node1 and node2 from graph
							removeExtraNodes();
							
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	/**
	 * Finds cycles of size 3 or 5 by adding two extra nodes into them. This can not always
	 * be done if all edges of the cycle are involved in a separate cycle
	 */
	private boolean widenCycle(ArrayList<BitVector> cycle, ArrayList<ArrayList<BitVector>> allCycles) {
		ArrayList<BitVector> fixingSpot = this.getTwoNodesFromCycle(cycle, allCycles);

		if(!fixingSpot.isEmpty()){
			int node1 = fixingSpot.get(0).getNumber();
			int node2 = fixingSpot.get(1).getNumber();
			int lastNode = this.getLastNode().getNumber();
			
			// add two new nodes
			int newNode1 = lastNode * 2;
			int newNode2 = newNode1 * 2;
			if (addTwoNodes()) {
				// remove edge from node1 -> node2
				removeEdge(new BitVector(node1 + node2));
				
				// add edge from new1 -> new2
				addEdge(new BitVector(newNode1 + newNode2));
				// add edge from node1 to new1
				addEdge(new BitVector(node1 + newNode1));
				// and edge from node2 to new1
				addEdge(new BitVector(node2 + newNode2));
				
				return true;
			}
		}
		return false;
	}

	/**
	 * Given all cycles, and one specific one, it finds an edge of that cycle that is not located in
	 * any cycles. This edge can be safely increased in size. If this is impossible, an empty list
	 * is returned. Automatically returns the first viable edge
	 */
	private ArrayList<BitVector> getTwoNodesFromCycle(ArrayList<BitVector> theCycle,
			ArrayList<ArrayList<BitVector>> allCycles) {

		ArrayList<String> potentialEdges = makeEdgesString(theCycle);
		for (ArrayList<BitVector> cycle : allCycles) {
			if (cycle.equals(theCycle)) {
				continue;
			}
			// removes all the overlapping edges as potential edges
			potentialEdges.removeAll(makeEdgesString(cycle));
		}

		ArrayList<BitVector> twoNodesFromEdge = new ArrayList<BitVector>();
		if (!potentialEdges.isEmpty()) {
			String edge = potentialEdges.get(0).replace("-", " ");
			Scanner edgeScanner = new Scanner(edge);
			int firstNode = edgeScanner.nextInt();
			int secondNode = edgeScanner.nextInt();
			edgeScanner.close();

			twoNodesFromEdge.add(new BitVector(1 << firstNode));
			twoNodesFromEdge.add(new BitVector(1 << secondNode));
		}
		return twoNodesFromEdge;
	}

	/**
	 * Expands one node of high degree by changing it to multiple nodes with lower degree.
	 * Guaranteed to preserve the Kekule cell, but might ruin cycle connectivity
	 */
	public boolean expandNode(BitVector node) {
		if(isPort(node)){
			throw new IllegalArgumentException("Only Non-Port nodes can be expanded");
		}
		
		// TODO shouldn't be necessary with comment below as well
		Cell kCell = GraphtoCell.makeCell(this);
		kCell.normalize();

		ArrayList<BitVector> allNeighbours = this.getAllNeighbours(node);

		int lastNode = 1 << (this.numNodes - 1);
		if (addTwoNodes()) {
			int newNode1 = lastNode * 2;
			int newNode2 = newNode1 * 2;

			for (BitVector bv : allNeighbours) {
				removeEdge(new BitVector(bv.getNumber() + node.getNumber()));
			}

			addEdge(new BitVector(node.getNumber() + newNode1));
			addEdge(new BitVector(node.getNumber() + newNode2));

			for (int a = 0; a < allNeighbours.size(); a++) {
				if (a + 1 > allNeighbours.size() / 2) {
					addEdge(new BitVector(newNode1 + allNeighbours.get(a).getNumber()));
				} else {
					addEdge(new BitVector(newNode2 + allNeighbours.get(a).getNumber()));
				}
			}

			// TODO remove this
			Cell cell2 = GraphtoCell.makeCell(this);
			cell2.normalize();

			if (kCell.equals(cell2)) {
				return true;
			} else {
				System.err.println("Expanding Node SHOULD preserve the Kekule cell always");
			}
		}
		return false;
	}

	/**
	 * Merges a node with degree2 and its two neighbours into one node
	 * while preserving the Kekule cell
	 */
	public boolean mergeNode(BitVector node) {
		ArrayList<BitVector> neighbours = this.getAllNeighbours(node);
		if (neighbours.size() != 2) {
			throw new IllegalArgumentException("Can only merge a node with degree 2");
		}
		BitVector u1 = neighbours.get(0);
		BitVector u2 = neighbours.get(1);
		// get all edges connected to new graph
		ArrayList<BitVector> allNeighbours = this.getAllNeighbours(u1);
		ArrayList<BitVector> allNeighbours2 = this.getAllNeighbours(u2);
		
		for (BitVector bv : allNeighbours) {
			removeEdge(new BitVector(bv.getNumber() + u1.getNumber()));
		}
		for (BitVector bv : allNeighbours2) {
			removeEdge(new BitVector(bv.getNumber() + u2.getNumber()));
		}
		addEdge(new BitVector(u1.getNumber() + u2.getNumber()));

		allNeighbours.remove(u2);
		allNeighbours.remove(node);
		allNeighbours2.remove(u1);
		allNeighbours2.remove(node);
		allNeighbours.addAll(allNeighbours2);
		allNeighbours = Utils.removeDups(allNeighbours);

		for (BitVector neighbour : allNeighbours) {
			addEdge(new BitVector(neighbour.getNumber() + node.getNumber()));
		}
		return true;
	}

	public boolean tryToEditForRealisticness() {
		// first check if port connected
		if(!tryToPortConnect()){
			return false;
		}

		//this should in theory always work
		removeExtraNodes();
		// check if entire graph connected
//		if (!isDisjoint()) {
//			// remove extra nodes
//			// if can't be removed, return false
//		}

		// check if degree is low
		if (hasInvalidDegree()) {
			//currently no way to fix high degree port
			int highest = this.getHighestPortDegree();
			if(highest >= 3){
				return false;
			}
			
			// try to split other nodes
			highest = this.getHighestDegree();
			while(highest >= 4){
				if(!expandNode(getHighestDegreeNode())){
					return false;
				}
				highest = this.getHighestDegree();
			}
			
			//TODO: this shouldn't ever happen i think
			// if still has crowded nodes return false
			if(this.hasInvalidDegree()){
				System.err.println("Expand Node failed to remedy a high node");
				return false;
			}
		}

		ArrayList<ArrayList<BitVector>> allCycles = this.getAllCycles();
		// check for bad cycle sizes
		if(!this.tryToFixCycleSize(allCycles)){
			return false;
		}

		// check for bad cycle connectivity
		// currently unfixable
		return !hasBadCycleConnectivity(allCycles);
	}

	/**
	 * Checks whether this graph is disjoint. Does this by breadth-first-searching the graph and
	 * counting how many different nodes it reaches. If not all nodes are reached, false is
	 * returned, otherwise true.
	 * 
	 * @return whether this (graph) is disjoint or not (as in connected)
	 */
	public boolean isDisjoint() {
		 if( this.countNodes() == this.numNodes){
			 return false;
		 }
		 return true;
		// TODO temporary i guess. find all usages of this method and fix them to the new way
//		return !this.isPortConnected();
	}

	public boolean isPortConnected() {
		Set<BitVector> reachable = findReachableNodes();
		ArrayList<BitVector> ports = new ArrayList<BitVector>();
		for(int i = 0; i < this.numPorts; i++){
			ports.add(new BitVector(1 << i));
		}
		return reachable.containsAll(ports);
	}

	public int countNodes() {
		return findReachableNodes().size();
	}

	public ArrayList<BitVector> findUnreachableNodes() {
		Set<BitVector> reachable = findReachableNodes();
		ArrayList<BitVector> unReachable = new ArrayList<BitVector>();

		ArrayList<BitVector> all = this.getAllNodes(-1);
		for (BitVector curr : all) {
			if (!reachable.contains(curr)) {
				unReachable.add(curr);
			}
		}

		return unReachable;

	}
	public Set<BitVector> findReachableNodes() {
		BitVector[] edges = this.edgeCell.getPA();
		// holds whether we already visited this node
		HashMap<BitVector, Boolean> reached = new HashMap<BitVector, Boolean>();

		// start searching at node 1
		BitVector first = new BitVector(1);
		LinkedList<BitVector> openList = new LinkedList<BitVector>();
		openList.add(first);

		while (!openList.isEmpty()) {

			BitVector parent = openList.pop();
			reached.put(parent, true);

			// search all edges
			for (BitVector edge : edges) {
				// if edges goes from this node to another
				if (edge.contains(parent.getNumber())) {
					// find out who another is
					BitVector another = new BitVector(edge.getNumber() - parent.getNumber());

					// if we haven't already been to another
					if (reached.get(another) == null && !openList.contains(another)) {
						// add to openList
						openList.add(another);
					}
				}
			}
		}

		return reached.keySet();
	}

	/**
	 * Iterates through each port is this graph and checks the degree. Returns the highest degree
	 * found. In order to model real molecules, the degree on all ports should be at most 2
	 * 
	 * @return the largest degree of all ports of this graph
	 */
	public int getHighestPortDegree() {
		int max = -1;
		int lastNode = 1 << (this.numPorts - 1);
		// cycle through all nodes
		for (int node = 1; node <= lastNode; node *= 2) {

			int degree = 0;
			Cell edges = this.getEdgeCell();
			// cycle through edges and count occurences of that node
			for (int i = 0; i < edges.size(); i++) {
				BitVector edge = edges.getPA()[i];
				if (edge.contains(node)) {
					degree++;
				}
			}

			if (degree > max) {
				max = degree;
			}
		}

		return max;
	}

	/**
	 * Translates a graph's Cell of bitVectors into a visual representation of the edges. Each edge
	 * is translated in the following way. 0101 = edge from 0 - 2 11000 = edge from 3 - 4
	 * 
	 * The name, #Nodes, and #Ports of the graph is also printed.
	 */
	public void writeGraph() {
		String title = "";
		if (this.name != null) {
			title += this.name + ": ";
		}
		title += this.numNodes + " Nodes, " + " " + this.numPorts + " Ports";
		System.out.println(title);
		String edges = "Edges: ";
		for (int i = 0; i < this.edgeCell.size(); i++) {
			BitVector edge = this.edgeCell.getPA()[i];
			int p = edge.firstNode();
			edge = new BitVector(edge.getNumber() - (1 << p));
			int q = edge.firstNode();
			edges += (p) + "-" + (q);

			if (i != this.edgeCell.size() - 1) {
				edges += ", ";
			}
		}
		System.out.println(edges);
	}

	/**
	 * Returns the last port of this graph in bitvector form, if 5 ports, lastPort = 0001 0000
	 */
	public BitVector getLastPort() {
		int lastPort = 1 << (this.numPorts - 1);
		return new BitVector(lastPort);
	}

	/**
	 * Returns the last node of this graph in bitvector form if 5 nodes, lastNode = 10000
	 * 
	 * @return lastNode in bitvector form
	 */
	public BitVector getLastNode() {
		int lastNode = 1 << (this.numNodes - 1);
		return new BitVector(lastNode);
	}

	/**
	 * Returns the set of nodes represented in a BitVector
	 * 
	 * @return, bitvector of all nodes
	 */
	public BitVector getNodeVector() {
		// bitVector = 1 * 2 ^ nodes - 1
		// gives you x 1s, where node = x
		int bitVector = (1 << numNodes) - 1;
		return new BitVector(bitVector);
	}

	/**
	 * Returns the set of ports in a bitvector. At this point, ports should be nodes 0 - ports - 1
	 * 
	 * @return
	 */
	public BitVector getPortVector() {
		int bitVector = (1 << numPorts) - 1;
		return new BitVector(bitVector);
	}

	public String getName() {
		return name;
	}

	public boolean addTwoNodes() {
		if (this.numNodes + 2 >= 31) {
			return false;
		}
		this.numNodes += 2;
		return true;
	}

	public Cell getEdgeCell() {
		return edgeCell;
	}

	public void setEdgeCell(Cell edgeCell) {
		this.edgeCell = edgeCell;
	}

	public int getNumPorts() {
		return numPorts;
	}

	public int getNumNodes() {
		return numNodes;
	}

	public int getNumEdges() {
		return this.edgeCell.size();
	}

	public void setNumNodes(int numNodes) {
		this.numNodes = numNodes;
		if (this.numNodes >= 31) {
			System.err.println("31 Nodes Reached");
		}
	}

	public void setName(String name2) {
		this.name = name2;
	}

	public void appendToName(String suffix) {
		this.name += suffix;
	}

	public String toString() {
		String name = "G ";
		BitVector[] list = this.edgeCell.getPA();
		for (int i = 0; i < list.length; i++) {
			BitVector current = list[i];

			name += current.firstBit() + "-" + current.remove(current.firstBit()).firstBit();

			name += " ";
		}

		return name;
	}
}
