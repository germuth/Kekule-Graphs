package empty;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;

/**
 * Graph
 * 
 * This represents an undirected, simple, (connected? not yet) graph.
 * 
 * In Kekule theory, certain nodes of the graphs are called ports and represent connections outside the graph. 
 * The graph keeps track of the number of total nodes, the number of ports, and which nodse are ports. 
 * The Edges of this graph are stored in an adjacency matrix, although each Edge is converted to BitVector
 * later in the program.
 * 
 * The Number of Nodes must never exceed 32 since the set of nodes is represented in BitVectors, and the BitVector
 * for a set of 32 size would overflow the integer.
 * cP <= cN <= 32
 * Above is CORRECT
 * 
 * TODO this classed could be re-worked to conform with the rest of the program better
 * 
 * @author Aaron
 *
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
		
		if( nC >= 31){
			System.err.println("31 Nodes Reached");
			return;
		}
		Set<BitVector> bvEdges = new HashSet<BitVector>();

		for (String x : edges) {
			Scanner edgeScanner = new Scanner(x);
			int firstNode = edgeScanner.nextInt();
			int secondNode = edgeScanner.nextInt();
			
			//turn each node number into bit vector (<<)
			//the add together to get bit vector of two nodes
			//each node in the edge
			bvEdges.add(new BitVector(
					(1 << firstNode) + 
					(1 << secondNode) ) );
			edgeScanner.close();
			
		}
		
		this.name = name;
		this.numNodes = nC;
		this.numPorts = nP;
		this.edgeCell = new Cell(bvEdges, numPorts);
	}
	
	//TODO 31 nodes shit
	public Graph(String name, int nP, int nC, Cell edges){
		this.numPorts = nP;
		this.numNodes = nC;
		if( this.numNodes >= 31){
			System.err.println("31 Nodes Reached");
		}
		this.edgeCell = edges;
		this.name = name;
	}
	
	public Graph(int nP, int nC, Cell edges){
		this.numPorts = nP;
		this.numNodes = nC;
		if( this.numNodes >= 31){
			System.err.println("31 Nodes Reached");
		}
		this.edgeCell = edges;
		this.name = "";
	}
	
	public Graph(Graph g){
		this.name = g.name;
		this.numNodes = g.numNodes;
		if( this.numNodes >= 31){
			System.err.println("31 Nodes Reached");
		}
		this.numPorts = g.numPorts;
		this.edgeCell = new Cell(g.edgeCell);
	}
	
	
	/**
	 * equals method. Does not compare based on port number, only edges
	 */
	@Override
	public boolean equals(Object obj) {
		Graph another = (Graph) obj;
		this.edgeCell.sortBySize();
		another.edgeCell.sortBySize();
		if(this.edgeCell.equals(another.edgeCell)){
			return true;
		}
		return false;
	}

	/**
	 * Adds an edge to this graph
	 * @param bv, BitVector representation of the edge you wish to add
	 */
	public void addEdge(BitVector bv){
		this.edgeCell.add(bv);
	}
	
	/**
	 * if this edge is
	 * (1) already contained in this graph
	 * (2) is an invalid edge (doesn't reach two nodes)
	 * (3) increase the degree on a node too high
	 * it is deemed a 'bad edge' and true will be returned
	 * otherwise, false
	 * @param edge
	 * @return true/false based on abve
	 */
	public boolean isBadEdge( BitVector edge){
		//if edge goes to itself..
		if( edge.getNumber() - edge.firstBit() == 0){
			return true;
		}
		//if we already have that edge
		if( this.edgeCell.contains( edge ) ){
			return true;
		}
		//if it makes degree too high
		Graph temp = new Graph(this);
		temp.addEdge(edge);
		if( temp.hasCrowdedNodes() ){
			return true;
		}
		return false;
	}
	
	/**
	 * checks whether No node has degree greater than 3, no port greater than 2
	 */
	public boolean hasCrowdedNodes(){
		if(this.getHighestDegree() > 3 || this.getHighestPortDegree() > 2){
			return true;
		}
		return false;
	}
	/**
	 * Returns the best guess towards whether this graph represents a realistic 
	 * molecule according to carbon chemistry. It currently checks the following:
	 * 		- No node has degree greater than 3, no port greater than 2
	 * 		- The Graph is connected
	 * 		- Cycle sizes are 5 or 6 (7 can exist in reality, but it is rare)
	 * 		- Cycle connectivity is realistic ( see isBadCycleConnectivity() )
	 */
	public boolean isRealistic(){
		//degree
		if(hasCrowdedNodes()){
			return false;
		}
		//cycle sizes
		ArrayList<ArrayList<BitVector>> allCycles = this.getAllCycles();
		for(ArrayList<BitVector> cycle: allCycles){
			if(cycle.size() != 5 && cycle.size() != 6){
				return false;
			}
		}
		//connected
		if(this.isDisjoint()){
			return false;
		}
		return !hasBadCycleConnectivity(allCycles);
	}
	
	/**
	 * Checks the graph for a bad cycle connectivity.
	 * Bad Cycle connectivity is when it is generally not realistic to carbon chemistry. For example
	 * 		- no two cycles can share more than one edge
	 * 		- a cycle of length 5 can only share an edge with one other cycle of length 5
	 * 			- it can share more with cycles of other sizes
	 */
	private boolean hasBadCycleConnectivity(ArrayList<ArrayList<BitVector>> allCycles){
		//iterate through all pairs of cycles
		//if any two have intersection greater than 2
		//this graph indeed has bad cycles
		for(int i = 0; i < allCycles.size(); i++){
			ArrayList<BitVector> cycle1 = allCycles.get(i);	
			for(int j = i + 1; j < allCycles.size(); j++){
				ArrayList<BitVector> cycle2 = new ArrayList<BitVector>( allCycles.get(j) );
				
				//cycle 2 becomes intersection
				cycle2.retainAll( cycle1 );
				
				if( cycle2.size() > 2){
					return true;
				}
			}
		}
		
		//check for cycles of length 5
		for(ArrayList<BitVector> cycle1: allCycles){
			if(cycle1.size() == 5){
				int partners = 0;
				for(ArrayList<BitVector> cycle2: allCycles){
					if(!cycle1.equals(cycle2) && cycle2.size() == 5){
						partners++;
					}
				}
				if(partners > 1){
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * Tries to fix all cycles whose size are not 5 or 6
	 * This can often be done by adding or removing two nodes, without
	 * affecting the graph's cell
	 */
	public void tryToFixCycleSize(){
		ArrayList<ArrayList<BitVector>> allCycles = this.getAllCycles();
		int before = this.getNumNodes();
		this.shortenCycles(allCycles);
		if(before != this.getNumNodes()){
			System.out.println(before + " shortened to " + this.getNumNodes());			
		}
		this.widenCycles(allCycles);
	}
	
	/**
	 * Returns the number of cycles in this graph. Does this by obtaining a spanning tree
	 * and counting the number of edges that we're removed
	 */
	public int getNumCycles() {
		// find spanning tree by DFS-ing the nodes
		Graph spanningTree = GraphToSMILES.getSpanningTree( this );

		return ( this.edgeCell.size() - spanningTree.edgeCell.size() );
		
	}

	/**
	 * Uses the 'flooding' algorithm to find all cycles in a graph. Excludes 
	 * so called 'super cycles' which contain all the nodes of two smaller
	 * cycles. 
	 * 
	 * start with packet at every node
	 * at every opportunity, send packet to all your neighbors
	 * except pipe you already sent it down
	 * if at any point you reach node you started at, add to cycles
	 * if at any point, packet length exceeds maximum, remove packet
	 * should find all cycles below max length
	 * then parse through and eliminate super cycles
	 * if union of two cycles equals any other cycles, remove that cycle
	 * return list if cycles
	 * 
	 * TODO
	 * http://efficientbits.blogspot.ca/2012/12/scaling-up-faster-ring-detection-in-cdk.html
	 */
	public ArrayList<ArrayList<BitVector>> getAllCycles(){
		ArrayList<ArrayList<BitVector>> allCycles = new ArrayList<ArrayList<BitVector>>();
		
		LinkedList<ArrayList<BitVector>> openList = new LinkedList<ArrayList<BitVector>>();
		
		//can detect all cycles by only starting packets at nodes with degree 3
		for(int i = 1; i <= this.getLastNode().getNumber(); i *= 2){
			BitVector currentNode = new BitVector(i);
			if( this.getDegree( currentNode) == 3){
				ArrayList<BitVector> packet = new ArrayList<BitVector>();
				packet.add(currentNode);
				openList.add( packet );
			}
		}
		//if no nodes with degree of 3, then either the graph contains no cycles, or 
		//the graph is one giant cycle, so we add random node
		if( openList.isEmpty() ){
			BitVector currentNode = new BitVector( 1 );
			ArrayList<BitVector> packet = new ArrayList<BitVector>();
			packet.add(currentNode);
			openList.add( packet );
		}
		while( !openList.isEmpty() ){
			
			ArrayList<BitVector> packet = openList.pop();
			BitVector parent = packet.get( packet.size() - 1);
			
			//if packet.end == packet.front
			//we have a cycle
			//ensure cycle is at least length 3
			if( packet.get(0) .equals( packet.get( packet.size() - 1) ) && packet.size() > 2){
				packet.remove( packet.size() - 1 );
				allCycles.add( packet );
				continue;
			}
			
			//search all neighbors
			ArrayList<BitVector> neighbours = this.getAllNeighbours( parent );
			for(BitVector buddy : neighbours){
				
				//don't send packet back to the neighbor that sent it to you
				if( packet.size() >= 2 && buddy.equals( packet.get( packet.size() - 2) ) ){
					continue;
				}
				
				//prevents cycles within cycles
				//5 -> 6 -> 7 -> 8 -> 6 -> 5 is not valid cycle
				//shouldn't be allowed to add node we already have in list,
				//unless that node is the starting node and we have found a legitimate cycle
				//if we already contain that node, and that node isn't the starting point, 
				//don't go this way
				if( packet.contains( buddy ) && !packet.get(0).equals( buddy )){
					continue;
				}
				
				//send packet everywhere else
				//as long as packet is below max length
				if( packet.size() < 12){
					ArrayList<BitVector> newPacket = new ArrayList<BitVector>(packet);
					newPacket.add( buddy );
					openList.add( newPacket );
				}
				
			}
		}
		
		//if list empty
		if( allCycles.isEmpty() ){
			return allCycles;
		}
		
		//delete all duplicates
		for(int i = 0; i < allCycles.size(); i++){
			ArrayList<BitVector> cycle1 = allCycles.get(i);
			if(cycle1 == null){
				continue;
			}
			for(int j = i + 1; j < allCycles.size(); j++){
				ArrayList<BitVector> cycle2 = allCycles.get(j);
				if(cycle2 == null){
					continue;
				}
				
				if(cycle1.size() == cycle2.size()){
					if(cycle1.containsAll(cycle2)){
						allCycles.set(j, null);
						continue;
					}
				}
			}
		}

		//remove super cycles
		allCycles = removeSuperCycles( allCycles );
		
		return allCycles;
	}
	
	/**
	 * Parses through a list of cycles, and removes super cycles.
	 * Super cycles are larger cycles that are made up of two combined cycles
	 */
	public ArrayList<ArrayList<BitVector>> removeSuperCycles( ArrayList<ArrayList<BitVector>> allCycles ){
		//for each cycle
		for(int i = 0; i < allCycles.size(); i++){
			if( allCycles.get(i) == null){
				continue;
			}
			ArrayList<BitVector> currentCycle = allCycles.get(i);
			
			ArrayList<String> currEdges = makeEdgesString(currentCycle);
			
			//if this one of the other cycles is a subset of this cycle, then 
			//this cycle is not chord-less, and should be removed 
			
			for(int j = 0; j < allCycles.size(); j++){
				if( allCycles.get(j) == null ||
						i == j){
					continue;
				}
				ArrayList<BitVector> cycle2 = allCycles.get(j);
				
				if( isSubSet(cycle2, currentCycle)){
					allCycles.set(i, null);
					continue;
				}
				
			}
			
			//If the other currently used cycles include every edge in this cycle so far
			//then do not include this cycle
			ArrayList<String> totalEdgesUsed = new ArrayList<String>();
			for(int j = 0; j < i; j++){
				ArrayList<BitVector> cycle2 = allCycles.get(j);
				if(cycle2 == null){
					continue;
				}
				ArrayList<String> edges2 = makeEdgesString(cycle2);
				totalEdgesUsed.addAll(edges2);
			}
			
			if(totalEdgesUsed.containsAll(currEdges)){
				allCycles.set(i, null);
			}
		}

		allCycles = Utils.removeNulls( allCycles );
	
		return allCycles;
	}

	public static ArrayList<String> makeEdgesString(ArrayList<BitVector> cycle){
		ArrayList<String> edges = new ArrayList<String>();
		for(int i = 1; i < cycle.size(); i++){
			String first = cycle.get(i-1).toString();
			String second = cycle.get(i).toString();
			if(first.compareTo(second) < 0 ){
				edges.add(first + "-" + second);
			}else{
				edges.add(second + "-" + first);
			}
		}
		
		String first = cycle.get( cycle.size() - 1).toString();
		String second = cycle.get(0).toString();
		if(first.compareTo(second) == -1){
			edges.add(first + "-" + second);
		}else{
			edges.add(second + "-" + first);
		}
		return edges;
	}
	
	
	//TODO make a lot of methods private
	/**
	 * Tests whether nextCycle is a subset of currentCycle. If Current cycle is a sub
	 * set of next cycle, unless equal.
	 * 
	 * @param nextCycle
	 * @param currentCycle
	 * @return
	 */
	private boolean isSubSet(ArrayList<BitVector> nextCycle, ArrayList<BitVector> currentCycle){
		
		if( nextCycle.size() > currentCycle.size() ){
			return false;
		}
		
		//iterate through nextCycle
		//if currentCycle doesn't contain all return false
		for(int i = 0; i < nextCycle.size(); i++){
			if( !currentCycle.contains( nextCycle.get(i)) ){
				return false;
			}
		}
		
		return true;
	}
	/**
	 * Removes an edge from this graph. 
	 * @param bv, BitVector representation of the edge you wist to remove
	 */
	public void removeEdge(BitVector bv){
		//if doesn't contain edge
		if( !this.edgeCell.contains(bv) ){
			return;
		}
		int ports = this.edgeCell.getNumPorts();
		
		BitVector[] edges = new BitVector[ this.edgeCell.size() - 1 ];
		int k = 0; 
		
		for(int i = 0; i < this.edgeCell.size(); i++){
			BitVector currentEdge = this.edgeCell.getPA()[i];
			if( !currentEdge.equals(bv) ){
				edges[k] = currentEdge;
				k++;
			}
		}
		this.edgeCell = new Cell( edges );
		this.edgeCell.setNumPorts( ports );
	}
	
	/**
	 * Returns a list of BitVector nodes. Each node is a neighbour of the inputted node
	 */
	public ArrayList<BitVector> getAllNeighbours(BitVector bv){
		ArrayList<BitVector> allNeighbours = new ArrayList<BitVector>();
		
		for(int i = 0; i < this.getEdgeCell().size(); i++){
			BitVector current = this.getEdgeCell().getPA()[i];
			
			if( current.contains( bv.getNumber() )){
				allNeighbours.add( new BitVector( current.getNumber() - bv.getNumber() ));
			}
		}
		
		return allNeighbours;
	}
	
	/**
	 * Returns whether the inputted bitvector is a port of this graph
	 */
	public boolean isPort(BitVector bv){
		int nodeNum = bv.firstNode();
		if( this.numPorts - 1 >= nodeNum ){
			return true;
		}
		return false;
	}
	/**
	 * Gets the highest degree of any given node in this graph and returns
	 * in integer form. 
	 * 
	 * Graph is undirected so indegree == outdegree
	 * @return the highest degree of any given node in this graph
	 */
	public int getHighestDegree(){
		int max = -1;
		int lastNode = 1 << ( this.numNodes - 1 );
		if(numNodes > 30){
			System.out.println("Big");
		}
		//cycle through all nodes
		for(int node = 1; node <= lastNode; node *= 2 ){
			
			int degree = 0;
			Cell edges = this.getEdgeCell();
			//cycle through edges and count occurrences of that node
			for(int i = 0; i < edges.size(); i++){
				BitVector edge = edges.getPA()[i];
				if(edge.contains(node)){
					degree++;
				}
			}
			
			if(degree > max){
				max = degree;
			}
		}
		
		return max;
	}
	
	/**
	 * Gets the degree of a given node. Nodes are entered in BitVector form
	 * @param bv, BitVector representation of node
	 * @return how many edges that node is present in
	 */
	public int getDegree(BitVector bv){
		
		int node = bv.getNumber();
		
		int degree = 0;
		Cell edges = this.getEdgeCell();
		//cycle through edges and count occurences of that node
		for(int i = 0; i < edges.size(); i++){
			BitVector edge = edges.getPA()[i];
			if(edge.contains(node)){
				degree++;
			}
		}
		return degree;
	}
	
	/**
	 * Find all nodes with specified degree
	 * if degree == -1, you will get all nodes
	 * @param degree
	 * @return
	 */
	public ArrayList<BitVector> getAllNodes(int degree){
		ArrayList<BitVector> degree1List = new ArrayList<BitVector>();
		
		int lastNode = 1 << ( this.numNodes - 1 );
		//cycle through all nodes
		for(int node = 1; node <= lastNode; node *= 2 ){
			
			int currDegree = 0;
			Cell edges = this.getEdgeCell();
			//cycle through edges and count occurences of that node
			for(int i = 0; i < edges.size(); i++){
				BitVector edge = edges.getPA()[i];
				if(edge.contains(node)){
					currDegree++;
				}
			}
			if(degree == -1 || degree == currDegree){
				degree1List.add( new BitVector(node));
			}
		}
		
		return degree1List;
	}
	
	/**
	 * Extends every port of this graph out one node, and replaces its former position with an 
	 * internal vertex. The interval vertex is bonded to everything the port was bonded to. The port
	 * is bonded to exclusively the internal vertex. 
	 * 
	 * This procedure can often be used to alleviate the degree on your ports, while maintaining
	 * the graph's cell. However, this procedure is not guaranteed to preserve the cell.
	 * 
	 * If the extended graph has a different cell, null is returned. Every port must be connected to the 
	 * graph (at least degree 1)
	 * @return
	 */
	public Graph extendPorts(){
		Cell cell = GraphtoCell.makeCell(this);
		cell.normalize();
		
		//extended graph
		Graph extended = new Graph(this);
		
		int lastNode = extended.getLastNode().getNumber();
		//ports always 0 1 2 3 4
		for(int port = 1; port <= (1 << ( extended.numPorts -1 ) ); port *=2){
			
			//add new node to replace port
			int newNode = lastNode*2;
			lastNode = newNode;
			extended.numNodes++;
			
			//find all edges which connect to extended port
			for(int i = 0; i < extended.edgeCell.size(); i++){
				BitVector edge = extended.edgeCell.getPA()[i];
				if( edge.contains(port) ){
					
					int otherNode = edge.getNumber() - port;
					
					//delete edges from port to other
					extended.removeEdge(edge);
					//add edge from port to other
					extended.addEdge( new BitVector( otherNode + newNode ) );
				}
			}
			
			extended.addEdge( new BitVector( newNode + port) );
		}
		
		extended.getEdgeCell().sortBySize();
		Cell newCell = GraphtoCell.makeCell(extended);
		if(newCell.size() != 0){
			newCell.normalize();
		}
		if( !newCell.equals(cell) ){
			//extended = new Graph(this);
			return null;
		}
		extended.setName( extended.getName() + "Extended");
		return extended;
	}
	
	/**
	 * Extends every port of this graph out one node, and replaces its former position with an 
	 * internal vertex. The interval vertex is bonded to everything the port was bonded to. The port
	 * is bonded to exclusively the internal vertex. 
	 * 
	 * This procedure can often be used to alleviate the degree on your ports, while maintaining
	 * the graph's cell. However, this procedure is not guaranteed to preserve the cell.
	 * 
	 * This version will return the graph regardless of whether or not the new cell matches or not
	 * @return
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
	 * TODO
	 * In the genetic algorithm, it's possible that it creates two new nodes, 
	 * and connects them to each other, but nothing else. This is disjoint, 
	 * which is not allowed. If the two vertices are not ports, we can
	 * simply remove them, which is done here. If they are ports, we must
	 * connect them, which is done in connect()
	 * 
	 * This method is currently not used. In its current implementation,
	 * only the last two nodes are checked for this condition, not every
	 * possible pair.
	 */
//	public void trimDisjoint(){
//		double beginFitness = this.getFitness();
//	
//		BitVector lastNode = this.getLastNode();
//		BitVector secondLast = new BitVector( lastNode.getNumber() / 2 );
//		
//		//make sure they aren't ports
//		if( this.numNodes < this.numPorts + 2){
//			return;
//		}
//		
//		//if they are only connected to one thing
//		if( this.getDegree(lastNode) == 1 && this.getDegree(secondLast) == 1){
//			//and that thing happens to be each other
//			BitVector edge = new BitVector( lastNode.getNumber() + secondLast.getNumber() );
//			if(this.edgeCell.contains( edge )){
//				this.removeEdge( edge );
//				this.setNumNodes( this.numNodes - 2 );
//			}
//		}
//		
//		GeneticAlgorithm.calculateFitness( this );
//		if( this.getFitness() != beginFitness){
//			System.err.println("Trim Disjoint is broken yo");
//		}
//	}

	/**
	 * Turns this graph from a disjoint graph to a connected graph.
	 * 
	 * Connects graph by adding two interval vertices. One of the vertices is connected to 
	 * a node on each half of the graph. The other internal vertex is attached only to the 
	 * other vertex. This means they must always share a double bond, meaning the 
	 * bonds added to connect the graphs can never be used for a double bond and the cell
	 * is alwasy the same (probably).
	 * 
	 * Returns whether it was successful in connecting the graph. Or true in the case that
	 * the graph was never disjoint in the first place
	 */
	public boolean tryToConnect(){
		if(!this.isDisjoint()){
			return true;
		}
		
		int before = this.countNodes();
		ArrayList<BitVector> nodes = this.getAllNodes(-1);
		Set<BitVector> allNodes = new HashSet<BitVector>();
		for(BitVector b : nodes){
			allNodes.add(b);
		}
		
		int lastNode = 1 << ( this.numNodes - 1 );
		//cycle through all pairs of nodes
		for(int first = 1; first <= lastNode; first*= 2 ){
			for(int second = first*2; second <= lastNode; second *= 2){
				int newNode1 = lastNode*2;
				int newNode2 = newNode1*2;
				//add two new nodes
				if(this.addTwoNodes()){
					//add edge from new1 -> new2
					this.addEdge(new BitVector( newNode1 + newNode2 ));
					//add edges from port 1 to new1
					this.addEdge(new BitVector( first + newNode1) );
					//and edge from port 2 to new1
					this.addEdge(new BitVector( second + newNode1) );
					
					//if there are more nodes accessible now than before
					if( before < this.countNodes() - 2){
						//call again if not completely this yet
						return tryToConnect();
					}
				}else{
					return false;
				}
			}
		}
		return false;
	}
	
	/**
	 * This method attempts to shorten all cycles of length 7 or greater.
	 * Some cycles may be unable to be shortened
	 * 
	 * This method calls itself everytime a cycle is found and fixed, which restarts and
	 * tries to fix all cycles again. 
	 */
	private void shortenCycles(ArrayList<ArrayList<BitVector>> allCycles) {
		ArrayList<ArrayList<BitVector>> localAllCycles = new ArrayList<ArrayList<BitVector>>();
		localAllCycles.addAll(allCycles);
		
		for(int i = 0; i < localAllCycles.size(); i++){
			if( localAllCycles.get(i).size() <= 6){
				localAllCycles.remove(i);
				i--;
			}
		}
		
		for(ArrayList<BitVector> theCycle: localAllCycles){
			//grab all pairs of nodes in cycle
			for(int i = 0; i < theCycle.size(); i++){
				BitVector node1 = theCycle.get(i);
				for(int j = i + 1; j < theCycle.size(); j++){
					BitVector node2 = theCycle.get(j);
					//create edge between them
					BitVector edge = new BitVector( node1.getNumber() +
							node2.getNumber() );
					if( this.getEdgeCell().contains( edge ) &&
							this.getDegree(node1) == 2 &&
							this.getDegree(node2) == 2){
						//if they aren't ports
						if( node1.getNumber() > (1 << (this.numPorts - 1)) &&
								node2.getNumber() > (1 << (this.numPorts - 1))){
							
							//get left
							BitVector left = null;
							ArrayList<BitVector> n = this.getAllNeighbours(node1);
							n.remove(node2);
							//only want nodes with degree 2
							if( n.size() == 1){
								left = n.get(0);
							}
							//get right
							BitVector right = null;
							ArrayList<BitVector> m = this.getAllNeighbours(node2);
							m.remove(node1);
							if( n.size() == 1){
								right = m.get(0);
							}
							
							//connect them, which excludes node1 and node2
							if( left != null && right != null){
//								this.removeEdge(new BitVector(node1.getNumber() + node2.getNumber()));
								this.removeEdge(new BitVector(left.getNumber() + node1.getNumber()));
								this.removeEdge(new BitVector(right.getNumber() + node2.getNumber()));
								this.addEdge(new BitVector(left.getNumber() + right.getNumber()));
								
								//we now need to reconstruct the graph
								//the two nodes that were removed are not guaranteed to be the last two nodes numerically, 
								//so we cannot simply go this.numNodes -= 2
								//we must iterate through each of the nodes, and rename the nodes
								//and then reset up our cell
								//TODO for now, it just adds to extra nodes to the graph...
								//TODO also uncomment the above thing
								
								//now that we have removed a cycle, we call the method again to try to remove another cycle
								//or make this one even smaller
								//we have to restart completely because at this point some nodes may not be considered again (node1, node2)
								shortenCycles(allCycles);
							}
						}
					}
				}
			}
		}
	}
	
	/**
	 * Gets all cycles of this graph, and turns and 4 cycles into 6 cycles, and
	 * any 3 cycles into 5 cycles. This fits carbon chemistry much better. Checks to see if 
	 * the cell or fitness changed, if it did, it writes errors to console.
	 */
	private void widenCycles(ArrayList<ArrayList<BitVector>> allCycles){
		//spots where we add nodes to alleviate small cycles
		ArrayList<ArrayList<BitVector>> fixingSpots = new ArrayList<ArrayList<BitVector>>();
		
		for(int i = 0; i < allCycles.size(); i++){
			
			ArrayList<BitVector> currentCycle = allCycles.get(i);
			
			//if square or triangle
			if( currentCycle.size() == 4 || currentCycle.size() == 3){
				ArrayList<BitVector> fixingSpot = this.getTwoNodesFromCycle(currentCycle, allCycles);
				if( ! fixingSpot.isEmpty() ){
					fixingSpots.add( fixingSpot );
				}
				
			}	
		}
		
		//go through each fixing spot and actually fix
		for(int i = 0; i < fixingSpots.size(); i++){
			ArrayList<BitVector> fixingSpot = fixingSpots.get(i);
			
			int node1 = fixingSpot.get(0).getNumber();
			int node2 = fixingSpot.get(1).getNumber();
			int lastNode = this.getLastNode().getNumber();

			// add two new nodes
			int newNode1 = lastNode * 2;
			int newNode2 = newNode1 * 2;
			if (this.addTwoNodes()) {
				// remove edge from node1 -> node2
				this.removeEdge(new BitVector(node1 + node2));

				// add edge from new1 -> new2
				this.addEdge(new BitVector(newNode1 + newNode2));
				// add edge from node1 to new1
				this.addEdge(new BitVector(node1 + newNode1));
				// and edge from node2 to new1
				this.addEdge(new BitVector(node2 + newNode2));
			}else{
				break;
			}
		}
		
	}
	
	/**
	 * Given all cycles, and one specific one, it finds an edge of that cycle that is 
	 * not located in any cycles. This edge can be safely increased in size.
	 * If this is impossible, an empty list is returned 
	 */
	private ArrayList<BitVector> getTwoNodesFromCycle(ArrayList<BitVector> theCycle, ArrayList<ArrayList<BitVector>> allCycles){	

		ArrayList<String> potentialEdges = makeEdgesString(theCycle);
		for(ArrayList<BitVector> cycle: allCycles){
			if(cycle.equals(theCycle)){
				continue;
			}
			//removes all the overlapping edges as potential edges
			potentialEdges.removeAll(makeEdgesString(cycle));
		}
		
		ArrayList<BitVector> twoNodesFromEdge = new ArrayList<BitVector>();
		if(!potentialEdges.isEmpty()){
			String edge = potentialEdges.get(0).replace("-", " ");
			Scanner edgeScanner = new Scanner(edge);
			int firstNode = edgeScanner.nextInt();
			int secondNode = edgeScanner.nextInt();
			edgeScanner.close();
			
			twoNodesFromEdge.add(new BitVector(1 << firstNode));
			twoNodesFromEdge.add(new BitVector(1 << secondNode));
		}
		return twoNodesFromEdge;
		
//		//if theCycle is the only cycle in the graph
//		//than any two adjacent nodes will work
//		if( allCycles.size() == 1 ){
//			
//			loop:
//			//grab first two from cycle who are connected
//			for(int i = 0; i < theCycle.size(); i++){
//				BitVector node1 = theCycle.get(i);
//				for(int j = i + 1; j < theCycle.size(); j++){
//					BitVector node2 = theCycle.get(j);
//					
//					BitVector edge = new BitVector( node1.getNumber() +
//							node2.getNumber() );
//					if( this.getEdgeCell().contains( edge )){
//						finalNode1 = node1;
//						finalNode2 = node2;
//						break loop;
//					}
//				}
//			}
//		} 
//		// more than one cycle in graph
//		else {
//			// find any two adjacent nodes of cycle which aren't both in the
//			// same cycle
//			// try all pairs one works\
//			nodeSelection:
//			for (int j = 0; j < currentCycle.size(); j++) {
//				BitVector node1 = currentCycle.get(j);
//				nodeSelectionIn:
//				for (int k = j + 1; k < currentCycle.size(); k++) {
//					BitVector node2 = currentCycle.get(k);
//
//					// make sure node1 and node2 connected in our cycle
//					if (this.edgeCell.contains(new BitVector(node1.getNumber()
//							+ node2.getNumber()))) {
//						//there must be no other cycle which contains this edge
//						
//						
//						// for each other cycle
//						for (int l = 0; l < allCycles.size(); l++) {
//							// exclude current Cycle
//							if (allCycles.get(l).equals(currentCycle)) {
//								continue;
//							}
//							
//							//if cycle contains both nodes
//							if (allCycles.get(l).contains(node1)
//									&& allCycles.get(l).contains(node2)) {
//								//this is not good
//								break nodeSelectionIn;
//							} 
//
//						}
//					
//						//made it to the end
//						finalNode1 = node1;
//						finalNode2 = node2;
//						break nodeSelection;
//
//					}
//						}	
//				} 	
//		}
//		
//		ArrayList<BitVector> spot = new ArrayList<BitVector>();
//		//if two nodes were found
//		if( finalNode1 != null){
//			spot.add( finalNode1 );
//			spot.add( finalNode2 );
//		}
//		return spot;
	}
	
	public Graph expandNode(){
		// get cell
		Cell kCell = GraphtoCell.makeCell(this);
		kCell.normalize();
		
		// try a merge
		ArrayList<BitVector> degree5 = this.getAllNodes(6);
		if(degree5.isEmpty()){
			degree5 = this.getAllNodes(5);
		}
		if(degree5.isEmpty()){
			degree5 = this.getAllNodes(4);
		}
		// if matches cell, return
		for (int i = 0; i < degree5.size(); i++) {
			BitVector node = degree5.get(i);

			ArrayList<BitVector> allNeighbours = this.getAllNeighbours(node);
			
			Graph expand = new Graph(this);
			expand.addTwoNodes();
			int lastNode = 1 << ( this.numNodes - 1 );
			int newNode1 = lastNode*2;
			int newNode2 = newNode1*2;
			
			for(BitVector bv: allNeighbours){
				expand.removeEdge(new BitVector(bv.getNumber() + node.getNumber()));
			}
			
			expand.addEdge(new BitVector(node.getNumber() + newNode1));
			expand.addEdge(new BitVector(node.getNumber() + newNode2));
			
			for(int a = 0; a < allNeighbours.size(); a++){
				if(a  + 1> allNeighbours.size() / 2){
					expand.addEdge(new BitVector(newNode1 + allNeighbours.get(a).getNumber()));
				} else{
					expand.addEdge(new BitVector(newNode2 + allNeighbours.get(a).getNumber()));
				}
			}

			Cell cell2 = GraphtoCell.makeCell(expand);
			cell2.normalize();

			if (kCell.equals(cell2)) {
				return expand;
			}
		}
		return this;
	}

	public Graph mergeNode(){
		//get cell
		Cell kCell = GraphtoCell.makeCell(this);
		if(kCell.size() != 0){
			kCell.normalize();
			
			//try a merge
			ArrayList<BitVector> degreeTwo = this.getAllNodes(2);
			//if matches cell, return
			for(int i = 0; i < degreeTwo.size(); i++){
				BitVector node = degreeTwo.get(i);
				
				ArrayList<BitVector> neighbours = this.getAllNeighbours(node);
				if(neighbours.size() != 2){
					System.out.println("weird");
					return this;
				}
				BitVector u1 = neighbours.get(0);
				BitVector u2 = neighbours.get(1);
				//get all edges connected to new graph
				ArrayList<BitVector> allNeighbours = this.getAllNeighbours(u1);
				//allNeighbours.remove(u2);
				//allNeighbours.remove(node);
				ArrayList<BitVector> allNeighbours2 = this.getAllNeighbours(u2);
				//allNeighbours2.remove(u1);
				//allNeighbours2.remove(node);
				
				//allNeighbours.addAll(allNeighbours2);
				
				//allNeighbours = Utils.removeDups(allNeighbours);
				
				Graph merged = new Graph(this);
				
				for(BitVector bv : allNeighbours){
					merged.removeEdge(new BitVector(bv.getNumber() + u1.getNumber()));
				}
				for(BitVector bv : allNeighbours2){
					merged.removeEdge(new BitVector(bv.getNumber() + u2.getNumber()));
				}
				merged.addEdge(new BitVector(u1.getNumber() + u2.getNumber()));
				
				allNeighbours.remove(u2);
				allNeighbours.remove(node);
				allNeighbours2.remove(u1);
				allNeighbours2.remove(node);
				allNeighbours.addAll(allNeighbours2);
				allNeighbours = Utils.removeDups(allNeighbours);
				
				for(BitVector bv : allNeighbours){
					merged.addEdge(new BitVector(bv.getNumber() + node.getNumber()));
				}
				
				Cell cell2 = GraphtoCell.makeCell(merged);
				cell2.normalize();
				
				if(kCell.equals(cell2)){
					return merged;
				}
			}
		}
		return this;
	}

	/**
	 * Checks whether this graph is planar. Uses Euller statements below.
	 * However, not all graphs in which that condition holds are necessarily
	 * planar. Planarity test algorithms are needed for an absolute answer.
	 * 
	 * For a simple, connected, planar graph with v vertices and e edges, the
	 * following simple planarity criteria hold: 
	 * Theorem 1. If v >= 3 then e <= 3v - 6; 
	 * Theorem 2. If v > 3 and there are no cycles of length 3, then e <= 2v - 4.
	 * 
	 * TODO not currently corect
	 * but it's not like it was useful anyway
	 * 
	 * @return whether this graph is probably planar or not
	 */
	public boolean isPlanar(){
		int numEdges = this.edgeCell.size();
		
		//first condition
		if( numEdges <= 3*this.numNodes - 6 ){
			
			//more than three nodes
			if( numNodes > 3){
				//second condition
				//if( this.isTriangleFree() && numEdges <= 2*this.numNodes - 4){
					return true;
				//}
				//failed second condition
				//else{
				//	return false;
				//}
			} 
			//meets first condition and has less than three nodes
			else{
				return true;
			}
		}
		//failed first condition
		return false;
	}

	/**
	 * Checks whether this graph is disjoint. Does this by breadth-first-searching the graph and counting
	 * how many different nodes it reaches. If not all nodes are reached, false is returned, otherwise true.
	 * @return whether this (graph) is disjoint or not (as in connected)
	 */
	public boolean isDisjoint() {
		if( this.countNodes() == this.numNodes){
			return false;
		}
		return true;
	}
	
	public int countNodes(){
		BitVector[] edges = this.edgeCell.getPA();
		//holds whether we already visited this node
		HashMap<BitVector, Boolean> reached = new HashMap<BitVector, Boolean>();
		int numberReached = 0;
		
		//start searching at node 1
		BitVector first = new BitVector(1);
		LinkedList<BitVector> openList = new LinkedList<BitVector>();
		openList.add(first);
		
		while( !openList.isEmpty() ){
			
			BitVector parent = openList.pop();
			reached.put( parent, true );
			numberReached++;
			
			//search all edges
			for(BitVector edge : edges){
				//if edges goes from this node to another
				if(edge.contains( parent.getNumber() ) ){
					//find out who another is
					BitVector another = new BitVector( edge.getNumber() - parent.getNumber() );
					
					//if we haven't already been to another
					if( reached.get( another ) == null && !openList.contains(another) ){
						//add to openList
						openList.add(another);	
					}
				}
			}
		}
		
		return numberReached;
	}
	
	/**
	 * Iterates through each port is this graph and checks the degree. Returns the 
	 * highest degree found. In order to model real molecules, the degree on all ports
	 * should be at most 2
	 * @return the largest degree of all ports of this graph
	 */
	public int getHighestPortDegree(){
		int max = -1;
		int lastNode = 1 << ( this.numPorts - 1 );
		//cycle through all nodes
		for(int node = 1; node <= lastNode; node *= 2 ){
			
			int degree = 0;
			Cell edges = this.getEdgeCell();
			//cycle through edges and count occurences of that node
			for(int i = 0; i < edges.size(); i++){
				BitVector edge = edges.getPA()[i];
				if(edge.contains(node)){
					degree++;
				}
			}
			
			if(degree > max){
				max = degree;
			}
		}
		
		return max;
	}
	
	/**
	 * Translates a graph over a Bit Vector
	 * @param bv
	 */
	public void translate(BitVector bv){
		Cell edges = this.edgeCell;

		while( !bv.isEmpty() ){
			int k = bv.firstBit();
			bv = new BitVector(bv.getNumber() - k);
			
			//new node we are adding
			BitVector p = new BitVector(1 << this.numNodes);
			
			//if any existing edges touch translation,
			//add edge from new node to what current and bv don't share
			for(int i = 0; i < edges.size(); i++){
				BitVector current = edges.getPA()[i];
				if(current.contains(k)){
					int x = current.getNumber() + p.getNumber() - k;
					edges.getPA()[i] = new BitVector(x);
				}
			}
			edges.add(new BitVector( p.getNumber() + k ));
			this.numNodes++;
		}
		
		edges.sortBySize();
	}
	
	/**
	 * Translates a graph's Cell of bitVectors into a visual representation
	 * of the edges. Each edge is translated in the following way. 
	 *  0101 = edge from 0 - 2
	 * 11000 = edge from 3 - 4
	 * 
	 * The name, #Nodes, and #Ports of the graph is also printed.
	 */
	public void writeGraph(){
		String title = "";
		if(this.name != null){
			if(this.isPlanar()){
				title += this.name + "_P: ";
			}
			else{
				title += this.name + ": ";
			}
		}
		title += this.numNodes +" Nodes, " + " " + this.numPorts + " Ports";
		System.out.println(title);
		String edges = "Edges: ";
		for(int i = 0; i < this.edgeCell.size(); i++){
			BitVector edge = this.edgeCell.getPA()[i];
			int p = edge.firstNode();
			edge = new BitVector( edge.getNumber() - ( 1 << p ) );
			int q = edge.firstNode();
			edges += ( p ) + "-" + ( q );
			
			if(i != this.edgeCell.size() - 1){
				edges += ", ";
			}
		}
		System.out.println(edges);
	}
	
	/**
	 * Returns the last port of this graph in bitvector form, 
	 * if 5 ports, lastPort = 0001 0000
	 */
	public BitVector getLastPort(){
		int lastPort = 1 << (this.numPorts - 1);
		return new BitVector(lastPort);
	}
	

	/**
	 * Returns the last node of this graph in bitvector form
	 * if 5 nodes, lastNode = 10000
	 * @return lastNode in bitvector form
	 */
	public BitVector getLastNode(){
		int lastNode = 1 << ( this.numNodes - 1 );
		return new BitVector( lastNode );
	}
	
	/**
	 * Returns the set of nodes represented in a BitVector
	 * @return, bitvector of all nodes
	 */
	public BitVector getNodeVector(){
		//bitVector = 1 * 2 ^ nodes   - 1
		//gives you x 1s, where node = x
		int bitVector = ( 1 << numNodes ) - 1;
		return new BitVector(bitVector);
	}
	
	/**
	 * Returns the set of ports in a bitvector. At this point, ports
	 * should be nodes 0 - ports - 1
	 * @return
	 */
	public BitVector getPortVector(){
		int bitVector = ( 1 << numPorts ) - 1;
		return new BitVector(bitVector);
	}

	public String getName() {
		return name;
	}
	
	public boolean addTwoNodes(){
		if(this.numNodes + 2 >= 31){
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
		if( this.numNodes >= 31){
			System.err.println("31 Nodes Reached");
		}
	}
	
	public void setName(String name2) {
		this.name = name2;	
	}
	
	/**
	 * Appends inputed string to this graphs name
	 * 
	 * this.name = this.name + input;
	 * @return
	 */
	public void appendName(String suffix){
		this.name += suffix;
	}
	
	public String toString(){
		String name = "G ";
		BitVector[] list =this.edgeCell.getPA();
		for(int i =0; i < list.length; i++){
			BitVector current = list[i];
			
			name += current.firstBit() + "-" + current.remove(current.firstBit()).firstBit();
			
			name += " ";
		}
		
		return name;
	}
}
