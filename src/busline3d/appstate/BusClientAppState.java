package busline3d.appstate;

import busline3d.command.SetNameCommand;
import busline3d.command.SetRadiusCommand;
import busline3d.command.WatchThisCommand;
import busline3d.message.NewPassengerMessage;
import busline3d.message.RadiusMessage;
import busline3d.message.SetNameMessage;
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
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.screen.ScreenController;
import util.NetworkAssetLocator;
import util.appstate.ClientAppState;

/**
 *
 * @author Volker Schuller
 */
public class BusClientAppState extends AbstractAppState implements ClientAppState.MessageHandler, ScreenController {

    private SimpleApplication app;
    private WorldAppState worldAppState;
    private Node busNode;
    private CameraNode camNode;
    private String stationname;
    private Nifty nifty;
    private Screen screen;

    BusClientAppState(String stationname) {
        this.stationname = stationname;
    }

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);
        this.app = (SimpleApplication) app;
        this.app.getFlyByCamera().setEnabled(false);
        app.getAssetManager().registerLocator("Textures/passengers/", NetworkAssetLocator.class);
        camNode = new CameraNode("Camera Node", this.app.getCamera());
        camNode.setControlDir(ControlDirection.SpatialToCamera);
        camNode.setLocalTranslation(new Vector3f(0, 5, 30));
        busNode = (Node) this.app.getRootNode().getChild("Bus");
        worldAppState = stateManager.getState(WorldAppState.class);
        ClientAppState clientAppState = stateManager.getState(ClientAppState.class);
        clientAppState.observeSpatial("Bus", busNode);
        clientAppState.setMessageHandler(this);
        clientAppState.sendMessage(new SetNameMessage(stationname));
        NetworkAssetLocator.setClientAppState(clientAppState);
    }

    @Override
    public void update(float tpf) {
        camNode.lookAt(busNode.getLocalTranslation(), Vector3f.UNIT_Y);
    }

    public boolean messageReceived(Client source, Message message) {
        if (message instanceof RadiusMessage) {
            float radius = ((RadiusMessage) message).getRadius();
            worldAppState.addCommand(new SetRadiusCommand(worldAppState, radius));
            return true;
        } else if (message instanceof WatchThisMessage) {
            String name = ((WatchThisMessage) message).getName();
            worldAppState.addCommand(new WatchThisCommand(app.getRootNode(), camNode, name));
            worldAppState.addCommand(new SetNameCommand(app.getRootNode(), worldAppState, name, stationname));
            return true;
        } else if (message instanceof NewPassengerMessage) {
            NewPassengerMessage npm = (NewPassengerMessage) message;
            NetworkAssetLocator.addPicture(npm.getName(), npm.getData());
            return true;
        }
        return false;
    }

    public void bind(Nifty nifty, Screen screen) {
        this.nifty = nifty;
        this.screen = screen;
    }

    public void onStartScreen() {
    }

    public void onEndScreen() {
    }

    public void newPassenger() {
        NetworkAssetLocator.openDialog();
    }
}
