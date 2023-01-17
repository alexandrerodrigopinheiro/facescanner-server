package hello;

import hello.service.FaceRecognitionService;
import hello.service.RecognizedFace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Base64Utils;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * endpoint list
 * POST /face-recognition/people.form       form data name, photo (for debug)
 * POST /face-recognition/people            json name, photo
 * GET  /face-recognition/people
 * GET  /face-recognition/people/{id}
 * GET  /face-recognition/people/{id}/name
 * GET  /face-recognition/people/{id}/photo
 * DELETE /face-recognition/people/{id}
 * POST /face-recognition/find-faces.form   form data photo return json (for debug)
 * POST /face-recognition/find-faces        json base64 photo return json
 * POST /face-recognition/find-faces.jpeg   form data photo return image (for debug)
 */
@RestController
@RequestMapping("/face-recognition")
public class PersonController {
    private static final Logger LOGGER = LoggerFactory.getLogger(PersonController.class);
    private final FaceRecognitionService faceRecognitionService;
    private final PersonService personService;

    @Autowired
    public PersonController(FaceRecognitionService faceRecognitionService, PersonService personService) {
        this.faceRecognitionService = faceRecognitionService;
        this.personService = personService;
    }

    @PostMapping("/people.form")
    public Person createForm(
            @RequestPart("name") String name,
            @RequestPart("photo") byte[] photo) {

        Person person = personService.save(new Person(name, photo));
        faceRecognitionService.register(person.getId(), photo);

        return person;
    }

    @CrossOrigin(origins = {"http://localhost:4200", "https://faceapp.tk"})
    @PostMapping("/people")
    @ResponseBody
    public Person create(@RequestBody Person person) {
        String photoBase64 = person.getPhotoBase64();
        byte[] photo = Base64Utils.decodeFromString(photoBase64);
        person.setPhoto(photo);
        person.setPhotoBase64("");  // no need to save base64 data

        Person savedPerson = personService.save(person);
        faceRecognitionService.register(savedPerson.getId(), photo);

        return savedPerson;
    }

    @CrossOrigin(origins = {"http://localhost:4200", "https://faceapp.tk"})
    @GetMapping("/people")
    public Iterable<Person> findAll() {
        return personService.findAll();
    }

    @GetMapping("/people/{id}")
    public Optional<Person> find(@PathVariable UUID id) {
        return personService.findById(id);
    }

    @GetMapping("/people/{id}/name")
    public String findName(@PathVariable UUID id) {
        Optional<Person> person = personService.findById(id);
        return person.map(Person::getName).orElse("");
    }

    @ResponseBody
    @GetMapping(value = "/people/{id}/photo", produces = MediaType.IMAGE_JPEG_VALUE)
    public byte[] findPhoto(@PathVariable UUID id) {
        Optional<Person> person = personService.findById(id);

        return person.map(Person::getPhoto).orElse(null);
    }

    @CrossOrigin(origins = {"http://localhost:4200", "https://faceapp.tk"})
    @DeleteMapping("/people/{id}")
    public void delete(@PathVariable UUID id) {
        personService.findById(id).ifPresent(person -> {
            faceRecognitionService.unregister(person.getId());
            personService.delete(person);
        });
    }

    @PostMapping("/find-faces.form")
    List<RecognizedFace> findFacesForm(@RequestPart("photo") byte[] imageData) {
        return faceRecognitionService.findAllInImage(imageData);
    }

    @CrossOrigin(origins = {"http://localhost:4200", "https://faceapp.tk"})
    @PostMapping("/find-faces")
    FacePhoto findFaces(@RequestBody String photoBase64) {
        byte[] photo = Base64Utils.decodeFromString(photoBase64);

        List<RecognizedFace> faces = faceRecognitionService.findAllInImage(photo);
        List<Person> persons = new ArrayList<>();

        final byte[][] clone = {photo.clone()};

        faces.forEach(face -> {
            if (face.getId() != null) {
                Optional<Person> person = personService.findById(face.getId());
                person.ifPresent(persons::add);
            } else {
                LOGGER.info("Id is NULL");
            }

            clone[0] = _writeRect(clone[0], face);
        });

        return new FacePhoto(faces, persons, clone[0]);
    }

    @ResponseBody
    @PostMapping(value = "/find-faces.jpeg", produces = MediaType.IMAGE_JPEG_VALUE)
    byte[] findFacesJpeg(@RequestPart("photo") byte[] photo) {
        List<RecognizedFace> faces = faceRecognitionService.findAllInImage(photo);

        final byte[][] clone = {photo.clone()};

        faces.forEach(face -> {
            clone[0] = _writeRect(clone[0], face);
        });

        return clone[0];
    }

    private byte[] _writeRect(byte[] photo, RecognizedFace face) {
        ImageType.Format imageFormat = ImageType.getFormat(photo);

        InputStream in = new ByteArrayInputStream(photo);
        BufferedImage image;

        try {
            image = ImageIO.read(in);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        Graphics2D graphics2D = image.createGraphics();
        graphics2D.drawRect(face.getX(), face.getY(), face.getWidth(), face.getHeight());
        graphics2D.setFont(new Font("TimesRoman", Font.PLAIN, 16));

        if (face.getId() != null) {
            Optional<Person> person = personService.findById(face.getId());
            person.ifPresent(person1 -> {
                graphics2D.drawString(person1.getName(), face.getX(), face.getY());
            });
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        try {
            ImageIO.write(image, imageFormat.name, bos);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return bos.toByteArray();
    }

}
