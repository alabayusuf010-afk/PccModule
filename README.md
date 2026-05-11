# PccModule

Implementation of the PCC (Pearson's Correlation Coefficient) based methodology for mobile robot perception, based on the research by **Arthur Miranda Neto**.

## Overview
This project implements a Dynamic Power Management (DPM) strategy for robotic vision systems. It uses PCC to detect redundancy between consecutive video frames, allowing the system to discard redundant data and save computational power.

## Key Features
- **PCC-based Frame Discarding**: Automatically discards frames with high correlation (defined by threshold $\theta$) to reduce CPU load.
- **ROI (Region of Interest) Selection (Experiment 2)**: Implements logic to prioritize processing of image regions with low correlation. 
- **Optimized Sorting**: In Experiment 2, blocks are sorted by correlation value (lowest first) to ensure the most dynamic parts of the scene are prioritized for processing.
- **ITA (Iterative Thresholding Algorithm)**: Simulation of the ITA point reduction sequence (Experiment 5: 1658 -> 1025 -> 688 -> 460 -> 289 -> 92).
- **Real-time Camera Integration**: Built with CameraX and Jetpack Compose for real-time analysis and visualization.
- **Optimized PCC Computation**: Uses row-buffer pixel access (`getPixels`) instead of `getPixel` for high-performance frame analysis.

## Tech Stack
- **Language**: Kotlin & Java
- **Camera API**: CameraX (Analysis, Video, Image Capture)
- **UI**: Jetpack Compose (Material 3)
- **Minimum SDK**: 24

## Research Context
This implementation demonstrates efficiency gains in unstructured environments (Experiment 2) by automatically selecting relevant "pixels of interest" and managing power consumption dynamically.

---
*Based on: "Automatic Regions-of-Interest Selection based on Pearson's Correlation Coefficient" by Arthur Miranda Neto.*
