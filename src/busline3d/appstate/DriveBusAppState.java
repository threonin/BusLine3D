package busline3d.appstate;

import busline3d.BusLine3D;
import busline3d.control.HamsterControl;
import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.bounding.BoundingBox;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.PhysicsCollisionEvent;
import com.jme3.bullet.collision.PhysicsCollisionListener;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.GhostControl;
import com.jme3.bullet.control.VehicleControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.input.ChaseCamera;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import de.lessvoid.nifty.Nifty;
import java.util.Collections;
import util.UtilFunctions;
import util.appstate.ServerAppState;

/**
 *
 * @author Volker Schuller
 */
public class DriveBusAppState extends AbstractAppState implements ActionListener, PhysicsCollisionListener {

    private static final int SPEED = 100;
    private final float STOPTIME = 2;
    private BusLine3D app;
    private VehicleControl busControl;
    private Node busNode;
    private final float accelerationForce = 1500.0f;
    private final float reverseForce = -500.0f;
    private final float brakeForce = 100.0f;
    private float steeringValue = 0;
    private float accelerationValue = 0;
    private boolean showInfo;
    private boolean pysicsDebug;
    private float radius;
    private boolean autopilot;
    private Vector3f autoLoc = new Vector3f();
    private Quaternion autoRot = new Quaternion();
    private float alphastep;
    private float aktAlpha;
    private float speedStep = FastMath.pow(SPEED, 2) / 100;
    private float aktSpeed = SPEED;
    private boolean middleReached;
    private boolean centerReached;
    private float stopped;
    private HudAppState hudAppState;
    private Nifty nifty;
    private int hamstercount;

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);
        this.app = (BusLine3D) app;
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
        chaseCam.setMinDistance(10);
        chaseCam.setDefaultDistance(40);
        chaseCam.setMaxDistance(100);
        this.app.getCamera().setFrustumFar(10000);
        hudAppState = new HudAppState();
        stateManager.attach(hudAppState);
        nifty = this.app.getNiftyDisplay().getNifty();
        nifty.fromXml("Interface/hud.xml", "empty", hudAppState);
    }

    private void initVehicleControl() {
        float stiffness = 50.0f;//200=f1 car
        float compValue = 0.2f; //(lower than damp!)
        float dampValue = 0.3f;
        final float mass = 400;

        //Create a hull collision shape for the chassis
        Geometry chasis = UtilFunctions.findGeom(busNode, "Chassis", false);
        CollisionShape carHull = CollisionShapeFactory.createDynamicMeshShape(chasis);

        //Create a vehicle control
        busControl = new VehicleControl(carHull, mass);
        busControl.setFriction(1);
        busControl.setCollisionGroup(PhysicsCollisionObject.COLLISION_GROUP_03);
        busControl.setCollideWithGroups(PhysicsCollisionObject.COLLISION_GROUP_01 | PhysicsCollisionObject.COLLISION_GROUP_02);
        busNode.addControl(busControl);

        //Setting default values for wheels
        busControl.setSuspensionCompression(compValue * 2.0f * FastMath.sqrt(stiffness));
        busControl.setSuspensionDamping(dampValue * 2.0f * FastMath.sqrt(stiffness));
        busControl.setSuspensionStiffness(stiffness);
        busControl.setMaxSuspensionForce(10000);

        Vector3f wheelDirection = new Vector3f(0, -1, 0);
        Vector3f wheelAxle = new Vector3f(-1, 0, 0);

        Geometry wheel_fr = UtilFunctions.findGeom(busNode, "Wheel RF", false);
        wheel_fr.center();
        BoundingBox bbox = (BoundingBox) wheel_fr.getModelBound();
        float wheelRadius = bbox.getYExtent();
        float back_wheel_h = (wheelRadius * 1.7f) - 1.2f;
        float front_wheel_h = (wheelRadius * 1.9f) - 1.2f;
        busControl.addWheel(wheel_fr.getParent(), bbox.getCenter().add(0, -front_wheel_h, 0),
                wheelDirection, wheelAxle, 0.2f, wheelRadius, true);
        Geometry wheel_fl = UtilFunctions.findGeom(busNode, "Wheel LF", false);
        wheel_fl.center();
        bbox = (BoundingBox) wheel_fl.getModelBound();
        busControl.addWheel(wheel_fl.getParent(), bbox.getCenter().add(0, -front_wheel_h, 0),
                wheelDirection, wheelAxle, 0.2f, wheelRadius, true);
        Geometry wheel_br = UtilFunctions.findGeom(busNode, "Wheel RB", false);
        wheel_br.center();
        bbox = (BoundingBox) wheel_br.getModelBound();
        busControl.addWheel(wheel_br.getParent(), bbox.getCenter().add(0, -back_wheel_h, 0),
                wheelDirection, wheelAxle, 0.2f, wheelRadius, false);
        Geometry wheel_bl = UtilFunctions.findGeom(busNode, "Wheel LB", false);
        wheel_bl.center();
        bbox = (BoundingBox) wheel_bl.getModelBound();
        busControl.addWheel(wheel_bl.getParent(), bbox.getCenter().add(0, -back_wheel_h, 0),
                wheelDirection, wheelAxle, 0.2f, wheelRadius, false);
        PhysicsSpace physicsSpace = this.app.getStateManager().getState(BulletAppState.class).getPhysicsSpace();
        physicsSpace.add(busControl);
        physicsSpace.addCollisionListener(this);
    }

    private void setupKeys(InputManager inputManager) {
        inputManager.addMapping("Lefts", new KeyTrigger(KeyInput.KEY_LEFT));
        inputManager.addMapping("Rights", new KeyTrigger(KeyInput.KEY_RIGHT));
        inputManager.addMapping("Ups", new KeyTrigger(KeyInput.KEY_UP));
        inputManager.addMapping("Downs", new KeyTrigger(KeyInput.KEY_DOWN));
        inputManager.addMapping("Space", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping("Reset", new KeyTrigger(KeyInput.KEY_RETURN));
        inputManager.addMapping("TogglePhysics", new KeyTrigger(KeyInput.KEY_P));
        inputManager.addMapping("ToggleInfo", new KeyTrigger(KeyInput.KEY_I));
        inputManager.addMapping("Hamster", new KeyTrigger(KeyInput.KEY_H));
        inputManager.addListener(this, "Lefts");
        inputManager.addListener(this, "Rights");
        inputManager.addListener(this, "Ups");
        inputManager.addListener(this, "Downs");
        inputManager.addListener(this, "Space");
        inputManager.addListener(this, "Reset");
        inputManager.addListener(this, "TogglePhysics");
        inputManager.addListener(this, "ToggleInfo");
        inputManager.addListener(this, "Hamster");
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
            if (value) {
                busControl.brake(brakeForce);
            } else {
                busControl.brake(0f);
            }
        }
        if (binding.equals("ToggleInfo")) {
            if (value) {
                showInfo = !showInfo;
                this.app.setDisplayFps(showInfo);
                this.app.setDisplayStatView(showInfo);
            }
        } else if (binding.equals("TogglePhysics")) {
            if (value) {
                pysicsDebug = !pysicsDebug;
                this.app.getStateManager().getState(BulletAppState.class).setDebugEnabled(pysicsDebug);
            }
        } else if (binding.equals("Reset")) {
            if (value) {
                aktAlpha = resetBus(0);
                alphastep = SPEED / radius;
                busControl.setKinematic(true);
                autopilot = true;
            }
        } else if (binding.equals("Hamster")) {
            if (value) {
                Node hamster = (Node) this.app.getAssetManager().loadModel("Models/tentaclehamster/tentaclehamster.j3o");
                String name = "ObjectHamster" + hamstercount++;
                hamster.setLocalTranslation(busNode.getWorldTranslation().add(busControl.getForwardVector(null).multLocal(20)));
                hamster.setLocalRotation(busNode.getWorldRotation().opposite());
                hamster.setLocalScale(2);
                ServerAppState serverAppState = app.getStateManager().getState(ServerAppState.class);
                if (serverAppState == null || serverAppState.getServer() == null) {
                    hamster.setName(name);
                    this.app.getRootNode().attachChild(hamster);
                } else {
                    hamster.setName("Hamster");
                    Vector3f location = hamster.getLocalTranslation().clone();
                    Quaternion rotation = hamster.getLocalRotation().clone();
                    hamster.removeFromParent();
                    hamster.setLocalTransform(Transform.IDENTITY);
                    hamster = serverAppState.addObservedSpatial(hamster, location, rotation, name, "Models/tentaclehamster/tentaclehamster.j3o");
                    serverAppState.addObjects(Collections.singletonList((Spatial) hamster));
                }
                BulletAppState bulletAppState = this.app.getStateManager().getState(BulletAppState.class);
                hamster.addControl(new HamsterControl(bulletAppState, serverAppState, name));
            }
        } else if (autopilot) {
            autopilot = false;
            busControl.setKinematic(false);
            busControl.setLinearVelocity(new Vector3f(-FastMath.sin(aktAlpha) * aktSpeed, 0, FastMath.cos(aktAlpha) * aktSpeed));
        }
    }

    public float resetBus(float oldradius) {
        radius = this.app.getStateManager().getState(WorldAppState.class).getRadius();
        Vector3f unit = busControl.getPhysicsLocation().setY(0).normalizeLocal();
        if (!unit.isUnitVector()) {
            unit = Vector3f.UNIT_X.clone();
        }
        float alpha = FastMath.atan2(unit.z, unit.x);
        if (oldradius > 0) {
            alpha *= oldradius / radius;
            unit.set(FastMath.cos(alpha), 0, FastMath.sin(alpha));
            aktSpeed = SPEED;
            alphastep = aktSpeed / radius;
        }
        Quaternion rot = new Quaternion(new float[]{0, -alpha, 0});
        unit.multLocal(radius + 3.75f).setY(-4.9f);
        busControl.setPhysicsLocation(unit);
        busControl.setPhysicsRotation(rot);
        busControl.setLinearVelocity(Vector3f.ZERO);
        busControl.setAngularVelocity(Vector3f.ZERO);
        busControl.resetSuspension();
        return alpha;
    }

    @Override
    public void update(float tpf) {
        super.update(tpf);
        if (autopilot) {
            if (middleReached) {
                if (stopped <= 0) {
                    increaseSpeed(tpf);
                } else if (stopped > STOPTIME + 0.1f) {
                    decreaseSpeed(tpf);
                } else {
                    stopped -= tpf;
                }
            } else if (aktSpeed < SPEED) {
                increaseSpeed(tpf);
            }
            aktAlpha += alphastep * tpf;
            autoLoc.set(FastMath.cos(aktAlpha) * (radius + 3.75f), -4.9f, FastMath.sin(aktAlpha) * (radius + 3.75f));
            busControl.setPhysicsLocation(autoLoc);
            autoRot.fromAngles(0, -aktAlpha, 0);
            busControl.setPhysicsRotation(autoRot);
        }
    }

    private void increaseSpeed(float tpf) {
        aktSpeed += speedStep * tpf;
        if (aktSpeed > SPEED) {
            aktSpeed = SPEED;
        }
        alphastep = aktSpeed / radius;
    }

    private void decreaseSpeed(float tpf) {
        aktSpeed -= speedStep * tpf;
        if (aktSpeed < 0) {
            aktSpeed = 0;
            stopped = STOPTIME;
        }
        alphastep = aktSpeed / radius;
    }

    public void collision(PhysicsCollisionEvent event) {
        if (event.getObjectA() instanceof GhostControl) {
            ghostCollision(event.getNodeA());
        } else if (event.getObjectB() instanceof GhostControl) {
            ghostCollision(event.getNodeB());
        }
    }

    public void ghostCollision(Spatial ghost) {
        if (ghost.getName().equals("middleGhost")) {
            middleReached = true;
            if (centerReached) {
                nifty.gotoScreen("empty");
            }
            centerReached = false;
        } else if (ghost.getName().equals("outerGhost")) {
            middleReached = false;
            if (centerReached) {
                nifty.gotoScreen("empty");
            }
            centerReached = false;
            stopped = STOPTIME + 1;
        } else {
            if (!centerReached) {
                nifty.gotoScreen("hud");
                hudAppState.setStation(ghost);
            }
            centerReached = true;
        }
    }
}
