package com.example.elo.service;

import com.example.elo.repository.DataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DataService {

    private final DataRepository dataRepository;

    @Autowired
    public DataService(DataRepository dataRepository) {
        this.dataRepository=dataRepository;
    }

    public List<String> getDataName(String groupName){
        return dataRepository.findNameByGroupName(groupName);
    }
    public int getDataNum(String groupName, String categoryName){
        return dataRepository.countData(groupName,categoryName);
    }

    public List<String> getRandomData(String groupName, String categoryName,Integer number){
        return dataRepository.findRandomData(groupName,categoryName,number);
    }
}
