package busline3d.appstate;

import busline3d.message.RadiusMessage;
import busline3d.message.WatchThisMessage;
import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.math.Vector3f;
import com.jme3.network.Client;
import com.jme3.network.Message;
import com.jme3.scene.CameraNode;
import com.jme3.scene.Node;
import com.jme3.scene.control.CameraControl.ControlDirection;
import util.appstate.ClientAppState;

/**
 *
 * @author Volker Schuller
 */
public class BusClientAppState extends AbstractAppState implements ClientAppState.MessageHandler {

    private SimpleApplication app;
    private Node busNode;
    private float radius;
    private CameraNode camNode;
    private String watchThis;
    private Node station;

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);
        this.app = (SimpleApplication) app;
        this.app.getFlyByCamera().setEnabled(false);
        camNode = new CameraNode("Camera Node", this.app.getCamera());
        camNode.setControlDir(ControlDirection.SpatialToCamera);
        camNode.setLocalTranslation(new Vector3f(0, 1, 6));
        busNode = (Node) this.app.getRootNode().getChild("Bus");
        ClientAppState clientAppState = stateManager.getState(ClientAppState.class);
        clientAppState.observeSpatial("Bus", busNode);
        clientAppState.setMessageHandler(this);
    }

    @Override
    public void update(float tpf) {
        camNode.lookAt(busNode.getLocalTranslation(), Vector3f.UNIT_Y);
        if (radius > 0) {
            this.app.getStateManager().getState(WorldAppState.class).setRadius(radius);
            radius = 0;
        }
        if (watchThis != null) {
            station = (Node) app.getRootNode().getChild(watchThis);
            if (station != null) {
                station.attachChild(camNode);
                watchThis = null;
            }
        }
    }

    public boolean messageReceived(Client source, Message message) {
        if (message instanceof RadiusMessage) {
            this.radius = ((RadiusMessage) message).getRadius();
            return true;
        } else if (message instanceof WatchThisMessage) {
            watchThis = ((WatchThisMessage) message).getName();
        }
        return false;
    }
}
