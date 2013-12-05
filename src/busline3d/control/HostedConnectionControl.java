package busline3d.control;

import com.jme3.network.HostedConnection;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.control.AbstractControl;

/**
 *
 * @author Volker Schuller
 */
public class HostedConnectionControl extends AbstractControl {

    private HostedConnection connection;

    public HostedConnectionControl(HostedConnection connection) {
        this.connection = connection;
    }

    public HostedConnection getConnection() {
        return connection;
    }

    public void setConnection(HostedConnection connection) {
        this.connection = connection;
    }

    @Override
    protected void controlUpdate(float tpf) {
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
    }
}
