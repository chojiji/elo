package com.example.elo.repository;

import com.example.elo.DTO.GroupInfo;
import com.example.elo.entity.GroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupRepository extends JpaRepository<GroupEntity,String>{


    @Query("SELECT new com.example.elo.DTO.GroupInfo(ge.groupName,ge.description) FROM GroupEntity ge")
    List<GroupInfo> findGroupNameAndDescription();

    @Query("SELECT ce.categoryName FROM GroupEntity ge JOIN ge.categoryEntity ce WHERE ge.groupName = :groupName")
    List<String> getCategoryNameByGroupName(@Param("groupName") String groupName);

}
