package cn.edu.thu.tsmart.tool.da.core.suggestion;

import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.widgets.Display;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.UndoEdit;

import cn.edu.thu.tsmart.tool.da.core.search.strategy.tmpl.fs.AbstractFixSite;

public class FilterableFix extends Fix implements Filterable{
	
	protected AbstractFixSite fixSite;
	
	protected IFile modifiedFile;
	protected int startposition;
	protected int originalLength;
	protected String originalString;
	protected String modifiedString;
	
	protected int fixLineNum;
	
	protected String fixType;
	
	protected double exprPriority;
	protected double fixSimilarity;
	
	protected Expression targetExpr;
	protected String targetExprType;
	protected String newExprString;
	protected boolean hasExpectedValue = false;
	protected String expectedValue = "false";
	
	protected UndoEdit undoEdit;
	
	public FilterableFix(AbstractFixSite fixSite, int startPosition, int originalLength, String modifiedString, String fixType, int fixLineNum, Expression targetExpr, String targetExprType, String newExprString) {
		this.fixSite = fixSite;
		
		this.modifiedFile = fixSite.getFile();
		this.startposition = startPosition;
		this.originalLength = originalLength;
		this.modifiedString = modifiedString;
		
		this.fixType = fixType;
		this.fixLineNum = fixLineNum;
		
		this.targetExpr = targetExpr;
		this.targetExprType = targetExprType;
		this.newExprString = newExprString;
	}
	
	public FilterableFix(AbstractFixSite fixSite, int startPosition, int originalLength, String modifiedString, String fixType, int fixLineNum, Expression targetExpr, String targetExprType, String newExprString, String passTCExpectedValue) {
		this.fixSite = fixSite;
		
		this.modifiedFile = fixSite.getFile();
		this.startposition = startPosition;
		this.originalLength = originalLength;
		this.modifiedString = modifiedString;
		
		this.fixType = fixType;
		this.fixLineNum = fixLineNum;
		
		this.targetExpr = targetExpr;
		this.targetExprType = targetExprType;
		this.newExprString = newExprString;
		this.hasExpectedValue = true;
		this.expectedValue = passTCExpectedValue;
	}
	
	
	public void setExprPriority(double priority){
		this.exprPriority = priority;
	}
	public double getExprPriority(){
		return this.exprPriority;
	}
	public void setFixSimilarity(double similarity){
		this.fixSimilarity = similarity;
	}
	public double getFixSimilarity(){
		return this.fixSimilarity;
	}

	@Override
	public void doFix() {
		System.out.println("enter do Fix()");
		
		ReplaceEdit replaceEdit = new ReplaceEdit(startposition, originalLength, modifiedString);
		ITextFileBuffer fileBuffer = ITextFileBufferManager.DEFAULT.getTextFileBuffer(fixSite.getFile().getFullPath(), LocationKind.IFILE);
		if(fileBuffer == null){
			System.out.println("FileBuffer Not Found: " + fixSite.getFile().getFullPath());
			try {
				ITextFileBufferManager.DEFAULT.connect(fixSite.getFile().getFullPath(), LocationKind.IFILE, new NullProgressMonitor());
				fileBuffer = ITextFileBufferManager.DEFAULT.getTextFileBuffer(fixSite.getFile().getFullPath(), LocationKind.IFILE);
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
		IDocument document = fileBuffer.getDocument();
		try {
			this.originalString = document.get(this.startposition, this.originalLength);
		} catch (BadLocationException e1) {
			e1.printStackTrace();
		}
		try {
			Display.getDefault().syncExec(new Runnable(){

				@Override
				public void run() {
					try {
						undoEdit = replaceEdit.apply(document);
					} catch (MalformedTreeException e) {
						e.printStackTrace();
					} catch (BadLocationException e) {
						e.printStackTrace();
					}
					
				}
				
			});
			fileBuffer.commit(new NullProgressMonitor(), true);
		} catch (MalformedTreeException e) {
			e.printStackTrace();
		} catch (CoreException e) {
			e.printStackTrace();
		}
		
	}

	@Override
	public void undoFix() {
		ITextFileBuffer fileBuffer = ITextFileBufferManager.DEFAULT.getTextFileBuffer(fixSite.getFile().getFullPath(), LocationKind.IFILE);
		IDocument document = fileBuffer.getDocument();
		if(undoEdit == null)
			return;
		else{
			try {
				Display.getDefault().syncExec(new Runnable(){

					@Override
					public void run() {
						try {
							undoEdit.apply(document);
						} catch (MalformedTreeException e) {
							e.printStackTrace();
						} catch (BadLocationException e) {
							e.printStackTrace();
						}				
					}
					
				});
				fileBuffer.commit(new NullProgressMonitor(), true);
			} catch (MalformedTreeException e) {
				e.printStackTrace();
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	@Override
	public IFile[] getModifiedFiles() {
		IFile[] files = new IFile[] {this.modifiedFile};
		return files;
	}

	public String getModifiedString() {
		return this.modifiedString;
	}
	
	public String toString(){
		
		if(this.originalString == null){
			ITextFileBuffer fileBuffer = ITextFileBufferManager.DEFAULT.getTextFileBuffer(fixSite.getFile().getFullPath(), LocationKind.IFILE);
			if(fileBuffer == null){
				System.out.println("FileBuffer Not Found: " + fixSite.getFile().getFullPath());
				try {
					ITextFileBufferManager.DEFAULT.connect(fixSite.getFile().getFullPath(), LocationKind.IFILE, new NullProgressMonitor());
					fileBuffer = ITextFileBufferManager.DEFAULT.getTextFileBuffer(fixSite.getFile().getFullPath(), LocationKind.IFILE);
				} catch (CoreException e) {
					e.printStackTrace();
				}
			}
			IDocument document = fileBuffer.getDocument();
			try {
				this.originalString = document.get(this.startposition, this.originalLength);
			} catch (BadLocationException e) {
				e.printStackTrace();
			}
		}
		return "Filterable Fix: " + this.originalString + " -> " + this.modifiedString;
		
	}

	

	@Override
	public boolean hasExpectedValue() {
		return hasExpectedValue;
	}

	@Override
	public String getExpectedValue() {
		return expectedValue;
	}

	@Override
	public Expression getTargetExpression() {
		return this.targetExpr;
	}

	@Override
	public String getNewExprString() {
		return this.newExprString;
	}

	@Override
	public String getTargetExprType() {
		return this.targetExprType;
	}
	
	public String getFixType() {
		return this.fixType;
	}
	
	public int getFixLineNum() {
		return this.fixLineNum;
	}

	@Override
	public String getFileName() {
		return this.modifiedFile.getName();
	}

	public AbstractFixSite getfixSite() {
		return fixSite;
	}

}
