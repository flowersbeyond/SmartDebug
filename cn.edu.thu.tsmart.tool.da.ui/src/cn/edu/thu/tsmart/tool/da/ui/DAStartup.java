package cn.edu.thu.tsmart.tool.da.ui;

import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointListener;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.ui.IStartup;

public class DAStartup implements IStartup {

	@Override
	public void earlyStartup() {
		// TODO Auto-generated method stub
		/*
		System.out.println("DA Started");
		
		DebugPlugin debugplugin = DebugPlugin.getDefault();
		IBreakpointManager bpManager = debugplugin.getBreakpointManager();
		if(bpManager != null){
			System.out.println("bpManager is not null");
			bpManager.addBreakpointListener(new IBreakpointListener(){

				@Override
				public void breakpointAdded(IBreakpoint breakpoint) {
					System.out.println("breakpointAdded");
				}

				@Override
				public void breakpointRemoved(IBreakpoint breakpoint, IMarkerDelta delta) {
					System.out.println("breakpointRemoved");
				}

				@Override
				public void breakpointChanged(IBreakpoint breakpoint, IMarkerDelta delta) {
					System.out.println("breakpointChanged");
				}
				
			});
		}
		*/
	}

}
