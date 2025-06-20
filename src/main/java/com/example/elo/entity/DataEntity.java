package com.example.elo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "datatable", uniqueConstraints = {@UniqueConstraint(columnNames = {"groupName", "name"})})
@Getter
@Setter
public class DataEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="data_ID")
    private Integer dataID;

    @Column(length = 60)
    private String name;

    @ManyToOne
    @JoinColumn(name = "group_name")
    private GroupEntity groupEntity;

    @OneToMany(mappedBy = "dataEntity", cascade = {CascadeType.MERGE, CascadeType.REMOVE})
    private List<CategoryDataRelation> categoryDataRelation = new ArrayList<>();
}
