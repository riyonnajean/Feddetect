Fed-Detect: Federated Learning-Based Android Malware Detection

Fed-Detect aims to enhance the security of Android devices by leveraging federated learning to detect and mitigate malware threats while preserving user privacy. 
The project addresses the growing concern of malware attacks on Android devices by implementing a decentralized machine learning approach that trains models 
directly on user devices without sharing raw data. This ensures that sensitive user data remains localized, significantly reducing the risk of data breaches and maintaining user privacy.

Software and Technologies Used
  Python(Flask): Used for implementing federated learning algorithms due to its simplicity and extensive libraries for machine learning.
  Java: Utilized for Android app development.
  ReactJS: For creating a dashboard for central server(federated server) and for developing a user-friendly and responsive interface for interactions with the malware detection system.

Machine Learning Frameworks:
  TensorFlow Lite: For intergrating the model into the android device.
  TensorFlow Federated: For the federated server.
  
Operating Systems:
  Android 20: Chosen operating system for the devices.
  Linux: Provides a stable and secure environment for the federated server.
  
Database used:
  Firebase: Used for real-time data synchronization and secure transmission of model updates between devices and the federated server.

