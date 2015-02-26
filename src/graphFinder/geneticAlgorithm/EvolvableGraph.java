package graphFinder.geneticAlgorithm;

import java.util.Random;

import empty.BitVector;
import empty.Cell;
import empty.Graph;
import empty.GraphtoCell;

public class EvolvableGraph implements Evolvable {
	private static Random rng = new Random();

	public double fitness;
	public Graph graph;

	public EvolvableGraph(Graph graph) {
		this.graph = graph;
		this.fitness = 0;
	}

	@Override
	public double getFitness() {
		return fitness;
	}

	@Override
	public void setFitness(double val) {
		this.fitness = val;
	}

	@Override
	/**
	 * Calculates the fitness of a given Graph g. Fitness is calculated as follows.
	 * The cell of Graph g is compared to the Cell we are looking for. For Every Port assignment
	 * that both share, the fitness value is increased by one. However, for every port assignment
	 * which g has but the cell doesn't, or a port assignment which is missing from g but present
	 * in the answer cell, the fitness is reduced one point. If the cell is empty ( a secluded port
	 * was made), the cell is automatically assigned a fitness of -10, so it is not used in future 
	 * generations.
	 * 
	 * Assigns the fitness value calculated to the graphs 'fitness' field.
	 * 
	 * @param g, the graph we are calculating the fitness for
	 */
	public void calculateFitness(Cell target) {
		// fitness already calculated
		if (this.getFitness() != 0) {
			return;
		}

		Cell gsCell = GraphtoCell.makeCell(this.graph);
		// cell may be empty if there is a secluded port
		if (gsCell.size() == 0) {
			this.setFitness(-10);
		} else {
			gsCell.normalize();
			gsCell.sortBySize();

			int answerIndex = 0;
			int gsIndex = 0;

			double fitness = 0;

			while (answerIndex != target.size() && gsIndex != gsCell.size()) {
				BitVector correct = target.getPA()[answerIndex];
				BitVector gBV = gsCell.getPA()[gsIndex];

				// if it has the port assignment
				if (correct.equals(gBV)) {
					fitness++;

					answerIndex++;
					gsIndex++;
				} else {
					fitness--;
					// if it doesn't have the port assignment
					if (correct.getNumber() < gBV.getNumber()) {
						answerIndex++;
					}
					// if it has extra port assignment
					else {
						gsIndex++;
					}
				}

			}

			// if answer or g have extra port assignments which were not reached
			// we must subtract 1 fitness for each of those
			// only one of these terms should ever be non-zero
			fitness -= ((target.size() - answerIndex) + (gsCell.size() - gsIndex));
			this.setFitness(fitness);
		}
	}

	@Override
	/**
	 * Randomly mutates a graph for the genetic Algorithm. Note: the original
	 * graph is left untouched, a new graph is created. 
	 * Graphs are mutated by the following operations
	 * X% chance to add a node
	 * X% chance (sort of) to remove a node
	 * 		- must ensure all edges going to that node deleted as well
	 * X% chance to add a random edge
	 * 		- must ensure edge doesn't overflow the max degree restrictions
	 * X% chance to remove a random edge
	 * 
	 * It's possible a graph goes through this method and doesn't get mutated at all
	 * 
	 * X% chance to extend out the ports. 
	 * 
	 * Percentages are determined by GAParameters class
	 * @param starting, The Graph we want to mutate
	 * @return mutant, the mutated graph
	 */
	public Evolvable mutate() {
		//TODO?
		int rank = this.graph.getEdgeCell().getNumPorts();
		Graph mutant = new Graph(this.graph);

		// add vertex
		if (rng.nextDouble() < GAParameters.getAddNodeChance() && mutant.getNumNodes() < 30) {
			mutant.setNumNodes(mutant.getNumNodes() + 1);
		}
		// remove vertex
		// must ensure we can delete a node
		if (rng.nextDouble() < GAParameters.getRemoveNodeChance() && mutant.getNumNodes() > rank) {
			// get random node
			// if port is deleted, node after that will be assigned new port automatically
			int node = 1 << rng.nextInt(mutant.getNumNodes());
			// and remove all edges with deleted vertex
			for (int i = 0; i < mutant.getEdgeCell().size(); i++) {
				BitVector edge = mutant.getEdgeCell().getPA()[i];
				if (edge == null) {
					System.out.println("WHAT IS HAPPENING");
				}
				if (edge.contains(node)) {
					mutant.removeEdge(edge);
				}
			}
			mutant.setNumNodes(mutant.getNumNodes() - 1);
		}

		// add edge
		if (rng.nextDouble() < GAParameters.getAddEdgeChance()) {
			int node1 = 1 << rng.nextInt(mutant.getNumNodes());
			int node2 = 1 << rng.nextInt(mutant.getNumNodes());
			BitVector newEdge = new BitVector(node1 + node2);

			int attempts = 0;
			// keep trying until edge is satisfactory
			// or dont' add any edge if you try over 15 times
			while (mutant.isBadEdge(newEdge) && attempts < 15) {
				node1 = 1 << rng.nextInt(mutant.getNumNodes());
				node2 = 1 << rng.nextInt(mutant.getNumNodes());
				newEdge = new BitVector(node1 + node2);
				attempts++;
			}

			if (attempts < 15) {
				mutant.addEdge(newEdge);
			}

		}

		// remove edge
		if (rng.nextDouble() < GAParameters.getRemoveEdgeChance()
				&& mutant.getEdgeCell().size() > 0) {
			BitVector removedEdge = mutant.getEdgeCell().getPA()[rng.nextInt(mutant.getEdgeCell()
					.size())];
			mutant.removeEdge(removedEdge);
		}

		// extend the ports out
		if (rng.nextDouble() < GAParameters.getExtendPortsChance()
				&& mutant.getNumNodes() < (30 - mutant.getNumPorts())) {
			mutant = mutant.extendPortsNoCell();
		}
		return new EvolvableGraph(mutant);
	}

