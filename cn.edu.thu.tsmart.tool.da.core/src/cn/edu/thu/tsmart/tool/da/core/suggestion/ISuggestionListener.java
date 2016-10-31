package cn.edu.thu.tsmart.tool.da.core.suggestion;

import java.util.ArrayList;

public interface ISuggestionListener {

	public void suggestionChanged(ArrayList<Fix> suggestions);

}
