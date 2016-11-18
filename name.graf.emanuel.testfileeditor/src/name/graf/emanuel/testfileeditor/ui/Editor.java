package name.graf.emanuel.testfileeditor.ui;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.Vector;

import org.eclipse.core.internal.resources.Marker;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.projection.ProjectionAnnotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;
import org.eclipse.jface.text.source.projection.ProjectionSupport;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.AnnotationPreference;
import org.eclipse.ui.texteditor.MarkerAnnotation;
import org.eclipse.ui.texteditor.MarkerAnnotationPreferences;
import org.eclipse.ui.texteditor.MarkerUtilities;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

import com.sun.xml.internal.txw2.Document;

import name.graf.emanuel.testfileeditor.model.TestFile;
import name.graf.emanuel.testfileeditor.ui.support.editor.ColorManager;
import name.graf.emanuel.testfileeditor.ui.support.editor.Configuration;
import name.graf.emanuel.testfileeditor.ui.support.editor.DocumentProvider;

public class Editor extends TextEditor implements Observer {
    private ColorManager colorManager;
    private OutlinePage fOutlinePage;
    private ProjectionSupport projectionSupport;
    private ProjectionAnnotationModel projectionAnnotationModel;
    private IAnnotationModel annotationModel;
    private Annotation[] oldAnnotations;
    private TestFile file;

    public Editor() {
        super();
        this.colorManager = new ColorManager();
        this.setSourceViewerConfiguration(
                new Configuration(this.colorManager, this));
        this.setDocumentProvider(new DocumentProvider());
    }

    @Override
    public void dispose() {
        this.colorManager.dispose();
        super.dispose();
    }

    @Override
    public void createPartControl(final Composite parent) {
        super.createPartControl(parent);
        final ProjectionViewer viewer = (ProjectionViewer) this.getSourceViewer();
        this.projectionSupport = new ProjectionSupport(viewer, this.getAnnotationAccess(), this.getSharedColors());
        this.projectionSupport.install();
        viewer.doOperation(ProjectionViewer.TOGGLE);
        this.projectionAnnotationModel = viewer.getProjectionAnnotationModel();
    }

    @Override
    protected ISourceViewer createSourceViewer(final Composite parent, final IVerticalRuler ruler, final int styles) {
        final ISourceViewer viewer = new ProjectionViewer(parent, ruler, this.getOverviewRuler(),
                this.isOverviewRulerVisible(), styles);
        this.getSourceViewerDecorationSupport(viewer);
        IEditorInput input = getEditorInput();
        IDocument document = getDocumentProvider().getDocument(input);
        annotationModel = getDocumentProvider().getAnnotationModel(input);
        file = new TestFile(input.getName());
        file.addObserver(this);
        file.setDocument(document);

        document.addDocumentListener(new IDocumentListener() {

            @Override
            public void documentChanged(DocumentEvent event) {
                file.reparse();
            }

            @Override
            public void documentAboutToBeChanged(DocumentEvent event) {
            }
        });

        return viewer;
    }

    public void updateFoldingStructure(final Vector<Position> positions) {
        final Annotation[] annotations = new Annotation[positions.size()];
        final HashMap<Annotation, Position> newAnnotations = new HashMap<Annotation, Position>();
        for (int i = 0; i < positions.size(); ++i) {
            final ProjectionAnnotation annotation = new ProjectionAnnotation();
            newAnnotations.put(annotation, positions.get(i));
            annotations[i] = annotation;
        }
        this.projectionAnnotationModel.modifyAnnotations(this.oldAnnotations, newAnnotations, (Annotation[]) null);
        this.oldAnnotations = annotations;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getAdapter(final Class<T> adapter) {
        if (IContentOutlinePage.class.equals(adapter)) {
            if (this.fOutlinePage == null) {
                this.fOutlinePage = new OutlinePage(this.getDocumentProvider(), this);
                if (this.getEditorInput() == null) {
                }
                this.fOutlinePage.setInput(this.getEditorInput());
            }
            return (T) this.fOutlinePage;
        } else if (TestFile.class.equals(adapter)) {
            return (T) file;
        }
        return super.getAdapter(adapter);
    }

    @Override
    protected void editorSaved() {
        if (this.fOutlinePage != null) {
            this.fOutlinePage.update();
        }

        if (file != null) {
            file.reparse();
        }

        super.editorSaved();
    }

    public OutlinePage getOutline() {
        return this.fOutlinePage;
    }

    @Override
    protected void handleCursorPositionChanged() {
        super.handleCursorPositionChanged();
    }

    @Override
    public void update(Observable object, Object info) {
        if (object instanceof TestFile) {
            TestFile testFile = (TestFile) object;
            
            if(!testFile.getDuplicatePositions().isEmpty()) {
                Set<Position> duplicates = testFile.getDuplicatePositions();
                FileEditorInput input = (FileEditorInput) getEditorInput();
                IDocument document = getDocumentProvider().getDocument(input);
                IFile file = input.getFile();
                AnnotationPreference preferences = getAnnotationPreferenceLookup().getAnnotationPreference("name.graf.emanuel.testfileeditor.annotations.MarkerAnnotation");
                
                try {
                    IMarker[] findMarkers = file.findMarkers("name.graf.emanuel.testfileeditor.markers.DuplicateTestMarker", false, IFile.DEPTH_INFINITE);

                    for (IMarker marker : findMarkers) {
                        int offset = MarkerUtilities.getCharStart(marker);
                        int length = MarkerUtilities.getCharEnd(marker) - offset;
                        Position position = new Position(offset, length);
                        if(!duplicates.contains(position)) {
                            marker.delete();
                        } else {
                            duplicates.remove(position);
                        }
                    }
                    
                    for (Position position : duplicates) {
                        IMarker marker = file.createMarker("name.graf.emanuel.testfileeditor.markers.DuplicateTestMarker");
                        MarkerUtilities.setCharStart(marker, position.offset);
                        MarkerUtilities.setCharEnd(marker, position.offset + position.length);
                        MarkerUtilities.setLineNumber(marker, document.getLineOfOffset(position.offset));
                        marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
                        marker.setAttribute(IMarker.PRIORITY, IMarker.PRIORITY_HIGH);
                    }
                } catch (CoreException | BadLocationException e) {

                }
            }
        }
    }
}
