package com.example.elo.repository;

import com.example.elo.DTO.InnerDTO.CategoryDataNum;
import com.example.elo.entity.CategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<CategoryEntity,Integer> {

    @Query("SELECT ce FROM CategoryEntity ce WHERE ce.groupEntity.groupName = :groupName AND ce.categoryName = :categoryName")
    CategoryEntity findByGroupNameAndCategoryName(@Param("groupName") String groupName, @Param("categoryName") String categoryName);

    @Query("SELECT new com.example.elo.DTO.InnerDTO.CategoryDataNum(c.categoryName, COUNT(cdr)) " +
            "FROM CategoryEntity c " +
            "JOIN CategoryDataRelation cdr ON cdr.categoryEntity = c " +
            "WHERE c.groupEntity.groupName = :groupName " +
            "GROUP BY c.categoryName")
    List<CategoryDataNum> getCategoryDataByGroupId(@Param("groupName") String groupName);

}
