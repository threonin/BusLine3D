package busline3d.appstate;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import util.appstate.ClientAppState;

/**
 *
 * @author Volker Schuller
 */
public class BusClientAppState extends AbstractAppState {

    private SimpleApplication app;
    private Node busNode;

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);
        this.app = (SimpleApplication) app;

        this.app.getFlyByCamera().setEnabled(true);
        this.app.getFlyByCamera().setMoveSpeed(100);

        busNode = (Node) this.app.getRootNode().getChild("Bus");
        stateManager.getState(ClientAppState.class).observeSpatial("Bus", busNode);
    }

    @Override
    public void update(float tpf) {
        this.app.getCamera().lookAt(busNode.getLocalTranslation(), Vector3f.UNIT_Y);
    }
}
