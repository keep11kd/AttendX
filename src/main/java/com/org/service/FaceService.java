package com.org.service;

import org.springframework.core.io.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.org.model.Attendance;
import com.org.repository.AttendanceRepository;

import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_videoio.*;
import org.bytedeco.opencv.opencv_objdetect.*;
import org.bytedeco.opencv.opencv_face.*;

import static org.bytedeco.opencv.global.opencv_imgproc.*;
import static org.bytedeco.opencv.global.opencv_imgcodecs.*;
import static org.bytedeco.opencv.global.opencv_core.*;

import java.io.File;
import java.io.IOException;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@Service
public class FaceService {

    @Autowired
    private AttendanceRepository attendanceRepository;

    private static final Map<Integer, String> userLabelMap = Map.of(
        1, "Anurag Singh",
        2, "Rohit Singh",
        3, "Virat Kohli"
    );

    public String recognizeAndMark() {
        try {
            VideoCapture camera = new VideoCapture(0);
            if (!camera.isOpened()) return "Error: Cannot access camera";

            Resource haarResource = new ClassPathResource("haarcascade_frontalface_alt.xml");
            File haarFile = createTempFile(haarResource, "haarcascade_frontalface_alt.xml");
            CascadeClassifier faceDetector = new CascadeClassifier(haarFile.getAbsolutePath());
            if (faceDetector.empty()) {
                camera.release();
                return "Error: Failed to load Haar Cascade classifier";
            }

            File modelFile = new File("src/main/resources/trained_faces/lbph_model.xml");
            if (!modelFile.exists()) {
                camera.release();
                return "Model not found; please train first by calling /api/face/train";
            }

            Resource modelResource = new ClassPathResource("trained_faces/lbph_model.xml");
            File tempModelFile = createTempFile(modelResource, "lbph_model.xml");
            LBPHFaceRecognizer recognizer = LBPHFaceRecognizer.create();
            recognizer.read(tempModelFile.getAbsolutePath());

            Mat colorFrame = new Mat();
            camera.read(colorFrame);
            Mat frame = new Mat();
            cvtColor(colorFrame, frame, COLOR_BGR2GRAY);

            RectVector faces = new RectVector();
            faceDetector.detectMultiScale(frame, faces);
            System.out.println("📷 Captured frame size: " + frame.size().width() + "x" + frame.size().height());
            System.out.println("🔍 Faces detected: " + faces.size());

            for (int i = 0; i < faces.size(); i++) {
                Rect face = faces.get(i);
                int x = Math.max(face.x() - 10, 0);
                int y = Math.max(face.y() - 10, 0);
                int width = Math.min(face.width() + 20, frame.cols() - x);
                int height = Math.min(face.height() + 20, frame.rows() - y);
                Rect paddedFace = new Rect(x, y, width, height);
                Mat faceROI = new Mat(frame, paddedFace);
                resize(faceROI, faceROI, new Size(160, 160));

                int[] label = new int[1];
                double[] confidence = new double[1];
                recognizer.predict(faceROI, label, confidence);

                System.out.println("👤 Predicted User ID: " + label[0] + ", Confidence: " + confidence[0]);

                if (confidence[0] < 98.0) {
                    int userId = label[0];
                    LocalDate date = LocalDate.now();

                    if (!attendanceRepository.existsByUserIdAndDate(userId, date)) {
                        Attendance att = new Attendance();
                        att.setUserId(userId);
                        att.setDate(date);
                        att.setTime(LocalTime.now());
                        attendanceRepository.save(att);

                        camera.release();
                        String name = userLabelMap.getOrDefault(userId, "Unknown");
                        return "✅ Attendance marked for: " + name + " (ID: " + userId + ")";
                    } else {
                        camera.release();
                        return "⚠️ Already marked today for User ID: " + userId;
                    }
                }
            }

            camera.release();
            return "❌ Face not recognized";

        } catch (Exception e) {
            return "💥 Error: " + e.getMessage();
        }
    }

