package util.appstate;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.network.ConnectionListener;
import com.jme3.network.Filters;
import com.jme3.network.HostedConnection;
import com.jme3.network.Message;
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
public class ServerAppState extends AbstractAppState implements ConnectionListener {

    public final ConnectionListenerBehaviour defaultConnectionListener = new DefaultConnectionListenerBehaviour();
    public final static int MAXOBJECTSPERMESSAGE = 300;
    private ConnectionListenerBehaviour listener = defaultConnectionListener;
    private SimpleApplication app;
    private BulletAppState bulletAppState;
    private ConcurrentHashMap<String, Spatial> changedSpatials = new ConcurrentHashMap<String, Spatial>();
    private Node rootNode;
    private ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
    private Server server;
    private int port;
    private int obnum;
    private boolean suspendMovement;

    public ServerAppState(int port) {
        this.port = port;
    }

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        this.app = (SimpleApplication) app;
        this.rootNode = this.app.getRootNode();
        this.bulletAppState = stateManager.getState(BulletAppState.class);

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

    public ObservedNode addRigidObservedSpatial(Spatial spatial, Vector3f location, Quaternion rotation, String type, float weight) {
        ObservedNode observedObject = addObservedSpatial(spatial, location, rotation, "Object" + obnum++, type);
        RigidBodyControl control = new RigidBodyControl(weight);
        observedObject.addControl(control);
        control.setSleepingThresholds(2, 2);
        control.setFriction(1);
        control.setApplyPhysicsLocal(true);
        bulletAppState.getPhysicsSpace().add(observedObject);
        return observedObject;
    }

    public ObservedNode addObservedSpatial(Spatial spatial, Vector3f location, Quaternion rotation, String name, String type) {
        ObservedNode observedObject = new ObservedNode(name, changedSpatials, type);
        observedObject.attachChild(spatial);
        observedObject.setLocalRotation(rotation);
        observedObject.setLocalTranslation(location);
        rootNode.attachChild(observedObject);
        return observedObject;
    }

    public void clearChangedSpatials() {
        changedSpatials.clear();
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

    public void setConnectionListenerBehaviour(ConnectionListenerBehaviour listener) {
        this.listener = listener;
    }

    public void connectionAdded(Server server, HostedConnection conn) {
        listener.connectionAdded(server, conn);
    }

    public void connectionRemoved(Server server, HostedConnection conn) {
        listener.connectionRemoved(server, conn);
    }

    public void addObjects(Iterable<Spatial> objects) {
        for (Message message : getDefinitionMessagesForObjects(objects)) {
            server.broadcast(message);
        }
    }

    public void addObjects(Iterable<Spatial> objects, HostedConnection exclude) {
        for (Message message : getDefinitionMessagesForObjects(objects)) {
            server.broadcast(Filters.notEqualTo(exclude), message);
        }
    }

    public boolean getSuspendMovement() {
        return suspendMovement;
    }

    public void setSuspendMovement(boolean suspendMovement) {
        this.suspendMovement = suspendMovement;
    }

    private Iterable<Message> getDefinitionMessagesForObjects(Iterable<Spatial> objects) {
        ArrayList<Message> messages = new ArrayList<Message>();
        ArrayList<ObjectDefinition> definitions = new ArrayList<ObjectDefinition>();
        int i = 1;
        for (Spatial spatial : objects) {
            if (spatial.getName().startsWith("Object")) {
                definitions.add(new ObjectDefinition(spatial.getName(), ((ObservedNode) spatial).getType(), spatial.getLocalTranslation(), spatial.getLocalRotation()));
                if (i++ >= MAXOBJECTSPERMESSAGE) {
                    messages.add(new DefinitionMessage(definitions.toArray(new ObjectDefinition[definitions.size()])).setReliable(true));
                    definitions = new ArrayList<ObjectDefinition>();
                    i = 1;
                }
            }
        }
        if (i > 1) {
            messages.add(new DefinitionMessage(definitions.toArray(new ObjectDefinition[definitions.size()])).setReliable(true));
        }
        return messages;
    }

    public interface ConnectionListenerBehaviour extends ConnectionListener {
    };

    private final class DefaultConnectionListenerBehaviour implements ConnectionListenerBehaviour {

        public void connectionAdded(Server server, HostedConnection conn) {
            setSuspendMovement(true);
            Logger.getLogger(ServerAppState.class.getName()).log(Level.INFO, "new connection");
            for (Message message : getDefinitionMessagesForObjects(rootNode.getChildren())) {
                server.broadcast(message);
            }
            app.getTimer().reset();
            server.broadcast(new ResetTimerMessage().setReliable(true));
            setSuspendMovement(false);
        }

        public void connectionRemoved(Server server, HostedConnection conn) {
            Logger.getLogger(ServerAppState.class.getName()).log(Level.INFO, "connection lost");
        }
    }

    private final class NetworkSynchronizer implements Runnable {

        @Override
        public void run() {
            try {
                if (suspendMovement || changedSpatials.isEmpty()) {
                    return;
                }
                Iterator<Map.Entry<String, Spatial>> iterator = changedSpatials.entrySet().iterator();
                float time = ServerAppState.this.app.getTimer().getTimeInSeconds();
                int size = changedSpatials.size();
                while (size > 0) {
                    ObjectData[] data = new ObjectData[size > MAXOBJECTSPERMESSAGE ? MAXOBJECTSPERMESSAGE : size];
                    for (int i = 0; i < data.length; i++) {
                        Map.Entry<String, Spatial> entry = iterator.next();
                        if (entry == null) {
                            return;
                        }
                        data[i] = new ObjectData(entry.getKey(), entry.getValue().getLocalTranslation(), entry.getValue().getLocalRotation());
                        changedSpatials.remove(entry.getKey());
                    }
                    server.broadcast(new MovementMessage(time, data));
                    size -= MAXOBJECTSPERMESSAGE;
                }
            } catch (Exception ex) {
                Logger.getLogger(ServerAppState.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
