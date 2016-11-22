package name.graf.emanuel.testfileeditor.ui;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMemberValuePair;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.SourceType;
import org.eclipse.jdt.internal.junit.model.TestCaseElement;
import org.eclipse.jdt.internal.junit.model.TestElement;
import org.eclipse.jdt.internal.junit.model.TestSuiteElement;
import org.eclipse.jdt.internal.junit.ui.TestRunnerViewPart;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.TextFileDocumentProvider;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;

import name.graf.emanuel.testfileeditor.model.TestFile;
import name.graf.emanuel.testfileeditor.model.node.Test;

@SuppressWarnings("restriction")
public class JumpToRTSHandler extends AbstractHandler {

    private static final Pattern TEST_NAME_PATTERN = Pattern.compile(".*\\[(.*)\\].*");

    private String className;
    private IJavaProject project;

    private Shell shell;

    private String testName;

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        IStructuredSelection selection = HandlerUtil.getCurrentStructuredSelection(event);

        if (selection instanceof TreeSelection) {
            shell = HandlerUtil.getActiveShell(event);
            getProject(event);
            getTestInfo((TestElement) selection.getFirstElement());
            jump();
        }

        return null;
    }

    private String getPathViaAnnotation(IType classType) throws JavaModelException {
        IAnnotation runFor = classType.getAnnotation("RunFor");
        if (runFor.exists()) {
            IMemberValuePair[] values = runFor.getMemberValuePairs();
            if (values.length == 1) {
                return (String) values[0].getValue();
            }
        }

        return null;
    }

    private IFile findFileInContainer(IContainer container, String filename) {

        try {
            for (IResource resource : container.members()) {
                if (resource instanceof IContainer) {
                    IFile found = findFileInContainer((IContainer) resource, filename);
                    if (found != null) {
                        return found;
                    }
                } else if (resource instanceof IFile) {
                    if (((IFile) resource).getName().equals(filename)) {
                        return (IFile) resource;
                    }
                }
            }
        } catch (CoreException e) {
            e.printStackTrace();
        }

        return null;
    }

    private String guessPath() throws CoreException {
        int lastDot = className.lastIndexOf('.');
        String testFileName = className.substring(lastDot + 1) + ".rts";
        IResource file = findFileInContainer(project.getProject(), testFileName);
        if (file != null) {
            return file.getProjectRelativePath().toString();
        }

        return null;
    }

    private void getProject(ExecutionEvent event) {
        TestRunnerViewPart view = (TestRunnerViewPart) HandlerUtil.getActivePart(event);
        project = view.getLaunchedProject();
    }

    private String getTestFileName(IType classType) throws CoreException {
        if (!(classType instanceof SourceType)) {
            return null;
        }

        String filePath = getPathViaAnnotation(classType);
        return filePath != null ? filePath : guessPath();
    }

    private void getTestInfo(TestElement entry) {
        if (entry instanceof TestCaseElement) {
            className = entry.getClassName();
            TestCaseElement testCase = (TestCaseElement) entry;
            Matcher matcher = TEST_NAME_PATTERN.matcher(testCase.getTestMethodName());
            if (matcher.matches()) {
                testName = matcher.group(1);
            }
        } else if (entry instanceof TestSuiteElement) {
            TestSuiteElement testSuite = (TestSuiteElement) entry;
            if (!testSuite.getClassName().contains(".")) {
                className = testSuite.getParent().getClassName();
            } else {
                className = testSuite.getTestName();
            }
            testName = testSuite.getTestName();
        }
    }

    private void jump() {
        if (className != null) {
            IType cls = null;
            try {
                cls = project.findType(className);
            } catch (JavaModelException e) {
                return;
            }

            try {
                IFile file = project.getProject().getFile(getTestFileName(cls));
                IWorkbench workbench = PlatformUI.getWorkbench();

                IWorkbenchPage page = workbench.getActiveWorkbenchWindow().getActivePage();
                FileEditorInput input = new FileEditorInput(file);
                IDocumentProvider provider = new TextFileDocumentProvider();
                provider.connect(input);
                IDocument document = provider.getDocument(input);

                TestFile testFile = new TestFile(input, provider);
                testFile.parse();
                IEditorDescriptor defaultEditor = workbench.getEditorRegistry().getDefaultEditor(file.getName());
                String editorId = defaultEditor.getId();

                if (!className.equals(testName)) {
                    for (Test test : testFile.getTests()) {
                        if (test.toString().equals(testName)) {
                            int line = document.getLineOfOffset(test.getPosition().getOffset());
                            IMarker lineMarker = file.createMarker(IMarker.TEXT);
                            lineMarker.setAttribute(IMarker.LINE_NUMBER, line + 1);
                            lineMarker.setAttribute(IDE.EDITOR_ID_ATTR, editorId);
                            IDE.openEditor(page, lineMarker);
                            lineMarker.delete();
                            return;
                        }
                    }
                } else {
                    IDE.openEditor(page, file);
                }
            } catch (CoreException | BadLocationException | NullPointerException e) {
                e.printStackTrace();
                MessageDialog.openError(shell, "Jump to RTS", "Failed to find associated RTS file.");
            }
        }
    }

}
