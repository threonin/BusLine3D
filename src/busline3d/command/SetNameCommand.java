package busline3d.command;

import busline3d.appstate.WorldAppState;
import com.jme3.scene.Node;

/**
 *
 * @author Volker Schuller
 */
public class SetNameCommand implements Command {

    private Node pnode;
    private String childName;
    private String nameToAssign;
    private WorldAppState worldAppState;

    public SetNameCommand(WorldAppState worldAppState, Node node, String nameToAssign) {
        this.worldAppState = worldAppState;
        this.pnode = node;
        this.nameToAssign = nameToAssign;
    }

    public SetNameCommand(Node rootNode, WorldAppState worldAppState, String childName, String nameToAssign) {
        this.pnode = rootNode;
        this.worldAppState = worldAppState;
        this.childName = childName;
        this.nameToAssign = nameToAssign;
    }

    public boolean execute() {
        if (childName == null) {
            worldAppState.addLabel(pnode, nameToAssign);
            return true;
        }
        Node node = (Node) pnode.getChild(childName);
        if (node == null) {
            return false;
        } else {
            worldAppState.addLabel(node, nameToAssign);
            return true;
        }
    }
}
