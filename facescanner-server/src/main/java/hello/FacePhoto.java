package hello;

import hello.service.RecognizedFace;
import org.springframework.util.Base64Utils;

import java.util.List;

public class FacePhoto {
    private List<RecognizedFace> faces;
    private List<Person> persons;
    private byte[] photo;

    public FacePhoto(List<RecognizedFace> faces, List<Person> persons, byte[] photo) {
        this.faces = faces;
        this.persons = persons;
        this.photo = photo;
    }

    public List<RecognizedFace> getFaces() {
        return faces;
    }

    public void setFaces(List<RecognizedFace> faces) {
        this.faces = faces;
    }

    public List<Person> getPersons() {
        return persons;
    }

    public void setPersons(List<Person> persons) {
        this.persons = persons;
    }

    public byte[] getPhoto() {
        return photo;
    }

    public void setPhoto(byte[] photo) {
        this.photo = photo;
    }

}
