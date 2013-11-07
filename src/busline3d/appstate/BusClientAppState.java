package busline3d.appstate;

import busline3d.message.RadiusMessage;
import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.math.Vector3f;
import com.jme3.network.Client;
import com.jme3.network.Message;
import com.jme3.scene.Node;
import util.appstate.ClientAppState;

/**
 *
 * @author Volker Schuller
 */
public class BusClientAppState extends AbstractAppState implements ClientAppState.MessageHandler {

    private SimpleApplication app;
    private Node busNode;
    private float radius;

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);
        this.app = (SimpleApplication) app;

        this.app.getFlyByCamera().setEnabled(true);
        this.app.getFlyByCamera().setMoveSpeed(100);

        busNode = (Node) this.app.getRootNode().getChild("Bus");
        ClientAppState clientAppState = stateManager.getState(ClientAppState.class);
        clientAppState.observeSpatial("Bus", busNode);
        clientAppState.setMessageHandler(this);
    }

    @Override
    public void update(float tpf) {
        this.app.getCamera().lookAt(busNode.getLocalTranslation(), Vector3f.UNIT_Y);
        if (radius > 0) {
            this.app.getStateManager().getState(WorldAppState.class).setRadius(radius);
            radius = 0;
        }
    }

    public boolean messageReceived(Client source, Message message) {
        if (message instanceof RadiusMessage) {
            this.radius = ((RadiusMessage) message).getRadius();
            return true;
        }
        return false;
    }
}