    public com.org.model.FacePredictionResponse predictFromImage(MultipartFile imageFile) {
        if (imageFile == null || imageFile.isEmpty()) {
            return new com.org.model.FacePredictionResponse(
                    false, null, null, null, "Please upload a non-empty image.");
        }

        Path tempImage = null;
        File haarFile = null;
        File tempModelFile = null;

        try {
            tempImage = Files.createTempFile("face_upload_", ".img");
            Files.write(tempImage, imageFile.getBytes());

            Resource haarResource = new ClassPathResource("haarcascade_frontalface_alt.xml");
            if (!haarResource.exists()) {
                return new com.org.model.FacePredictionResponse(
                        false, null, null, null, "Face detection resource not found.");
            }

            haarFile = createTempFile(haarResource, "haarcascade_frontalface_alt.xml");
            CascadeClassifier faceDetector = new CascadeClassifier(haarFile.getAbsolutePath());

            if (faceDetector.empty()) {
                return new com.org.model.FacePredictionResponse(
                        false, null, null, null, "Failed to load face detector.");
            }

            Resource modelResource = new ClassPathResource("trained_faces/lbph_model.xml");
            if (!modelResource.exists()) {
                return new com.org.model.FacePredictionResponse(
                        false, null, null, null, "Trained model not found. Train the model first.");
            }

            tempModelFile = createTempFile(modelResource, "lbph_model.xml");
            LBPHFaceRecognizer recognizer = LBPHFaceRecognizer.create();
            recognizer.read(tempModelFile.getAbsolutePath());

            Mat image = imread(tempImage.toAbsolutePath().toString());
            if (image.empty()) {
                return new com.org.model.FacePredictionResponse(
                        false, null, null, null, "Invalid or unsupported image.");
            }

            Mat gray = new Mat();
            if (image.channels() > 1) {
                cvtColor(image, gray, COLOR_BGR2GRAY);
            } else {
                gray = image.clone();
            }

            RectVector faces = new RectVector();
            faceDetector.detectMultiScale(gray, faces);

            if (faces.size() == 0) {
                return new com.org.model.FacePredictionResponse(
                        false, null, null, null, "No face detected in the uploaded image.");
            }

            Rect face = faces.get(0);
            int x = Math.max(face.x() - 10, 0);
            int y = Math.max(face.y() - 10, 0);
            int width = Math.min(face.width() + 20, gray.cols() - x);
            int height = Math.min(face.height() + 20, gray.rows() - y);

            if (width <= 0 || height <= 0) {
                return new com.org.model.FacePredictionResponse(
                        false, null, null, null, "Detected face has invalid dimensions.");
            }

            Mat faceROI = new Mat(gray, new Rect(x, y, width, height));
            Mat normalizedFace = new Mat();
            resize(faceROI, normalizedFace, new Size(160, 160));

            int[] label = new int[1];
            double[] confidence = new double[1];
            recognizer.predict(normalizedFace, label, confidence);

            if (confidence[0] >= 80.0 || !userLabelMap.containsKey(label[0])) {
                return new com.org.model.FacePredictionResponse(
                        false, null, null, confidence[0], "Face not recognized.");
            }

            int userId = label[0];
            return new com.org.model.FacePredictionResponse(
                    true,
                    userId,
                    userLabelMap.get(userId),
                    confidence[0],
                    "Face recognized. Attendance was not marked.");

        } catch (Exception e) {
            return new com.org.model.FacePredictionResponse(
                    false, null, null, null, "Prediction failed: " + e.getMessage());
        } finally {
            try {
                if (tempImage != null) {
                    Files.deleteIfExists(tempImage);
                }
                if (haarFile != null) {
                    Files.deleteIfExists(haarFile.toPath());
                }
                if (tempModelFile != null) {
                    Files.deleteIfExists(tempModelFile.toPath());
                }
            } catch (IOException cleanupError) {
                System.err.println("Could not delete temporary prediction files: "
                        + cleanupError.getMessage());
            }
        }
    }
    public String recognizeFromImage(MultipartFile imageFile) {
        try {
            Path tempImage = Files.createTempFile("uploaded_", imageFile.getOriginalFilename());
            Files.write(tempImage, imageFile.getBytes());
            System.out.println("📄 Uploaded image saved to: " + tempImage.toFile().getAbsolutePath());

            Resource haarResource = new ClassPathResource("haarcascade_frontalface_alt.xml");
            File haarFile = createTempFile(haarResource, "haarcascade_frontalface_alt.xml");
            CascadeClassifier faceDetector = new CascadeClassifier(haarFile.getAbsolutePath());
            if (faceDetector.empty()) return "Error: Failed to load Haar Cascade classifier";

            File modelFile = new File("src/main/resources/trained_faces/lbph_model.xml");
            if (!modelFile.exists()) return "Model not found; please train first by calling /api/face/train";

            Resource modelResource = new ClassPathResource("trained_faces/lbph_model.xml");
            File tempModelFile = createTempFile(modelResource, "lbph_model.xml");
            LBPHFaceRecognizer recognizer = LBPHFaceRecognizer.create();
            recognizer.read(tempModelFile.getAbsolutePath());

            Mat image = imread(tempImage.toFile().getAbsolutePath());
            if (image.empty()) return "Error: Invalid image file";

            if (image.channels() > 1) {
                Mat gray = new Mat();
                cvtColor(image, gray, COLOR_BGR2GRAY);
                image = gray;
            }

            RectVector faces = new RectVector();
            faceDetector.detectMultiScale(image, faces);

            if (faces.size() > 0) {
                Rect face = faces.get(0);
                int x = Math.max(face.x() - 10, 0);
                int y = Math.max(face.y() - 10, 0);
                int width = Math.min(face.width() + 20, image.cols() - x);
                int height = Math.min(face.height() + 20, image.rows() - y);
                Rect paddedFace = new Rect(x, y, width, height);
                Mat faceROI = new Mat(image, paddedFace);
                resize(faceROI, faceROI, new Size(160, 160));

                int[] label = new int[1];
                double[] confidence = new double[1];
                recognizer.predict(faceROI, label, confidence);

                System.out.println("👤 Predicted User ID: " + label[0] + ", Confidence: " + confidence[0]);

                if (confidence[0] < 80) {
                    int userId = label[0];
                    LocalDate date = LocalDate.now();

                    if (!attendanceRepository.existsByUserIdAndDate(userId, date)) {
                        Attendance att = new Attendance();
                        att.setUserId(userId);
                        att.setDate(date);
                        att.setTime(LocalTime.now());
                        attendanceRepository.save(att);
                        String name = userLabelMap.getOrDefault(userId, "Unknown");
                        return "✅ Attendance marked for: " + name + " (ID: " + userId + ")";
                    } else {
                        return "⚠️ Already marked today for User ID: " + userId;
                    }
                }
            }

            return "❌ Face not recognized";

        } catch (Exception e) {
            return "💥 Error: " + e.getMessage();
        }
    }

