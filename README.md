# AttendX — Face Recognition Attendance System

AttendX is a Java-based attendance management project built with Spring Boot, JavaCV/OpenCV, and MySQL. It provides REST APIs for face recognition and attendance workflows.

## Technology Stack

- Java 21
- Spring Boot 3.2.4
- Maven
- JavaCV / OpenCV
- Spring Data JPA / Hibernate
- MySQL

## Features

- Face recognition using an LBPH model and Haar Cascade face detection.
- Webcam-based attendance workflow.
- Attendance marking from uploaded images.
- Face prediction endpoint separate from attendance marking.
- APIs for capturing training images and training the recognition model.

## Prerequisites

- JDK 21
- MySQL
- Maven, or the included Maven Wrapper
- A locally trained face-recognition model

## Configuration

The database configuration is in `src/main/resources/application.properties`.

Set the `DB_PASSWORD` environment variable to your local MySQL password before running the application. Never commit database credentials.

## Build and Test

Run `mvn clean test` or use the included Maven Wrapper.

## Run the Application

Run `mvn spring-boot:run`. The default application port is 8080.

## Face Recognition Model

The trained `lbph_model.xml` is intentionally excluded from this repository. Prepare authorized training images locally and use the application's training workflow before using recognition endpoints.

The Haar Cascade resource is expected at `src/main/resources/haarcascade_frontalface_alt.xml`.

## API Endpoints

- `GET /api/face/train`
- `GET /api/face/mark-attendance`
- `POST /api/face/mark-attendance-from-image`
- `POST /api/face/predict`
- `GET /api/face/capture/{userId}/{numImages}`
- `GET /api/face/clear-images/{userId}`

Review the controller source for additional image-management endpoints and request details.

## Privacy

Use only face images you have permission to process. Keep personal training images and generated recognition models out of public source control.
