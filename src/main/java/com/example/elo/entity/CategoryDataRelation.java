package com.example.elo.entity;

import jakarta.persistence.*;
import lombok.*;


@Entity
@Getter
@Setter
@Table(name = "categorydatarelation", uniqueConstraints = {@UniqueConstraint(columnNames = {"category_ID", "data_ID"})})
public class CategoryDataRelation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "relation_ID")
    private Integer relationID;

    @ManyToOne
    @JoinColumn(name = "category_ID")
    private CategoryEntity categoryEntity;

    @ManyToOne
    @JoinColumn(name = "data_ID")
    private DataEntity dataEntity;

    private Integer elo;

    @Version
    private Integer version;
}
