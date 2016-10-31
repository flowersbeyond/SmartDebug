package cn.edu.thu.tsmart.tool.da.core.fl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Set;

import cn.edu.thu.tsmart.tool.da.core.Logger;

public class FaultLocalizer {
	private static ArrayList<FaultLocalizationListener> listeners = new ArrayList<FaultLocalizationListener>();
	private Spectrum allSpec;
	private int allPassCount = 0;
	private int allFailCount = 0;
	
	private Logger logger;
	
	public FaultLocalizer(Logger logger){
		this.logger = logger;
		allSpec = new Spectrum();
	}
	
	public ArrayList<BasicBlock> localize(){
		BlockProfileComparator comparator = new BlockProfileComparator(allPassCount, allFailCount);
		// why putting the calculation logic in sorting (in the compare() method of Comparator<BlockProfile>)?
		allSpec.allBlockProfile.sort(comparator);
		
		ArrayList<BasicBlock> sortedBBList = new ArrayList<BasicBlock>();
		for(BlockProfile bp: allSpec.allBlockProfile){
			logger.log(Logger.DEBUG_MODE, "FAULT_LOCALIZATION_RESULT", bp.bblock.toString() + ":" + bp.score);
			if(bp.score <= 0.0000000001)
				break;
			sortedBBList.add(bp.bblock);
		}
		fireFaultLocalizationFinished(sortedBBList);
		return sortedBBList;
	}

	public void mergeNewTrace(Set<BasicBlock> blockList, boolean testResult) {		
		if(testResult)
			allPassCount ++;
		else
			allFailCount ++;
		
		Spectrum specFrag = new Spectrum();			
		for(BasicBlock bb: blockList){
			BlockProfile bProfile = specFrag.find(bb);
			if(bProfile == null){
				bProfile = new BlockProfile(bb);
				specFrag.addProfile(bProfile);
			}
			if(testResult){
				bProfile.passHitCount ++;
				bProfile.passTestCaseCount ++;
			} else {
				bProfile.failHitCount ++;
				bProfile.failTestCaseCount ++;
			}

		}
		allSpec.mergeSpectrum(specFrag);
	}

	public static void registerListener(FaultLocalizationListener listener){
		listeners.add(listener);
	}
	
	public static void removeListener(FaultLocalizationListener listener){
		listeners.remove(listener);
	}

	private static void fireFaultLocalizationFinished(ArrayList<BasicBlock> results){
		for(FaultLocalizationListener listener: listeners){
			listener.localizationFinished(results);
		}
	}
}

class BlockProfile implements Comparable<BlockProfile>{
	protected BasicBlock bblock;
	protected int passHitCount;
	protected int failHitCount;
	protected int passTestCaseCount;
	protected int failTestCaseCount;
	
	protected double score;
	
	BlockProfile(BasicBlock bb){
		this.bblock = bb;
		this.score = -1;
	}

	@Override
	public int compareTo(BlockProfile other) {
		if(this.getScore() < other.getScore())
			return 1;
		if(this.getScore() > other.getScore())
			return -1;
		return 0;
	}
	
	public double getScore(){
		return score;		
	}
	public void setScore(double score){
		this.score = score;
	}
}


class BlockProfileComparator implements Comparator<BlockProfile>{
	
	int allFailCount;
	int allPassCount;
	public BlockProfileComparator(int allPassCount, int allFailCount){
		this.allFailCount = allFailCount;
		this.allPassCount = allPassCount;
	}

	@Override
	public int compare(BlockProfile arg0, BlockProfile arg1) {
		ensureScore(arg0);
		ensureScore(arg1);
		
		if(arg0.getScore() < arg1.getScore())
			return 1;
		if(arg0.getScore() > arg1.getScore())
			return -1;
		return 0;
	}
	
	private void ensureScore(BlockProfile profile){
		if(profile.getScore() < 0){
			//Ochiai fl metric
			double score = (double)profile.failTestCaseCount
					/ Math.sqrt(allFailCount * (profile.failTestCaseCount + profile.passTestCaseCount));
			profile.setScore(score);
		}
	}
}


class Spectrum{
	ArrayList<BlockProfile> allBlockProfile = new ArrayList<BlockProfile>();
	
	public BlockProfile find(BasicBlock block){
		for(BlockProfile bp: allBlockProfile){
			if(bp.bblock.getSSABasicBlock().equals(block.getSSABasicBlock()))
				return bp;
		}
		return null;
	}

	public void addProfile(BlockProfile profile) {
		allBlockProfile.add(profile);
		
	}
	
	public void mergeSpectrum(Spectrum specFrag){
		for(BlockProfile bp: specFrag.allBlockProfile){
			BlockProfile bpHere = find(bp.bblock);
			if(bpHere == null){
				allBlockProfile.add(bp);
				if(bp.failTestCaseCount > 0)
					bp.failTestCaseCount = 1;
				if(bp.passTestCaseCount > 0)
					bp.passTestCaseCount = 1;
			}
			else{
				bpHere.failHitCount += bp.failHitCount;
				if(bp.failTestCaseCount > 0)
					bpHere.failTestCaseCount ++;
				bpHere.passHitCount += bp.passHitCount;
				if(bp.passTestCaseCount > 0)
					bpHere.passTestCaseCount ++;
			}
		}
	}
}