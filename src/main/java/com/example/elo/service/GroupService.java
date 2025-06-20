package com.example.elo.service;

import com.example.elo.DTO.GroupInfo;
import com.example.elo.repository.DataRepository;
import com.example.elo.repository.GroupRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GroupService {
    private GroupRepository groupRepository;

    private Map<String, String> groupRepresentativeData = new ConcurrentHashMap<>();

    @Autowired
    public GroupService(GroupRepository groupRepository){
        this.groupRepository=groupRepository;
    }


    public List<GroupInfo> getGroupInfo(){
        List<GroupInfo>groupNameAndDescription=groupRepository.findGroupNameAndDescription();
        return groupNameAndDescription;
    }
}
