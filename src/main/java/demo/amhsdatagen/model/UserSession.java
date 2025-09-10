package demo.amhsdatagen.model;

import java.time.LocalDateTime;

public class UserSession {
    private String userId;
    private boolean isLoggedIn;
    private LocalDateTime lastActivity;

    public UserSession() {
        this.isLoggedIn = false;
        this.lastActivity = LocalDateTime.now();
    }

    public UserSession(String userId) {
        this.userId = userId;
        this.isLoggedIn = true;
        this.lastActivity = LocalDateTime.now();
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public boolean isLoggedIn() {
        return isLoggedIn;
    }

    public void setLoggedIn(boolean loggedIn) {
        isLoggedIn = loggedIn;
    }

    public LocalDateTime getLastActivity() {
        return lastActivity;
    }

    public void setLastActivity(LocalDateTime lastActivity) {
        this.lastActivity = lastActivity;
    }

    public void updateLastActivity() {
        this.lastActivity = LocalDateTime.now();
    }

    public void logout() {
        this.userId = null;
        this.isLoggedIn = false;
    }
}
