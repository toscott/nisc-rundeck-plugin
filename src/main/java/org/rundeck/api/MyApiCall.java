package org.rundeck.api;

public class MyApiCall extends ApiCall {
    /**
     * Build a new instance, linked to the given RunDeck client
     *
     * @param client holding the RunDeck url and the credentials
     * @throws IllegalArgumentException
     *          if client is null
     */
    public MyApiCall(RundeckClient client) throws IllegalArgumentException {
        super(client);
    }
}
