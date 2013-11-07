package busline3d.appstate;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.bounding.BoundingBox;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.VehicleControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.input.ChaseCamera;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix3f;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import util.appstate.ServerAppState;

/**
 *
 * @author Volker Schuller
 */
public class DriveBusAppState extends AbstractAppState implements ActionListener {

    private SimpleApplication app;
    private VehicleControl busControl;
    private Node busNode;
    private final float accelerationForce = 1500.0f;
    private final float reverseForce = -500.0f;
    private final float brakeForce = 100.0f;
    private float steeringValue = 0;
    private float accelerationValue = 0;
    private Vector3f jumpForce = new Vector3f(0, 3000, 0);

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);
        this.app = (SimpleApplication) app;
        ServerAppState serverAppState = stateManager.getState(ServerAppState.class);
        Node model = (Node) this.app.getRootNode().getChild("Bus");
        Vector3f location = model.getLocalTranslation().clone();
        Quaternion rotation = model.getLocalRotation().clone();
        model.removeFromParent();
        model.setLocalTransform(Transform.IDENTITY);
        busNode = serverAppState.addObservedSpatial(model, location, rotation, "Bus", "");
        initVehicleControl();
        setupKeys(this.app.getInputManager());
        this.app.getFlyByCamera().setEnabled(false);
        ChaseCamera chaseCam = new ChaseCamera(this.app.getCamera(), busNode, this.app.getInputManager());
        chaseCam.setSmoothMotion(true);
        chaseCam.setMinDistance(15);
        chaseCam.setDefaultDistance(40);
        chaseCam.setMaxDistance(100);
    }

    private void initVehicleControl() {
        float stiffness = 50.0f;//200=f1 car
        float compValue = 0.2f; //(lower than damp!)
        float dampValue = 0.3f;
        final float mass = 400;

        //Create a hull collision shape for the chassis
        Geometry chasis = findGeom(busNode, "Chassis", false);
        CollisionShape carHull = CollisionShapeFactory.createDynamicMeshShape(chasis);

        //Create a vehicle control
        busControl = new VehicleControl(carHull, mass);
        busControl.setFriction(1);
        busNode.addControl(busControl);

        //Setting default values for wheels
        busControl.setSuspensionCompression(compValue * 2.0f * FastMath.sqrt(stiffness));
        busControl.setSuspensionDamping(dampValue * 2.0f * FastMath.sqrt(stiffness));
        busControl.setSuspensionStiffness(stiffness);
        busControl.setMaxSuspensionForce(10000);

        Vector3f wheelDirection = new Vector3f(0, -1, 0);
        Vector3f wheelAxle = new Vector3f(-1, 0, 0);

        Geometry wheel_fr = findGeom(busNode, "Wheel RF", false);
        wheel_fr.center();
        BoundingBox bbox = (BoundingBox) wheel_fr.getModelBound();
        float wheelRadius = bbox.getYExtent();
        float back_wheel_h = (wheelRadius * 1.7f) - 1.2f;
        float front_wheel_h = (wheelRadius * 1.9f) - 1.2f;
        busControl.addWheel(wheel_fr.getParent(), bbox.getCenter().add(0, -front_wheel_h, 0),
                wheelDirection, wheelAxle, 0.2f, wheelRadius, true);
        Geometry wheel_fl = findGeom(busNode, "Wheel LF", false);
        wheel_fl.center();
        bbox = (BoundingBox) wheel_fl.getModelBound();
        busControl.addWheel(wheel_fl.getParent(), bbox.getCenter().add(0, -front_wheel_h, 0),
                wheelDirection, wheelAxle, 0.2f, wheelRadius, true);
        Geometry wheel_br = findGeom(busNode, "Wheel RB", false);
        wheel_br.center();
        bbox = (BoundingBox) wheel_br.getModelBound();
        busControl.addWheel(wheel_br.getParent(), bbox.getCenter().add(0, -back_wheel_h, 0),
                wheelDirection, wheelAxle, 0.2f, wheelRadius, false);
        Geometry wheel_bl = findGeom(busNode, "Wheel LB", false);
        wheel_bl.center();
        bbox = (BoundingBox) wheel_bl.getModelBound();
        busControl.addWheel(wheel_bl.getParent(), bbox.getCenter().add(0, -back_wheel_h, 0),
                wheelDirection, wheelAxle, 0.2f, wheelRadius, false);
        this.app.getStateManager().getState(BulletAppState.class).getPhysicsSpace().add(busControl);
    }

    private void setupKeys(InputManager inputManager) {
        inputManager.addMapping("Lefts", new KeyTrigger(KeyInput.KEY_LEFT));
        inputManager.addMapping("Rights", new KeyTrigger(KeyInput.KEY_RIGHT));
        inputManager.addMapping("Ups", new KeyTrigger(KeyInput.KEY_UP));
        inputManager.addMapping("Downs", new KeyTrigger(KeyInput.KEY_DOWN));
        inputManager.addMapping("Space", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping("Reset", new KeyTrigger(KeyInput.KEY_RETURN));
        inputManager.addListener(this, "Lefts");
        inputManager.addListener(this, "Rights");
        inputManager.addListener(this, "Ups");
        inputManager.addListener(this, "Downs");
        inputManager.addListener(this, "Space");
        inputManager.addListener(this, "Reset");
    }

    public void onAction(String binding, boolean value, float tpf) {
        if (binding.equals("Lefts")) {
            if (value) {
                steeringValue += .5f;
            } else {
                steeringValue += -.5f;
            }
            busControl.steer(steeringValue);
        } else if (binding.equals("Rights")) {
            if (value) {
                steeringValue += -.5f;
            } else {
                steeringValue += .5f;
            }
            busControl.steer(steeringValue);
        } else if (binding.equals("Ups")) {
            if (value) {
                accelerationValue += accelerationForce;
            } else {
                accelerationValue -= accelerationForce;
            }
            busControl.accelerate(accelerationValue);
        } else if (binding.equals("Downs")) {
            if (value) {
                accelerationValue += reverseForce;
            } else {
                accelerationValue -= reverseForce;
            }
            busControl.accelerate(accelerationValue);
        } else if (binding.equals("Space")) {
            /*if (value) {
             busControl.applyImpulse(jumpForce, Vector3f.ZERO);
             }*/
            if (value) {
                busControl.brake(brakeForce);
            } else {
                busControl.brake(0f);
            }
        } else if (binding.equals("Reset")) {
            if (value) {
                resetBus();
            } else {
            }
        }
    }

    private Geometry findGeom(Spatial spatial, String name, boolean found) {
        if (spatial instanceof Node) {
            Node node = (Node) spatial;
            for (int i = 0; i < node.getQuantity(); i++) {
                Spatial child = node.getChild(i);
                if (found || (child.getName() != null && child.getName().startsWith(name))) {
                    return findGeom(child, name, true);
                }
                Geometry result = findGeom(child, name, false);
                if (result != null) {
                    return result;
                }
            }
        } else if (spatial instanceof Geometry) {
            if (found || spatial.getName().startsWith(name)) {
                return (Geometry) spatial;
            }
        }
        return null;
    }

    public void resetBus() {
        float radius = this.app.getStateManager().getState(WorldAppState.class).getRadius();
        busControl.setPhysicsLocation(new Vector3f(-radius - 10, 0, 0));
        busControl.setPhysicsRotation(new Matrix3f());
        busControl.setLinearVelocity(Vector3f.ZERO);
        busControl.setAngularVelocity(Vector3f.ZERO);
        busControl.resetSuspension();
    }
}
