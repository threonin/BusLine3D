package util.control;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.control.AbstractControl;
import com.jme3.system.Timer;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 *
 * @author Volker Schuller
 */
public class InterpolationControl extends AbstractControl {

    boolean active;
    private Vector3f nextTrans = new Vector3f();
    private Quaternion nextRot = new Quaternion();
    private float nexttime;
    private Vector3f deltaTrans = new Vector3f();
    private Quaternion deltaRot = new Quaternion();
    private ConcurrentLinkedQueue<LocRotTime> coming = new ConcurrentLinkedQueue<LocRotTime>();
    private Timer timer;

    public InterpolationControl(ConcurrentLinkedQueue<LocRotTime> coming, Timer timer) {
        this.coming = coming;
        this.timer = timer;
    }

    @Override
    protected void controlUpdate(float tpf) {
        if (!active) {
            nextStep();
            if (!active) {
                return;
            }
        }
        float time = this.timer.getTimeInSeconds();
        if (nexttime <= time) {
            nextStep();
        }
        if (active) {
            spatial.setLocalTranslation(deltaTrans.mult(time - nexttime).add(nextTrans));
            spatial.setLocalRotation(deltaRot.mult(time - nexttime).add(nextRot));
        }
    }

    private void nextStep() {
        LocRotTime locrottime = coming.poll();
        if (locrottime == null) {
            active = false;
            return;
        }
        float time = this.timer.getTimeInSeconds();
        deltaTrans.set(spatial.getLocalTranslation());
        deltaRot.set(spatial.getLocalRotation());
        float sampletime = time;
        while (nexttime <= time) {
            nextTrans.set(locrottime.getTranslation());
            nextRot.set(locrottime.getRot());
            nexttime = locrottime.getTime();
            if (nexttime <= time) {
                locrottime = coming.poll();
                if (locrottime == null) {
                    spatial.setLocalTranslation(nextTrans);
                    spatial.setLocalRotation(nextRot);
                    active = false;
                    return;
                }
                deltaTrans.set(nextTrans);
                deltaRot.set(nextRot);
                sampletime = nexttime;
            }
        }
        float speed = 1.0f / (sampletime - nexttime);
        deltaTrans.subtractLocal(nextTrans).multLocal(speed);
        deltaRot.subtractLocal(nextRot).multLocal(speed);
        active = true;
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
    }
}
