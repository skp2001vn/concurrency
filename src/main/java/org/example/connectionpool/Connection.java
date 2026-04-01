package org.example.connectionpool;

public class Connection {
    private final int id;

    /**
     * Creates a connection with the given identifier.
     *
     * @param id the connection identifier
     */
    public Connection(int id) {
        this.id = id;
    }

    /**
     * Returns the connection identifier.
     *
     * @return the connection identifier
     */
    public int getId() {
        return id;
    }

}
