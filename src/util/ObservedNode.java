/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package util;

import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.control.VehicleControl;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author Volker Schuller
 */
public class ObservedNode extends Node {

    private String type;
    private PhysicsRigidBody rigidBody;
    private ConcurrentHashMap<String, Spatial> changedSpatials;

    public ObservedNode(String name, ConcurrentHashMap<String, Spatial> changedSpatials, String type) {
        super(name);
        this.changedSpatials = changedSpatials;
        this.type = type;
    }

    public String getType() {
        return type;
    }

    @Override
    protected void setTransformRefresh() {
        super.setTransformRefresh();
        if (rigidBody == null) {
            rigidBody = getControl(RigidBodyControl.class);
            if (rigidBody == null) {
                rigidBody = getControl(VehicleControl.class);
            }
        } else if (rigidBody.isActive()) {
            changedSpatials.put(name, this);
        }
    }
}
