package com.example.elo.service;

import com.example.elo.DTO.InnerDTO.CategoryDataNum;
import com.example.elo.repository.CategoryRepository;
import com.example.elo.repository.GroupRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    private GroupRepository groupRepository;
    private CategoryRepository categoryRepository;

    @Autowired
    public CategoryService(GroupRepository groupRepository, CategoryRepository categoryRepository){
        this.groupRepository=groupRepository;
        this.categoryRepository = categoryRepository;
    }

    public List<String> getCategoryName(String groupName){
        return groupRepository.getCategoryNameByGroupName(groupName);
    }
    public List<CategoryDataNum> getCategoryDataNum(String groupName){
        return categoryRepository.getCategoryDataByGroupId(groupName);
    }
}
