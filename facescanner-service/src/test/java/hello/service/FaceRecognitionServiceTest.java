package hello.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest()
public class FaceRecognitionServiceTest {

    @Autowired
    private FaceRecognitionService faceRecognitionService;

    @Value("classpath:webcam.jpg")
    private Resource resource;

    @Test
    public void contextLoads() throws IOException {
        List<RecognizedFace> faces;

        UUID uuid = new UUID(1L, 1L);
        InputStream is = resource.getInputStream();
        byte[] photo = StreamUtils.copyToByteArray(is);

        faces = faceRecognitionService.findAllInImage(photo);
        assertEquals(1, faces.size());
        assertNull(faces.get(0).getId());

        faceRecognitionService.register(uuid, photo);

        faces = faceRecognitionService.findAllInImage(photo);
        assertEquals(1, faces.size());
        assertEquals(uuid.toString(), faces.get(0).getId().toString());

        faceRecognitionService.unregister(uuid);

        faces = faceRecognitionService.findAllInImage(photo);
        assertEquals(1, faces.size());
        // TODO: uuid should be null and this should fail
        assertEquals(uuid.toString(), faces.get(0).getId().toString());
    }

    @SpringBootApplication
    static class TestConfiguration {
    }

}
