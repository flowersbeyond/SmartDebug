package cn.edu.thu.tsmart.tool.da.core;

import java.util.ArrayList;
import java.util.LinkedList;

import cn.edu.thu.tsmart.tool.da.core.suggestion.Fix;

public class CandidateQueue {

	private LinkedList<Fix> candidates = new LinkedList<Fix>();
	public CandidateQueue(){
		
	}
	
	public void appendNewFixes(ArrayList<Fix> fixes){
		for(Fix f: fixes){
			candidates.add(f);
			System.out.println(candidates.size() + " candidate added");
		}
	}
	
	public Fix getNextFix(){
		System.out.println(candidates.size() + " candidate removed");
		return candidates.poll();
	}

	public int getSize() {
		return candidates.size();
	}

	public boolean hasNextFix() {
		
		return !candidates.isEmpty();
	}
	
	public void clearCache(){
		candidates.clear();
	}
}
