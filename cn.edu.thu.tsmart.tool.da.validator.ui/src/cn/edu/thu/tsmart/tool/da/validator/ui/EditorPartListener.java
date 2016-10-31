package cn.edu.thu.tsmart.tool.da.validator.ui;

import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.TypedListener;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.texteditor.AbstractTextEditor;

public class EditorPartListener implements IPartListener{

	@Override
	public void partActivated(IWorkbenchPart part) {
		// TODO Auto-generated method stub
		if(part instanceof AbstractTextEditor){
			AbstractTextEditor editor = (AbstractTextEditor)part;
			IVerticalRulerInfo verticalRuler = (IVerticalRulerInfo) editor.getAdapter(IVerticalRulerInfo.class);
			
			Listener listeners[] = verticalRuler.getControl().getListeners(SWT.MouseDown);
			boolean needNewListener = true;
			for(int i = 0; i < listeners.length; i ++){
				if(listeners[i] instanceof TypedListener){
					TypedListener tl = (TypedListener)listeners[i];
					if(tl.getEventListener() instanceof VerticalRulerListener){
						needNewListener = false;
						break;
					}
				}
			}
			if(needNewListener)
				verticalRuler.getControl().addMouseListener(new VerticalRulerListener(verticalRuler, editor));
			
		}
		
	}

	@Override
	public void partBroughtToTop(IWorkbenchPart part) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void partClosed(IWorkbenchPart part) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void partDeactivated(IWorkbenchPart part) {
		if(part instanceof AbstractTextEditor){
			AbstractTextEditor editor = (AbstractTextEditor)part;
			IVerticalRulerInfo verticalRuler = (IVerticalRulerInfo) editor.getAdapter(IVerticalRulerInfo.class);
			Listener listeners[] = verticalRuler.getControl().getListeners(SWT.MouseDown);
			for(int i = 0; i < listeners.length; i ++){
				if(listeners[i] instanceof TypedListener){
					TypedListener tl = (TypedListener)listeners[i];
					if(tl.getEventListener() instanceof VerticalRulerListener){
						verticalRuler.getControl().removeMouseListener((VerticalRulerListener)(tl.getEventListener()));
					}
				}
			}
			
		}
		
	}

	@Override
	public void partOpened(IWorkbenchPart part) {
		// TODO Auto-generated method stub
		
	}

}
