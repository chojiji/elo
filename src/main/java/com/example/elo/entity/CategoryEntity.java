package com.example.elo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "categories",uniqueConstraints = {@UniqueConstraint(columnNames = {"categoryName", "groupName"})})
public class CategoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_ID")
    private Integer categoryID;

    @Column(length = 72)
    private String categoryName;

    @ManyToOne
    @JoinColumn(name = "group_name")
    private GroupEntity groupEntity;

    @OneToMany(mappedBy = "categoryEntity", cascade = {CascadeType.MERGE, CascadeType.REMOVE})
    private List<CategoryDataRelation> categoryDataRelation = new ArrayList<>();
}

