/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package busline3d.appstate;

import busline3d.control.BusControl;
import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.light.AmbientLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.network.serializing.Serializer;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Cylinder;
import com.jme3.texture.Texture;
import com.jme3.util.SkyFactory;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.controls.TextField;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.screen.ScreenController;
import java.util.logging.Level;
import java.util.logging.Logger;
import util.appstate.ClientAppState;
import util.appstate.ServerAppState;
import util.mesh.RingMesh;
import util.message.DefinitionMessage;
import util.message.MovementMessage;
import util.message.ObjectData;
import util.message.ObjectDefinition;
import util.message.ResetTimerMessage;

/**
 *
 * @author Volker Schuller
 */
public class StartMenuAppState extends AbstractAppState implements ScreenController {

    private Nifty nifty;
    private Screen screen;
    private SimpleApplication app;
    private AppStateManager stateManager;
    private Node rootNode;

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);
        this.stateManager = stateManager;
        this.app = (SimpleApplication) app;
        this.rootNode = this.app.getRootNode();
    }

    public void bind(Nifty nifty, Screen screen) {
        this.nifty = nifty;
        this.screen = screen;
    }

    public void onStartScreen() {
    }

    public void onEndScreen() {
    }

    public void singleplayer() {
        nifty.removeScreen("start");
        startServer(0);
    }

    public void serverdialog() {
        nifty.gotoScreen("server");
    }

    public void server() {
        int port = getPort();
        if (port != 0) {
            nifty.removeScreen("server");
            initSerializer();
            startServer(port);
        }
    }

    private void startServer(int port) {
        startGame();
        BulletAppState bulletAppState = new BulletAppState();
        bulletAppState.setThreadingType(BulletAppState.ThreadingType.PARALLEL);
        stateManager.attach(bulletAppState);
        stateManager.attach(new ServerAppState(port));
        stateManager.attach(new BusServerAppState());
    }

    public void clientdialog() {
        nifty.gotoScreen("client");
    }

    public void client() {
        int port = getPort();
        if (port != 0) {
            String host = screen.findNiftyControl("host", TextField.class).getRealText();
            initSerializer();
            startGame();
            stateManager.attach(new ClientAppState(host, port));
            stateManager.attach(new BusClientAppState());
            nifty.removeScreen("client");
        }
    }

    private int getPort() {
        int port = 0;
        try {
            port = Integer.parseInt(screen.findNiftyControl("port", TextField.class).getRealText());
        } catch (NumberFormatException ex) {
            Logger.getLogger(BusClientAppState.class.getName()).log(Level.SEVERE, "invalid number!");
        }
        return port;
    }

    private void initSerializer() {
        Serializer.registerClass(ObjectData.class);
        Serializer.registerClass(MovementMessage.class);
        Serializer.registerClass(ObjectDefinition.class);
        Serializer.registerClass(DefinitionMessage.class);
        Serializer.registerClass(ResetTimerMessage.class);
    }

    public void startGame() {
        buildFloor();
        AmbientLight al = new AmbientLight();
        al.setColor(ColorRGBA.White.mult(0.05f));
        rootNode.addLight(al);
        rootNode.setShadowMode(RenderQueue.ShadowMode.Cast);

        Node busNode = (Node) this.app.getAssetManager().loadModel("Models/bus/bus.j3o");
        busNode.setName("Bus");
        busNode.setLocalTranslation(-310, 0, 0);
        rootNode.attachChild(busNode);
        busNode.addControl(new BusControl());

        stateManager.attach(new SunAppState());
    }

    private void buildFloor() {
        Cylinder floorCylinder = new Cylinder(4, 100, 400, 1, true);
        Geometry floor = new Geometry("floor", floorCylinder);
        floor.setLocalRotation(new Quaternion(new float[]{FastMath.PI / 2, 0, 0}));
        floor.setLocalTranslation(0, -5.5f, 0);
        AssetManager assetManager = this.app.getAssetManager();
        Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        mat.setBoolean("UseMaterialColors", true);
        mat.setColor("Diffuse", new ColorRGBA(0.2f, 0.6f, 0.2f, 1));
        mat.setColor("Ambient", new ColorRGBA(0.2f, 0.6f, 0.2f, 1));
        floor.setMaterial(mat);
        floor.setShadowMode(RenderQueue.ShadowMode.Receive);
        rootNode.attachChild(floor);

        Geometry streetGeom = new Geometry("StreetMesh", new RingMesh(300f, 315f, 200, false, 100));
        Material mat2 = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        Texture streettex = assetManager.loadTexture("Textures/street/street.png");
        streettex.setWrap(Texture.WrapMode.Repeat);
        mat2.setBoolean("UseMaterialColors", true);
        mat2.setColor("Ambient", ColorRGBA.White.mult(2));
        mat2.setColor("Diffuse", ColorRGBA.White);
        mat2.setTexture("DiffuseMap", streettex);
        streetGeom.setMaterial(mat2);
        streetGeom.setLocalTranslation(0, -4.9f, 0);
        streetGeom.setShadowMode(RenderQueue.ShadowMode.Receive);
        rootNode.attachChild(streetGeom);

        Spatial sky = SkyFactory.createSky(
                assetManager, "Textures/clouds/clouds.jpg", true);
        sky.setShadowMode(RenderQueue.ShadowMode.Off);
        rootNode.attachChild(sky);
    }
}
