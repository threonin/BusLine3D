package busline3d.command;

import busline3d.appstate.BusServerAppState;
import busline3d.appstate.DriveBusAppState;
import busline3d.appstate.WorldAppState;
import busline3d.control.PassengerControl;
import busline3d.message.WatchThisMessage;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.collision.shapes.CylinderCollisionShape;
import com.jme3.bullet.collision.shapes.MeshCollisionShape;
import com.jme3.bullet.control.GhostControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.network.Filters;
import com.jme3.network.HostedConnection;
import com.jme3.network.Server;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import util.appstate.ServerAppState;
import static util.appstate.ServerAppState.MAXOBJECTSPERMESSAGE;
import util.mesh.RingMesh;
import util.message.MovementMessage;
import util.message.ObjectData;

/**
 *
 * @author Volker Schuller
 */
public class AddStationCommand implements Command {

    private final static float[] OFFSETS = new float[]{-70, -40, -10, 30, 60, 90};
    private final static String[] RANDOMOBJECTS = new String[]{"Models/tree1/tree1.j3o", "Models/tree2/tree2.j3o", "Models/house1/house1.j3o"};
    private final static float MINDIST = 18;
    private final static float MAXDIST = 75;
    private final static Iterator<String> STATIONNAMES = Arrays.asList("HTL Pinkafeld", "Oberwart", "Bad Tatzmansdorf", "Großpetersdorf", "Hartberg").iterator();
    private final static Iterator<String> PASSENGERNAMES = Arrays.asList("Merkel", "Obama", "Putin", "Al-Assad", "Faymann", "Spindelegger", "Lukaschenka", "Kim-Jong-Un", "Hollande", "Morales", "Abbas", "Mugabe", "Elizabeth_II", "Carl_XVI_Gustaf", "Juan_Carlos_I").iterator();
    private BulletAppState bulletAppState;
    private ServerAppState serverAppState;
    private WorldAppState worldAppState;
    private DriveBusAppState driveBusAppState;
    private BusServerAppState busServerAppState;
    private AssetManager assetManager;
    private Server server;
    private HostedConnection conn;
    private ArrayList<Node>[] randomObjects;
    private ArrayList<Float>[] distances;
    private boolean singleplayer;
    private float radius;
    private float oldradius;

    public AddStationCommand(AppStateManager stateManager, AssetManager assetManager, Server server, HostedConnection conn, ArrayList<Node>[] randomObjects, ArrayList<Float>[] distances, boolean singleplayer, float radius, float oldradius) {
        this.assetManager = assetManager;
        this.server = server;
        this.conn = conn;
        this.randomObjects = randomObjects;
        this.distances = distances;
        this.singleplayer = singleplayer;
        this.radius = radius;
        this.oldradius = oldradius;
        serverAppState = stateManager.getState(ServerAppState.class);
        worldAppState = stateManager.getState(WorldAppState.class);
        driveBusAppState = stateManager.getState(DriveBusAppState.class);
        busServerAppState = stateManager.getState(BusServerAppState.class);
        bulletAppState = stateManager.getState(BulletAppState.class);
    }

