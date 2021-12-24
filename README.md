# ReadMe
The repository is dedicated to a course at Tsinghua University.
* Course Name: Essentials to Signal Processing and Data Management for AIoT Applications
* Course ID: 86010053
* Semester: Fall 2021

The group term project aims to develop a healthcare system to monitor neck movements with acceleration sensors. The group consist of three members. My main contribution in the project centers around the remote server for data warehousinng, which was powered with Flask to implement a RESTful API. Jiexin developed the prototype for data collection using Arduino, as well as an Android user interface.



## Data Collection and Model Training

* `Signal_processing/readSerialApater.py`: record the data transmitted by IMU.
  * Plugin in the USB adapter first.
  * Bind the IMU sensor in the software `蓝牙5.0多连上位机`.
* `Signal_processing/motion_svm.py`: read raw data and train the ML model.



## Import the ML Model to Android

* `Signal_processing/motion_svm_pmml.py`: save the model in `.pmml` format.
* Convert `.pmml` to `.ser`: 
  * Use this lib: https://github.com/jpmml/jpmml-android;
  * Use jdk 8;
  * Change the version in the pmml file from 4.4 to 4.3 manually.
* Put the ser file into the `assets` folder in Android project.



## Android App

* Blue tooth connection  part is based on https://github.com/BestCoderXQX/MagkareBle4.0 .
* Add `implementation 'org.jpmml:pmml-evaluator:1.3.6'` to module level `build.gradle` to import the ML model.
* The remote server is based on th Firebase. Change the `google-services.json` for your configuration.

