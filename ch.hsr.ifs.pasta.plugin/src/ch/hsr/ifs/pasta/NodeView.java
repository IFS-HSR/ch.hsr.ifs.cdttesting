package ch.hsr.ifs.pasta;

import java.util.Dictionary;
import java.util.Hashtable;

import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

public class NodeView extends ViewPart {

    @Override
    public void createPartControl(Composite parent) {
        final NodeWidget nodeWidget = new NodeWidget(parent);
        registerEventHandler("ASTNODE", new EventHandler() {

            @Override
            public void handleEvent(Event event) {
                IASTNode astNode = (IASTNode) event.getProperty("ASTNODE");
                if (astNode != null) {
                    nodeWidget.displayNode(astNode);
                }
            }
        });
    }

    @Override
    public void setFocus() {

    }

    public void registerEventHandler(String topic, EventHandler handler) {
        BundleContext ctx = FrameworkUtil.getBundle(ASTView.class).getBundleContext();
        Dictionary<String, String> props = new Hashtable<String, String>();
        props.put(EventConstants.EVENT_TOPIC, topic);
        ServiceRegistration reg = ctx.registerService(EventHandler.class.getName(), handler, props);
    }
}
