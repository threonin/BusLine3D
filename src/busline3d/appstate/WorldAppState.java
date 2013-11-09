package busline3d.appstate;

import busline3d.command.Command;
import busline3d.control.BusControl;
import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
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
import com.jme3.texture.Texture;
import com.jme3.util.SkyFactory;
import java.util.concurrent.ConcurrentLinkedQueue;
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

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);
        this.app = (SimpleApplication) app;
        this.rootNode = this.app.getRootNode();
        font = this.app.getAssetManager().loadFont("Interface/Fonts/Default.fnt");

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

        this.app.getStateManager().attach(new SunAppState());
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
        Spatial sky = SkyFactory.createSky(
                this.app.getAssetManager(), "Textures/clouds/clouds.jpg", true);
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
        float offset = font.getLineWidth(name) / 20;
        label = font.createLabel(name);
        label.scale(0.1f);
        label.setLocalTranslation(-offset, 9, 0);
        node.attachChild(label);
        label2 = font.createLabel(name);
        label2.rotate(0, FastMath.PI, 0);
        label2.scale(0.1f);
        label2.setLocalTranslation(offset, 9, 0);
        node.attachChild(label2);
    }
}
