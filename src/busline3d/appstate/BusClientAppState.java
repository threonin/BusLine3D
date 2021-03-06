package busline3d.appstate;

import busline3d.command.AnimationCommand;
import busline3d.command.PassengerStationCommand;
import busline3d.command.SetNameCommand;
import busline3d.command.SetRadiusCommand;
import busline3d.command.WatchThisCommand;
import busline3d.control.PassengerControl;
import busline3d.message.AnimationMessage;
import busline3d.message.NewPassengerMessage;
import busline3d.message.PassengerBusMessage;
import busline3d.message.PassengerStationMessage;
import busline3d.message.RadiusMessage;
import busline3d.message.SetNameMessage;
import busline3d.message.WatchThisMessage;
import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
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
import util.NetworkAssetLocator.NameCallback;
import util.appstate.ClientAppState;

/**
 *
 * @author Volker Schuller
 */
public class BusClientAppState extends AbstractAppState implements ActionListener, ClientAppState.MessageHandler, ScreenController {

    private SimpleApplication app;
    private WorldAppState worldAppState;
    private boolean showInfo;
    private Node busNode;
    private CameraNode camNode;
    private String stationname;
    private PassengerControl passengerControl;
    private Nifty nifty;
    private Screen screen;

    BusClientAppState(String stationname) {
        this.stationname = stationname;
    }

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);
        this.app = (SimpleApplication) app;
        worldAppState = stateManager.getState(WorldAppState.class);
        ClientAppState clientAppState = stateManager.getState(ClientAppState.class);
        StartMenuAppState startMenuAppState = stateManager.getState(StartMenuAppState.class);
        if (clientAppState.getError() != null) {
            startMenuAppState.setClientData(clientAppState.getHost(), clientAppState.getPort(), stationname, clientAppState.getError());
            nifty.removeScreen("station_hud");
            stateManager.detach(this);
            stateManager.detach(clientAppState);
            stateManager.detach(worldAppState);
            nifty.fromXml("Interface/screen.xml", "client", startMenuAppState);
            return;
        } else {
            if (startMenuAppState != null) {
                stateManager.detach(startMenuAppState);
            }
        }
        this.app.getFlyByCamera().setEnabled(false);
        this.app.getCamera().setFrustumFar(10000);
        app.getAssetManager().registerLocator("Textures/passengers/", NetworkAssetLocator.class);
        camNode = new CameraNode("Camera Node", this.app.getCamera());
        camNode.setControlDir(ControlDirection.SpatialToCamera);
        camNode.setLocalTranslation(new Vector3f(0, 5, 30));
        busNode = (Node) this.app.getRootNode().getChild("Bus");
        clientAppState.observeSpatial("Bus", busNode);

        clientAppState.setMessageHandler(this);
        clientAppState.sendMessage(new SetNameMessage(stationname));
        NetworkAssetLocator.setClientAppState(clientAppState);
        setupKeys(this.app.getInputManager());
    }

    private void setupKeys(InputManager inputManager) {
        inputManager.addMapping("ToggleInfo", new KeyTrigger(KeyInput.KEY_I));
        inputManager.addListener(this, "ToggleInfo");
    }

    public void onAction(String binding, boolean value, float tpf) {
        if (binding.equals("ToggleInfo")) {
            if (value) {
                showInfo = !showInfo;
                this.app.setDisplayFps(showInfo);
                this.app.setDisplayStatView(showInfo);
            }
        }
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
            worldAppState.addCommand(new SetNameCommand(app.getRootNode(), worldAppState, this, name, stationname));
            return true;
        } else if (message instanceof NewPassengerMessage) {
            NewPassengerMessage npm = (NewPassengerMessage) message;
            NetworkAssetLocator.addPicture(npm.getName(), npm.getData());
            return true;
        } else if (message instanceof PassengerBusMessage) {
            PassengerBusMessage pbm = (PassengerBusMessage) message;
            if (pbm.getName() == null) {
                worldAppState.getPassengerControl().removePassenger(pbm.getIndex());
            } else {
                worldAppState.getPassengerControl().addPassenger(pbm.getIndex(), pbm.getName());
            }
        } else if (message instanceof PassengerStationMessage) {
            PassengerStationMessage psm = (PassengerStationMessage) message;
            worldAppState.addCommand(new PassengerStationCommand(this, psm));
        } else if (message instanceof AnimationMessage) {
            AnimationMessage anm = (AnimationMessage) message;
            worldAppState.addCommand(new AnimationCommand(app.getRootNode(), anm.getObject(), anm.getAnimation(), anm.getSpeed()));
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
        NetworkAssetLocator.openDialog(new NameCallback() {
            public void returnName(String name) {
                passengerControl.addPassenger(name);
            }
        });
    }

    public void setPassengerControl(PassengerControl passengerControl) {
        this.passengerControl = passengerControl;
    }

    public PassengerControl getPassengerControl() {
        return this.passengerControl;
    }
}
