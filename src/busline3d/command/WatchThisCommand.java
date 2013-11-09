package busline3d.command;

import com.jme3.scene.CameraNode;
import com.jme3.scene.Node;

/**
 *
 * @author Volker Schuller
 */
public class WatchThisCommand implements Command {

    private Node rootNode;
    private CameraNode camNode;
    private String name;

    public WatchThisCommand(Node rootNode, CameraNode camNode, String name) {
        this.rootNode = rootNode;
        this.camNode = camNode;
        this.name = name;
    }

    public boolean execute() {
        Node node = (Node) rootNode.getChild(name);
        if (node == null) {
            return false;
        }
        node.attachChild(camNode);
        return true;
    }
}
