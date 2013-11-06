package busline3d.appstate;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Dome;
import util.appstate.ServerAppState;

/**
 *
 * @author Volker Schuller
 */
public class BusServerAppState extends AbstractAppState {

    private SimpleApplication app;
    private ServerAppState serverAppState;
    private Node rootNode;

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        this.app = (SimpleApplication) app;
        this.rootNode = this.app.getRootNode();
        serverAppState = stateManager.getState(ServerAppState.class);
        BulletAppState bulletAppState = stateManager.getState(BulletAppState.class);

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

        addRandomObjects();
        stateManager.attach(new DriveBusAppState());
    }

    private void addRandomObjects() {
        for (float alpha = 0; alpha < FastMath.PI * 2; alpha += 0.1) {
            for (int i = 230; i <= 290; i += 30) {
                generateRandomObject(alpha, i, true);
            }
            for (int i = 320; i <= 380; i += 30) {
                generateRandomObject(alpha, i, false);
            }
        }
    }

    private void generateRandomObject(float alpha, int i, boolean direction) {
        String type = FastMath.rand.nextBoolean() ? "Models/tree1/tree1.j3o" : (FastMath.rand.nextBoolean() ? "Models/tree2/tree2.j3o" : (FastMath.rand.nextBoolean() ? "Models/house1/house1.j3o" : (FastMath.rand.nextBoolean() ? "Models/busstop_sign/busstop_sign.j3o" : "Models/busstop/busstop.j3o")));
        Spatial object = (Spatial) app.getAssetManager().loadModel(type);
        Vector3f location = new Vector3f(FastMath.cos(alpha) * i, -5f, FastMath.sin(alpha) * i);
        Quaternion rotation = new Quaternion(new float[]{0, -alpha + (direction ? 1 : -1) * FastMath.PI / 2, 0});
        RigidBodyControl control = serverAppState.addRigidObservedSpatial(object, location, rotation, type, (Float) object.getUserData("weight"));
        control.setSleepingThresholds(2, 2);
        control.setFriction(1);
    }
}
