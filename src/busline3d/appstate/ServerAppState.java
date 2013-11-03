package busline3d.appstate;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.PhysicsCollisionEvent;
import com.jme3.bullet.collision.PhysicsCollisionListener;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.network.ConnectionListener;
import com.jme3.network.HostedConnection;
import com.jme3.network.Network;
import com.jme3.network.Server;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Dome;
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

    public ServerAppState(int port) {
        this.port = port;
    }

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        this.app = (SimpleApplication) app;
        this.rootNode = this.app.getRootNode();
        bulletAppState = new BulletAppState();
        bulletAppState.setThreadingType(BulletAppState.ThreadingType.PARALLEL);
        stateManager.attach(bulletAppState);

        RigidBodyControl floorBody = new RigidBodyControl(0);
        Spatial floor = rootNode.getChild("floor");
        floor.addControl(floorBody);
        floorBody.setFriction(1);
        bulletAppState.getPhysicsSpace().add(floor);

        Dome dome = new Dome(Vector3f.ZERO, 10, 100, 400, true);
        Geometry floorDome = new Geometry("floor dome", dome);
        floorDome.setLocalTranslation(0, -5, 0);
        floorDome.addControl(new RigidBodyControl(0));
        bulletAppState.getPhysicsSpace().add(floorDome);

        bulletAppState.getPhysicsSpace().addCollisionListener(this);
        addRandomObjects();
        stateManager.attach(new DriveBusAppState(changedSpatials));

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

    private void addRandomObjects() {
        int treenum = 0;
        for (float alpha = 0; alpha < FastMath.PI * 2; alpha += 0.1) {
            for (int i = 230; i <= 290; i += 30) {
                generateRandomObject(alpha, i, true, treenum++);
            }
            for (int i = 320; i <= 380; i += 30) {
                generateRandomObject(alpha, i, false, treenum++);
            }
        }
    }

    private void generateRandomObject(float alpha, int i, boolean direction, int obnum) {
        String type = FastMath.rand.nextBoolean() ? "Models/tree1/tree1.j3o" : (FastMath.rand.nextBoolean() ? "Models/tree2/tree2.j3o" : "Models/busstop/busstop.j3o");
        Node object = (Node) app.getAssetManager().loadModel(type);
        object.scale(5);
        ObservedNode observedObject = new ObservedNode("Object" + obnum, changedSpatials, type);
        observedObject.rotate(0, -alpha + (direction ? 1 : -1) * FastMath.PI / 2, 0);
        observedObject.setLocalTranslation(FastMath.cos(alpha) * i, -5f, FastMath.sin(alpha) * i);
        observedObject.attachChild(object);
        RigidBodyControl control = new RigidBodyControl(5000f);
        control.setEnabled(false);
        observedObject.addControl(control);
        control.setSleepingThresholds(2, 2);
        control.setFriction(1);
        bulletAppState.getPhysicsSpace().add(observedObject);
        rootNode.attachChild(observedObject);
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
            exec.shutdownNow();
            server.close();
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
