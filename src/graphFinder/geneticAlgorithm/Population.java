package graphFinder.geneticAlgorithm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import empty.Cell;
import empty.Graph;
import empty.GraphToSMILES;
import empty.GraphtoCell;
import empty.Utils;

/**
 * Population
 * 
 * A Population is list of graph used for the genetic algorithm. The list
 * is sorted by fitness so that the most fit graphs are at the begning of the list.
 *
 */
public class Population {
	private ArrayList<Evolvable> population;
	private Random random;
	
	public Population(ArrayList<Evolvable> nextGen){
		Collections.sort(nextGen);
		this.population = nextGen;
		this.random = new Random();
//		double sum = 0;
//		for(int i = 0; i < this.size(); i++){
//			sum += this.population.get(i).getFitness();
//		}
//		System.out.println("Average: " + (sum / this.size()));
	}
	
	public Evolvable getBest(){
		return this.population.get(0);
	}
	
	public Evolvable getRandom(){
		return this.population.get( this.random.nextInt(this.size()) );
	}

	/**
	 * Prints the current state of the population to Standard output.
	 * This includes the fitness of:
	 * The Best EvolvableGraph
	 * Average EvolvableGraph
	 * The Worst EvolvableGraph
	 * 50th EvolvableGraph ( top 10% in population of 500)
	 */
	public void printAverage(){
		int sum = 0;
		for(int i = 0; i < this.size(); i++){
			sum += this.population.get(i).getFitness();
		}
		System.out.println("Best: " + this.population.get(0).getFitness() );
		System.out.println("Average: " + (sum / this.size()));
		System.out.println("Worst: " + this.population.get( this.size()-1 ).getFitness() );
		System.out.println("50th " + this.population.get( 50).getFitness() );
	}
	
	public int getBestLength(int fitness){
		if( this.getBest().getFitness() != fitness ){
			return 0;
		} else{
			int count = 0;
			for(int i = 0; i < this.population.size(); i++){
				if( this.population.get(i).getFitness() == fitness){
					count++;
				} else{
					break;
				}
			}
			return count;
		}
	}
	
	public void prune(int maxFitness){
		for(int i = 0; i < this.population.size(); i++){
			Evolvable g = this.population.get(i);
			if(g.getFitness() == maxFitness){
//				//TODO should use evolvable interface
				EvolvableGraph gg = (EvolvableGraph) g;
				if(!gg.graph.tryToEditForRealisticness()){
					g.setFitness(g.getFitness() - 1.0);
				}
			}else{
				break;
			}
		}
		Collections.sort(this.population);
	}
	
	/**
	 * Outputs the best Evolvable's of the population. Usually used at the end
	 * of the genetic algorithm. Currently returns all graphs who have maximum
	 * fitness
	 */
	public ArrayList<Evolvable> getBest(int bestFitness){
		ArrayList<Evolvable> answer = new ArrayList<Evolvable>();
		for(int i = 0; i < this.size(); i++){
			if( this.population.get(i).getFitness() == bestFitness){
				answer.add( this.population.get(i) );
			} else{
				break;
			}
		}
		return answer;
	}
	
	/**
	 * Gets the next starting 100 EvolvableGraphs for the genetic
	 * algorithm. 
	 * 
	 * The list of EvolvableGraphs in a population is kept in sorted order. 
	 * This means the EvolvableGraphs with the highest fitness will be first. 
	 * Therefore we take the first 90 EvolvableGraphs in the population, and
	 * 10 random ones to ensure genetic diversity. 
	 * @return 100 EvolvableGraphs, compromising the survivors of last generation
	 */
	public ArrayList<Evolvable> getNextGeneration(){
		//for next generation the best 90 are picked
		ArrayList<Evolvable> nextGen = new ArrayList<Evolvable>();
		for(int i = 0; i < GAParameters.getEliteNumber(); i++){
			nextGen.add(population.get(i));
		}
		//and 10 random ones
		for(int i = 0; i < GAParameters.getRandomNumber(); i++){
			nextGen.add( this.getRandom() );
		}
		
		return nextGen;
	}
	
	public Evolvable get(int i ){
		return this.population.get(i);
	}
	public int size(){
		return this.population.size();
	}
	
}