    public boolean execute() {
        if (oldradius != 0) {
            worldAppState.setRadius(radius);
            busServerAppState.addDome();
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
        if (singleplayer) {
            addRandomObjects();
        } else {
            broadcastChanges(allChangedNodes, i);
        }
        driveBusAppState.resetBus(oldradius);
        return true;
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

    private void broadcastChanges(ObjectData[] allChangedNodes, int i) {
        serverAppState.clearChangedSpatials();
        serverAppState.addObjects(addRandomObjects(), conn);
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
        conn.send(new WatchThisMessage(((Node) conn.getAttribute("station")).getName()));
    }

    private Iterable<Spatial> addRandomObjects() {
        ArrayList<Spatial> newObjects = new ArrayList<Spatial>();
        int i = 0;
        for (float offset : OFFSETS) {
            float offsetradius = radius + offset;
            float startoffset = oldradius == 0 ? 0 : ((oldradius + offset) * FastMath.PI * 2) / offsetradius;
            float min = MINDIST / offsetradius;
            float diff = MAXDIST / offsetradius - min;
            float max = FastMath.PI * 2 - min;
            float stationpos = (i == 2) ? ((max - startoffset - diff) * FastMath.nextRandomFloat() + startoffset) : 10;
            for (float alpha = startoffset; alpha <= max; alpha += (FastMath.nextRandomFloat() * diff + min)) {
                if (alpha >= stationpos) {
                    stationpos = 10;
                    Node station = generateStation(alpha, offsetradius, i, newObjects);
                    String stationname;
                    if (singleplayer) {
                        stationname = STATIONNAMES.next();
                    } else {
                        conn.setAttribute("station", station);
                        stationname = conn.getAttribute("stationname");
                    }
                    if (stationname != null) {
                        worldAppState.addCommand(new SetNameCommand(worldAppState, station, stationname));
                    }
                } else {
                    Node object = generateRandomObject(alpha, offsetradius, offset < 0);
                    randomObjects[i].add(object);
                    distances[i].add(alpha * offsetradius);
                    newObjects.add(object);
                }
            }
            i++;
        }
        return newObjects;
    }

    private Node generateStation(float alpha, float offsetradius, int i, ArrayList<Spatial> newObjects) {
        Spatial busstopmodel = (Spatial) assetManager.loadModel("Models/busstop/busstop.j3o");
        Node busstop = placeObject(alpha, offsetradius, true, busstopmodel, "Models/busstop/busstop.j3o");
        randomObjects[i].add(busstop);
        distances[i].add(alpha * offsetradius);
        GhostControl innerGhost = new GhostControl(new CylinderCollisionShape(new Vector3f(20, 10, 0), 1));
        innerGhost.setCollisionGroup(PhysicsCollisionObject.COLLISION_GROUP_02);
        innerGhost.setCollideWithGroups(PhysicsCollisionObject.COLLISION_GROUP_03);
        busstop.addControl(innerGhost);
        bulletAppState.getPhysicsSpace().add(innerGhost);

        GhostControl middleGhost = new GhostControl(new MeshCollisionShape(new RingMesh(42, 43, 10, true, 1, 40)));
        middleGhost.setCollisionGroup(PhysicsCollisionObject.COLLISION_GROUP_02);
        middleGhost.setCollideWithGroups(PhysicsCollisionObject.COLLISION_GROUP_03);
        Node middle = new Node("middleGhost");
        middle.addControl(middleGhost);
        busstop.attachChild(middle);
        bulletAppState.getPhysicsSpace().add(middleGhost);

        GhostControl outerGhost = new GhostControl(new MeshCollisionShape(new RingMesh(59, 60, 10, true, 1, 40)));
        outerGhost.setCollisionGroup(PhysicsCollisionObject.COLLISION_GROUP_02);
        outerGhost.setCollideWithGroups(PhysicsCollisionObject.COLLISION_GROUP_03);
        Node outer = new Node("outerGhost");
        outer.addControl(outerGhost);
        busstop.attachChild(outer);
        bulletAppState.getPhysicsSpace().add(outerGhost);

        String[] passengers = new String[6];
        if (singleplayer) {
            passengers[0] = PASSENGERNAMES.next();
            passengers[1] = PASSENGERNAMES.next();
            passengers[2] = PASSENGERNAMES.next();
        }
        busstop.addControl(new PassengerControl(passengers));
        newObjects.add(busstop);
        Spatial busstopsignmodel = (Spatial) assetManager.loadModel("Models/busstop_sign/busstop_sign.j3o");
        Node busstopsign = placeObject(alpha + 10 / offsetradius, offsetradius, true, busstopsignmodel, "Models/busstop_sign/busstop_sign.j3o");
        randomObjects[i].add(busstopsign);
        distances[i].add(alpha * offsetradius + 10);
        newObjects.add(busstopsign);
        return busstop;
    }

    private Node generateRandomObject(float alpha, float radius, boolean direction) {
        String type = RANDOMOBJECTS[FastMath.rand.nextInt(RANDOMOBJECTS.length)];
        Node object = (Node) assetManager.loadModel(type);
        return placeObject(alpha, radius, direction, object, type);
    }

    private Node placeObject(float alpha, float radius, boolean direction, Spatial object, String type) {
        Vector3f location = calculateLocation(alpha, radius);
        Quaternion rotation = calculateRotation(alpha, direction);
        Node node = serverAppState.addRigidObservedSpatial(object, location, rotation, type, (Float) object.getUserData("weight"));
        return node;
    }

    private Vector3f calculateLocation(float alpha, float radius) {
        return new Vector3f(FastMath.cos(alpha) * radius, -5f, FastMath.sin(alpha) * radius);
    }

    private Quaternion calculateRotation(float alpha, boolean direction) {
        return new Quaternion(new float[]{0, -alpha + (direction ? 1 : -1) * FastMath.PI / 2, 0});
    }
}
