package com.example.elo.service;


import com.example.elo.entity.CategoryDataRelation;
import com.example.elo.entity.CategoryEntity;
import com.example.elo.entity.DataEntity;
import com.example.elo.entity.GroupEntity;
import com.example.elo.repository.CategoryDataRelationRepository;
import com.example.elo.repository.CategoryRepository;
import com.example.elo.repository.DataRepository;
import com.example.elo.repository.GroupRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.List;


@Service
public class AdminManagement {
    final String path = "C:/elo/src/main/resources/images/";
    private final GroupRepository groupRepository;
    private final DataRepository dataRepository;
    private final CategoryRepository categoryRepository;
    private final CategoryDataRelationRepository categoryDataRelationRepository;

    @PersistenceContext
    private EntityManager entityManager;


    @Autowired
    public AdminManagement(GroupRepository groupRepository,
                             DataRepository dataRepository,
                             CategoryRepository categoryRepository,
                             CategoryDataRelationRepository categoryDataRelationRepository) {
        this.groupRepository = groupRepository;
        this.dataRepository = dataRepository;
        this.categoryRepository = categoryRepository;
        this.categoryDataRelationRepository = categoryDataRelationRepository;
    }

    @Transactional
    public void upsert(String groupName){
        String filePath=path+groupName;
        File folder = new File(filePath);
        if (folder==null){
            System.out.println("imageset of "+groupName+" doesn't exists.");
            return;
        }
        GroupEntity groupEntity=groupEntityElseGet(groupName);
        CategoryEntity categoryEntity = categoryEntityElseGet(groupEntity,groupName,groupName+"(전체)");
        File[] files = folder.listFiles();
        for (File file:files){
            String dataName=file.getName().split("\\.")[0];
            DataEntity dataEntity = dataEntityElseGet(groupEntity,groupName,dataName);
            CategoryDataRelation categoryDataRelation = categoryDataRelationElseGet(categoryEntity,dataEntity,groupName,groupName+"(전체)",dataName);
        }
    }

    @Transactional
    public void addDataToCategory(String groupName, String categoryName, List<String> dataList){
        String filePath=path+groupName;
        File folder = new File(filePath);
        if (folder==null){
            System.out.println("imageset of "+groupName+" doesn't exists.");
            return;
        }
        GroupEntity groupEntity=groupEntityElseGet(groupName);
        CategoryEntity categoryEntity = categoryEntityElseGet(groupEntity,groupName,categoryName);
        for(String data:dataList){
            DataEntity dataEntity = dataEntityElseGet(groupEntity,groupName,data);
            CategoryDataRelation categoryDataRelation = categoryDataRelationElseGet(categoryEntity,dataEntity,groupName,categoryName,data);
        }
    }
    private GroupEntity groupEntityElseGet(String groupName){
        return groupRepository.findById(groupName)
                .map(existingGroupEntity -> {
                    System.out.println("Group named " + groupName + " already exists.");
                    return existingGroupEntity;
                })
                .orElseGet(() -> {
                    GroupEntity newGroupEntity = new GroupEntity();
                    newGroupEntity.setGroupName(groupName);
                    newGroupEntity.setDescription(groupName + " 모음입니다.");
                    entityManager.persist(newGroupEntity);
                    return newGroupEntity;
                });
    }
    private CategoryEntity categoryEntityElseGet(GroupEntity groupEntity,String groupName, String categoryName){
        CategoryEntity categoryEntity = categoryRepository.findByGroupNameAndCategoryName(groupName,categoryName);
        if (categoryEntity==null){
            categoryEntity = new CategoryEntity();
            categoryEntity.setCategoryName(categoryName);
        }
        else{System.out.println("Category named " + categoryName + " already exists.");}
        categoryEntity.setGroupEntity(groupEntity);
        groupEntity.getCategoryEntity().add(categoryEntity);
        entityManager.persist(categoryEntity);
        return categoryEntity;
    }
    private DataEntity dataEntityElseGet(GroupEntity groupEntity,String groupName,String dataName){
        DataEntity dataEntity = dataRepository.findByGroupNameAndName(groupName, dataName);
        if (dataEntity == null) {
            dataEntity = new DataEntity();
            dataEntity.setName(dataName);
        }
        else{System.out.println("Data named " + dataName + " already exists.");}
        dataEntity.setGroupEntity(groupEntity);
        groupEntity.getDataEntity().add(dataEntity);
        entityManager.persist(dataEntity);
        return dataEntity;
    }
    private CategoryDataRelation categoryDataRelationElseGet(CategoryEntity categoryEntity, DataEntity dataEntity, String groupName,String categoryName,String dataName){
        CategoryDataRelation categoryDataRelation = categoryDataRelationRepository.findByGroupNameAndCategoryNameAndDataName(groupName,categoryName,dataName);
        if (categoryDataRelation==null){
            categoryDataRelation = new CategoryDataRelation();
            categoryDataRelation.setElo(1500);
        }
        else{System.out.println("At "+groupName+", relation with "+categoryName+", "+dataName+" already exists.");}
        categoryEntity.getCategoryDataRelation().add(categoryDataRelation);
        categoryDataRelation.setCategoryEntity(categoryEntity);
        categoryDataRelation.setDataEntity(dataEntity);
        dataEntity.getCategoryDataRelation().add(categoryDataRelation);
        entityManager.persist(categoryDataRelation);
        return categoryDataRelation;
    }

    public void deleteGroup(String groupName){
        if (groupRepository.existsById(groupName)) {
            groupRepository.deleteById(groupName);
        } else {
            System.out.println("group named " + groupName + " doesn't exist.");
        }
    }
    public void deleteCategory(String groupName,String categoryName){

        CategoryEntity existingCategoryEntity = categoryRepository.findByGroupNameAndCategoryName(groupName,categoryName);
        if (existingCategoryEntity==null){
            System.out.println("at "+groupName+", category named "+categoryName+" doesn't exist.");
            return;
        }
        categoryRepository.delete(existingCategoryEntity);
    }

    public void deleteRelation(String groupName, String categoryName,String name){
        CategoryDataRelation existingCategoryDataRelation = categoryDataRelationRepository.findByGroupNameAndCategoryNameAndDataName(groupName,categoryName,name);
        if(existingCategoryDataRelation==null){
            System.out.println("at "+groupName+", relation with "+categoryName+", "+name+" doesn't exist.");
            return;
        }
        categoryDataRelationRepository.delete(existingCategoryDataRelation);

    }
    public void deleteData(String groupName,String name){
        DataEntity existingDataEntity = dataRepository.findByGroupNameAndName(groupName,name);
        if (existingDataEntity==null){
            System.out.println("at "+groupName+", data named "+name+" doesn't exist.");
            return;
        }
        dataRepository.delete(existingDataEntity);
    }

}
