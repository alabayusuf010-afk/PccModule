# PccModule: Wireless VR Performance Optimization

This repository contains the implementation and validation of the **PccModule** (Point Cloud Compression) as part of an ongoing 5G research project at the Federal University of Lavras (UFLA).

## Project Overview
The project focuses on enhancing the Quality of Experience (QoE) for Virtual Reality (VR) by optimizing computational resource usage using Dynamic Performance Management (DPM).

## Key Results (Experiment 2)
Initial performance validation focused on the `processFrames` method. The following results were obtained using the Android Profiler:

| Scenario | CPU Usage (%) |
| :--- | :--- |
| **Baseline (DPM OFF)** | 197.10% |
| **Optimized (DPM ON)** | **77.84%** |

**Achievement:** A computational load reduction of approximately **32%**, ensuring smoother VR rendering while maintaining network efficiency.

## Technical Implementation
- **Core Logic:** `app/src/main/java/com/example/pccmodule/PccModule.java`.
- **Development Environment:** Android Studio, OpenCV Android SDK.
- **Validation:** Real-time performance monitoring via Android Profiler.

## Researcher
**Sulaimon Alaba Yusuf**  
Student and Researcher, Federal University of Lavras (UFLA).
