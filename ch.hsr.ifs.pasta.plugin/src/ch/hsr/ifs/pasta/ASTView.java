package ch.hsr.ifs.pasta;

import java.net.URL;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTNodeSelector;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.cdt.core.model.IWorkingCopy;
import org.eclipse.cdt.ui.CUIPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.part.ViewPart;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

import ch.hsr.ifs.pasta.events.PastaEventConstants;

public class ASTView extends ViewPart {

	@Override
	public void createPartControl(final Composite parent) {

		final ASTWidget treeView = new ASTWidget(parent);

		getViewSite().getActionBars().getToolBarManager().add(new Action() {

			@Override
			public void run() {
				treeView.drawAST(getAST());
			}

			@Override
			public ImageDescriptor getImageDescriptor() {
				final Bundle bundle = FrameworkUtil.getBundle(this.getClass());
				final URL url = FileLocator.find(bundle, new Path("icons/refresh.gif"), null);
				return ImageDescriptor.createFromURL(url);
			}

			@Override
			public String getText() {
				return "refresh";
			}

		});

		treeView.drawAST(getAST());
		treeView.setListener(new NodeSelectionListener() {

			@Override
			public void nodeSelected(final IASTNode node) {
				final Map<String, Object> map = new HashMap<>();
				map.put(PastaEventConstants.ASTNODE, node);
				doPostEvent(PastaEventConstants.ASTNODE, map);
			}

		});

		registerEventHandler(PastaEventConstants.SHOW_SELECTION, new EventHandler() {

			@Override
			public void handleEvent(final Event event) {

				final ISelection selection = (ISelection) event.getProperty(PastaEventConstants.SELECTION);
				if (selection instanceof ITextSelection) {
					treeView.drawAST(getAST());
					final IASTNodeSelector selector = getAST().getNodeSelector(getAST().getFilePath());
					treeView.showSelectedNode(selector.findEnclosingNode(((ITextSelection) selection).getOffset(),
							((ITextSelection) selection).getLength()));
				}
			}
		});
	}

	private void registerEventHandler(final String topic, final EventHandler handler) {
		final BundleContext ctx = FrameworkUtil.getBundle(ASTView.class).getBundleContext();
		final Dictionary<String, String> props = new Hashtable<>();
		props.put(EventConstants.EVENT_TOPIC, topic);
		ctx.registerService(EventHandler.class.getName(), handler, props);
	}

	private IASTTranslationUnit getAST() {
		final IEditorInput editorInput = CUIPlugin.getActivePage().getActiveEditor().getEditorInput();
		final IWorkingCopy workingCopy = CUIPlugin.getDefault().getWorkingCopyManager().getWorkingCopy(editorInput);
		IIndex index;
		try {
			index = CCorePlugin.getIndexManager().getIndex(workingCopy.getCProject());
		} catch (final CoreException e) {
			throw new RuntimeException(e);
		}
		try {
			index.acquireReadLock();
			return workingCopy.getAST(index, ITranslationUnit.AST_SKIP_ALL_HEADERS)
					.copy(IASTNode.CopyStyle.withoutLocations);
		} catch (final CoreException | InterruptedException e) {
			throw new RuntimeException(e);
		} finally {
			index.releaseReadLock();
		}
	}

	private void doPostEvent(final String topic, final Map<String, Object> map) {
		final Event event = new Event(topic, map);
		final BundleContext ctx = FrameworkUtil.getBundle(ASTView.class).getBundleContext();
		final ServiceReference<?> ref = ctx.getServiceReference(EventAdmin.class.getName());
		if (ref != null) {
			final EventAdmin admin = (EventAdmin) ctx.getService(ref);
			admin.sendEvent(event);
			ctx.ungetService(ref);
		}
	}

	@Override
	public void setFocus() {
	}

}
