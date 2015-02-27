package graphFinder.geneticAlgorithm;

import java.util.ArrayList;
import java.util.Random;

import empty.Cell;
import empty.Graph;
import empty.GraphtoCell;
import empty.MutateMain;
import graphFinder.GraphFinder;
import graphFinder.Randomly;

public class GeneticAlgorithm implements GraphFinder{
	private static Random rng = new Random();
	
	public static ArrayList<ArrayList<Graph>> findGraphForAllCells(ArrayList<Cell> classification) {
		ArrayList<ArrayList<Graph>> graphsForEachCell = new ArrayList<ArrayList<Graph>>();
		for(int i = 0; i < 214; i++){
			graphsForEachCell.add(new ArrayList<Graph>());
		}
		
		for(int i = 0; i < classification.size(); i++){
			Cell cell = classification.get(i);
			//Generate the initial population randomly
			ArrayList<Evolvable> toBeEvolved = new ArrayList<Evolvable>();
			for(int j = 0; j < GAParameters.getPopulationSize(); j++){
				EvolvableGraph eG = new EvolvableGraph(Randomly.createRandomGraph(cell.getNumPorts()));
				
				Cell gsCell = GraphtoCell.makeCell(eG.graph);
				// cell may be empty if there is a secluded port
				if (gsCell.size() != 0) {
					gsCell.normalize();
					gsCell.sortBySize();
				}
				int index = classification.indexOf(gsCell);
//				if(index == 4 && i == 4){
//					System.out.println("BLEAK POINT");
//				}
				
				eG.calculateFitness(cell);
				toBeEvolved.add(eG);
			}
			Population population = new Population(toBeEvolved);
			ArrayList<Evolvable> ans = GeneticAlgorithm.run(cell, population);
			//convert back to graph
			ArrayList<Graph> graphs = new ArrayList<Graph>();
			for(Evolvable e: ans){
				graphs.add(((EvolvableGraph)e).graph);
			}
			
			//TODO
			//make sure my genetic algorithm isn't bad
			for(Graph g: graphs){
				// test what cell this graph is
				Cell gsCell = GraphtoCell.makeCell(g);
				if (gsCell.size() != 0) {
					gsCell.normalize();
					gsCell.sortBySize();
					if (gsCell.equals(cell)) {
						if(g.tryToEditForRealisticness()){
							graphsForEachCell.get(i).add(g);
						}else{
							System.out.println("GRAPH WAS UNFIXABLE");
//							boolean y = g.isFixable();
//							boolean x = g.isRealistic();
//							ArrayList<Graph> gss = new ArrayList<Graph>();
//							gss.add(g);
//							MutateMain.showGraphs(gss);
//							x = x || false;
						}
					}else{
						System.out.println("SOMETHING WRONG WITH GENETIC ALGORITHM");
					}
				}
			}
			if(graphsForEachCell.get(i).isEmpty()){
				System.out.println((i+1) + ": None found");
				System.out.println("WASTED");
			}else{
				System.out.println((i+1) + ": " + graphsForEachCell.get(i).size() + " found");
			}
		}
		
		return graphsForEachCell;
	}
	
	public static ArrayList<Evolvable> run(Cell cell, Population population){
		int maxFitness = cell.size();

//		long startTime = System.currentTimeMillis();
		population = geneticAlgorithm(population, cell, maxFitness);
		
//		population.printAverage();
//		if( population.getBestLength( maxFitness) < 1){
//			System.out.println("No Graphs Found.");
//		}else{
//			System.out.println("Graphs Found");
//		}
		
//		long duration = System.currentTimeMillis() - startTime;
//		System.out.println("Time Taken: " + (double)duration/1000.0 + " seconds.");
		
		return population.getBest(maxFitness);
	}

	/**
	 * Runs the genetic algorithm. A population of graphs is evolved towards the required cell. 
	 */
	private static Population geneticAlgorithm(Population population, Cell target, int maxFitness){
		
		ArrayList<Evolvable> nextGen = null;
		for(int i = 0; i < GAParameters.getIterations() ; i++){
			population.prune(maxFitness);
			
			//if we've found enough graphs, quit
			if( population.getBestLength(maxFitness) >= GAParameters.getMinimumGraphsRequired() ){
				break;
			}
			
			//print out progress report every 10 percent
//			double progress = (double)i/(double)GAParameters.getIterations();
//			if((progress*100) % 5 == 0){
			if(i % 5 == 0 && i != 0){
				System.out.println((i + 1));
			}
			
			//grab 100 graphs for the next iteration
			//90 are elite
			//10 are random for genetic diversity
			nextGen = population.getNextGeneration();
			
			//mutate 200 random Graphs and add to nextGen
			//since we're picking from 100 elites
			//likely to get each graph twice
			//must add mutants to separate list to ensure they are not crossover-ed this iteration
			ArrayList<Evolvable> mutants = new ArrayList<Evolvable>();
			for(int j = 0; j < GAParameters.getMutantNumber(); j++){
				Evolvable mutant = nextGen.get( rng.nextInt( nextGen.size()) );
				mutant = mutant.mutate();
				mutant.setFitness( 0 );
				mutants.add( mutant );
			}
			
			//Perform crossover
			//two random graphs from elite are chosen and combined
			for(int j = 0; j < GAParameters.getCrossoverNumber(); j++){
				//TODO use 4 parents
				//get two random parents
				Evolvable parent1 = nextGen.get( rng.nextInt( nextGen.size()) );
				Evolvable parent2 = nextGen.get( rng.nextInt( nextGen.size()) );
				
				Evolvable child = parent1.crossoverWith(parent2);
				child.setFitness( 0 );
				nextGen.add(child);
			}
			
			nextGen.addAll(mutants);
			
			// fitness function on every graph
			for (int j = 0; j < nextGen.size(); j++) {
				Evolvable current = nextGen.get(j);
				current.calculateFitness(target);
			}
			
			population = new Population( nextGen );
		}
		return population;
	}
}
