public class TransportUser {
    private String username;
    private String passwordHash;
    private String salt;
    private String fullName;
    private String role;

    public TransportUser(String username, String passwordHash, String salt, String fullName, String role) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.salt = salt;
        this.fullName = fullName;
        this.role = role;
    }

    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getSalt() { return salt; }
    public String getFullName() { return fullName; }
    public String getRole() { return role; }

    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setRole(String role) { this.role = role; }
}

