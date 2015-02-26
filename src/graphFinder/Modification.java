package graphFinder;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;

import empty.Cell;
import empty.Graph;
import empty.Utils;

public class Modification implements GraphFinder{
	
	public static ArrayList<ArrayList<Graph>> findGraphForAllCells(ArrayList<Cell> classification) {
	
		ArrayList<ArrayList<Graph>> graphsForEachCell = new ArrayList<ArrayList<Graph>>();
		for(int i = 0; i < 214; i++){
			graphsForEachCell.add(new ArrayList<Graph>());
		}
		ArrayList<Graph> hesselinkGraphs = getRank6Graphs();
		for(int i = 0; i < hesselinkGraphs.size(); i++){
			Graph g = hesselinkGraphs.get(i);
			//try to fix the graph
			g.tryToConnect();
			for(int k = 0; k < 10; k++){
				g.mergeNode();
				g.tryToFixCycleSize();
			}
			
			if(g.isRealistic()){
				g.setName("K" + (i+1) + "-" + "H-edited");
				graphsForEachCell.get(i).add(g);
			}
		}
		
		return graphsForEachCell;
	}
	
	public static ArrayList<Graph> getRank6Graphs(){
		ArrayList<Graph> graphs = new ArrayList<Graph>();
		int graphCount = 1;
		File f = new File("Rank6HesselinkGraphs.txt");
		try {
			Scanner s = new Scanner(f);
			
			while(s.hasNextLine()){
				String next = s.nextLine();
				if( next.contains("Gra6nr")){
					Scanner s1 = new Scanner(next);
					s1.next();
					int numPorts = s1.nextInt();
					String numNodesO = s1.next();
					numNodesO = numNodesO.substring(0, numNodesO.length() - 1);
					int numNodes = Integer.parseInt(numNodesO);
					Set<String> edges = new TreeSet<String>();
					while( s1.hasNext()){
						String temp = s1.next();
						temp = temp.replace(",", " ");
						temp = temp.replace(".", " ");
						temp = temp.replace("-", " ");
						edges.add(temp);
					}
					
					Graph current = new Graph("graph" + graphCount++, numPorts, numNodes, edges);
					graphs.add(current);
				}else if(next.contains("No graph found")){
					//Hesselink didn't find every graph
					//still want to associate correct numbers which each one though
					graphCount++;
				}
			}
		} catch( FileNotFoundException ex){
			ex.printStackTrace();
		}
		
		return graphs;
	}
}
