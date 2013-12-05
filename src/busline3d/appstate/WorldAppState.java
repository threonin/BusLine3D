package busline3d.appstate;

import busline3d.command.Command;
import busline3d.control.BusControl;
import busline3d.control.PassengerControl;
import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.bounding.BoundingBox;
import com.jme3.font.BitmapFont;
import com.jme3.light.AmbientLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Cylinder;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;
import com.jme3.util.SkyFactory;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import util.UtilFunctions;
import util.mesh.RingMesh;

/**
 *
 * @author Volker Schuller
 */
public class WorldAppState extends AbstractAppState {

    private SimpleApplication app;
    private Node rootNode;
    private Material floorMat;
    private Material streetMat;
    private Geometry floor;
    private Geometry street;
    private float radius;
    private ConcurrentLinkedQueue<Command> commands = new ConcurrentLinkedQueue<Command>();
    private BitmapFont font;
    private HashMap<String, Material> materialsForPassengers = new HashMap<String, Material>();
    private Material glass;
    private PassengerControl passengerControl;

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);
        this.app = (SimpleApplication) app;
        this.rootNode = this.app.getRootNode();
        font = this.app.getAssetManager().loadFont("Interface/Fonts/FreeSans.fnt");

        initMaterials();
        addSky();
        AmbientLight al = new AmbientLight();
        al.setColor(ColorRGBA.White.mult(0.05f));
        rootNode.addLight(al);
        rootNode.setShadowMode(RenderQueue.ShadowMode.Cast);

        Node busNode = (Node) this.app.getAssetManager().loadModel("Models/bus/bus.j3o");
        busNode.setName("Bus");
        rootNode.attachChild(busNode);
        busNode.addControl(new BusControl());
        Geometry[] geometries = new Geometry[10];
        BoundingBox bbox = (BoundingBox) UtilFunctions.findGeom(busNode, "LWindow 1", false).getModelBound();
        Quad quad = new Quad(bbox.getZExtent() * 2, bbox.getYExtent() * 2);
        Quaternion leftQuat = new Quaternion(new float[]{0, FastMath.PI / 2, 0});
        geometries[0] = addWindow(busNode, leftQuat, "LWindow 1", quad, true);
        geometries[1] = addWindow(busNode, leftQuat, "LWindow 2", quad, true);
        geometries[2] = addWindow(busNode, leftQuat, "LWindow 3", quad, true);
        geometries[3] = addWindow(busNode, leftQuat, "LWindow 4", quad, true);
        geometries[4] = addWindow(busNode, leftQuat, "LWindow 5", quad, true);
        Quaternion rightQuat = new Quaternion(new float[]{0, -FastMath.PI / 2, 0});
        geometries[5] = addWindow(busNode, rightQuat, "RWindow 1", quad, false);
        geometries[6] = addWindow(busNode, rightQuat, "RWindow 2", quad, false);
        geometries[7] = addWindow(busNode, rightQuat, "RWindow 3", quad, false);
        geometries[8] = addWindow(busNode, rightQuat, "RWindow 4", quad, false);
        geometries[9] = addWindow(busNode, rightQuat, "RWindow 5", quad, false);
        passengerControl = new PassengerControl(geometries, this, false);
        busNode.addControl(passengerControl);

        this.app.getStateManager().attach(new SunAppState());
    }

    private Geometry addWindow(Node busNode, Quaternion rotation, String name, Quad quad, boolean left) {
        Geometry oldwindow = UtilFunctions.findGeom(busNode, name, false);
        BoundingBox bbox = (BoundingBox) oldwindow.getModelBound();
        Geometry newGeom = new Geometry(name + "_new", quad);
        newGeom.setMaterial(glass);
        newGeom.setLocalRotation(rotation);
        newGeom.setLocalTranslation(bbox.getCenter().add(left ? 0.01f : -0.01f, -bbox.getYExtent(), left ? bbox.getZExtent() : -bbox.getZExtent()));
        oldwindow.getParent().attachChild(newGeom);
        return newGeom;
    }

    private void initMaterials() {
        AssetManager assetManager = this.app.getAssetManager();
        floorMat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        floorMat.setBoolean("UseMaterialColors", true);
        floorMat.setColor("Diffuse", new ColorRGBA(0.2f, 0.6f, 0.2f, 1));
        floorMat.setColor("Ambient", new ColorRGBA(0.2f, 0.6f, 0.2f, 1));
        streetMat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        Texture streettex = assetManager.loadTexture("Textures/street/street.png");
        streettex.setWrap(Texture.WrapMode.Repeat);
        streetMat.setBoolean("UseMaterialColors", true);
        streetMat.setColor("Ambient", ColorRGBA.White.mult(2));
        streetMat.setColor("Diffuse", ColorRGBA.White);
        streetMat.setTexture("DiffuseMap", streettex);
        glass = assetManager.loadMaterial("Materials/Generated/bus-Glass.j3m");
    }

    public float getRadius() {
        return radius;
    }

    public void setRadius(float radius) {
        this.radius = radius;
        this.app.getStateManager().getState(SunAppState.class).setRadius(radius);
        buildFloor();
    }

    public void buildFloor() {
        if (floor != null) {
            floor.removeFromParent();
            street.removeFromParent();
        }
        Cylinder floorCylinder = new Cylinder(4, 100, radius + 110, 1, true);
        floor = new Geometry("floor", floorCylinder);
        floor.setLocalRotation(new Quaternion(new float[]{FastMath.PI / 2, 0, 0}));
        floor.setLocalTranslation(0, -5.5f, 0);
        floor.setMaterial(floorMat);
        floor.setShadowMode(RenderQueue.ShadowMode.Receive);
        rootNode.attachChild(floor);
        street = new Geometry("StreetMesh", new RingMesh(radius, radius + 15, (int) radius, false, ((int) radius / 3)));
        street.setMaterial(streetMat);
        street.setLocalTranslation(0, -4.9f, 0);
        street.setShadowMode(RenderQueue.ShadowMode.Receive);
        rootNode.attachChild(street);
    }

    private void addSky() {
        AssetManager assetManager = this.app.getAssetManager();
        Texture west = assetManager.loadTexture("Textures/clouds/west.jpg");
        Texture east = assetManager.loadTexture("Textures/clouds/east.jpg");
        Texture north = assetManager.loadTexture("Textures/clouds/north.jpg");
        Texture south = assetManager.loadTexture("Textures/clouds/south.jpg");
        Texture up = assetManager.loadTexture("Textures/clouds/up.jpg");
        Texture down = assetManager.loadTexture("Textures/clouds/down.jpg");
        Spatial sky = SkyFactory.createSky(assetManager, west, east, north, south, up, down);
        sky.setShadowMode(RenderQueue.ShadowMode.Off);
        rootNode.attachChild(sky);
    }

    public void addCommand(Command command) {
        commands.add(command);
    }

    @Override
    public void update(float tpf) {
        super.update(tpf);
        for (Command command : commands) {
            if (command.execute()) {
                commands.remove(command);
            }
        }
    }

    public void addLabel(Node node, String name) {
        Spatial label = node.getChild("label1");
        Spatial label2 = node.getChild("label2");
        if (label != null) {
            label.removeFromParent();
            label2.removeFromParent();
        }
        float offset = font.getLineWidth(name) / 40;
        label = font.createLabel(name);
        label.scale(0.05f);
        label.setLocalTranslation(-offset, 9, 0);
        node.attachChild(label);
        label2 = font.createLabel(name);
        label2.rotate(0, FastMath.PI, 0);
        label2.scale(0.05f);
        label2.setLocalTranslation(offset, 9, 0);
        node.attachChild(label2);
    }

    public Material getMaterialForPassenger(String name) {
        if (!materialsForPassengers.containsKey(name)) {
            addMaterialForPassenger(name);
        }
        return materialsForPassengers.get(name);
    }

    private void addMaterialForPassenger(String name) {
        AssetManager assetManager = this.app.getAssetManager();
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setTexture("ColorMap", assetManager.loadTexture("Textures/passengers/" + name + ".jpg"));
        materialsForPassengers.put(name, mat);
    }

    public PassengerControl generatePassengerControlForStation(Node busstop) {
        Geometry[] geometries = new Geometry[8];
        Quad quad = new Quad(1.5f, 1.5f);
        for (int j = 0; j < 4; j++) {
            geometries[j] = new Geometry(j + "_passenger", quad);
            geometries[j].setLocalTranslation(j * 1.6f - 3.2f, 3.6f, 0.2f);
            geometries[j].setMaterial(glass);
            busstop.attachChild(geometries[j]);
            geometries[j + 4] = new Geometry((j + 4) + "_passenger", quad);
            geometries[j + 4].setLocalTranslation(j * 1.6f - 3.2f, 2f, 0.2f);
            geometries[j + 4].setMaterial(glass);
            busstop.attachChild(geometries[j + 4]);
        }
        return new PassengerControl(geometries, this, true);
    }

    public PassengerControl getPassengerControl() {
        return passengerControl;
    }

    public Material getGlass() {
        return glass;
    }
}
