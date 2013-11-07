package busline3d.appstate;

import busline3d.control.BusControl;
import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.shadow.EdgeFilteringMode;
import com.jme3.texture.Texture;

/**
 *
 * @author Volker Schuller
 */
public class SunAppState extends AbstractAppState {

    private SimpleApplication app;
    private Node rootNode;
    private Geometry sun;
    private DirectionalLight sunlight = new DirectionalLight();
    private DirectionalLightShadowRenderer dlsr;
    private Vector3f sunvector = new Vector3f();
    private boolean sunthere = true;
    private BusControl control;
    private float sunradius;

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);
        this.app = (SimpleApplication) app;
        this.rootNode = this.app.getRootNode();
        control = this.rootNode.getChild("Bus").getControl(BusControl.class);
        AssetManager assetManager = this.app.getAssetManager();

        sunvector.set(0, -1f, 0).normalizeLocal();
        sunlight.setDirection(sunvector);
        rootNode.addLight(sunlight);

        final int SHADOWMAP_SIZE = 1024;
        dlsr = new DirectionalLightShadowRenderer(assetManager, SHADOWMAP_SIZE, 3);
        dlsr.setLight(sunlight);
        dlsr.setShadowIntensity(0.5f);
        dlsr.setEdgeFilteringMode(EdgeFilteringMode.PCFPOISSON);
        this.app.getViewPort().addProcessor(dlsr);

        Quad sunQuad = new Quad(100, 100);
        sun = new Geometry("sun", sunQuad);
        Material sunMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        Texture streettex = assetManager.loadTexture("Textures/sun/sun.png");
        sunMat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        sunMat.setTexture("ColorMap", streettex);
        sun.setQueueBucket(RenderQueue.Bucket.Transparent);
        sun.setMaterial(sunMat);
        sun.setShadowMode(RenderQueue.ShadowMode.Off);
        sun.setLocalTranslation(0, 460, 0);
        sun.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);
        rootNode.attachChild(sun);
    }

    @Override
    public void update(float tpf) {
        float time = app.getTimer().getTimeInSeconds();
        float daycos = FastMath.cos(time / 50);
        if (daycos > 0) {
            if (!sunthere) {
                control.switchLights(false);
                rootNode.attachChild(sun);
                this.app.getViewPort().addProcessor(dlsr);
                rootNode.addLight(sunlight);
                sunthere = true;
            }
            float daysin = FastMath.sin(time / 50);
            sunlight.setColor(ColorRGBA.White.mult(daycos + 0.7f));
            sunvector.set(daysin, -daycos - 0.05f, 0).normalizeLocal();
            sunlight.setDirection(sunvector);
            sun.setLocalTranslation(-daysin * sunradius, daycos * sunradius - 150, 0);
            sun.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);
        } else {
            if (sunthere) {
                control.switchLights(true);
                sun.removeFromParent();
                rootNode.removeLight(sunlight);
                this.app.getViewPort().removeProcessor(dlsr);
                sunthere = false;
            }
        }
    }

    public void setRadius(float radius) {
        this.sunradius = radius + 260;
    }

    @Override
    public void cleanup() {
        sun.removeFromParent();
        rootNode.removeLight(sunlight);
        this.app.getViewPort().removeProcessor(dlsr);
        sunthere = false;
        super.cleanup();
    }
}
