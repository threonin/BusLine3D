package busline3d.appstate;

import busline3d.message.RadiusMessage;
import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.network.Filters;
import com.jme3.network.HostedConnection;
import com.jme3.network.Server;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Dome;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import util.appstate.ServerAppState;
import util.appstate.ServerAppState.ConnectionListenerBehaviour;
import static util.appstate.ServerAppState.MAXOBJECTSPERMESSAGE;
import util.message.MovementMessage;
import util.message.ObjectData;

/**
 *
 * @author Volker Schuller
 */
public class BusServerAppState extends AbstractAppState implements ConnectionListenerBehaviour {

    private final static float[] OFFSETS = new float[]{-70, -40, -10, 30, 60, 90};
    private final static String[] RANDOMOBJECTS = new String[]{"Models/tree1/tree1.j3o", "Models/tree2/tree2.j3o", "Models/house1/house1.j3o"};
    private final static float MINDIST = 18;
    private final static float MAXDIST = 75;
    private final static float CIRCUMFERENCEINCREMENT = 600;
    private SimpleApplication app;
    private ServerAppState serverAppState;
    private WorldAppState worldAppState;
    private Node rootNode;
    private float radius = CIRCUMFERENCEINCREMENT / (2 * FastMath.PI);
    private ArrayList<Node>[] randomObjects = new ArrayList[6];
    private ArrayList<Float>[] distances = new ArrayList[6];
    private BulletAppState bulletAppState;
    private Spatial floor;
    private Geometry floorDome;
    private boolean addStation;
    private Server server;
    private HostedConnection conn;
    private boolean firstStation = true;

    public BusServerAppState() {
        for (int i = 0; i < 6; i++) {
            this.randomObjects[i] = new ArrayList<Node>();
            this.distances[i] = new ArrayList<Float>();
        }
    }

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        this.app = (SimpleApplication) app;
        this.rootNode = this.app.getRootNode();
        worldAppState = stateManager.getState(WorldAppState.class);
        worldAppState.setRadius(radius);
        serverAppState = stateManager.getState(ServerAppState.class);
        serverAppState.setConnectionListenerBehaviour(this);
        bulletAppState = stateManager.getState(BulletAppState.class);

        addDome();