	@Override
	/**
	 * Crossover is perform by iterating through the
	 * edges of both graphs. If both graphs share an edge, the child gets that edge. If both
	 * graphs do not share an edge, the child has a 50 - 50 chance to inherit that edge.
	 * 
	 * In some cases the child will not get all of the above edges, in order to keep the child's
	 * degree below the maximum limitation.
	 */
	public Evolvable crossoverWith(Evolvable parent2) {
		EvolvableGraph otherParent = (EvolvableGraph) parent2;
		Cell one = this.graph.getEdgeCell();
		one.sortBySize();
		Cell two = otherParent.graph.getEdgeCell();
		two.sortBySize();
		int indexOne = 0;
		int indexTwo = 0;

		// TODO is this right?
		int rank = one.getNumPorts();

		Cell childCell = new Cell();
		childCell.setNumPorts(rank);

		// must set number of nodes at end of method, for now it's set to max of both parents
		// fitness assigned to zero in constructor
		Graph child = new Graph("C(" + this.graph.getName() + ")(" + otherParent.graph.getName()
				+ ")", rank, 0, childCell);
		child.setNumNodes(Math.max(this.graph.getNumNodes(), otherParent.graph.getNumNodes()));

		// iterate through both edges
		while (indexOne != one.size() && indexTwo != two.size()) {
			BitVector edge = one.getPA()[indexOne];
			BitVector edge2 = two.getPA()[indexTwo];

			// if they both have that edge
			// child gets it
			if (edge.equals(edge2)) {
				if (!child.isBadEdge(edge)) {
					child.addEdge(edge);
				}
				indexOne++;
				indexTwo++;
			}
			// if only one of them has the edge
			else {
				// since edges are sorted by size
				// the smaller edge we know for sure is not in the other graph
				// only increment the graph with the smaller edge
				BitVector smallerEdge;
				if (edge.getNumber() > edge2.getNumber()) {
					smallerEdge = edge2;
					indexTwo++;
				} else {
					smallerEdge = edge;
					indexOne++;
				}
				// 50-50 chance to get this edge
				if (rng.nextDouble() < 0.50) {
					if (!child.isBadEdge(smallerEdge)) {
						child.addEdge(smallerEdge);
					}
				}
			}

		}
		// for every remaining edge
		// we must get 50 50 chance to add
		// every remaining in first
		for (int i = 0; indexOne < one.size(); indexOne++) {
			BitVector edge = one.getPA()[indexOne];

			// 50-50 chance to get this edge
			if (rng.nextDouble() < 0.50) {
				if (!child.isBadEdge(edge)) {
					child.addEdge(edge);
				}
			}
		}
		// every remaining edge in second
		for (int i = 0; indexTwo < one.size(); indexTwo++) {
			BitVector edge = one.getPA()[indexTwo];

			// 50-50 chance to get this edge
			if (rng.nextDouble() < 0.50) {
				if (!child.isBadEdge(edge)) {
					child.addEdge(edge);
				}
			}
		}

		// first node is numbered 0
		int numberNodes = childCell.getHighestNode() + 1;
		child.setNumNodes(numberNodes);

		return new EvolvableGraph(child);
	}

	@Override
	public int compareTo(Evolvable o) {
		if (this.fitness < o.getFitness()) {
			return 1;
		} else if (this.fitness > o.getFitness()) {
			return -1;
		}
		return 0;
	}
}
