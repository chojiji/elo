package com.example.elo.repository;

import com.example.elo.DTO.EloData;
import com.example.elo.entity.CategoryDataRelation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryDataRelationRepository extends JpaRepository<CategoryDataRelation, Integer> {

    @Query("SELECT cdr FROM CategoryDataRelation cdr Join cdr.categoryEntity ce Join cdr.dataEntity de WHERE ce.groupEntity.groupName = :groupName AND ce.categoryName = :categoryName AND de.name = :name")
    CategoryDataRelation findByGroupNameAndCategoryNameAndDataName(@Param("groupName") String groupName, @Param("categoryName") String categoryName, @Param("name") String name);


    @Query("SELECT d.name, cdr.elo " +
            "FROM CategoryDataRelation cdr " +
            "JOIN cdr.dataEntity d " +
            "JOIN cdr.categoryEntity c " +
            "JOIN c.groupEntity g " +
            "WHERE g.groupName = :groupName " +
            "AND c.categoryName = :categoryName")
    List<Object[]> findRawDataNameAndEloByGroupAndCategory(
            @Param("groupName") String groupName,
            @Param("categoryName") String categoryName);

    @Query("SELECT cdr FROM CategoryDataRelation cdr " +
            "WHERE cdr.categoryEntity.groupEntity.groupName = :groupName " +
            "AND cdr.categoryEntity.categoryName = :categoryName " +
            "AND cdr.dataEntity.name = :dataName")
    Optional<CategoryDataRelation> findRelationByNames(
            @Param("groupName") String groupName,
            @Param("categoryName") String categoryName,
            @Param("dataName") String dataName);

}
