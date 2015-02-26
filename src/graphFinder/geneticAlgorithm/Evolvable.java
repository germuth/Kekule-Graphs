package graphFinder.geneticAlgorithm;

import empty.Cell;

public interface Evolvable extends Comparable<Evolvable>{
	public double getFitness();
	public void setFitness(double val);
	public void calculateFitness(Cell target);
	
	public Evolvable mutate();
	public Evolvable crossoverWith(Evolvable parent2);
}
