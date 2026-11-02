# Smart Public Transportation Route Optimization System

A robust, full-featured Java Swing desktop application designed for city transit planning and passenger route navigation. It models urban bus corridors, performs dynamic shortest-path optimization using Dijkstra's algorithm, manages bus fleet allocation, tracks stop-by-stop seat availability in real time, simulates road disruptions, and persists all data in a local SQLite database.

---

## Key Features

### 1. Role-Based Navigation & Security
* **Welcome Screen**: Clean, responsive navy-blue themed landing page presenting distinct portals for **Transport** administrators and **Passengers**.
* **Transport Administrator Authentication**: Secure login authenticated against the local SQLite database using salted PBKDF2 (`PBKDF2WithHmacSHA256`) password hashing.
  * **Demo Username**: `admin`
  * **Demo Password**: `admin123`
* **Passenger Transit Portal**: Unauthenticated, dedicated passenger interface providing corridor navigation, real-time seat availability tracking, and trip request submissions.

### 2. Route Optimization & Network Algorithms
* **Dijkstra's Shortest Path Algorithm**: Calculates minimum travel-time paths across the road network while automatically avoiding closed roads.
* **Dynamic Alternative Detours**: Automatically recalculates alternative paths when primary roads are blocked or disrupted.
* **Bus Fleet Allocation**: Evaluates bus assignments based on route sequence order (`source -> destination`), operational status, and available seat capacity.
* **Transparent Decision Explanations**: Details step-by-step road segments, travel times, and assigned bus capacity, or provides clear, understandable failure reasons when routes are blocked, unserved, or fully booked.

### 3. Dynamic Stop-by-Stop Seat Tracking
* Calculates passenger loads across scheduled trips using the corridor formula:
  $$\text{Available Seats} = \text{Previous Available} + \text{Alighting} - \text{Boarding}$$
* Displays live seat counts and capacity bars for each stop along the active transit corridor.

### 4. Disruption Simulation & Fleet Operations
* **Road Disruptions**: Simulate temporary road closures or traffic congestion delays with instant Dijkstra route recalculation.
* **Fleet Management**: Mark buses as operational or broken down/in maintenance; allocations adapt dynamically.
* **Stop Synchronization**: Unified source of truth across all panels—adding, updating, or deleting stops updates all Transport and Passenger dropdowns immediately without restarting.
* **Reports & Data Export**: View live summary statistics and export network data as CSV files.

---

## Project Structure

```text
mini_project/
├── .gitignore                     # Git ignore rules for bytecode, caches, and runtime databases
├── compile.bat                    # Windows batch build script (javac)
├── run.bat                        # Windows batch launch script (java)
├── PROJECT_GUIDE.md               # Architectural reference & viva guide
├── README.md                      # Project documentation and setup guide
├── lib/
│   └── sqlite-jdbc-3.53.4.0.jar   # Embedded SQLite JDBC driver
├── data/
│   └── .gitkeep                   # Local application data directory (stores transport.db)
└── *.java                         # Java Swing source files
    ├── Main.java                  # Application entry point
    ├── MainFrame.java             # Main application frame & navigation router
    ├── WelcomePanel.java          # Role selection landing screen
    ├── LoginPanel.java            # Secure Transport Admin authentication panel
    ├── DashboardPanel.java        # Central administrative overview & corridor monitor
    ├── PassengerPanel.java        # Passenger transit portal & request booking
    ├── RouteOptimizationPanel.java# Dijkstra route finding interface
    ├── DecisionExplanationPanel.java # Transparent allocation & route explanation UI
    ├── DisruptionSimulatorPanel.java # Road disruption & bus breakdown simulator
    ├── StopsPanel.java            # Bus stops CRUD management
    ├── RoadsPanel.java            # Road network CRUD management
    ├── FleetPanel.java            # Bus fleet CRUD management
    ├── RequestsPanel.java         # Passenger transit requests manager
    ├── ReportsPanel.java          # Operations reports & CSV export
    ├── TransportNetwork.java      # Core network graph model & Dijkstra algorithm
    ├── DatabaseManager.java       # SQLite connection, schema, and transaction management
    ├── DataStore.java             # Shared application network state singleton
    ├── PasswordUtils.java         # Salted PBKDF2 password hashing utility
    ├── TripSeatTracker.java       # Corridor seat tracking calculation engine
    └── Model & DTO classes:
        ├── Bus.java
        ├── Stop.java
        ├── Road.java
        ├── RoadStatus.java
        ├── Trip.java
        ├── TripStopRecord.java
        ├── PassengerRequest.java
        ├── AllocationRecord.java
        ├── AllocationResult.java
        ├── TransportUser.java
        ├── RouteResult.java
        └── DisruptionRecord.java
```

---

## Getting Started

### Prerequisites
* **Java Development Kit (JDK)**: Version 11 or higher (OpenJDK, Oracle JDK, or Eclipse Temurin).
* **Git**: To clone and version-control the project.

### How to Compile and Run

#### Using Windows Batch Scripts:
1. Open the project directory:
   ```cmd
   cd C:\Users\LENOVO\Downloads\mini\mini_project
   ```
2. Compile all source files:
   ```cmd
   compile.bat
   ```
3. Run the application:
   ```cmd
   run.bat
   ```

#### Using Command Prompt / PowerShell directly:
```cmd
javac -cp ".;lib/*" *.java
java -cp ".;lib/*" Main
```

---

## Database Initialization
On the very first run, `DatabaseManager` automatically creates a local SQLite database at `data/transport.db` and initializes:
1. All relational tables with constraints (`stops`, `roads`, `buses`, `trips`, `trip_stop_records`, `passenger_requests`, `route_allocations`, `transport_users`, `disruptions`).
2. The initial active corridor stops: `Erode` ➔ `Nandha College` ➔ `Perundurai` ➔ `Kongu College`.
3. Standard and express fleet buses: `BUS-101` and `BUS-102`.
4. Scheduled corridor trips and road segments.
5. Default administrator credentials (`admin` / `admin123`).

No external database installation or manual SQL execution is required.
