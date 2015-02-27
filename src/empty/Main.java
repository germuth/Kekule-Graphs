package empty;
import graphFinder.Modification;
import graphFinder.Randomly;
import graphFinder.TemplateMolecules;
import graphFinder.geneticAlgorithm.GeneticAlgorithm;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

//look for all uses of merge node and fix them becuase it actually returns a value
public class Main {
	private static Scanner input;
	public static void main(String[] args) {
		input = new Scanner(System.in);
		int answer = 0;
		
		while (true) {
			printMenu();
			answer = input.nextInt();
			input.nextLine();
			switch (answer) {
			case 1:
				findByRandom();
				break;
			case 2: 
				findByTemplate();
				break;
			case 3:
				findByModification();
				break;
			case 4: 
				findByGeneticAlg();
				break;
			case 5:
				findAllByRandom();
				break;
			case 6: 
				findAllByTemplate();
				break;
			case 7:
				findAllByModification();
				break;
			case 8: 
				findAllByGeneticAlg();
				break;
			case 9: 
				System.exit(0);
			case 10:
				test();
				break;
			default:
				System.out.println("Number not Understood. Try Again");
			}
			
		}
	}
	
	private static void test(){
//		ArrayList<ArrayList<Graph>> graphs = TemplateMolecules.findGraphForAllCells(getClassification(6), 6);
		for(int i = 0; i < 100; i++){
			Graph original = Randomly.createRandomGraph(6);
			
			if(original.isPortConnected()){
				Graph test = new Graph(original);
				test.removeExtraNodes();
				
				ArrayList<Graph> toView = new ArrayList<Graph>();
				toView.add(original);
				toView.add(test);
				
				if(original.getNumNodes() != test.getNumNodes()){
					original.writeGraph();
					test.writeGraph();
					MutateMain.showGraphs(toView);				
				}
					
			}
		}
	}
	
	private static void findAllByGeneticAlg() {
		int rank = getRank();
		ArrayList<ArrayList<Graph>> graphs = GeneticAlgorithm.findGraphForAllCells(getClassification(rank));
		showResults(graphs);
	}

	private static void findAllByModification() {
		int rank = getRank();
		ArrayList<ArrayList<Graph>> graphs = Modification.findGraphForAllCells(getClassification(rank));
		showResults(graphs);
	}

	private static void findAllByTemplate() {
		int rank = getRank();
		ArrayList<ArrayList<Graph>> graphs = TemplateMolecules.findGraphForAllCells(getClassification(rank), rank);
		showResults(graphs);
	}

	private static void findAllByRandom() {
		System.out.println("How many iterations?");
		int iterations = input.nextInt();
		ArrayList<ArrayList<Graph>> graphs = Randomly.findGraphForAllCells(getClassification(getRank()), iterations);
		showResults(graphs);
	}

	private static void findByGeneticAlg() {
		// TODO Auto-generated method stub
		
	}

	private static void findByModification() {
		// TODO Auto-generated method stub
		
	}

	private static void findByTemplate() {
				
	}

	private static void findByRandom() {
		// TODO Auto-generated method stub
		
	}
	
	private static void showResults(ArrayList<ArrayList<Graph>> graphsForEachCell){

		ArrayList<Integer> found = new ArrayList<Integer>();
		ArrayList<Integer> notFound = new ArrayList<Integer>();
		for(int i = 0; i < graphsForEachCell.size(); i++){
			if(graphsForEachCell.get(i).isEmpty()){
				notFound.add(i + 1);
			}else{
				found.add(i + 1);
			}
		}
		System.out.println("Total Graphs: " + (found.size() + notFound.size()));
		System.out.println("Found:" + found.size());
		System.out.println(found);
		System.out.println("Not Found: " + notFound.size());
		System.out.println(notFound);
		System.out.println("");
		ArrayList<Graph> pruned = pruneResults(graphsForEachCell);
		
		System.out.println("Would you like to print out each graph? (Y/N)");
		if(input.next().trim().toLowerCase().contains("y")){
			for(int i = 0; i < pruned.size(); i++){
				Graph curr = pruned.get(i);
				System.out.print("K" + (i+1) + " ");
				if(curr == null){
					System.out.println("No Graph Found!");
				} else {
					pruned.get(i).writeGraph();
				}
			}
		}
		System.out.println("Would you like to view the results? (Y/N)");
		if(input.next().trim().toLowerCase().contains("y")){
			MutateMain.showGraphs(Utils.removeNulls(pruned));
		}
	}
	
	private static ArrayList<Graph> pruneResults(ArrayList<ArrayList<Graph>> graphsForEachCell) {
		ArrayList<Graph> theGraphs = new ArrayList<Graph>();
		for (int i = 0; i < graphsForEachCell.size(); i++) {
			ArrayList<Graph> current = graphsForEachCell.get(i);

			if (current.isEmpty()) {
				theGraphs.add(null);
			} else {
				// TODO? preferentially show graphs with least edges
				Collections.sort(current, new Comparator<Graph>() {
					@Override
					public int compare(Graph o1, Graph o2) {
						return new Integer(o1.getNumEdges()).compareTo(o2.getNumEdges());
//						return new Integer(o2.getNumEdges()).compareTo(o1.getNumEdges());
					}
				});
				
				Graph best = current.get(0);
				best.getEdgeCell().sortBySize();
				best.getEdgeCell().removeDuplicates();
				theGraphs.add(best);
			}
		}
		return theGraphs;
	}
	
	private static ArrayList<Cell> getClassification(int rank) {
		// reading classification
		File f = new File("FullClassificationRank" + rank + ".txt");
		Scanner s = null;
		try {
			s = new Scanner(f);
		} catch (FileNotFoundException e) {
			System.err.println("File was unable to be found/read");
			e.printStackTrace();
		}
		s.nextLine();
		s.nextLine();
		s.nextLine();

		ArrayList<Cell> classifications = new ArrayList<Cell>();

		Cell inCell = null;
		try {
			
		} catch (Exception e1) {
			System.err.println("Cell was unable to be read from file");
			e1.printStackTrace();
		}

		do  {
			try {
				s.nextLine();
				String bitVectors = s.nextLine();
				Scanner lineScanner = new Scanner(bitVectors);

				Set<BitVector> allBVs = new HashSet<BitVector>();

				while (lineScanner.hasNext()) {
					String bitvector = lineScanner.next();
					bitvector = bitvector.trim();
					int number = Integer.parseInt(bitvector);
					BitVector bV = new BitVector(number);
					allBVs.add(bV);
				}
	
				inCell = new Cell(allBVs, rank);
				lineScanner.close();

				classifications.add(inCell);
			} catch (Exception e) {
				inCell = null;
			}
		} while (inCell != null);
		s.close();
		
		return classifications;
	}

	private static int getRank() {
		System.out.println("Which rank of Kekule Cells?");
		return input.nextInt();
	}
	
	private static void printMenu(){
		System.out.println("Finding Stable Graphs for Kekule Cells:");
		
		System.out.println("1. Find a Graph for a Cell - Randomly");
		System.out.println("2. Find a Graph for a Cell - Template Molecules");
		System.out.println("3. Find a Graph for a Cell - Modification");
		System.out.println("4. Find a Graph for a Cell - Genetic Algorithm");
		
		System.out.println("5. Find Graphs for all Cells in a Rank- Randomly");
		System.out.println("6. Find Graphs for all Cells in a Rank - Template Molecules");
		System.out.println("7. Find Graphs for all Cells in a Rank - Modification");
		System.out.println("8. Find Graphs for all Cells in a Rank - Genetic Algorithm");
		
		System.out.println("9. Quit");
	}

}

