package com.example.vericert.domain;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="tenant")
public class Tenant {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;
    @Column(nullable=false)
    private String name;
    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    private Plan plan = Plan.FREE;
    @Column(nullable=false)
    private String status = "ACTIVE";
    @Column(name="created_at",nullable=false)
    private Instant createdAt = Instant.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Plan getPlan() {
        return plan;
    }

    public void setPlan(Plan plan) {
        this.plan = plan;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}