    public void trainModel() throws IOException {
        File trainingDir = new File("C:/training_images");
        if (!trainingDir.exists() || !trainingDir.isDirectory()) {
            throw new IOException("Training directory not found: " + trainingDir.getAbsolutePath());
        }

        List<Mat> imagesList = new ArrayList<>();
        List<Integer> labelsList = new ArrayList<>();
        CascadeClassifier faceDetector = new CascadeClassifier(
            new ClassPathResource("haarcascade_frontalface_alt.xml").getFile().getAbsolutePath()
        );

        for (File userDir : trainingDir.listFiles(File::isDirectory)) {
            int userId = Integer.parseInt(userDir.getName().replace("user", ""));
            for (File imageFile : userDir.listFiles((dir, name) -> name.endsWith(".jpg") || name.endsWith(".png"))) {
                Mat image = imread(imageFile.getAbsolutePath());
                if (image.empty()) continue;

                if (image.channels() > 1) {
                    Mat gray = new Mat();
                    cvtColor(image, gray, COLOR_BGR2GRAY);
                    image = gray;
                }

                RectVector faces = new RectVector();
                faceDetector.detectMultiScale(image, faces);
                if (faces.size() > 0) {
                    Rect face = faces.get(0);
                    Mat faceROI = new Mat(image, face);
                    resize(faceROI, faceROI, new Size(160, 160));
                    imagesList.add(faceROI);
                    labelsList.add(userId);
                }
            }
        }

        if (imagesList.isEmpty()) throw new IOException("No valid training images found");

        MatVector images = new MatVector(imagesList.size());
        for (int i = 0; i < imagesList.size(); i++) {
            images.put(i, imagesList.get(i));
        }

        Mat labels = new Mat(labelsList.size(), 1, CV_32SC1);
        IntBuffer labelsBuf = labels.createBuffer();
        for (int i = 0; i < labelsList.size(); i++) {
            labelsBuf.put(i, labelsList.get(i));
        }

        LBPHFaceRecognizer recognizer = LBPHFaceRecognizer.create();
        recognizer.train(images, labels);

        File outputDir = new File("src/main/resources/trained_faces");
        if (!outputDir.exists()) outputDir.mkdirs();

        File modelFile = new File(outputDir, "lbph_model.xml");
        recognizer.save(modelFile.getAbsolutePath());
        System.out.println("✅ Model saved to: " + modelFile.getAbsolutePath());
    }

