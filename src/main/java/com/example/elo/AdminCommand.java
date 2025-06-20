package com.example.elo;

import com.example.elo.service.AdminManagement;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

@Component
public class AdminCommand implements CommandLineRunner {

    private final AdminManagement adminManagement;

    public AdminCommand(AdminManagement adminManagement) {
        this.adminManagement = adminManagement;
    }

    @Override
    public void run(String... args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("명령어를 입력하세요.");
            String input = scanner.nextLine();
            switch (input) {
                case "1":
                    System.out.print("업데이트를 원하는 그룹이름을 입력하세요.(그룹)");
                    String groupName = scanner.nextLine();
                    adminManagement.upsert(groupName);
                    break;
                case "2":
                    System.out.print("삭제를 원하는 그룹이름을 입력하세요.(그룹)");
                    String groupName_2 = scanner.nextLine();
                    adminManagement.deleteGroup(groupName_2);
                    break;
                case "3":
                    System.out.print("삭제를 원하는 카테고리 이름을 입력하세요.");
                    String categoryName_3 = scanner.nextLine();
                    System.out.print("해당 카테고리가 속한 그룹의 이름을 입력하세요.");
                    String groupName_3 = scanner.nextLine();
                    adminManagement.deleteCategory(groupName_3,categoryName_3);
                    break;
                case "4":
                    System.out.print("삭제를 원하는 데이터 이름을 입력하세요.");
                    String dataName_4 = scanner.nextLine();
                    System.out.print("해당 데이터가 속한 그룹의 이름을 입력하세요.");
                    String groupName_4 = scanner.nextLine();
                    adminManagement.deleteData(groupName_4,dataName_4);
                    break;
                case "5":
                    System.out.print("삭제를 원하는 관계를 갖고있는 데이터 이름을 입력하세요.");
                    String dataName_5 = scanner.nextLine();
                    System.out.print("해당 데이터가 관계를 맺은 카테고리의 이름을 입력하세요.");
                    String categoryName_5 = scanner.nextLine();
                    System.out.print("해당 데이터가 속한 그룹의 이름을 입력하세요.");
                    String groupName_5 = scanner.nextLine();
                    adminManagement.deleteRelation(groupName_5,categoryName_5,dataName_5);
                    break;
                case "6":
                    System.out.print("추가를 원하는 카테고리가 속한 그룹의 이름을 입력하세요.");
                    String groupName_6 = scanner.nextLine();
                    System.out.print("추가를 원하는 카테고리의 이름을 입력하세요.");
                    String categoryName_6 = scanner.nextLine();
                    List<String> dataList=new ArrayList<>();
                    while (true){
                        String input_6 = scanner.nextLine();
                        if(input_6.equals("fin")){
                            break;
                        }
                        dataList.add(input_6);
                    }
                    adminManagement.addDataToCategory(groupName_6,categoryName_6,dataList);

                default:
                    System.out.println("잘못된 명령어입니다.");
            }
        }
    }
}
