# PccModule

Implementation of the PCC (Pearson's Correlation Coefficient) based methodology for mobile robot perception, based on the research by **Arthur Miranda Neto**.

## Overview
This project implements a Dynamic Power Management (DPM) strategy for robotic vision systems. It uses PCC to detect redundancy between consecutive video frames, allowing the system to discard redundant data and save computational power.

## Key Features
- **PCC-based Frame Discarding**: Automatically discards frames with high correlation (defined by threshold $\theta$) to reduce CPU load.
- **ROI (Region of Interest) Selection**: Implements "Experiment 2" logic to prioritize processing of image regions with low correlation (highest change/interest).
- **ITA (Iterative Thresholding Algorithm)**: Simulation of the ITA point reduction sequence (Experiment 5).
- **Real-time Camera Integration**: Built with CameraX and Jetpack Compose for real-time analysis and visualization.
- **Data Logging**: Built-in experiment data logging to Logcat for analysis (timestamp, $\theta$, $r_1$, discard rate, etc.).

## Tech Stack
- **Language**: Kotlin & Java
- **Camera API**: CameraX (Analysis, Video, Image Capture)
- **UI**: Jetpack Compose (Material 3)
- **Minimum SDK**: 24

## Research Context
This implementation is designed to demonstrate the efficiency gains in unstructured environments (Experiment 2) by automatically selecting relevant "pixels of interest" and managing power consumption dynamically.

---
*Based on: "Automatic Regions-of-Interest Selection based on Pearson's Correlation Coefficient" by Arthur Miranda Neto.*
