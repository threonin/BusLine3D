package util;

import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

/**
 *
 * @author Volker Schuller
 */
public class UtilFunctions {

    private UtilFunctions() {
    }

    public static Geometry findGeom(Spatial spatial, String name, boolean found) {
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
}
