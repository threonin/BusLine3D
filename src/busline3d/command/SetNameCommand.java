package busline3d.command;

import busline3d.appstate.BusClientAppState;
import busline3d.appstate.WorldAppState;
import busline3d.control.PassengerControl;
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
    private BusClientAppState busClientAppState;

    public SetNameCommand(WorldAppState worldAppState, Node node, String nameToAssign) {
        this.worldAppState = worldAppState;
        this.pnode = node;
        this.nameToAssign = nameToAssign;
    }

    public SetNameCommand(Node rootNode, WorldAppState worldAppState, BusClientAppState busClientAppState, String childName, String nameToAssign) {
        this.pnode = rootNode;
        this.worldAppState = worldAppState;
        this.busClientAppState = busClientAppState;
        this.childName = childName;
        this.nameToAssign = nameToAssign;
    }

    public boolean execute() {
        if (childName == null) {
            worldAppState.addLabel(pnode, nameToAssign);
            pnode.setUserData("stationname", nameToAssign);
            return true;
        }
        Node node = (Node) pnode.getChild(childName);
        if (node == null) {
            return false;
        } else {
            worldAppState.addLabel(node, nameToAssign);
            node.setUserData("stationname", nameToAssign);
            PassengerControl passengerControl = worldAppState.generatePassengerControlForStation(node);
            node.addControl(passengerControl);
            busClientAppState.setPassengerControl(passengerControl);
            return true;
        }
    }
}
