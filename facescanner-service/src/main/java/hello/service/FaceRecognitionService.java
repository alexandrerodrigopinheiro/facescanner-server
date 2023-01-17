package hello.service;

import org.bytedeco.javacpp.facescanner;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_imgcodecs;
import org.bytedeco.javacpp.opencv_imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StreamUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class FaceRecognitionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(FaceRecognitionService.class);
    private static final String FACESCANNER_DB_NAME = "people";

    @Value("classpath:facescanner/configure.txt")
    private Resource faceScannerConfigResource;
    @Value("classpath:facescanner/model/resnetsmall_5DB_2loss_140000_kaiya3.dat")
    private Resource faceScannerModelResource;
    @Value("classpath:facescanner/visitor.db")
    private Resource faceScannerDBResource;

    private facescanner.CIDRecognitionLocal recognitionLocal;
    private boolean recognitionLocalInitialized = false;
    private final Object faceScannerLock = new Object();

    public FaceRecognitionService() {}

    @PostConstruct
    public void initializeFaceScanner() throws IOException {
        synchronized (faceScannerLock) {
            _initializeFaceScanner();
        }
    }

    private void _initializeFaceScanner() throws IOException {
        recognitionLocal = facescanner.CIDRecognitionLocal.GetInstance();

        InputStream configIS = faceScannerConfigResource.getInputStream();
        byte[] faceScannerConfigData = StreamUtils.copyToByteArray(configIS);
        recognitionLocal.LoadConfigure(faceScannerConfigData, faceScannerConfigData.length);

        InputStream modelIS = faceScannerModelResource.getInputStream();
        byte[] faceScannerModelData = StreamUtils.copyToByteArray(modelIS);
        if (recognitionLocal.Initialize(faceScannerModelData, faceScannerModelData.length) != 0) {
            LOGGER.error("Unable to initialize FaceScanner recognitionLocal.");
            throw new IOException("Initialize error");
        }

        recognitionLocalInitialized = true;

        InputStream dbIS = faceScannerDBResource.getInputStream();
        byte[] faceScannerDBData = StreamUtils.copyToByteArray(dbIS);
        File dbFile = File.createTempFile(FACESCANNER_DB_NAME, ".db");
        FileOutputStream os = new FileOutputStream(dbFile);
        os.write(faceScannerDBData);

        recognitionLocal.CreateResetFaceDatabase(dbFile.getAbsolutePath().getBytes());
        recognitionLocal.LoadFaceDatabase(dbFile.getAbsolutePath().getBytes());
    }

    public void register(UUID id, byte[] photo) {
        synchronized (faceScannerLock) {
            _register(id, photo);
        }
    }

    private void _register(UUID id, byte[] photo) {
        opencv_core.IplImage originalImage = null;
        opencv_core.IplImage normalizedImage = null;
        int[] nbFaces = new int[] {0};
        facescanner.FaceRecPos facePositions = new facescanner.FaceRecPos();

        File faceFile = _createImageFile(photo);
        if (faceFile == null) return;

        try {
            originalImage = opencv_imgcodecs.cvLoadImage(faceFile.getAbsolutePath());
            normalizedImage = _resizeImage(originalImage);

            // Find the exact face location
            facescanner.BgrImage bgrImage = _createFaceScannerImage(normalizedImage);
            recognitionLocal.GetImageFacePos(bgrImage, facePositions, nbFaces);

            if (nbFaces[0] < 0) {
                LOGGER.warn("No face detected for the ID: {}", id);
                return;
            }

            facescanner.FaceRecPos bestFacePosition = new facescanner.FaceRecPos();
            int maxArea = -1;

            for (long i = 0; i < nbFaces[0]; i++) {
                facescanner.FaceRecPos facePosition = facePositions.position(i);
                int area = Math.abs((facePosition.top() - facePosition.bottom()) * (facePosition.right() - facePosition.left()));
                if (area > maxArea) {
                    maxArea = area;
                    bestFacePosition.bottom(facePosition.bottom());
                    bestFacePosition.top(facePosition.top());
                    bestFacePosition.right(facePosition.right());
                    bestFacePosition.left(facePosition.left());
                }
            }

            // Register the face
            LOGGER.warn("ADDING " + id);

            facescanner.SingleFaceReigisterInfo faceRegistrationInfo = new facescanner.SingleFaceReigisterInfo();
            faceRegistrationInfo.IDFaceDatabase().putString(FACESCANNER_DB_NAME);
            faceRegistrationInfo.IDName().putString(id.toString());
            if (recognitionLocal.RegisterFace(bgrImage, bestFacePosition, faceRegistrationInfo) != 0) {
                LOGGER.warn("Unable to register the face with the ID: {}", id);
            }
        } finally {
            releaseImageIfNotNull(originalImage);
            releaseImageIfNotNull(normalizedImage);
            deleteFileIfExists(faceFile);
        }
    }

    public List<RecognizedFace> findAllInImage(byte[] imageData) {
        synchronized(faceScannerLock) {
            return _findAllInImage(imageData);
        }
    }

    private List<RecognizedFace> _findAllInImage(byte[] imageData) {
        opencv_core.IplImage originalImage = null;
        opencv_core.IplImage image = null;
        int[] nbFaces = new int[] {0};
        facescanner.SingleFaceResult faceResults = new facescanner.SingleFaceResult();

        File photoFile = _createImageFile(imageData);
        if (photoFile == null) return null;

        ArrayList<RecognizedFace> faces = new ArrayList<>();

        // Open the image in a format compatible with OpenCV and resize it in order to have normalized size
        try {
            originalImage = opencv_imgcodecs.cvLoadImage(photoFile.getAbsolutePath());
            image = _resizeImage(originalImage);

            facescanner.BgrImage bgrImage = _createFaceScannerImage(image);

            recognitionLocal.ProcessImage(bgrImage, faceResults, nbFaces);

            if (nbFaces[0] <= 0) {
                LOGGER.debug("No face detected in the image.");
                return Collections.emptyList();
            }

            for (long i = 0; i < nbFaces[0]; i++) {
                facescanner.SingleFaceResult faceResult = faceResults.position(i);

                UUID uuid = null;

                if (faceResults.decision() == 0) {
                    uuid = UUID.fromString(faceResult.IDName().getString());
                }

                RecognizedFace face = new RecognizedFace(
                        faceResult.sFacePos().left(),
                        faceResult.sFacePos().top(),
                        faceResult.sFacePos().right() - faceResult.sFacePos().left(),
                        faceResult.sFacePos().bottom() - faceResult.sFacePos().top(),
                        uuid
                );

                faces.add(face);
            }
        } finally {
            releaseImageIfNotNull(originalImage);
            releaseImageIfNotNull(image);
            deleteFileIfExists(photoFile);
        }

        return faces;
    }

    private File _createImageFile(byte[] photo) {
        File faceFile = null;

        try {
            faceFile = File.createTempFile("face", ".jpg");
            FileCopyUtils.copy(photo, faceFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return faceFile;
    }

    private opencv_core.IplImage _resizeImage(opencv_core.IplImage originalImage) {
        int normalizedWidth = originalImage.width() / 4 * 4;
        int normalizedHeight = originalImage.height() / 4 * 4;
        // change vertically long to horizontally long
        int height = Math.min(normalizedWidth, normalizedHeight);

        opencv_core.IplImage normalizedImage = opencv_core.cvCreateImage(
                opencv_core.cvSize(normalizedWidth, height),
                originalImage.depth(),
                originalImage.nChannels()
        );
        opencv_imgproc.cvResize(originalImage, normalizedImage);

        return normalizedImage;
    }

    private facescanner.BgrImage _createFaceScannerImage(opencv_core.IplImage image) {
        facescanner.BgrImage bgrImage = new facescanner.BgrImage();
        bgrImage.channel(image.nChannels());
        bgrImage.width(image.width());
        bgrImage.height(image.height());
        bgrImage.pData(image.imageData());

        return bgrImage;
    }

    private void releaseImageIfNotNull(opencv_core.IplImage image) {
        if (image != null) {
            opencv_core.cvReleaseImage(image);
        }
    }

    private void deleteFileIfExists(File file) {
        if (file != null && file.exists()) {
            file.delete();
        }
    }

    public void unregister(UUID id) {
        synchronized(faceScannerLock) {
            _unregister(id);
        }
    }

    private void _unregister(UUID id) {
        LOGGER.warn("DELETING " + id);

        facescanner.SingleFaceReigisterInfo faceRegistrationInfo = new facescanner.SingleFaceReigisterInfo(1);
        faceRegistrationInfo.IDFaceDatabase().putString(FACESCANNER_DB_NAME);
        faceRegistrationInfo.IDName().putString(id.toString());

        int result = recognitionLocal.DeleteRegisteredID(faceRegistrationInfo);

        if (result == 0) {
            LOGGER.info("Delete success {}", id);
        } else {
            // TODO: always 1
            LOGGER.warn("Unable to unregister the face with the ID: {}", id);
        }
    }

    @PreDestroy
    public void uninitializeFaceScanner() {
        synchronized (faceScannerLock) {
            _uninitializeFaceScanner();
        }
    }

    private void _uninitializeFaceScanner() {
        if (recognitionLocalInitialized) {
            recognitionLocal.Uninitialize();
            facescanner.CIDRecognitionLocal.ReleaseInstance(recognitionLocal);
        }
    }

}
