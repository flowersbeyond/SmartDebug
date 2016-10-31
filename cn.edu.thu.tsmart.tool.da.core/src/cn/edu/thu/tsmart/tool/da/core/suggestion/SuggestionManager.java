package cn.edu.thu.tsmart.tool.da.core.suggestion;

import java.util.ArrayList;

public class SuggestionManager {

	private ArrayList<Fix> suggestions = new ArrayList<Fix>();
	private static ArrayList<ISuggestionListener> listeners = new ArrayList<ISuggestionListener>();
	
	public void confirmFix(Fix suggestion){
		if(suggestion instanceof FilterableSetFix){
			suggestions.addAll(((FilterableSetFix)suggestion).getFixes());
		} else {
			suggestions.add(suggestion);
		}
		fireSuggestionChanged();
	}
	
	private void fireSuggestionChanged(){
		for(ISuggestionListener listener: listeners){
			listener.suggestionChanged(suggestions);
		}
	}
	
	public static void registerSuggestionListener(ISuggestionListener listener){
		listeners.add(listener);
	}
	public static void removeSuggestionListener(ISuggestionListener listener){
		listeners.remove(listener);
	}
	
}
