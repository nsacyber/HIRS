package hirs.repository.spacewalk;


import java.io.Serializable;

/**
 * Wraps credentials for Spacewalk connections.
 */
public class Credentials implements Serializable {
    private String userName, password;

    /**
     * Initializes Credentials.
     * @param userName the username
     * @param password  the password
     */
    public Credentials(final String userName, final String password) {
        this.userName = userName;
        this.password = password;
    }

    /**
     * Sets the user name.
     * @param userName the username
     */
    public void setUserName(final String userName) {
        this.userName = userName;
    }

    /**
     * Gets the user name.
     * @return the username
     */
    public String getUserName() {
        return userName;
    }

    /**
     * Sets the password.
     * @param password the password
     */
    public void setPassword(final String password) {
        this.password = password;
    }

    /**
     * Gets the password.
     * @return the password
     */
    public String getPassword() {
        return password;
    }
}
