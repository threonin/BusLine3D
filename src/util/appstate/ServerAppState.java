package util.appstate;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.PhysicsCollisionEvent;
import com.jme3.bullet.collision.PhysicsCollisionListener;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.network.ConnectionListener;
import com.jme3.network.HostedConnection;
import com.jme3.network.Network;
import com.jme3.network.Server;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import util.message.DefinitionMessage;
import util.message.MovementMessage;
import util.message.ObjectData;
import util.message.ObjectDefinition;
import util.ObservedNode;
import util.message.ResetTimerMessage;

/**
 *
 * @author Volker Schuller
 */
public class ServerAppState extends AbstractAppState implements PhysicsCollisionListener, ConnectionListener {

    private SimpleApplication app;
    private BulletAppState bulletAppState;
    private ConcurrentHashMap<String, Spatial> changedSpatials = new ConcurrentHashMap<String, Spatial>();
    private Node rootNode;
    private ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
    private Server server;
    private int port;
    private int obnum;

    public ServerAppState(int port) {
        this.port = port;
    }

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        this.app = (SimpleApplication) app;
        this.rootNode = this.app.getRootNode();
        this.bulletAppState = stateManager.getState(BulletAppState.class);

        bulletAppState.getPhysicsSpace().addCollisionListener(this);

        if (port > 0) {
            try {
                server = Network.createServer(port);
                server.start();
                server.addConnectionListener(this);
                exec.scheduleAtFixedRate(new NetworkSynchronizer(), 0, 75, TimeUnit.MILLISECONDS);
                Logger.getLogger(ServerAppState.class.getName()).log(Level.INFO, "Server started at port {0,number,#}", port);
            } catch (IOException ex) {
                Logger.getLogger(ServerAppState.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        //bulletAppState.setDebugEnabled(true);
    }

    public RigidBodyControl addRigidObservedSpatial(Spatial spatial, Vector3f location, Quaternion rotation, String type, float weight) {
        ObservedNode observedObject = addObservedSpatial(spatial, location, rotation, "Object" + obnum++, type);
        RigidBodyControl control = new RigidBodyControl(weight);
        control.setEnabled(false);
        observedObject.addControl(control);
        bulletAppState.getPhysicsSpace().add(observedObject);
        return control;
    }

    public ObservedNode addObservedSpatial(Spatial spatial, Vector3f location, Quaternion rotation, String name, String type) {
        ObservedNode observedObject = new ObservedNode(name, changedSpatials, type);
        observedObject.attachChild(spatial);
        observedObject.setLocalRotation(rotation);
        observedObject.setLocalTranslation(location);
        rootNode.attachChild(observedObject);
        return observedObject;
    }

    public void collision(PhysicsCollisionEvent event) {
        if (event.getNodeB().getName().startsWith("Object")) {
            if (!event.getNodeA().getName().startsWith("floor")) {
                event.getNodeB().getControl(RigidBodyControl.class).setEnabled(true);
                if (event.getNodeA().getName().startsWith("Object")) {
                    event.getNodeA().getControl(RigidBodyControl.class).setEnabled(true);
                }
            }
        } else if (event.getNodeA().getName().startsWith("Object")) {
            if (!event.getNodeB().getName().startsWith("floor")) {
                event.getNodeA().getControl(RigidBodyControl.class).setEnabled(true);
                if (event.getNodeB().getName().startsWith("Object")) {
                    event.getNodeB().getControl(RigidBodyControl.class).setEnabled(true);
                }
            }
        }
    }

    @Override
    public void cleanup() {
        if (port > 0) {
            try {
                exec.shutdownNow();
            } catch (Exception ex) {
            }
            try {
                server.close();
            } catch (Exception ex) {
            }
        }
        super.cleanup();
    }

    public void connectionAdded(Server server, HostedConnection conn) {
        Logger.getLogger(ServerAppState.class.getName()).log(Level.INFO, "new connection");
        this.app.getTimer().reset();
        ArrayList<ObjectDefinition> objects = new ArrayList<ObjectDefinition>();
        for (Spatial spatial : this.rootNode.getChildren()) {
            if (spatial.getName().startsWith("Object")) {
                objects.add(new ObjectDefinition(spatial.getName(), ((ObservedNode) spatial).getType(), spatial.getLocalTranslation(), spatial.getLocalRotation()));
            }
        }
        conn.send(new DefinitionMessage(objects.toArray(new ObjectDefinition[objects.size()])).setReliable(true));
        server.broadcast(new ResetTimerMessage().setReliable(true));
    }

    public void connectionRemoved(Server server, HostedConnection conn) {
        Logger.getLogger(ServerAppState.class.getName()).log(Level.INFO, "connection lost");
    }

    private final class NetworkSynchronizer implements Runnable {

        @Override
        public void run() {
            try {
                Iterator<Map.Entry<String, Spatial>> iterator = changedSpatials.entrySet().iterator();
                float time = ServerAppState.this.app.getTimer().getTimeInSeconds();
                ObjectData[] data = new ObjectData[changedSpatials.size()];
                for (int i = 0; i < data.length; i++) {
                    Map.Entry<String, Spatial> entry = iterator.next();
                    data[i] = new ObjectData(entry.getKey(), entry.getValue().getLocalTranslation(), entry.getValue().getLocalRotation());
                    changedSpatials.remove(entry.getKey());
                }
                server.broadcast(new MovementMessage(time, data));
            } catch (Exception ex) {
                Logger.getLogger(ServerAppState.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
