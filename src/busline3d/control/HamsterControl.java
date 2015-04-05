package busline3d.control;

import busline3d.message.AnimationMessage;
import busline3d.message.PassengerBusMessage;
import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.animation.AnimEventListener;
import com.jme3.animation.LoopMode;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.control.Control;
import util.appstate.ServerAppState;

/**
 *
 * @author Volker Schuller
 */
public class HamsterControl extends AbstractControl implements AnimEventListener {

    private BulletAppState bulletAppState;
    private ServerAppState serverAppState;
    private BetterCharacterControl physicsCharacter;
    private Vector3f viewDirection = new Vector3f(0, 0, 1);
    private AnimChannel channel;
    private Node hamster;
    private float brownNoise;

    public HamsterControl(BulletAppState bulletAppState, ServerAppState serverAppState) {
        this.bulletAppState = bulletAppState;
        this.serverAppState = serverAppState;
    }

    @Override
    public void setSpatial(Spatial spatial) {
        super.setSpatial(spatial);
        hamster = (Node) spatial;
        Node hamsternode = (Node) hamster.getChild("Hamster");
        if (hamsternode.getChild("Hamster") instanceof Node) {
            hamsternode = (Node) hamsternode.getChild("Hamster");
        }
        AnimControl anim = hamsternode.getControl(AnimControl.class);
        anim.addListener(this);
        channel = anim.createChannel();
        playAnim("walk");
        physicsCharacter = hamster.getControl(BetterCharacterControl.class);
        if (physicsCharacter == null) {
            physicsCharacter = new BetterCharacterControl(7f, 20f, 1000f);
            physicsCharacter.setJumpForce(Vector3f.UNIT_Y.mult(20000));
            hamster.addControl(physicsCharacter);
        }
        if (bulletAppState != null) {
            bulletAppState.getPhysicsSpace().add(physicsCharacter);
        }
    }

    @Override
    public Control cloneForSpatial(Spatial spatial) {
        HamsterControl newcontrol = new HamsterControl(bulletAppState, serverAppState);
        newcontrol.setSpatial(spatial);
        return newcontrol;
    }

    @Override
    protected void controlUpdate(float tpf) {
        if ("walk".equals(channel.getAnimationName())) {
            physicsCharacter.setWalkDirection(hamster.getWorldRotation().mult(Vector3f.UNIT_Z).mult(10));
            brownNoise += 0.5f - (float) Math.random();
            if (Math.random() < 0.05) {
                Quaternion randomRotate = new Quaternion().fromAngleAxis(FastMath.PI * tpf * brownNoise * 0.1f, Vector3f.UNIT_Y);
                randomRotate.multLocal(viewDirection);
                physicsCharacter.setViewDirection(viewDirection);
            }
        }
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
    }

    public void onAnimCycleDone(AnimControl control, AnimChannel channel, String animName) {
        if (physicsCharacter.isOnGround() && Math.random() < 0.1) {
            physicsCharacter.jump();
            playAnim("jump");
            channel.setSpeed(0.333f);
        } else {
            playAnim("walk");
        }
    }

    public void onAnimChange(AnimControl control, AnimChannel channel, String animName) {
    }

    private void playAnim(String animName) {
        if (serverAppState != null) {
            serverAppState.getServer().broadcast(new AnimationMessage("Hamster", animName, 0.333f).setReliable(true));
        }
        channel.setAnim(animName);
        channel.setLoopMode(LoopMode.DontLoop);
    }
}
