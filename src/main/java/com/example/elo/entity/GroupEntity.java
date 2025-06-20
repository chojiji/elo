package com.example.elo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;


@Entity
@Getter
@Setter
@Table(name = "groups")
public class GroupEntity {
    @Id
    @Column(length = 60)
    private String groupName;

    @Column(length = 150)
    private String description;

    @OneToMany(mappedBy = "groupEntity", cascade = {CascadeType.MERGE, CascadeType.REMOVE})
    private List<DataEntity> dataEntity = new ArrayList<>();

    @OneToMany(mappedBy = "groupEntity", cascade = {CascadeType.MERGE, CascadeType.REMOVE})
    private List<CategoryEntity> categoryEntity = new ArrayList<>();
}
