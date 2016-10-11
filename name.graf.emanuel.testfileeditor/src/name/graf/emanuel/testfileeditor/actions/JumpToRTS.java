package name.graf.emanuel.testfileeditor.actions;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMemberValuePair;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.SourceType;
import org.eclipse.jdt.internal.junit.model.TestCaseElement;
import org.eclipse.jdt.internal.junit.model.TestElement;
import org.eclipse.jdt.internal.junit.model.TestRoot;
import org.eclipse.jdt.internal.junit.model.TestSuiteElement;
import org.eclipse.jdt.internal.junit.ui.TestRunnerViewPart;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.TextFileDocumentProvider;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.texteditor.IDocumentProvider;

import ch.hsr.ifs.cdttesting.TestingPlugin;
import name.graf.emanuel.testfileeditor.ui.Test;
import name.graf.emanuel.testfileeditor.ui.TestFile;

@SuppressWarnings("restriction")
public class JumpToRTS implements IViewActionDelegate {

    private static final Pattern TEST_NAME_PATTERN = Pattern.compile(".*\\[(.*)\\].*");
    private Shell shell;
    private String className;
    private IJavaProject project;
    private String testName;

    public JumpToRTS() {
        super();
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

    private String getPathViaExtension(IType classType) throws CoreException {
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IConfigurationElement[] elements = registry.getConfigurationElementsFor(TestingPlugin.XML_EXTENSION_POINT_ID);
        String className = classType.getFullyQualifiedName();

        for (IConfigurationElement element : elements) {
            AbstractUIPlugin extension = (AbstractUIPlugin) element.createExecutableExtension("activatorClass");
            String sourceLocation = element.getAttribute("sourceLocation");
            String packageName = extension.getClass().getPackage().getName();
            String pathSuffix = className.substring(packageName.length()).replace('.', '/') + ".rts";
            String suspectPath = sourceLocation + pathSuffix;
            if (extension.getClass().getResourceAsStream(suspectPath) != null) {
                return suspectPath;
            }
        }

        return null;
    }

    private String getTestFileName(IType classType) throws CoreException {
        if (!(classType instanceof SourceType)) {
            return null;
        }

        String filePath = getPathViaAnnotation(classType);
        return filePath != null ? filePath : getPathViaExtension(classType);
    }

    @Override
    public void init(IViewPart view) {
        TestRunnerViewPart testRunnerView = (TestRunnerViewPart) view;
        project = testRunnerView.getLaunchedProject();
    }

    @Override
    public void run(IAction action) {
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
                IDocumentProvider provider = new TextFileDocumentProvider();
                provider.connect(file);
                IDocument document = provider.getDocument(file);

                TestFile testFile = new TestFile(file.getName());
                testFile.setDocument(document);
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
                MessageDialog.openError(shell, "Jump to RTS", "Failed to find associated RTS file.");
            }
        }
    }

    @Override
    public void selectionChanged(IAction action, ISelection selection) {
        if (selection instanceof TreeSelection) {
            TreeSelection treeSelection = (TreeSelection) selection;
            TestElement treeElement = (TestElement) treeSelection.getFirstElement();

            if (treeElement instanceof TestCaseElement) {
                className = treeElement.getClassName();
                TestCaseElement testCase = (TestCaseElement) treeElement;
                Matcher matcher = TEST_NAME_PATTERN.matcher(testCase.getTestMethodName());
                if (matcher.matches()) {
                    testName = matcher.group(1);
                }
            } else if (treeElement instanceof TestSuiteElement) {
                TestSuiteElement testSuite = (TestSuiteElement) treeElement;
                if (!testSuite.getClassName().contains(".")) {
                    className = testSuite.getParent().getClassName();
                } else {
                    className = testSuite.getTestName();
                }
                testName = testSuite.getTestName();
            }
        }
    }

    public void setActivePart(IAction action, IWorkbenchPart targetPart) {
        shell = targetPart.getSite().getShell();
    }

}