        stateManager.attach(new DriveBusAppState());
    }

    private void addDome() {
        if (floorDome != null) {
            bulletAppState.getPhysicsSpace().remove(floorDome);
        }
        if (floor != null) {
            bulletAppState.getPhysicsSpace().remove(floor);
        }
        RigidBodyControl floorBody = new RigidBodyControl(0);
        floor = rootNode.getChild("floor");
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
        this.server = server;
        this.conn = conn;
        if (firstStation) {
            conn.send(new RadiusMessage(radius).setReliable(true));
        } else {
            radius += CIRCUMFERENCEINCREMENT / (FastMath.PI * 2);
            server.broadcast(new RadiusMessage(radius).setReliable(true));
        }
        addStation = true;
    }

    @Override
    public void update(float tpf) {
        super.update(tpf);
        if (addStation) {
            addStation();
        }
    }

    private void addStation() {
        addStation = false;
        if (!firstStation) {
            worldAppState.setRadius(radius);
            addDome();
        }
        int numberofobjects = 0;
        for (ArrayList<Node> controlList : randomObjects) {
            numberofobjects += controlList.size();
        }
        ObjectData[] allChangedNodes = new ObjectData[numberofobjects];
        int i = 0;
        int obnum = 0;
        for (ArrayList<Node> rndObjectList : randomObjects) {
            obnum = moveRandomObjects(i++, rndObjectList, allChangedNodes, obnum);
        }
        serverAppState.clearChangedSpatials();
        serverAppState.addObjects(addRandomObjects(), conn);
        if (firstStation) {
            firstStation = false;
        }
        serverAppState.defaultConnectionListener.connectionAdded(server, conn);
        if (allChangedNodes.length <= MAXOBJECTSPERMESSAGE) {
            server.broadcast(Filters.notEqualTo(conn), new MovementMessage(0, allChangedNodes).setReliable(true));
        } else {
            int size = allChangedNodes.length;
            i = 0;
            while (size > 0) {
                ObjectData[] data = Arrays.copyOfRange(allChangedNodes, i, i + (size > MAXOBJECTSPERMESSAGE ? MAXOBJECTSPERMESSAGE : size));
                server.broadcast(Filters.notEqualTo(conn), new MovementMessage(0, data).setReliable(true));
                size -= MAXOBJECTSPERMESSAGE;
                i += MAXOBJECTSPERMESSAGE;
            }
        }
        this.app.getStateManager().getState(DriveBusAppState.class).resetBus();
    }

    private Iterable<Spatial> addRandomObjects() {
        ArrayList<Spatial> newObjects = new ArrayList<Spatial>();
        int i = 0;
        float prevradius = radius - CIRCUMFERENCEINCREMENT / (FastMath.PI * 2);
        for (float offset : OFFSETS) {
            float offsetradius = radius + offset;
            float startoffset = firstStation ? 0 : ((prevradius + offset) * FastMath.PI * 2) / offsetradius;
            float min = MINDIST / offsetradius;
            float diff = MAXDIST / offsetradius - min;
            float max = FastMath.PI * 2 - min;
            for (float alpha = startoffset; alpha <= max; alpha += (FastMath.nextRandomFloat() * diff + min)) {
                Node object = generateRandomObject(alpha, offsetradius, offset < 0);
                randomObjects[i].add(object);
                distances[i].add(alpha * offsetradius);
                newObjects.add(object);
            }
            i++;
        }
        return newObjects;
    }

    private Node generateRandomObject(float alpha, float radius, boolean direction) {
        //String type = FastMath.rand.nextBoolean() ? "Models/tree1/tree1.j3o" : (FastMath.rand.nextBoolean() ? "Models/tree2/tree2.j3o" : (FastMath.rand.nextBoolean() ? "Models/house1/house1.j3o" : (FastMath.rand.nextBoolean() ? "Models/busstop_sign/busstop_sign.j3o" : "Models/busstop/busstop.j3o")));
        String type = RANDOMOBJECTS[FastMath.rand.nextInt(RANDOMOBJECTS.length)];
        Node object = (Node) app.getAssetManager().loadModel(type);
        return placeObject(alpha, radius, direction, object, type);
    }

    private Node placeObject(float alpha, float radius, boolean direction, Spatial object, String type) {
        Vector3f location = calculateLocation(alpha, radius);
        Quaternion rotation = calculateRotation(alpha, direction);
        Node control = serverAppState.addRigidObservedSpatial(object, location, rotation, type, (Float) object.getUserData("weight"));
        return control;
    }

    private Vector3f calculateLocation(float alpha, float radius) {
        return new Vector3f(FastMath.cos(alpha) * radius, -5f, FastMath.sin(alpha) * radius);
    }

    private Quaternion calculateRotation(float alpha, boolean direction) {
        return new Quaternion(new float[]{0, -alpha + (direction ? 1 : -1) * FastMath.PI / 2, 0});
    }

    private int moveRandomObjects(int row, ArrayList<Node> rndObjectList, ObjectData[] allChangedNodes, int obnum) {
        float offsetradius = radius + OFFSETS[row];
        Iterator<Float> itDistances = distances[row].iterator();
        for (Node spatial : rndObjectList) {
            RigidBodyControl control = spatial.getControl(RigidBodyControl.class);
            float alpha = itDistances.next() / offsetradius;
            Vector3f location = calculateLocation(alpha, offsetradius);
            control.setPhysicsLocation(location);
            Quaternion rotation = calculateRotation(alpha, row <= 2);
            control.setPhysicsRotation(rotation);
            control.setLinearVelocity(Vector3f.ZERO);
            control.setAngularVelocity(Vector3f.ZERO);
            allChangedNodes[obnum++] = new ObjectData(spatial.getName(), location, rotation);
        }
        return obnum;
    }

    public void connectionRemoved(Server server, HostedConnection conn) {
        serverAppState.defaultConnectionListener.connectionRemoved(server, conn);
    }
}
