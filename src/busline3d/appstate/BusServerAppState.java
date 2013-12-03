package busline3d.appstate;

import busline3d.command.AddStationCommand;
import busline3d.command.SetNameCommand;
import busline3d.control.PassengerControl;
import busline3d.message.NewPassengerMessage;
import busline3d.message.RadiusMessage;
import busline3d.message.SetNameMessage;
import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.network.Filters;
import com.jme3.network.HostedConnection;
import com.jme3.network.Message;
import com.jme3.network.MessageListener;
import com.jme3.network.Server;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Dome;
import java.util.ArrayList;
import java.util.Map.Entry;
import util.NetworkAssetLocator;
import util.appstate.ServerAppState;
import util.appstate.ServerAppState.ConnectionListenerBehaviour;

/**
 *
 * @author Volker Schuller
 */
public class BusServerAppState extends AbstractAppState implements ConnectionListenerBehaviour, MessageListener<HostedConnection> {

    private final static float CIRCUMFERENCEINCREMENT = 600;
    private SimpleApplication app;
    private ServerAppState serverAppState;
    private WorldAppState worldAppState;
    private float radius = CIRCUMFERENCEINCREMENT / (2 * FastMath.PI);
    private ArrayList<Node>[] randomObjects = new ArrayList[6];
    private ArrayList<Float>[] distances = new ArrayList[6];
    private Spatial floor;
    private Geometry floorDome;
    private boolean firstStation = true;
    private boolean singleplayer;

    public BusServerAppState(boolean singleplayer) {
        this.singleplayer = singleplayer;
        for (int i = 0; i < 6; i++) {
            this.randomObjects[i] = new ArrayList<Node>();
            this.distances[i] = new ArrayList<Float>();
        }
    }

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        this.app = (SimpleApplication) app;
        worldAppState = stateManager.getState(WorldAppState.class);
        worldAppState.setRadius(radius);
        if (singleplayer) {
            addAddStationCommand(null, null, 0);
            for (int i = 0; i < 4; i++) {
                float oldradius = radius;
                radius += CIRCUMFERENCEINCREMENT / (FastMath.PI * 2);
                addAddStationCommand(null, null, oldradius);
            }
        } else {
            app.getAssetManager().registerLocator("Textures/passengers/", NetworkAssetLocator.class);
            serverAppState = stateManager.getState(ServerAppState.class);
            serverAppState.setConnectionListenerBehaviour(this);
            serverAppState.addMessageListener(this);
        }
        addDome();
    }

    private void addAddStationCommand(Server server, HostedConnection conn, float oldradius) {
        worldAppState.addCommand(new AddStationCommand(app.getStateManager(), app.getAssetManager(), server, conn, randomObjects, distances, singleplayer, radius, oldradius));
    }

    public void addDome() {
        BulletAppState bulletAppState = this.app.getStateManager().getState(BulletAppState.class);
        if (floorDome != null) {
            bulletAppState.getPhysicsSpace().remove(floorDome);
        }
        if (floor != null) {
            bulletAppState.getPhysicsSpace().remove(floor);
        }
        RigidBodyControl floorBody = new RigidBodyControl(0);
        floor = app.getRootNode().getChild("floor");
        floor.addControl(floorBody);
        floorBody.setFriction(1);
        bulletAppState.getPhysicsSpace().add(floor);
        Dome dome = new Dome(Vector3f.ZERO, 10, 100, radius + 110, true);
        floorDome = new Geometry("floor dome", dome);
        floorDome.setLocalTranslation(0, -5, 0);
        floorDome.addControl(new RigidBodyControl(0));
        bulletAppState.getPhysicsSpace().add(floorDome);
    }

    public void connectionAdded(Server server, HostedConnection conn) {
        serverAppState.setSuspendMovement(true);
        float oldradius = radius;
        if (firstStation) {
            conn.send(new RadiusMessage(radius).setReliable(true));
        } else {
            radius += CIRCUMFERENCEINCREMENT / (FastMath.PI * 2);
            server.broadcast(new RadiusMessage(radius).setReliable(true));
        }
        for (Entry<String, byte[]> entry : NetworkAssetLocator.getPictures().entrySet()) {
            conn.send(new NewPassengerMessage(entry.getKey(), entry.getValue()).setReliable(true));
        }
        addAddStationCommand(server, conn, firstStation ? 0 : oldradius);
        firstStation = false;
    }

    public void connectionRemoved(Server server, HostedConnection conn) {
        serverAppState.defaultConnectionListener.connectionRemoved(server, conn);
    }

    public void messageReceived(HostedConnection source, Message message) {
        if (message instanceof SetNameMessage) {
            String name = ((SetNameMessage) message).getName();
            source.setAttribute("stationname", name);
            Node station = source.getAttribute("station");
            if (station != null) {
                worldAppState.addCommand(new SetNameCommand(worldAppState, station, name));
            }
        } else if (message instanceof NewPassengerMessage) {
            NewPassengerMessage npm = (NewPassengerMessage) message;
            serverAppState.getServer().broadcast(Filters.notEqualTo(source), message);
            NetworkAssetLocator.addPicture(npm.getName(), npm.getData());
            Node station = source.getAttribute("station");
            if (station != null) {
                String[] passengers = station.getControl(PassengerControl.class).getPassengers();
                for (int i = 0; i < passengers.length; i++) {
                    if (passengers[i] == null) {
                        passengers[i] = npm.getName();
                        break;
                    }
                }
            }
        }
    }
}
