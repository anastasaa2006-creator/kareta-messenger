package ru.gr0946x.server.db.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nick", nullable = false, unique = true)
    private String nick;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    public User() {}

    public User(String nick, String passwordHash) {
        this.nick = nick;
        this.passwordHash = passwordHash;
    }

    public Long getId() { return id; }
    public String getNick() { return nick; }
    public void setNick(String nick) { this.nick = nick; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
}