# AttendX - Face Recognition Attendance System

AttendX is a Java-based backend application for face recognition and attendance management. It uses Spring Boot, JavaCV/OpenCV, and MySQL to support face prediction, attendance workflows, and training-image management.

> **Project type:** Backend REST API
> **Java:** 21
> **Spring Boot:** 3.2.4

## Features

- Face detection using a Haar Cascade classifier.
- Face recognition using an LBPH model.
- Camera-based attendance workflow.
- Attendance marking from an uploaded image.
- Face prediction without marking attendance.
- Training-image capture and model training.
- Menu-image upload and retrieval.
- Local training-image cleanup.
- MySQL-backed attendance persistence.

## Technology Stack

| Technology | Purpose |
|---|---|
| Java 21 | Application language |
| Spring Boot 3.2.4 | REST API framework |
| Spring Web | HTTP endpoints and multipart uploads |
| Spring Data JPA / Hibernate | Database persistence |
| MySQL | Attendance database |
| JavaCV / OpenCV | Face detection and recognition |
| Maven | Build and dependency management |

## Architecture

Client / API Tool
       |
       v
Spring Boot REST Controller
       |
       v
    FaceService
       |
       +--------------------+
       |                    |
       v                    v
 JavaCV / OpenCV     AttendanceRepository
       |                    |
       v                    v
 Face Detection           MySQL
       |
       v
 Local Image Files

## Prerequisites

Install or configure the following before running the project:

- JDK 21
- MySQL Server
- Maven, or the Maven Wrapper included in the repository
- A compatible camera for camera-based workflows
- Authorized face images for model training

## Getting Started

### 1. Clone the repository

git clone https://github.com/keep11kd/AttendX.git
Set-Location AttendX

### 2. Create the database

Run this SQL in MySQL:

CREATE DATABASE faceattendance;

### 3. Configure the database password

The application configuration uses:

- Database: `faceattendance`
- Username: `root`
- Password: supplied through the `DB_PASSWORD` environment variable

In PowerShell, set the password for the current terminal session without writing it into the repository:

$securePassword = Read-Host "Enter MySQL password" -AsSecureString
$env:DB_PASSWORD = [System.Net.NetworkCredential]::new("", $securePassword).Password
Remove-Variable securePassword

Keep this PowerShell session open while building and running the application. Never commit database credentials.

### 4. Build and test

Using Maven:

mvn clean test

Or, using the Maven Wrapper:

.\mvnw.cmd clean test

### 5. Start the application

mvn spring-boot:run

The default local base URL is:

`http://localhost:8080`

## REST API Reference

All endpoints use the `/api/face` base path.

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/capture/{userId}/{numImages}` | Capture training images |
| GET | `/train` | Train the recognition model |
| GET | `/mark-attendance` | Run camera-based attendance |
| POST | `/mark-attendance-from-image` | Mark attendance from an uploaded image |
| POST | `/predict` | Predict a face without marking attendance |
| POST | `/upload-menu-image/{userId}` | Upload a menu image |
| GET | `/menu-images/{userId}/view/{imageName}` | Retrieve a menu image |
| GET | `/clear-images/{userId}` | Delete files in a user's training-image folder |

### Capture training images

GET /api/face/capture/{userId}/{numImages}

Replace `userId` with the target user ID and `numImages` with the requested number of images.

### Train the model

GET /api/face/train

Trains the recognition model. The controller reports the output model filename as `lbph_model.xml` on success.

### Mark attendance using a camera

GET /api/face/mark-attendance

Runs the camera-based recognition and attendance workflow. A compatible camera and local runtime configuration may be required.

### Mark attendance from an image

POST /api/face/mark-attendance-from-image

Send a multipart form upload with the field name `image`.

PowerShell example using `curl.exe`:

curl.exe -X POST "http://localhost:8080/api/face/mark-attendance-from-image" -F "image=@C:\path\to\authorized-face.jpg"

Replace the example path with a real image path.

### Predict a face

POST /api/face/predict

Send a multipart form upload with the field name `image`. This endpoint returns a `FacePredictionResponse` containing:

- `recognized`
- `userId`
- `name`
- `confidence`
- `message`

Example:

curl.exe -X POST "http://localhost:8080/api/face/predict" -F "image=@C:\path\to\authorized-face.jpg"

### Upload a menu image

POST /api/face/upload-menu-image/{userId}

Send a multipart form upload using the field name `menuImage`.

curl.exe -X POST "http://localhost:8080/api/face/upload-menu-image/1" -F "menuImage=@C:\path\to\menu-image.jpg"

### View a menu image

GET /api/face/menu-images/{userId}/view/{imageName}

Returns the requested image when it exists in the configured local directory.

### Clear training images

GET /api/face/clear-images/{userId}

Deletes files from the specified user's training-image directory. **This operation is destructive.** Verify the user ID before calling it.

## Configuration and Storage

The current `application.properties` configures the application to use:

- `jdbc:mysql://localhost:3306/faceattendance`
- MySQL username `root`
- Password environment variable `DB_PASSWORD`
- Hibernate schema setting `ddl-auto=update`
- Multipart file limit of 20 MB
- Multipart request limit of 22 MB

The current implementation uses Windows-specific image directories, including:

- `C:/training_images/user{userId}/`
- `C:/menu_images/user{userId}/`

Ensure the required directories, permissions, and local hardware are configured before testing relevant endpoints.

## Recognition Model

The Haar Cascade resource is located at:

`src/main/resources/haarcascade_frontalface_alt.xml`

The trained `lbph_model.xml` is not included in the repository. Prepare authorized training images and train the model locally before using recognition functionality. Confirm the service's expected model path before running the training workflow.

## Security and Privacy

- Do not commit passwords, tokens, or other secrets.
- Use face images only with appropriate authorization.
- Treat face images, biometric predictions, and attendance records as sensitive personal data.
- Review authentication and authorization before exposing endpoints publicly.
- Validate uploaded files and restrict file types and sizes appropriately.
- Restrict access to image directories and database records.
- Protect destructive operations such as training-image cleanup.
- Review the current use of GET for operations that change state before deploying publicly.

## Known Limitations

- The documented application is a backend REST API; a hosted frontend or live demo is not included here.
- Camera-based functionality depends on local hardware and runtime support.
- Image paths are Windows-specific.
- The trained recognition model must be prepared locally.
- Endpoint examples should be tested against the configured local environment.
- A successful build does not by itself prove every API workflow works end-to-end.

## License

No license is documented in this README. Add an appropriate license file before explicitly granting reuse or redistribution rights.
