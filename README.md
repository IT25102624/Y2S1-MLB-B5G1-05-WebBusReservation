# 🚍 Web-Based Bus Ticket Reservation System

> **Course:** SE2030 - Software Engineering (Year 2, Semester 1 - 2026)  
> **Group ID:** `2026-Y2-S1-MLB-B5G1-05`  
> **Institution:** Sri Lanka Institute of Information Technology (SLIIT)

---

## 📌 Project Overview
The **Web-based Bus Ticket Reservation System** is a modern, centralized digital platform designed to automate and streamline the end-to-end bus ticketing process. Developed as a robust Java-based web application, the system replaces traditional manual ticketing operations—such as terminal queueing, paper tickets, and verbal seat inquiries—with an efficient, real-time solution for passengers, conductors, fleet operators, and administrators.

---

## 🚀 Key Features & Functional Modules

### 👤 1. Identity, Role & Conductor Boarding Management
* **Assigned Student:** Prabhashwara P. L. D. N. (`IT25100756`)
* **Primary Users:** Passenger, Conductor & System Admin
* **Description:** Manages secure registration, user profiles, and Role-Based Access Control (RBAC). Provides conductors with a live interface to view route manifests, track boarding stops, and verify digital tickets.

### 🔍 2. Search Bus Schedules & Routes
* **Assigned Student:** Imanjith R. D. D. (`IT25102624`)
* **Primary User:** Passenger / Customer
* **Description:** Allows passengers to query active bus routes with dynamic filters including departure location, destination, travel dates, departure times, fares, and bus types.

### 💺 3. Seat Selection & Real-Time Booking
* **Assigned Student:** Supun N. E. G. A. (`IT25101791`)
* **Primary User:** Passenger / Customer
* **Description:** Features an interactive seating map with temporary seat-locking mechanisms and concurrency controls to prevent double-booking.

### 💳 4. Payment Processing, E-Ticketing & Refunds
* **Assigned Student:** Gunawardhana R. D. S. A. (`IT25103561`)
* **Primary User:** Passenger / Customer
* **Description:** Handles secure online payment simulations, generates verifiable digital E-tickets with QR codes, and automates time-based refund processing upon cancellation.

### 🔔 5. Notifications, Feedback & Luggage Tracking
* **Assigned Student:** Wanniarachchi P. S. (`IT25103670`)
* **Primary User:** Passenger / Customer
* **Description:** Sends automated SMS/Email updates, manages post-journey ratings, and assigns unique digital luggage tags linked to E-tickets to prevent lost baggage.

### 📊 6. Business Analytics & Executive Dashboard
* **Assigned Student:** Shehanga H. L. S. (`IT25101677`)
* **Primary Users:** System Administrator / Fleet Manager
* **Description:** Provides interactive administrative analytics tracking overall revenue, peak travel windows, route popularity, driver performance, and customer satisfaction.

---

## ⚙️ Non-Functional Highlights
* **Performance:** Swift response times (2–3 seconds) supporting concurrent user sessions.
* **Reliability & Concurrency:** Robust transaction handling to guarantee zero duplicate seat reservations.
* **Security:** Encrypted user credentials, safe authentication protocols, and HTTPS support.
* **Usability:** Fully responsive layout designed for desktop, tablet, and mobile web browsers.

---

## 👥 Engineering Team

| IT Number | Student Name | Assigned Module |
| :--- | :--- | :--- |
| **IT25100756** | Prabhashwara P. L. D. N. | Role Management & Conductor Boarding |
| **IT25102624** | Imanjith R. D. D. | Search Schedules & Routes |
| **IT25101791** | Supun N. E. G. A. | Seat Selection & Reservations |
| **IT25103561** | Gunawardhana R. D. S. A. | Payments, E-Tickets & Refunds |
| **IT25103670** | Wanniarachchi P. S. | Notifications, Reviews & Luggage Tagging |
| **IT25101677** | Shehanga H. L. S. | Business Analytics & Dashboards |

---

## 🛠️ Technology Stack & Architecture

- **Backend Framework:** Spring Boot 3.3.3
- **Language / Runtime:** Java 21 (LTS)
- **Persistence & ORM:** Spring Data JPA / Hibernate 6.5
- **Security:** Spring Security 6 & BCrypt
- **Database:** Microsoft SQL Server (SSMS)
- **Build Tool:** Apache Maven (with Maven Wrapper included)

---

## 💻 Developer Getting Started Guide

### 1. Prerequisites
- **JDK 21** installed and configured in `JAVA_HOME`.
- **Microsoft SQL Server (Express / Developer)** and **SQL Server Management Studio (SSMS)** running locally on port `1433`.
- **IntelliJ IDEA** (recommended) or VS Code.

### 2. Database Setup (SSMS)
1. Open SSMS and create a database named `BusReservationDB`:
   ```sql
   CREATE DATABASE BusReservationDB;
   ```
2. Open and execute [`src/main/resources/schema-sqlserver.sql`](src/main/resources/schema-sqlserver.sql) to initialize the 3NF schema, tables, and constraints.

### 3. Application Configuration
1. In `src/main/resources/`, copy `application.yml.example` to `application.yml`:
   - **Windows PowerShell:**
     ```powershell
     Copy-Item src/main/resources/application.yml.example src/main/resources/application.yml
     ```
   - **Bash / macOS / Linux:**
     ```bash
     cp src/main/resources/application.yml.example src/main/resources/application.yml
     ```
2. In your local `application.yml`, update your SQL Server credentials (`username` and `password`).

### 4. Build and Run
- **Compile and verify dependencies:**
  - Windows:
    ```powershell
    .\mvnw.cmd clean compile
    ```
  - macOS / Linux:
    ```bash
    ./mvnw clean compile
    ```
- **Run the Spring Boot application:**
  - Windows:
    ```powershell
    .\mvnw.cmd spring-boot:run
    ```
  - macOS / Linux:
    ```bash
    ./mvnw spring-boot:run
    ```
- **Base API URL:** `http://localhost:8080/api/v1`

---

## 📅 Project Implementation Roadmap

* **Week 3:** Project Proposal & Scope Finalization
* **Week 4-5:** System Design, UML Diagrams & Database Modeling
* **Week 6-7:** Sprint 1 — Core Architecture, Auth & Route Booking
* **Week 8-9:** Sprint 2 — Payment Processing, E-Ticketing & Admin Dashboard
* **Week 10:** Phase 2 Evaluation (75% Working System Demo)
* **Week 11-12:** Integration, End-to-End Testing & Bug Fixes
* **Week 13:** Final Presentation & Viva
* **Week 14:** Final Code Refactoring & Documentation Submission
