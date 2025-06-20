package com.example.elo.repository;

import com.example.elo.entity.DataEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface DataRepository extends JpaRepository<DataEntity,Integer> {

    @Query("SELECT de.name FROM DataEntity de WHERE de.groupEntity.groupName = :groupName")
    List<String> findNameByGroupName(@Param("groupName") String groupName);

    @Query("SELECT de FROM DataEntity de WHERE de.groupEntity.groupName = :groupName AND de.name = :name")
    DataEntity findByGroupNameAndName(@Param("groupName") String groupName, @Param("name") String name);

    @Query("SELECT COUNT(d) " +
            "FROM DataEntity d " +
            "JOIN d.groupEntity g " +
            "JOIN d.categoryDataRelation cdr " +
            "JOIN cdr.categoryEntity c " +
            "WHERE g.groupName = :groupName AND c.categoryName = :categoryName")
    int countData(@Param("groupName") String groupName,
                  @Param("categoryName") String categoryName);

    @Query(value = "SELECT dt.name " +
            "FROM datatable dt " +
            "JOIN categorydatarelation cdr ON dt.data_ID = cdr.data_ID " +
            "JOIN categories c ON c.category_ID = cdr.category_ID " +
            "JOIN groups g ON g.group_name = c.group_name " +
            "WHERE g.group_name = :groupName AND c.category_name = :categoryName " +
            "ORDER BY RAND() LIMIT :limit", nativeQuery = true)
    List<String> findRandomData(@Param("groupName") String groupName,
                                                          @Param("categoryName") String categoryName,
                                                          @Param("limit") int limit);
}
