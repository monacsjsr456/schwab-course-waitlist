package com.schwab.registration;

import java.util.*;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("==================================================================");
        System.out.println("   CHARLES SCHWAB ENTERPRISE COURSE REGISTRATION SYSTEM PLATFORM  ");
        System.out.println("==================================================================");
        
        // 1. Interactive Setup
        System.out.print("Enter Course Code (e.g., CS102): ");
        String courseCode = scanner.nextLine().trim();
        
        System.out.print("Enter Course Seat Capacity: ");
        int capacity = Integer.parseInt(scanner.nextLine().trim());
        
        System.out.print("Enter Required Prerequisites (comma separated, or press Enter for none): ");
        String prereqInput = scanner.nextLine().trim();
        Set<String> prerequisites = new HashSet<>();
        if (!prereqInput.isEmpty()) {
            for (String p : prereqInput.split(",")) {
                prerequisites.add(p.trim());
            }
        }
        
        // Initialize the Enterprise Service Engine
        CourseOfferingService service = new CourseOfferingService(courseCode, capacity, prerequisites);
        Map<String, CourseOfferingService.LearnerProfile> studentRegistry = new HashMap<>();
        
        System.out.println("\n🚀 Service System Initialized Successfully!");
        System.out.println("Course: " + courseCode + " | Capacity: " + capacity + " | Prerequisites: " + prerequisites);
        
        // 2. Interactive Operation Loop
        while (true) {
            System.out.println("\n==================================================================");
            System.out.println("CHOOSE AN ACTION: [1] Enroll Student  [2] Drop Student  [3] View State  [4] Exit");
            System.out.print("Enter choice (1-4): ");
            String choice = scanner.nextLine().trim();
            
            if ("4".equals(choice)) {
                System.out.println("Exiting System Simulator. Thank you!");
                break;
            }
            
            switch (choice) {
                case "1":
                    System.out.print("Enter Student ID / Name: ");
                    String studentId = scanner.nextLine().trim();
                    
                    System.out.print("Enter Student's Completed Prerequisites (comma separated, or press Enter): ");
                    String studentPrereqInput = scanner.nextLine().trim();
                    Set<String> studentPrereqs = new HashSet<>();
                    if (!studentPrereqInput.isEmpty()) {
                        for (String p : studentPrereqInput.split(",")) {
                            studentPrereqs.add(p.trim());
                        }
                    }
                    
                    // Create and register profile state
                    CourseOfferingService.LearnerProfile profile = new CourseOfferingService.LearnerProfile(studentId, studentPrereqs);
                    studentRegistry.put(studentId, profile);
                    
                    try {
                        String result = service.enroll(profile);
                        System.out.println("➔ SYSTEM RESULT: SUCCESS - Status is now [" + result + "]");
                    } catch (Exception e) {
                        System.out.println("➔ SYSTEM REJECTION: " + e.getMessage());
                    }
                    break;
                    
                case "2":
                    System.out.print("Enter Student ID to Drop: ");
                    String dropId = scanner.nextLine().trim();
                    
                    try {
                        String result = service.drop(dropId, studentRegistry);
                        System.out.println("➔ SYSTEM RESULT: SUCCESS - Action code [" + result + "]");
                    } catch (Exception e) {
                        System.out.println("➔ SYSTEM REJECTION: " + e.getMessage());
                    }
                    break;
                    
                case "3":
                    System.out.println("\n------------------------------------------------");
                    System.out.println("📊 LIVE STATE MONITOR FOR " + courseCode + ":");
                    System.out.println("   Active Enrolled Seats : " + service.getEnrolledCount() + " / " + capacity);
                    System.out.println("   Waitlist Line Size   : " + service.getWaitlistCount());
                    System.out.println("   FIFO Waitlist Order  : " + service.getWaitlistOrder());
                    System.out.println("------------------------------------------------");
                    break;
                    
                default:
                    System.out.println("Invalid choice. Please pick between 1 and 4.");
            }
        }
        scanner.close();
    }
}

