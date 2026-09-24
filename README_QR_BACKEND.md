# CatchBusLK – QR Staff Authentication + Schedule/Live Bus Module

This package extends the supplied CatchBusLK frontend with a Spring Boot backend for:

- Conductor-specific QR scan + login
- Driver-specific QR scan + login
- JWT authentication
- Role-based access control
- User registration/login validation
- Staff assignment to a trip
- Staff schedule returned immediately after authentication
- Previous bus on the same route
- Latest live GPS position of that preceding bus
- GPS sharing from the current bus so the following bus can see it
- Existing conductor passenger/booking boarding endpoint
- SQL Server integration using the supplied `BusReservationDB` structure

## Stack

Frontend: React 18 + Vite + Tailwind  
Backend: Java 21 + Spring Boot 3.3.13  
Database: Microsoft SQL Server  
Authentication: BCrypt + JWT

## 1. Database

The original database script is in:

`database/script.sql`

Run it in SQL Server first.

Then run:

`database/module_schema.sql`

The module schema adds only the tables required for the new staff/QR/live-location functionality:

- `staff_profiles`
- `staff_assignments`
- `bus_location_updates`
- `operations_messages`

It does not replace the existing booking, bus, route, trip, seat or user tables.

## 2. Backend configuration

Open:

`backend/src/main/resources/application.properties`

Set your SQL Server credentials:

```properties
DB_URL=jdbc:sqlserver://localhost:1433;databaseName=BusReservationDB;encrypt=true;trustServerCertificate=true
DB_USERNAME=sa
DB_PASSWORD=YOUR_SQL_SERVER_PASSWORD
```

You can also use environment variables instead of editing the file:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
JWT_SECRET
CORS_ALLOWED_ORIGIN
DEMO_SEED
SERVER_PORT
```

The backend defaults to port `8081`, so it does not conflict with a project already using `8080`.

## 3. Run backend

Requirements:

- JDK 21
- Maven 3.9+

From `backend`:

```bash
mvn spring-boot:run
```

or:

```bash
mvn clean package
java -jar target/catchbus-backend-1.0.0.jar
```

Health check:

`GET http://localhost:8081/api/health`

## 4. Demo accounts

With `DEMO_SEED=true` (default), the backend creates demo staff only if those users do not already exist.

Conductor:

```text
Email: conductor@catchbus.lk
Password: Conductor@123
QR token: CONDUCTOR-DEMO-QR
```

Driver:

```text
Email: driver@catchbus.lk
Password: Driver@123
QR token: DRIVER-DEMO-QR
```

Admin:

```text
Email: admin@catchbus.lk
Password: Admin@123
```

Change these credentials for any real deployment.

## 5. Operations login and QR workflow

The staff login page has separate role tabs:

**Conductor**

1. Select Conductor, Driver, or Administrator.
2. Enter the normal account email and password.
3. On success, the user is sent to the role-specific operations view.
4. The conductor can then use `Scan passenger QR` in the boarding panel.
5. The dashboard loads the assigned trip, preceding bus map, GPS controls, and issue messaging.

**Driver**

Drivers do not need a QR scanner. They sign in normally and receive the same schedule, preceding-bus map, GPS, and issue-reporting features.

The conductor QR scanner is for passenger booking/ticket payloads after login.

The QR payload can be a plain token such as:

```text
CONDUCTOR-DEMO-QR
```

or a URL containing:

```text
?role=CONDUCTOR&token=CONDUCTOR-DEMO-QR
```

The login page understands both.

## 6. Mobile camera note

Camera QR scanning normally requires a secure context:

- `https://...` works
- `localhost` works for local development
- an ordinary `http://192.168.x.x:5173` LAN address may be blocked by the browser

For a phone test over Wi-Fi, use HTTPS or a suitable development HTTPS tunnel.

If the browser does not support `BarcodeDetector`, the login page keeps a manual QR payload field as a fallback.

## 7. Live location logic

The current staff member can press:

`Start GPS sharing`

The browser sends the current bus GPS to:

`POST /api/staff/location`

The dashboard polls:

`GET /api/staff/dashboard`

every 10 seconds.

The backend determines the "preceding bus" as:

- the trip on the same route
- with a departure time earlier than the logged-in user's assigned trip
- closest to that assigned trip
- excluding cancelled trips

The latest `bus_location_updates` row for that preceding trip is returned.

Therefore the intended chain is:

```text
Bus A → sends GPS
        ↓
Bus B logs in
        ↓
Bus B sees Bus A's latest location
        ↓
Bus B sends GPS
        ↓
Bus C logs in
        ↓
Bus C sees Bus B's latest location
```

## 8. Frontend

From `frontend`:

```bash
npm install
npm run dev
```

Vite runs on:

`http://localhost:5173`

The supplied Vite proxy points `/api` to:

`http://localhost:8081`

If the backend is hosted elsewhere, set:

```text
VITE_API_BASE_URL=http://YOUR_BACKEND_HOST:8081/api
```

## 9. Existing authentication

The backend supports:

```text
POST /api/auth/passenger/register
POST /api/auth/passenger/login
POST /api/auth/staff/login
POST /api/auth/admin/login
```

Staff protected APIs:

```text
GET  /api/staff/dashboard
POST /api/staff/location
POST /api/conductor/board
```

Admin APIs used by the supplied admin dashboard:

```text
GET    /api/admin/users
PATCH  /api/admin/users/{id}/rbac
DELETE /api/admin/users/{id}
POST   /api/admin/users/staff
```

## 10. Important integration point for your final database

When you provide the separate final SQL Server database, the backend should be mapped to that exact schema.

The current implementation expects these original tables/columns:

- `users`
- `buses`
- `routes`
- `trips`
- `bookings`

and the new module tables from `database/module_schema.sql`.

If your final database has different column names, employee tables, assignment tables, QR fields, or a different meaning for "previous bus", those SQL queries should be adjusted rather than creating duplicate data structures.

## Project structure

```text
catchbuslk-frontend1/
├── database/
│   ├── script.sql
│   └── module_schema.sql
├── frontend/
│   ├── src/
│   │   ├── components/
│   │   │   └── QrScanner.jsx
│   │   └── pages/
│   │       ├── StaffLogin.jsx
│   │       ├── StaffDashboard.jsx
│   │       ├── ConductorDashboard.jsx
│   │       └── DriverDashboard.jsx
│   └── ...
└── backend/
    ├── pom.xml
    └── src/main/
        ├── java/lk/catchbus/api/
        └── resources/application.properties
```
