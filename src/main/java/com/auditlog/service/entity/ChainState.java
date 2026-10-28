package com.auditlog.service.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "chain_state")
public class ChainState {
    @Id
    private short id;
    @Column(nullable = false)
    private long lastSequence;
    @Column(nullable = false, length = 64)
    private String lastHash;

    protected ChainState() {
    }

    public ChainState(short id, long s, String h) {
        this.id = id;
        lastSequence = s;
        lastHash = h;
    }

    public long getLastSequence() {
        return lastSequence;
    }

    public String getLastHash() {
        return lastHash;
    }

    public void advance(long s, String h) {
        lastSequence = s;
        lastHash = h;
    }
}
