package eu.pb4.graves.network;

public class GraveClientState {
    public static volatile boolean graveCreated = false;

    public static void reset() {
        graveCreated = false;
    }
}
