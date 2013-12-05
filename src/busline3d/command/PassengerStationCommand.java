package busline3d.command;

import busline3d.appstate.BusClientAppState;
import busline3d.control.PassengerControl;
import busline3d.message.PassengerStationMessage;

/**
 *
 * @author Volker Schuller
 */
public class PassengerStationCommand implements Command {

    private BusClientAppState busClientAppState;
    private PassengerStationMessage psm;

    public PassengerStationCommand(BusClientAppState busClientAppState, PassengerStationMessage psm) {
        this.busClientAppState = busClientAppState;
        this.psm = psm;
    }

    public boolean execute() {
        PassengerControl passengerControl = busClientAppState.getPassengerControl();
        if (passengerControl == null) {
            return false;
        }
        if (psm.getName() == null) {
            passengerControl.removePassenger(psm.getIndex());
        } else {
            passengerControl.addPassenger(psm.getIndex(), psm.getName());
        }
        return true;
    }
}
