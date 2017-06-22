package cn.edu.thu.tsmart.tool.da.core.suggestion;

import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.widgets.Display;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.UndoEdit;

import cn.edu.thu.tsmart.tool.da.core.search.strategy.gnr.fs.StatementFixSite;

public class MethodFix extends Fix{
	private StatementFixSite fixSite;
	private IFile modifiedFile;
	private int startposition;
	private int originalLength;
	
	private String originalString;
	private String modifiedString;
	
	private String fixType;
	private int fixLineNum;
	
	private UndoEdit undoEdit;
	private TextEdit doEdit;
	
	/*ASTNode pre;
	ASTNode post;*/	
	
	
	public MethodFix(StatementFixSite fixSite, int startposition, int originalLength, String modifiedString, String fixType, int fixLineNum){
		this.fixSite = fixSite;
		this.modifiedFile = fixSite.getFile();
		this.startposition = startposition;
		this.originalLength = originalLength;		
		this.modifiedString = modifiedString;
		
		this.fixType = fixType;
		this.fixLineNum = fixLineNum;
	}
	
	@Override
	public String toString(){
		if(this.doEdit != null)
			return this.doEdit.toString();
		return "?? -> " + modifiedString;
	}


	@Override
	public void doFix() {
		ReplaceEdit replaceEdit = new ReplaceEdit(startposition, originalLength, modifiedString);
		doEdit = replaceEdit;
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
			Display.getDefault().syncExec(new Runnable(){

				@Override
				public void run() {
					try {
						undoEdit = replaceEdit.apply(document);
					} catch (MalformedTreeException e) {
						System.out.println("malformed tree: "+ replaceEdit.toString());
						e.printStackTrace();
					} catch (BadLocationException e) {
						System.out.println("malformed tree: "+ replaceEdit.toString());
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

	@Override
	public String getFixType() {
		return this.fixType;
	}

	@Override
	public int getFixLineNum() {
		return this.fixLineNum;
	}
	
	@Override
	public String getFileName(){
		return this.modifiedFile.getName();
	}
}
