package demo.amhsdatagen.model;

import java.time.LocalDateTime;

public class UserSession {
    private String userId;
    private boolean isLoggedIn;
    private boolean isSuperuser;
    private LocalDateTime lastActivity;

    public UserSession() {
        this.isLoggedIn = false;
        this.isSuperuser = false;
        this.lastActivity = LocalDateTime.now();
    }

    public UserSession(String userId) {
        this.userId = userId;
        this.isLoggedIn = true;
        this.isSuperuser = false;
        this.lastActivity = LocalDateTime.now();
    }

    public UserSession(String userId, boolean isSuperuser) {
        this.userId = userId;
        this.isLoggedIn = true;
        this.isSuperuser = isSuperuser;
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

    public boolean isSuperuser() {
        return isSuperuser;
    }

    public void setSuperuser(boolean superuser) {
        isSuperuser = superuser;
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
