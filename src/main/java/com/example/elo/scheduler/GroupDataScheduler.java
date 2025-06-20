package com.example.elo.scheduler;

import com.example.elo.DTO.GroupInfo;
import com.example.elo.service.DataService;
import com.example.elo.service.GroupService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GroupDataScheduler {

    private final GroupService groupService;
    private final DataService dataService;
    private final Map<String, String> groupRepresentativeData = new ConcurrentHashMap<>();

    @Autowired
    public GroupDataScheduler(GroupService groupService, DataService dataService) {
        this.groupService = groupService;
        this.dataService = dataService;
    }

    @PostConstruct
    public void initializeGroupData() {
        updateAllGroupData();
    }

    @Scheduled(fixedRate = 600000)
    public void updateAllGroupData() {
        List<GroupInfo> groupInfoList = groupService.getGroupInfo();
        for (GroupInfo groupInfo : groupInfoList) {
            String groupName=groupInfo.getGroupName();
            List<String> representativeData = dataService.getRandomData(groupName,groupName+"(전체)",1);
            groupRepresentativeData.put(groupName,representativeData.get(0));
        }
    }

    public String getRepresentativeData(String groupName){
        return groupRepresentativeData.get(groupName);
    }
}
