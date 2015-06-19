package graphFinder;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.Set;

import display.MutateMain;
import shared.BitVector;
import shared.Cell;
import shared.Graph;
import shared.GraphtoCell;
import shared.PowerSet;
import shared.Utils;

public class TemplateMolecules {

	public static ArrayList<ArrayList<Graph>> findGraphForAllCells(ArrayList<Cell> classification, int rank) {
		ArrayList<ArrayList<Graph>> graphsForEachCell = new ArrayList<ArrayList<Graph>>();
		//TODO hardcoded for 6
		for(int i = 0; i < 214; i++){
			graphsForEachCell.add(new ArrayList<Graph>());
		}
		try {
			// read templates from txt
			// TODO add comments to this file explaining how it works
			// make graph so that viable ports go from 0 to possible ports
			//TODO also needs 2 blank line at end
			File f = new File("TemplateMolecules.txt");
			Scanner fScanner = new Scanner(f);

			TemplateMolecule next = readTemplateMolecule(fScanner);
			while (fScanner.hasNext()) {
				// try to find graphs
				ArrayList<Graph> graphs = next.getAllGraphs(rank);
				
				// test each graph for its cell
				for (Graph curr : graphs) {
					// test what cell this graph is
					Cell gsCell = GraphtoCell.makeCell(curr);
					if (gsCell.size() != 0) {
						gsCell.normalize();
						gsCell.sortBySize();
						int index = classification.indexOf(gsCell);
						if (index != -1) {
							curr.setName("K" + (index+1) + "-" + curr.getName());
							graphsForEachCell.get(index).add(curr);
						}
					}
				}

				next = readTemplateMolecule(fScanner);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return graphsForEachCell;
	}

	public static TemplateMolecule readTemplateMolecule(Scanner input) {
		String name = input.nextLine();
		if (name.isEmpty()) {
			return null;
		}
		int numNodes = input.nextInt();
		int possiblePorts = input.nextInt();
		input.nextLine();

		String inputEdges = input.nextLine();
		String inputExtraEdges = input.nextLine();
		if (!inputExtraEdges.isEmpty()) {
			input.nextLine();
		}

		return new TemplateMolecule(name, numNodes, possiblePorts, inputEdges, inputExtraEdges);
	}

	/**
	 * Parses long string of form 0-1-2-3-4-5-6-7-8-9 and returns string array of format {0 1, 1 2,
	 * 2 3, 3 4, 4 5, etc}
	 * 
	 * @param inputEdges
	 * @return
	 */
	public static Set<String> parseForEdgesCompact(String inputEdges, int[] remapping) {
		Set<String> edges = new HashSet<String>();

		// should get
		// { "1", "2", "3", "4" } etc
		// numbers are at every even index of array
		String[] edgeArray = inputEdges.split("-");

		int index = 0;
		String first = edgeArray[index];
		index++;
		first = first.trim();
		String second = edgeArray[index];
		index++;
		second = second.trim();

		// convert first and second to ints
		int fir = Integer.parseInt(first);
		int sec = Integer.parseInt(second);

		// use remapping
		if (remapping != null) {
			edges.add(remapping[fir] + " " + remapping[sec]);
		} else {
			edges.add(first + " " + second);
		}
		while (index < edgeArray.length) {
			first = second;
			second = edgeArray[index];
			second = second.trim();
			index++;

			// convert first and second to ints
			fir = Integer.parseInt(first);
			sec = Integer.parseInt(second);

			// use remapping
			if (remapping != null) {
				edges.add(remapping[fir] + " " + remapping[sec]);
			} else {
				edges.add(first + " " + second);
			}
		}

		return edges;
	}

	/**
	 * Parses long string of form 0-1, 5-6, 8-9 and returns string array of format {0 1, 1 2, 2 3, 3
	 * 4, 4 5, etc}
	 * 
	 * @param inputEdges
	 * @return
	 */
	public static Set<String> parseForEdges(String inputEdges, int[] remapping) {
		Set<String> extraEdges = new HashSet<String>();

		// should get
		// { "0-1", "5-6", "8-9"} etc
		// ranges are at every even index of array
		String[] edgeArray = inputEdges.split(",");

		int index = 0;
		while (index < edgeArray.length) {
			// grab every second element
			String edge = edgeArray[index];
			index++;
			// use other method to parse 0-1, and return "0 1"
			Set<String> singleEdge = parseForEdgesCompact(edge, remapping);
			// add to set of extra edges
			extraEdges.addAll(singleEdge);
		}

		return extraEdges;
	}

	/**
	 * Remaps the nodes so the ports are the first 0 - numPorts nodes. This allows the edge numbers
	 * to be changed based off of the new node permutation. This is what makes the difference
	 * between what nodes your ports are on. In stead of moving the algorithm to our ports, we move
	 * every other node around so the ports are first.
	 * 
	 * @param nodeNum
	 *            , the number of nodes
	 * @param portNum
	 *            , the number of ports
	 * @param ports
	 *            , a string of ports
	 * @return port remapping array
	 */
	public static int[] getPortPermutation(int nodeNum, int portNum, String ports) {
		int[] remapping = new int[nodeNum];

		// fill with below zero
		for (int i = 0; i < remapping.length; i++) {
			remapping[i] = -1;
		}

		Scanner s = new Scanner(ports);
		// keep track of current node
		int currentNode = 0;

		while (s.hasNext()) {
			String num = s.next();
			int number = Integer.parseInt(num);
			remapping[number] = currentNode++;
		}

		for (int i = 0; i < remapping.length; i++) {
			if (remapping[i] < 0) {
				remapping[i] = currentNode++;
			}
		}
		s.close();

		return remapping;
	}
}
/**
 * Template Molecule
 * 
 * This class represents a template Molecule. Template Molecules are molecules read in from a
 * library (text file). Based off the information there, a graph of the molecule is given along with
 * every possible node that could be a port. We can then try every possible combination and see the
 * resulting graph and therefore cell.
 * 
 * This is a useful technique for finding realistic graphs for many cells.
 */
class TemplateMolecule {

	// name of molecule
	private String name;

	// number of nodes
	private int numNodes;

	// the number of nodes which can have a port
	// those nodes are numbered 1 - i
	private int possiblePorts;

	// String of edges 1-2-3-4
	private String edges;

	// extra edges 1-2,2-3
	private String extraEdges;

	// constructor
	public TemplateMolecule(String name, int nN, int possiblePorts, String edges, String extraEdges) {
		this.name = name;
		this.numNodes = nN;
		this.possiblePorts = possiblePorts;
		this.edges = edges;
		this.extraEdges = extraEdges;
	}

	// must give number of ports you are looking for
	// in getALlGraphs()
	public ArrayList<Graph> getAllGraphs(int rank) {
		ArrayList<Graph> myGraphs = new ArrayList<Graph>();

		// set of all possible ports
		Set<Integer> allPossiblePorts = new HashSet<Integer>();
		for (int i = 0; i <= possiblePorts; i++) {
			allPossiblePorts.add(i);
		}

		PowerSet<Integer> powerSet = new PowerSet<Integer>(allPossiblePorts, rank, rank);

		Iterator<Set<Integer>> portGroups = powerSet.iterator();
		// graph number
		int i = 0;
		while (portGroups.hasNext()) {
			Set<Integer> portGroup = portGroups.next();

			if (portGroup.isEmpty()) {
				continue;
			}
			// convert set to string
			String ports = "";
			for (Integer a : portGroup) {
				ports += a + " ";
			}

			int[] portRemapping = getPortPermutation(numNodes, rank, ports);

			// parses close edge format "0-1-2-3"
			Set<String> edges = parseForEdgesCompact(this.edges, portRemapping);

			if (!this.extraEdges.isEmpty()) {
				// parses extra edge format "0-1, 1-2"
				Set<String> extraEdges = parseForEdges(this.extraEdges, portRemapping);
				edges.addAll(extraEdges);
			}
			Graph current = new Graph(this.name + i, rank, this.numNodes, edges);
			i++;
			myGraphs.add(current);
		}

		return myGraphs;
	}

	/**
	 * Parses long string of form 0-1-2-3-4-5-6-7-8-9 and returns string array of format {0 1, 1 2,
	 * 2 3, 3 4, 4 5, etc}
	 * 
	 * @param inputEdges
	 * @return
	 */
	public static Set<String> parseForEdgesCompact(String inputEdges, int[] remapping) {
		Set<String> edges = new HashSet<String>();

		// should get
		// { "1", "2", "3", "4" } etc
		// numbers are at every even index of array
		String[] edgeArray = inputEdges.split("-");

		int index = 0;
		String first = edgeArray[index];
		index++;
		first = first.trim();
		String second = edgeArray[index];
		index++;
		second = second.trim();

		// convert first and second to ints
		int fir = Integer.parseInt(first);
		int sec = Integer.parseInt(second);

		// use remapping
		if (remapping != null) {
			edges.add(remapping[fir] + " " + remapping[sec]);
		} else {
			edges.add(first + " " + second);
		}
		while (index < edgeArray.length) {
			first = second;
			second = edgeArray[index];
			second = second.trim();
			index++;

			// convert first and second to ints
			fir = Integer.parseInt(first);
			sec = Integer.parseInt(second);

			// use remapping
			if (remapping != null) {
				edges.add(remapping[fir] + " " + remapping[sec]);
			} else {
				edges.add(first + " " + second);
			}
		}

		return edges;
	}

	/**
	 * Parses long string of form 0-1, 5-6, 8-9 and returns string array of format {0 1, 1 2, 2 3, 3
	 * 4, 4 5, etc}
	 * 
	 * @param inputEdges
	 * @return
	 */
	public static Set<String> parseForEdges(String inputEdges, int[] remapping) {
		Set<String> extraEdges = new HashSet<String>();

		// should get
		// { "0-1", "5-6", "8-9"} etc
		// ranges are at every even index of array
		String[] edgeArray = inputEdges.split(",");

		int index = 0;
		while (index < edgeArray.length) {
			// grab every second element
			String edge = edgeArray[index];
			index++;
			// use other method to parse 0-1, and return "0 1"
			Set<String> singleEdge = parseForEdgesCompact(edge, remapping);
			// add to set of extra edges
			extraEdges.addAll(singleEdge);
		}

		return extraEdges;
	}

	/**
	 * Remaps the nodes so the ports are the first 0 - numPorts nodes. This allows the edge numbers
	 * to be changed based off of the new node permutation. This is what makes the difference
	 * between what nodes your ports are on. In stead of moving the algorithm to our ports, we move
	 * every other node around so the ports are first.
	 * 
	 * @param nodeNum
	 *            , the number of nodes
	 * @param portNum
	 *            , the number of ports
	 * @param ports
	 *            , a string of ports
	 * @return port remapping array
	 */
	public static int[] getPortPermutation(int nodeNum, int portNum, String ports) {
		int[] remapping = new int[nodeNum];

		// fill with below zero
		for (int i = 0; i < remapping.length; i++) {
			remapping[i] = -1;
		}

		Scanner s = new Scanner(ports);
		// keep track of current node
		int currentNode = 0;

		while (s.hasNext()) {
			String num = s.next();
			int number = Integer.parseInt(num);
			remapping[number] = currentNode++;
		}

		for (int i = 0; i < remapping.length; i++) {
			if (remapping[i] < 0) {
				remapping[i] = currentNode++;
			}
		}
		s.close();

		return remapping;
	}
}
