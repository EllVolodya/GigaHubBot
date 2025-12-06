package org.example;

import java.util.HashMap;
import java.util.Map;

public class StateManager {

    private final Map<Long, String> states = new HashMap<>();

    // Set state
    public void setState(Long userId, String state) {
        states.put(userId, state);
        System.out.println("[STATE] Set userId=" + userId + " → " + state);
    }

    // Get state
    public String getState(Long userId) {
        return states.get(userId);
    }

    // Clear state
    public void clearState(Long userId) {
        states.remove(userId);
        System.out.println("[STATE] Cleared state for userId=" + userId);
    }

    // Debug print
    public void printState(Long userId) {
        System.out.println("[STATE] userId=" + userId + " | state=" + states.get(userId));
    }
}
