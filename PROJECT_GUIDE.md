# PROJECT_GUIDE.md

## 1. Project Overview & Problem Statement
The **Smart Public Transportation Route Optimization** system is a Java Swing desktop application that helps a city planner model a public‑transport network. It lets the user define bus stops, roads, a fleet of buses and passenger requests, then compute the fastest route between stops using Dijkstra and allocate buses to requests based on capacity and availability.

## 2. System Workflow
1. **Add Stops** – via the *Stops* panel; stored in `TransportNetwork.stops`.
2. **Add Roads** – via the *Road Network* panel; each road has a travel‑time weight and a status (`OPEN`/`CLOSED`). Stored in `TransportNetwork.roads`.
3. **Add Buses** – via the *Fleet* panel; each bus has total capacity, mutable `availableSeats` and an `available` flag.
4. **Add Passenger Requests** – via the *Requests* panel; each request includes source, destination, passenger count and a status.
5. **Route Optimization** – the *Route Optimization* panel calls `TransportNetwork.findShortestPath(sourceId, destId)`. Dijkstra builds an adjacency list from all **OPEN** roads and returns the minimum‑travel‑time path.
6. **Bus Allocation** – the *Decision Explanation* panel triggers `TransportNetwork.allocateBuses()`. Requests are processed in priority order, the shortest reachable route is found, and the first bus that is **available** and has enough **available seats** is assigned. Allocation results are recorded in `AllocationRecord`.
7. **Disruption Simulator** – users can close a road, increase its travel time, or mark a bus as unavailable; the underlying network is updated, so subsequent route optimisation and allocation automatically respect the new state.
8. **Reports** – the *Reports* panel shows live summary statistics and can export CSV files.

## 3. Core Data‑Structures & Algorithms
| Structure | Usage in Code | Purpose |
|-----------|----------------|---------|
| **PriorityQueue** (`java.util.PriorityQueue`) | `TransportNetwork.allocateBuses()` (requests sorted by passenger count) | Simple priority order – larger groups are allocated first.
| **HashMap** (`java.util.HashMap`) | `TransportNetwork.stopById`, `roadKeyMap`, `busById`, `requestById` | O(1) lookup of entities by their IDs.
| **Weighted Graph** (implicit via `Road` objects) | `TransportNetwork.findShortestPath()` builds adjacency list from open roads. | Represents roads as weighted edges (`travelTime`).
| **Dijkstra** | `TransportNetwork.findShortestPath()` | Computes the minimum total travel time path while ignoring closed roads.

## 4. Important Classes & Responsibilities
- **`TransportNetwork`** – central model; stores all stops, roads, buses, requests, allocation records; provides methods to add entities, run Dijkstra, and allocate buses.
- **`Stop`** – DTO for a bus stop (id, name, description).
- **`Road`** – connects two stops, holds travel time and status.
- **`Bus`** – id, type, total capacity, mutable `availableSeats` and `available` flag.
- **`PassengerRequest`** – source, destination, passenger count, status.
- **`AllocationRecord`** – immutable record of a successful (or failed) allocation, storing request id, bus id, route, travel time, etc.
- **`DecisionExplanationPanel`** – UI showing allocation results and route explanation.
- **`DisruptionSimulatorPanel`** – UI to change road status/travel time and bus availability.
- **`ReportsPanel`**, **`RouteOptimizationPanel`**, **`FleetPanel`**, **`RequestsPanel`**, **`StopsPanel`** – UI panels for CRUD operations.
- **`DataStore`** – singleton that gives every panel access to the shared `TransportNetwork` instance.

## 5. How Bus Capacity & Availability are Checked
In `TransportNetwork.allocateBuses()` the logic is:
```java
if (bus.isAvailable() && bus.getAvailableSeats() >= request.getPassengerCount()) {
    // assign request to this bus
}
```
- `isAvailable()` reflects whether the bus is broken or already fully booked.
- After a successful assignment `bus.setAvailableSeats(bus.getAvailableSeats() - request.getPassengerCount())`. When seats reach `0`, `bus.setAvailable(false)` marks the bus unavailable for further requests.

## 6. Effect of Disruptions
- **Road Closure** – `Road.setStatus(RoadStatus.CLOSED)`; Dijkstra ignores closed roads, so routes that used that road become unreachable.
- **Increased Travel Time** – `Road.setTravelTime(newTime)`; the weight in the graph changes, causing Dijkstra to possibly choose alternative routes.
- **Bus Breakdown** – `bus.setAvailable(false)`; the bus is removed from the candidate pool during allocation. Restoring availability with `setAvailable(true)` makes it usable again.
All UI panels call `MainFrame.refreshDashboard()` after a change, keeping combo‑boxes and statistics up‑to‑date.

## 7. How to Compile & Run (Windows)
1. Open Command Prompt and navigate to the project folder:
   ```cmd
   cd D:\mini_project
   ```
2. Compile the sources (no external libraries needed):
   ```cmd
   javac *.java
   ```
3. Run the application:
   ```cmd
   java Main
   ```
   The main window with a dark‑navy sidebar will appear.

## 8. Viva‑Ready Explanation (Beginner Friendly)
- **What it does** – Models a small city bus system, finds the fastest route between stops, and assigns a bus to each passenger group.
- **Algorithms** – Dijkstra gives the minimum‑travel‑time path; a greedy priority‑queue ordering (largest request first) decides which request to handle first.
- **Data structures** – HashMaps give constant‑time lookup for stops, roads, buses and requests; the adjacency list built from `Road` objects turns the network into a weighted graph suitable for Dijkstra.
- **Limitations** – Allocation is greedy, not globally optimal; a request is either fully assigned to a single bus or left unallocated; no persistence, no multi‑trip scheduling, no advanced traffic simulation.
- **Demo tips** – Show how closing a road instantly changes the route in the *Route Optimization* panel and how the allocation table updates when a bus becomes unavailable.

---
*Prepared for a second‑year CSE project demonstration.*
