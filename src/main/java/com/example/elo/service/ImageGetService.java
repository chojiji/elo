package com.example.elo.service;

import com.example.elo.DTO.GroupInfo;
import com.example.elo.scheduler.GroupDataScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
@Service
public class ImageGetService {

    GroupDataScheduler groupDataScheduler;

    @Autowired
    public ImageGetService(GroupDataScheduler groupDataScheduler){
        this.groupDataScheduler=groupDataScheduler;
    }

    public void getMainImageToDTO(List<GroupInfo> groupInfo,boolean isThumbnails) {
        String baseURL="";
        if (isThumbnails){
            baseURL="thumbnails/";
        }
        for (GroupInfo group:groupInfo){
            String groupName= group.getGroupName();
            String dataName= groupDataScheduler.getRepresentativeData(groupName);
            String imageURL=baseURL+groupName+"/"+dataName+".png";
            group.setMainImage(imageURL);
        }
    }
}
