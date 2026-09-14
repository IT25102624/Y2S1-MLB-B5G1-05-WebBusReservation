@echo off
title Bus Reservation System
echo ==========================================================
echo Starting Bus Ticket Reservation System (Optimized JVM)
echo Port: 8080 ^| Context Path: /api/v1
echo ==========================================================
cd /d "%~dp0"
java -Xmx384m -Xms128m -jar target\bus-ticket-reservation-system-1.0.0-SNAPSHOT.jar
pause