    public String captureTrainingImages(int userId, int numImages) throws IOException {
        VideoCapture camera = new VideoCapture(0);
        if (!camera.isOpened()) return "Error: Cannot access camera";

        Resource haarResource = new ClassPathResource("haarcascade_frontalface_alt.xml");
        File haarFile = createTempFile(haarResource, "haarcascade_frontalface_alt.xml");
        CascadeClassifier faceDetector = new CascadeClassifier(haarFile.getAbsolutePath());
        if (faceDetector.empty()) {
            camera.release();
            return "Error: Failed to load Haar Cascade classifier";
        }

        File outputDir = new File("C:/training_images/user" + userId);
        if (!outputDir.exists()) outputDir.mkdirs();

        Mat colorFrame = new Mat();
        int count = 0;
        while (count < numImages) {
            camera.read(colorFrame);
            Mat frame = new Mat();
            cvtColor(colorFrame, frame, COLOR_BGR2GRAY);

            RectVector faces = new RectVector();
            faceDetector.detectMultiScale(frame, faces);

            if (faces.size() > 0) {
                Rect face = faces.get(0);
                Mat faceROI = new Mat(frame, face);
                resize(faceROI, faceROI, new Size(160, 160));
                File outputFile = new File(outputDir, "image" + (count + 1) + ".jpg");
                imwrite(outputFile.getAbsolutePath(), faceROI);
                count++;
            }

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        camera.release();
        return "✅ Captured " + numImages + " images for user" + userId;
    }

    public String uploadMenuImage(MultipartFile imageFile, int userId) {
        try {
            File dir = new File("C:/menu_images/user" + userId);
            if (!dir.exists()) dir.mkdirs();
            String fileName = "menu_" + System.currentTimeMillis() + ".jpg";
            File dest = new File(dir, fileName);
            imageFile.transferTo(dest);

            return "✅ Menu image uploaded successfully: " + dest.getAbsolutePath();
        } catch (IOException e) {
            return "💥 Error uploading menu image: " + e.getMessage();
        }
    }


    private File createTempFile(Resource resource, String fileName) throws IOException {
        Path tempFile = Files.createTempFile("face_", fileName);
        Files.copy(resource.getInputStream(), tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        System.out.println("📄 Created temp file: " + tempFile.toFile().getAbsolutePath());
        return tempFile.toFile();
    }
}
