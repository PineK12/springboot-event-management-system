# springboot-event-management-system
A Spring Boot web application for event management with role-based access control and QR code attendance.

# Vadoo – Event Management System

## Description
Vadoo is a web-based event management system built with Spring Boot and Thymeleaf.
The system supports multiple user roles including Admin, Organizer, and Student,
each with specific permissions and functionalities.

## User Roles & Features

### Admin
- Manage all events
- Review and approve events created by organizers

### Organizer
- Create and manage events
- Generate QR codes for each event
- Scan QR codes to mark student attendance

### Student
- View and register for approved events
- Receive a unique QR code for each registered event
- Use QR code for attendance check-in

## Key Features
- Role-based access control
- Event approval workflow
- QR code generation and scanning
- Attendance management per event

## Tech Stack
- Java
- Spring Boot
- Thymeleaf
- MySQL
- HTML, CSS, JavaScript

## Role
- Backend Developer / Full-stack Developer (Personal Project)

## Run Locally
1. Clone the repository
2. Configure MySQL connection in `application.yml`
3. Run the application using Spring Boot
4. Access the application at `http://localhost:8080`

## Learning Outcomes
- Implemented role-based authorization in Spring Boot
- Designed event approval workflow
- Integrated QR code generation and scanning
- Worked with Thymeleaf for server-side rendering
